package com.example.fluentui.ui.components

import android.Manifest
import android.app.Application
import android.app.WallpaperManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.palette.graphics.Palette
import com.example.fluentui.data.NotificationRepo
import com.example.fluentui.data.SettingsManager
import com.example.fluentui.ui.system.TileConfig
import com.example.fluentui.ui.system.TileIcon
import com.example.fluentui.ui.system.TileSize
import com.example.fluentui.ui.system.TileType
import com.example.fluentui.ui.utils.IconCache
import com.example.fluentui.wallpaper.MicaParams
import com.example.fluentui.wallpaper.NoiseGenerator
import com.example.fluentui.wallpaper.WallpaperRepository
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Collections
import java.util.UUID

sealed class NavigationEvent {
    object ScrollToHome : NavigationEvent()
}

class AppViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "AppViewModel"
        private const val GRID_COLUMNS = 6
        private const val GRID_MAX_SEARCH_ROWS = 20
        private const val PREFS_NAME = "fluent_dashboard_final"
        private const val KEY_SAVED_TILES = "saved_tiles_data"
        private const val DEBOUNCE_SAVE_MS = 500L
    }

    private val packageManager = application.packageManager
    private val context = application.applicationContext
    private val settingsManager = SettingsManager(application)
    private val dashboardPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()
    private var micaCalculationJob: Job? = null

    val notificationCounts = NotificationRepo.counts
    val otherApps = mutableStateOf<List<AppModel>>(emptyList())
    val dashboardTiles = mutableStateListOf<TileConfig>()

    private val _navigationEvent = Channel<NavigationEvent>(Channel.BUFFERED)
    val navigationEvent = _navigationEvent.receiveAsFlow()

    private val _micaParams = MutableStateFlow(createDefaultMicaParams(context))
    val micaParams = _micaParams.asStateFlow()

    val transparencyEnabled = settingsManager.transparencyEnabled.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = true
    )

    // [ NOUVEAU ] : Couleur d'accentuation synchronisée avec tes paramètres
    val accentColor = settingsManager.accentColor.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = Color(0xFF0078D4) // Windows Blue par défaut
    )

    private val saveDebouncer = MutableSharedFlow<Unit>(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    private val packageReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
            Log.d(TAG, "Package state changed, reloading app drawer.")
            viewModelScope.launch { loadInstalledApps() }
        }
    }

    private val themeReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
            Log.d(TAG, "System theme or wallpaper changed, recalculating Mica parameters.")
            refreshWallpaperVisuals()
        }
    }

    init {
        registerSystemReceivers()
        setupAutoSave()
        viewModelScope.launch {
            loadInstalledApps()
            loadTilesFromStorage()
            refreshWallpaperVisuals()
        }
    }

    override fun onCleared() {
        super.onCleared()
        try {
            context.unregisterReceiver(packageReceiver)
            context.unregisterReceiver(themeReceiver)
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "Receivers already unregistered.", e)
        }
    }

    private fun registerSystemReceivers() {
        try {
            val packageFilter = IntentFilter().apply {
                addAction(Intent.ACTION_PACKAGE_ADDED)
                addAction(Intent.ACTION_PACKAGE_REMOVED)
                addAction(Intent.ACTION_PACKAGE_CHANGED)
                addDataScheme("package")
            }
            context.registerReceiver(packageReceiver, packageFilter)

            val themeFilter = IntentFilter().apply {
                addAction(Intent.ACTION_CONFIGURATION_CHANGED)
                addAction(Intent.ACTION_WALLPAPER_CHANGED)
            }
            context.registerReceiver(themeReceiver, themeFilter)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register system receivers", e)
        }
    }

    fun onHomePressed() {
        viewModelScope.launch { _navigationEvent.send(NavigationEvent.ScrollToHome) }
    }

    fun moveApp(fromIndex: Int, toIndex: Int) {
        val list = otherApps.value.toMutableList()
        if (fromIndex in list.indices && toIndex in list.indices) {
            if (fromIndex < toIndex) {
                for (i in fromIndex until toIndex) Collections.swap(list, i, i + 1)
            } else {
                for (i in fromIndex downTo toIndex + 1) Collections.swap(list, i, i - 1)
            }
            otherApps.value = list
        }
    }

    fun pinApp(app: AppModel) {
        // Interception pour générer les widgets customisés
        if (app.packageName.contains("clock", ignoreCase = true) || app.packageName.contains("alarm", ignoreCase = true)) {
            addInternalWidget(TileType.CLOCK)
            addInternalWidget(TileType.STOPWATCH)
            addInternalWidget(TileType.TIMER)
            return
        }

        viewModelScope.launch(Dispatchers.Default) {
            val type = TileType.APPLICATION
            val defaultSize = TileSize.SMALL
            val (freeX, freeY) = findFirstFreePosition(defaultSize, dashboardTiles)

            var tile = TileConfig(
                id = UUID.randomUUID().toString(),
                title = app.label,
                icon = null,
                size = defaultSize,
                type = type,
                packageName = app.packageName,
                gridX = freeX,
                gridY = freeY
            )

            if (type == TileType.APPLICATION) {
                try {
                    val appIcon = packageManager.getApplicationIcon(app.packageName)
                    tile = tile.copy(icon = TileIcon.AndroidIcon(appIcon))
                } catch (e: PackageManager.NameNotFoundException) {
                    Log.w(TAG, "Failed to load icon for pinned app: ${app.packageName}")
                }
            }

            withContext(Dispatchers.Main) {
                dashboardTiles.add(tile)
                saveDebouncer.tryEmit(Unit)
            }
        }
    }

    /**
     * Creates and pins an internal system widget (Clock, Timer, Stopwatch) to the dashboard.
     */
    fun addInternalWidget(widgetType: TileType) {
        val newWidget = TileConfig(
            id = "widget_${widgetType.name}_${System.currentTimeMillis()}",
            title = when(widgetType) {
                TileType.CLOCK -> "Horloge"
                TileType.TIMER -> "Minuteur"
                TileType.STOPWATCH -> "Chrono"
                else -> "Widget"
            },
            packageName = "fluent.internal.${widgetType.name.lowercase()}",
            size = when(widgetType) {
                TileType.CLOCK -> TileSize.MEDIUM
                TileType.TIMER -> TileSize.LARGE
                TileType.STOPWATCH -> TileSize.MEDIUM
                else -> TileSize.SMALL
            },
            type = widgetType,
            gridX = -1, // La grille va trouver une place automatiquement grâce au Packer
            gridY = -1
        )

        val currentTiles = dashboardTiles.toList().toMutableList()
        currentTiles.add(newWidget)
        updateTilesOrder(currentTiles)
    }

    fun updateTilesOrder(newTiles: List<TileConfig>) {
        if (dashboardTiles.size == newTiles.size) {
            for (i in newTiles.indices) {
                if (dashboardTiles[i] != newTiles[i]) dashboardTiles[i] = newTiles[i]
            }
        } else {
            dashboardTiles.clear()
            dashboardTiles.addAll(newTiles)
        }
        saveDebouncer.tryEmit(Unit)
    }

    fun unpinTile(id: String) {
        dashboardTiles.removeAll { it.id == id }
        saveDebouncer.tryEmit(Unit)
    }

    fun resizeTile(tileId: String, newSize: TileSize) {
        val index = dashboardTiles.indexOfFirst { it.id == tileId }
        if (index != -1) {
            dashboardTiles[index] = dashboardTiles[index].copy(size = newSize)
            saveDebouncer.tryEmit(Unit)
        }
    }

    private fun findFirstFreePosition(
        newItemSize: TileSize,
        currentTiles: List<TileConfig>
    ): Pair<Int, Int> {
        val itemCols = getColSpan(newItemSize)
        val itemRows = getRowSpan(newItemSize)
        for (row in 0 until GRID_MAX_SEARCH_ROWS) {
            for (col in 0..(GRID_COLUMNS - itemCols)) {
                if (!isSpaceOccupied(col, row, itemCols, itemRows, currentTiles)) {
                    return Pair(col, row)
                }
            }
        }
        return Pair(0, GRID_MAX_SEARCH_ROWS)
    }

    private fun isSpaceOccupied(
        x: Int, y: Int, w: Int, h: Int, existingTiles: List<TileConfig>
    ): Boolean {
        return existingTiles.any { tile ->
            val tileW = getColSpan(tile.size)
            val tileH = getRowSpan(tile.size)
            x < tile.gridX + tileW && x + w > tile.gridX &&
                    y < tile.gridY + tileH && y + h > tile.gridY
        }
    }

    private fun getColSpan(size: TileSize): Int = when (size) {
        TileSize.TINY -> 1; TileSize.SMALL -> 2
        TileSize.MEDIUM -> 4; TileSize.LARGE -> 4; else -> 2
    }

    private fun getRowSpan(size: TileSize): Int = when (size) {
        TileSize.TINY -> 1; TileSize.SMALL -> 2
        TileSize.MEDIUM -> 2; TileSize.LARGE -> 4; else -> 2
    }

    fun refreshWallpaperVisuals() {
        micaCalculationJob?.cancel()
        micaCalculationJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                val hasPermission = checkWallpaperPermission(context)
                if (!hasPermission) {
                    Log.w(TAG, "Wallpaper permission denied. Using fallback.")
                    applyFallbackMicaParams()
                    return@launch
                }

                val drawable = WallpaperManager.getInstance(context).drawable
                if (drawable != null) {
                    updateMicaFromBitmap(drawableToBitmap(drawable))
                } else {
                    applyFallbackMicaParams()
                }
            } catch (e: SecurityException) {
                Log.e(TAG, "SecurityException reading wallpaper.", e)
                applyFallbackMicaParams()
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error in refreshWallpaperVisuals.", e)
                applyFallbackMicaParams()
            }
        }
    }

    private fun checkWallpaperPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.READ_MEDIA_IMAGES
            ) == PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(
                        context, Manifest.permission.READ_EXTERNAL_STORAGE
                    ) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    private suspend fun updateMicaFromBitmap(originalBitmap: Bitmap) =
        withContext(Dispatchers.Default) {
            try {
                val blurResult = com.example.fluentui.ui.utils.BitmapHelper
                    .createBlur(originalBitmap, radius = 25)
                val finalBitmap = blurResult.getOrNull()

                val paletteSource = finalBitmap ?: originalBitmap
                val palette = Palette.from(paletteSource).maximumColorCount(32).generate()

                val isSystemDark = (context.resources.configuration.uiMode
                        and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

                val defaultColorInt = if (isSystemDark) 0xFF0078D4.toInt() else 0xFFFFFFFF.toInt()
                val seedColorInt = if (isSystemDark) {
                    palette.getVibrantColor(palette.getDominantColor(defaultColorInt))
                } else {
                    palette.getLightVibrantColor(palette.getDominantColor(defaultColorInt))
                }
                val seedColor = Color(seedColorInt)

                val tintStrength = if (isSystemDark) 0.12f else 0.30f
                val tintBrush = androidx.compose.ui.graphics.SolidColor(
                    seedColor.copy(alpha = tintStrength)
                )

                val surfaceColor = if (isSystemDark) Color(0xFF181818) else Color(0xFFF9F9F9)
                val surfaceAlpha = if (isSystemDark) 0.65f else 0.50f

                val noiseBrush = runCatching { NoiseGenerator.generate(isSystemDark) }.getOrNull()
                val noiseAlpha = if (isSystemDark) 0.05f else 0.12f

                val params = MicaParams(
                    surfaceColor = surfaceColor,
                    surfaceAlpha = surfaceAlpha,
                    tintBrush = tintBrush,
                    tintAlpha = 1f,
                    primaryColor = if (isSystemDark) Color.White else Color.Black,
                    noiseBrush = noiseBrush,
                    dominantColor = seedColor,
                    noiseAlpha = noiseAlpha,
                    contentColor = if (isSystemDark) Color.White else Color.Black,
                    blurredWallpaperBitmap = finalBitmap,
                    isDark = isSystemDark
                )

                withContext(Dispatchers.Main) {
                    _micaParams.value = params
                    WallpaperRepository.micaParams.value = params
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error calculating Mica from bitmap", e)
                applyFallbackMicaParams()
            }
        }

    private suspend fun applyFallbackMicaParams() = withContext(Dispatchers.Main) {
        val fallback = createDefaultMicaParams(context)
        _micaParams.value = fallback
        WallpaperRepository.micaParams.value = fallback
    }

    fun toggleTransparency(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            settingsManager.setTransparencyEnabled(enabled)
        }
    }

    fun setCustomWallpaper(uri: Uri) {
        micaCalculationJob?.cancel()
        micaCalculationJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                val savedFile = copyUriToInternalStorage(context, uri)
                if (savedFile == null) {
                    Log.e(TAG, "Impossible de copier l'image en stockage interne.")
                    return@launch
                }

                val bitmap = decodeBitmapFromFile(savedFile)
                if (bitmap == null) {
                    Log.e(TAG, "Décodage bitmap échoué.")
                    return@launch
                }

                updateMicaFromBitmap(bitmap)
                settingsManager.setWallpaper(type = "FILE", value = savedFile.absolutePath)

                try {
                    WallpaperManager.getInstance(context).setBitmap(bitmap)
                } catch (e: SecurityException) {
                    Log.w(TAG, "Permission refusée pour setWallpaper système.", e)
                }

            } catch (e: Exception) {
                Log.e(TAG, "Erreur setCustomWallpaper", e)
            }
        }
    }

    private suspend fun decodeBitmapFromFile(file: File): Bitmap? =
        withContext(Dispatchers.IO) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    val source = ImageDecoder.createSource(file)
                    ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                        decoder.isMutableRequired = true
                        decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                    }
                } else {
                    val rotation = try {
                        val exif = ExifInterface(file.absolutePath)
                        when (exif.getAttributeInt(
                            ExifInterface.TAG_ORIENTATION,
                            ExifInterface.ORIENTATION_NORMAL
                        )) {
                            ExifInterface.ORIENTATION_ROTATE_90  -> 90f
                            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                            else -> 0f
                        }
                    } catch (e: Exception) { 0f }

                    val original = BitmapFactory.decodeFile(file.absolutePath)

                    if (rotation != 0f && original != null) {
                        val matrix = Matrix().apply { postRotate(rotation) }
                        val rotated = Bitmap.createBitmap(
                            original, 0, 0,
                            original.width, original.height,
                            matrix, true
                        )
                        if (rotated != original) original.recycle()
                        rotated
                    } else {
                        original
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Erreur décodage fichier image", e)
                null
            }
        }

    private suspend fun copyUriToInternalStorage(context: Context, uri: Uri): File? =
        withContext(Dispatchers.IO) {
            try {
                val wallpaperDir = File(context.filesDir, "wallpapers").apply { mkdirs() }
                wallpaperDir.listFiles()?.forEach { it.delete() }
                val destFile = File(wallpaperDir, "wallpaper_${System.currentTimeMillis()}.jpg")
                context.contentResolver.openInputStream(uri)?.use { input ->
                    destFile.outputStream().use { output ->
                        input.copyTo(output, bufferSize = 8 * 1024)
                    }
                }
                if (destFile.exists() && destFile.length() > 0) destFile else null
            } catch (e: Exception) {
                Log.e(TAG, "Erreur copyUriToInternalStorage", e)
                null
            }
        }

    fun updateWallpaperPreference(type: String, value: String) {
        viewModelScope.launch(Dispatchers.IO) {
            settingsManager.setWallpaper(type, value)
            if (type == "SOLID") {
                val colorInt = value.toIntOrNull() ?: android.graphics.Color.BLACK
                val bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
                bitmap.eraseColor(colorInt)
                updateMicaFromBitmap(bitmap)
            }
        }
    }

    private fun setupAutoSave() {
        viewModelScope.launch {
            saveDebouncer.debounce(DEBOUNCE_SAVE_MS).collectLatest {
                saveTilesToStorage()
            }
        }
    }

    private suspend fun saveTilesToStorage() = withContext(Dispatchers.IO) {
        try {
            val serializableList = dashboardTiles.map { it.copy(icon = null) }
            dashboardPrefs.edit()
                .putString(KEY_SAVED_TILES, gson.toJson(serializableList))
                .apply()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save dashboard layout", e)
        }
    }

    private suspend fun loadTilesFromStorage() = withContext(Dispatchers.IO) {
        val json = dashboardPrefs.getString(KEY_SAVED_TILES, null)
        val savedList: List<TileConfig> = try {
            if (json != null) {
                gson.fromJson(json, object : TypeToken<List<TileConfig>>() {}.type)
            } else emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Corrupted JSON, resetting layout.", e)
            emptyList()
        }

        val rehydrated = savedList.mapNotNull { tile ->
            if (tile.id == null) return@mapNotNull null
            val tileWithSize = if (tile.size == null) tile.copy(size = TileSize.SMALL) else tile

            // [ FIX CRITIQUE ] : Ne pas supprimer les widgets internes
            // Les widgets customisés n'ont pas d'icône système, on les retourne directement.
            if (tileWithSize.type != TileType.APPLICATION) {
                return@mapNotNull tileWithSize
            }

            if (tileWithSize.packageName.isNotEmpty()) {
                try {
                    val drawable = packageManager.getApplicationIcon(tileWithSize.packageName)
                    tileWithSize.copy(
                        icon = TileIcon.AndroidIcon(
                            BitmapDrawable(context.resources, drawableToBitmap(drawable))
                        )
                    )
                } catch (e: PackageManager.NameNotFoundException) {
                    Log.w(TAG, "App uninstalled, dropping tile: ${tileWithSize.packageName}")
                    null
                }
            } else {
                tileWithSize
            }
        }

        withContext(Dispatchers.Main) {
            dashboardTiles.clear()
            dashboardTiles.addAll(rehydrated)
        }
    }

    private suspend fun loadInstalledApps() = withContext(Dispatchers.IO) {
        try {
            val intent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }

            val activities = packageManager.queryIntentActivities(intent, 0)
            val seenPackages = mutableSetOf<String>()

            val list = activities.mapNotNull { resolveInfo ->
                try {
                    val packageName = resolveInfo.activityInfo.packageName
                    if (!seenPackages.add(packageName)) return@mapNotNull null

                    if (IconCache.get(packageName) == null) {
                        val iconBitmap = drawableToBitmap(resolveInfo.loadIcon(packageManager))
                        IconCache.put(packageName, iconBitmap)
                    }
                    AppModel(resolveInfo.loadLabel(packageManager).toString(), packageName)
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to load info for package", e)
                    null
                }
            }.sortedBy { it.label.lowercase() }

            withContext(Dispatchers.Main) { otherApps.value = list }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load installed apps", e)
        }
    }

    private fun drawableToBitmap(drawable: Drawable): Bitmap {
        if (drawable is BitmapDrawable && drawable.bitmap != null) return drawable.bitmap
        val width = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else 1
        val height = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else 1
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }

    fun hasNotificationPermission(): Boolean {
        val listeners = android.provider.Settings.Secure.getString(
            context.contentResolver, "enabled_notification_listeners"
        )
        return listeners?.contains(context.packageName) == true
    }
}

private fun createDefaultMicaParams(context: Context): MicaParams {
    val isSystemDark = (context.resources.configuration.uiMode
            and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
    return MicaParams(
        surfaceColor = if (isSystemDark) Color(0xFF181818) else Color(0xFFF3F3F3),
        surfaceAlpha = 0.85f,
        tintBrush = androidx.compose.ui.graphics.SolidColor(Color.Transparent),
        tintAlpha = 0f,
        primaryColor = if (isSystemDark) Color.White else Color.Black,
        noiseBrush = null,
        dominantColor = Color(0xFF0078D4),
        noiseAlpha = 0f,
        contentColor = if (isSystemDark) Color.White else Color.Black,
        blurredWallpaperBitmap = null,
        isDark = isSystemDark
    )
}