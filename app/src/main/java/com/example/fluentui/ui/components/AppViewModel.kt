/**
 * @file AppViewModel.kt
 * @description Core ViewModel managing home screen state, app drawer, and system gestures.
 * Optimized for eager DataStore observation and strictly decoupled from OEM wallpaper APIs.
 * Now manages the global Action Center overlay triggered by SwipeDownService.
 */

package com.example.fluentui.ui.components

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import kotlinx.coroutines.flow.map
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.palette.graphics.Palette
import com.example.fluentui.data.NotificationRepo
import com.example.fluentui.data.SettingsManager
import com.example.fluentui.ui.database.TileDatabase
import com.example.fluentui.ui.database.TileRepository
import com.example.fluentui.ui.system.TileConfig
import com.example.fluentui.ui.system.TileIcon
import com.example.fluentui.ui.system.TileSize
import com.example.fluentui.ui.system.TileType
import com.example.fluentui.ui.utils.IconCache
import com.example.fluentui.wallpaper.MicaParams
import com.example.fluentui.wallpaper.MicaState
import com.example.fluentui.wallpaper.NoiseGenerator
import com.example.fluentui.wallpaper.WallpaperRepository
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
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
        private const val TAG                  = "AppViewModel"
        private const val GRID_COLUMNS         = 6
        private const val GRID_MAX_SEARCH_ROWS = 20
        private const val PREFS_NAME           = "fluent_dashboard_final"
        private const val KEY_SAVED_TILES      = "saved_tiles_data"
        private const val KEY_SAVED_FOLDERS    = "saved_drawer_folders" // Clé pour tes dossiers
    }

    private val packageManager   = application.packageManager
    private val context          = application.applicationContext
    private val settingsManager  = SettingsManager(application)
    private val dashboardPrefs   = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson             = Gson()

    // =========================================================================
    // SYSTEM GESTURES (Eagerly Loaded)
    // =========================================================================

    val swipeDownEnabled: StateFlow<Boolean> = settingsManager.swipeDownEnabled
        .stateIn(scope = viewModelScope, started = SharingStarted.Eagerly, initialValue = true)
    val showBadges: StateFlow<Boolean> = settingsManager.showBadges
        .stateIn(scope = viewModelScope, started = SharingStarted.Eagerly, initialValue = true)

    // =========================================================================
    // GLOBAL ACTION CENTER STATE (Overlay triggered by SwipeDownService)
    // =========================================================================

    private val _isActionCenterOpen = MutableStateFlow(false)
    val isActionCenterOpen: StateFlow<Boolean> = _isActionCenterOpen.asStateFlow()

    fun toggleActionCenter(isOpen: Boolean) {
        _isActionCenterOpen.value = isOpen
    }

    // =========================================================================
    // REPOSITORIES & STATE
    // =========================================================================

    private val tileRepository = TileRepository(TileDatabase.getDatabase(application).tileDao())

    val notificationCounts = NotificationRepo.counts
    val otherApps          = mutableStateOf<List<AppModel>>(emptyList())
    val dashboardTiles     = mutableStateListOf<TileConfig>()

    // État pour les dossiers (remplace la BDD Room complexe)
    private val _drawerFolders = MutableStateFlow<List<AppFolder>>(emptyList())
    val drawerFolders = _drawerFolders.asStateFlow()

    private val _navigationEvent = Channel<NavigationEvent>(Channel.BUFFERED)
    val navigationEvent = _navigationEvent.receiveAsFlow()

    private val _micaParams = MutableStateFlow(createDefaultMicaParams(context))
    val micaParams = _micaParams.asStateFlow()

    val wallpaperRepo = WallpaperRepository(application)

    val transparencyEnabled = settingsManager.transparencyEnabled.stateIn(
        scope = viewModelScope, started = SharingStarted.Eagerly, initialValue = true
    )

    val accentColor: StateFlow<Color> = settingsManager.accentColor
        .map { Color(it) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = Color(0xFF0078D4)
        )
    private val packageReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
            Log.d(TAG, "Package state changed, reloading app drawer.")
            viewModelScope.launch { loadInstalledApps() }
        }
    }

    private val themeReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
            Log.d(TAG, "System theme changed, recalculating Mica parameters.")
            val currentBitmaps = wallpaperRepo.bitmaps.value
            viewModelScope.launch {
                if (currentBitmaps != null) {
                    updateMicaFromBitmaps(currentBitmaps.sharp, currentBitmaps.blurred)
                } else {
                    applyFallbackMicaParams()
                }
            }
        }
    }

    // =========================================================================
    // INIT : THE PERFECT REACTIVE LOOP
    // =========================================================================

    init {
        registerSystemReceivers()
        viewModelScope.launch {
            loadInstalledApps()
            loadTilesFromStorage()
            loadDrawerFolders() // Charge les dossiers au démarrage

            // Écoute réactive du Repository : Dès qu'un fond d'écran est chargé, on calcule le Mica
            wallpaperRepo.bitmaps.collect { bitmaps ->
                if (bitmaps != null) {
                    updateMicaFromBitmaps(bitmaps.sharp, bitmaps.blurred)
                } else {
                    applyFallbackMicaParams()
                }
            }
        }
    }

    // =========================================================================
    // FOLDERS PERSISTENCE (Lightweight JSON approach)
    // =========================================================================

    fun syncDrawerFolders(newFolders: List<AppFolder>) {
        _drawerFolders.value = newFolders
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val json = gson.toJson(newFolders)
                dashboardPrefs.edit().putString(KEY_SAVED_FOLDERS, json).apply()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save drawer folders", e)
            }
        }
    }

    private fun loadDrawerFolders() {
        try {
            val json = dashboardPrefs.getString(KEY_SAVED_FOLDERS, null)
            if (json != null) {
                val type = object : TypeToken<List<AppFolder>>() {}.type
                val saved: List<AppFolder>? = gson.fromJson(json, type)
                if (saved != null) {
                    _drawerFolders.value = saved
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load drawer folders", e)
        }
    }

    // =========================================================================
    // PUBLIC API
    // =========================================================================

    /**
     * Force le rafraîchissement des visuels (Mica) depuis l'extérieur.
     * Utilise le Repository pour éviter les crashs de permissions Android 14.
     */
    fun refreshWallpaperVisuals(isDarkOverride: Boolean? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            val currentBitmaps = wallpaperRepo.bitmaps.value

            if (currentBitmaps != null) {
                // Si on a déjà les images en mémoire, on recalcule juste le Mica
                updateMicaFromBitmaps(currentBitmaps.sharp, currentBitmaps.blurred, isDarkOverride)
            } else {
                // Sinon, on force le chargement depuis les paramètres actuels
                val type = settingsManager.wallpaperType.first()
                val value = settingsManager.wallpaperValue.first()
                wallpaperRepo.load(type, value)
            }
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
            }
            context.registerReceiver(themeReceiver, themeFilter)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register system receivers", e)
        }
    }

    fun onHomePressed() {
        // Closes the Action Center if the user presses the home button
        _isActionCenterOpen.value = false

        // Sends event to UI to scroll back to top/home
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

    // =========================================================================
    // TILES MUTATORS & PERSISTENCE
    // =========================================================================

    fun toggleDynamicState(tileId: String) {
        val index = dashboardTiles.indexOfFirst { it.id == tileId }
        if (index != -1) {
            dashboardTiles[index] = dashboardTiles[index].copy(isDynamic = !dashboardTiles[index].isDynamic)
            saveTilesNow()
        }
    }

    fun pinApp(app: AppModel) {
        viewModelScope.launch(Dispatchers.Default) {
            val pkg = app.packageName.lowercase()

            // 1. Déterminer le type (Netflix est une APPLICATION avec un visuel custom)
            val finalType = when {
                pkg.contains("music") || pkg.contains("spotify") || pkg.contains("deezer") -> TileType.MEDIA
                pkg.contains("weather") || pkg.contains("meteo") -> TileType.WEATHER
                pkg.contains("calendar") -> TileType.CALENDAR
                pkg.contains("clock") -> TileType.CLOCK
                // Netflix reste une APPLICATION mais on forcera le visuel dans le Dispatcher
                else -> TileType.APPLICATION
            }

            val (freeX, freeY) = findFirstFreePosition(TileSize.SMALL, dashboardTiles)

            // 2. Création de la tuile avec la condition isDynamic corrigée
            val tile = TileConfig(
                id = UUID.randomUUID().toString(),
                title = app.label,
                icon = null,
                size = TileSize.SMALL,
                type = finalType,
                packageName = app.packageName,
                gridX = freeX,
                gridY = freeY,
                // ICI : On active le mode dynamique pour les widgets ET pour ton Netflix custom
                isDynamic = (finalType != TileType.APPLICATION) || pkg == "com.netflix.mediaclient"
            )

            val iconTile = try {
                val appIcon = packageManager.getApplicationIcon(app.packageName)
                tile.copy(icon = TileIcon.AndroidIcon(appIcon))
            } catch (e: PackageManager.NameNotFoundException) { tile }

            withContext(Dispatchers.Main) {
                dashboardTiles.add(iconTile)
                saveTilesNow()
            }
        }
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
        saveTilesNow()
    }

    fun unpinTile(id: String) {
        dashboardTiles.removeAll { it.id == id }
        saveTilesNow()
    }

    fun resizeTile(tileId: String, newSize: TileSize) {
        val index = dashboardTiles.indexOfFirst { it.id == tileId }
        if (index != -1) {
            dashboardTiles[index] = dashboardTiles[index].copy(size = newSize)
            saveTilesNow()
        }
    }

    private fun findFirstFreePosition(newItemSize: TileSize, currentTiles: List<TileConfig>): Pair<Int, Int> {
        val itemCols = getColSpan(newItemSize)
        val itemRows = getRowSpan(newItemSize)
        for (row in 0 until GRID_MAX_SEARCH_ROWS) {
            for (col in 0..(GRID_COLUMNS - itemCols)) {
                if (!isSpaceOccupied(col, row, itemCols, itemRows, currentTiles)) return Pair(col, row)
            }
        }
        return Pair(0, GRID_MAX_SEARCH_ROWS)
    }

    private fun isSpaceOccupied(x: Int, y: Int, w: Int, h: Int, existingTiles: List<TileConfig>): Boolean =
        existingTiles.any { tile ->
            val tileW = getColSpan(tile.size)
            val tileH = getRowSpan(tile.size)
            x < tile.gridX + tileW && x + w > tile.gridX && y < tile.gridY + tileH && y + h > tile.gridY
        }

    private fun getColSpan(size: TileSize): Int = when (size) {
        TileSize.TINY -> 1; TileSize.SMALL -> 2; TileSize.MEDIUM -> 4; TileSize.LARGE -> 4; else -> 2
    }

    private fun getRowSpan(size: TileSize): Int = when (size) {
        TileSize.TINY -> 1; TileSize.SMALL -> 2; TileSize.MEDIUM -> 2; TileSize.LARGE -> 4; else -> 2
    }

    private fun saveTilesNow() {
        viewModelScope.launch(Dispatchers.IO) {
            try { tileRepository.upsertAll(dashboardTiles.toList()) }
            catch (e: Exception) { Log.e(TAG, "Failed to save tiles", e) }
        }
    }

    private suspend fun loadTilesFromStorage() = withContext(Dispatchers.IO) {
        if (tileRepository.count() == 0) migrateFromSharedPrefsToRoom()

        val entities = TileDatabase.getDatabase(context).tileDao().observeAll().first()
        val toDelete = mutableListOf<String>()

        val rehydrated = entities.mapNotNull { entity ->
            val config = tileRepository.entityToConfig(entity) ?: return@mapNotNull null
            when (config.type) {
                TileType.APPLICATION -> {
                    if (config.packageName.isNotEmpty()) {
                        try {
                            val drawable = packageManager.getApplicationIcon(config.packageName)
                            config.copy(icon = TileIcon.AndroidIcon(BitmapDrawable(context.resources, drawableToBitmap(drawable))))
                        } catch (e: PackageManager.NameNotFoundException) {
                            toDelete.add(config.id)
                            null
                        }
                    } else config
                }
                else -> config
            }
        }

        toDelete.forEach { tileRepository.delete(it) }

        withContext(Dispatchers.Main) {
            dashboardTiles.clear()
            dashboardTiles.addAll(rehydrated)
        }
    }

    private suspend fun migrateFromSharedPrefsToRoom() = withContext(Dispatchers.IO) {
        try {
            val json = dashboardPrefs.getString(KEY_SAVED_TILES, null) ?: return@withContext
            val old: List<TileConfig> = gson.fromJson(json, object : TypeToken<List<TileConfig>>() {}.type) ?: return@withContext
            if (old.isEmpty()) return@withContext
            tileRepository.upsertAll(old)
            dashboardPrefs.edit().remove(KEY_SAVED_TILES).commit()
        } catch (e: Exception) { Log.e(TAG, "Migration failed", e) }
    }

    // =========================================================================
    // MICA ENGINE & WALLPAPER SETTINGS
    // =========================================================================

    private suspend fun updateMicaFromBitmaps(sharp: Bitmap, blurred: Bitmap, isDarkOverride: Boolean? = null) =
        withContext(Dispatchers.Default) {
            try {
                // Calcule les couleurs dominantes depuis l'image nette
                val palette = Palette.from(sharp).maximumColorCount(32).generate()

                val isSystemDark = isDarkOverride ?: ((context.resources.configuration.uiMode
                        and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES)

                val defaultColorInt = if (isSystemDark) 0xFF0078D4.toInt() else 0xFFFFFFFF.toInt()
                val seedColorInt    = if (isSystemDark) {
                    palette.getVibrantColor(palette.getDominantColor(defaultColorInt))
                } else {
                    palette.getLightVibrantColor(palette.getDominantColor(defaultColorInt))
                }

                val seedColor    = Color(seedColorInt)
                val tintBrush    = androidx.compose.ui.graphics.SolidColor(seedColor.copy(alpha = if (isSystemDark) 0.12f else 0.30f))
                val surfaceColor = if (isSystemDark) Color(0xFF181818) else Color(0xFFF9F9F9)
                val noiseBrush   = runCatching { NoiseGenerator.generate(isSystemDark) }.getOrNull()

                val params = MicaParams(
                    surfaceColor           = surfaceColor,
                    surfaceAlpha           = if (isSystemDark) 0.65f else 0.50f,
                    tintBrush              = tintBrush,
                    tintAlpha              = 1f,
                    primaryColor           = if (isSystemDark) Color.White else Color.Black,
                    noiseBrush             = noiseBrush,
                    dominantColor          = seedColor,
                    noiseAlpha             = if (isSystemDark) 0.05f else 0.12f,
                    contentColor           = if (isSystemDark) Color.White else Color.Black,
                    blurredWallpaperBitmap = blurred, // Injection de l'image floutée par le Repo
                    isDark                 = isSystemDark
                )

                withContext(Dispatchers.Main) {
                    _micaParams.value          = params
                    MicaState.micaParams.value = params
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error calculating Mica from bitmaps", e)
                applyFallbackMicaParams(isDarkOverride)
            }
        }

    private suspend fun applyFallbackMicaParams(isDarkOverride: Boolean? = null) = withContext(Dispatchers.Main) {
        val fallback = createDefaultMicaParams(context, isDarkOverride)
        _micaParams.value          = fallback
        MicaState.micaParams.value = fallback
    }

    fun toggleTransparency(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) { settingsManager.setTransparencyEnabled(enabled) }
    }

    /**
     * Définit le fond d'écran de l'application uniquement.
     * Copie le fichier en interne et met à jour les paramètres. Le Repository fera le reste.
     */
    fun setCustomWallpaper(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val savedFile = copyUriToInternalStorage(context, uri) ?: run {
                    Log.e(TAG, "Impossible de copier l'image en stockage interne.")
                    return@launch
                }
                // Déclenche la boucle réactive : Settings -> UI -> Repo -> AppViewModel
                settingsManager.setWallpaper(type = "FILE", value = savedFile.absolutePath)
                Log.d(TAG, "Fond d'écran interne mis à jour : ${savedFile.absolutePath}")
            } catch (e: Exception) {
                Log.e(TAG, "Erreur setCustomWallpaper", e)
            }
        }
    }

    /**
     * Définit une couleur ou une ressource pour l'application.
     */
    fun updateWallpaperPreference(type: String, value: String) {
        viewModelScope.launch(Dispatchers.IO) {
            settingsManager.setWallpaper(type, value)
        }
    }

    private suspend fun copyUriToInternalStorage(context: Context, uri: Uri): File? =
        withContext(Dispatchers.IO) {
            try {
                val wallpaperDir = File(context.filesDir, "wallpapers").apply { mkdirs() }
                wallpaperDir.listFiles()?.forEach { it.delete() }
                val destFile = File(wallpaperDir, "wallpaper_${System.currentTimeMillis()}.jpg")
                context.contentResolver.openInputStream(uri)?.use { input ->
                    destFile.outputStream().use { output -> input.copyTo(output, 8 * 1024) }
                }
                if (destFile.exists() && destFile.length() > 0) destFile else null
            } catch (e: Exception) {
                Log.e(TAG, "Erreur copyUriToInternalStorage", e)
                null
            }
        }

    // =========================================================================
    // APP LIST & UTILS
    // =========================================================================

    private suspend fun loadInstalledApps() = withContext(Dispatchers.IO) {
        try {
            val intent     = Intent(Intent.ACTION_MAIN, null).apply { addCategory(Intent.CATEGORY_LAUNCHER) }
            val activities = packageManager.queryIntentActivities(intent, 0)
            val seenPkgs   = mutableSetOf<String>()

            val list = activities.mapNotNull { resolveInfo ->
                try {
                    val pkg = resolveInfo.activityInfo.packageName
                    if (!seenPkgs.add(pkg)) return@mapNotNull null
                    if (IconCache.get(pkg) == null)
                        IconCache.put(pkg, drawableToBitmap(resolveInfo.loadIcon(packageManager)))
                    AppModel(resolveInfo.loadLabel(packageManager).toString(), pkg)
                } catch (e: Exception) { null }
            }.sortedBy { it.label.lowercase() }

            withContext(Dispatchers.Main) { otherApps.value = list }
        } catch (e: Exception) { Log.e(TAG, "Failed to load installed apps", e) }
    }

    private fun drawableToBitmap(drawable: Drawable): Bitmap {
        if (drawable is BitmapDrawable && drawable.bitmap != null) return drawable.bitmap
        val width  = if (drawable.intrinsicWidth  > 0) drawable.intrinsicWidth  else 1
        val height = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else 1
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }

    fun hasNotificationPermission(): Boolean {
        val listeners = android.provider.Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
        return listeners?.contains(context.packageName) == true
    }


}


// =============================================================================
// HELPERS TOP-LEVEL
// =============================================================================

private fun createDefaultMicaParams(context: Context, isDarkOverride: Boolean? = null): MicaParams {
    val isSystemDark = isDarkOverride ?: ((context.resources.configuration.uiMode
            and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES)
    return MicaParams(
        surfaceColor           = if (isSystemDark) Color(0xFF181818) else Color(0xFFF3F3F3),
        surfaceAlpha           = 0.85f,
        tintBrush              = androidx.compose.ui.graphics.SolidColor(Color.Transparent),
        tintAlpha              = 0f,
        primaryColor           = if (isSystemDark) Color.White else Color.Black,
        noiseBrush             = null,
        dominantColor          = Color(0xFF0078D4),
        noiseAlpha             = 0f,
        contentColor           = if (isSystemDark) Color.White else Color.Black,
        blurredWallpaperBitmap = null,
        isDark                 = isSystemDark
    )
}