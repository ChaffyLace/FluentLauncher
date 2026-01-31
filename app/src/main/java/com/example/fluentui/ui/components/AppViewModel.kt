package com.example.fluentui.ui.components

import android.app.Application
import android.app.WallpaperManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.util.Log // Import pour les Logs
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.core.graphics.drawable.toBitmap
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
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import java.util.*

sealed class NavigationEvent {
    object ScrollToHome : NavigationEvent()
}

class AppViewModel(application: Application) : AndroidViewModel(application) {

    private val pm = application.packageManager
    private val context = application.applicationContext
    private val settingsManager = SettingsManager(application)
    private val dashboardPrefs = context.getSharedPreferences("fluent_dashboard_final", Context.MODE_PRIVATE)
    private val gson = Gson()

    private var micaCalculationJob: Job? = null

    val notificationCounts = NotificationRepo.counts
    val otherApps = mutableStateOf<List<AppModel>>(emptyList())
    val dashboardTiles = mutableStateListOf<TileConfig>()

    private val _navigationEvent = Channel<NavigationEvent>(Channel.BUFFERED)
    val navigationEvent = _navigationEvent.receiveAsFlow()

    private val defaultMica = MicaParams(
        surfaceColor = Color(0xFF202020),
        surfaceAlpha = 0.6f,
        tintBrush = Brush.verticalGradient(listOf(Color.Transparent, Color.Transparent)),
        tintAlpha = 0f,
        primaryColor = Color.White,
        noiseBrush = null,
        dominantColor = Color(0xFF0078D4),
        noiseAlpha = 0f,
        contentColor = Color.White,
        blurredWallpaperBitmap = null,
        isDark = true
    )

    private val _micaParams = MutableStateFlow(defaultMica)
    val micaParams = _micaParams.asStateFlow()

    val transparencyEnabled = settingsManager.transparencyEnabled.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = true
    )

    private val saveDebouncer = MutableSharedFlow<Unit>(replay = 0, extraBufferCapacity = 1, onBufferOverflow = kotlinx.coroutines.channels.BufferOverflow.DROP_OLDEST)

    private val packageReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) { viewModelScope.launch { loadInstalledApps() } }
    }

    private val themeReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) { refreshWallpaperVisuals() }
    }

    init {
        runCatching {
            val f = IntentFilter().apply {
                addAction(Intent.ACTION_PACKAGE_ADDED)
                addAction(Intent.ACTION_PACKAGE_REMOVED)
                addAction(Intent.ACTION_PACKAGE_CHANGED)
                addDataScheme("package")
            }
            context.registerReceiver(packageReceiver, f)

            val themeFilter = IntentFilter().apply {
                addAction(Intent.ACTION_CONFIGURATION_CHANGED)
                addAction(Intent.ACTION_WALLPAPER_CHANGED)
            }
            context.registerReceiver(themeReceiver, themeFilter)
        }

        viewModelScope.launch {
            loadInstalledApps()
            loadTilesFromStorage()
            refreshWallpaperVisuals()
        }
        setupAutoSave()
    }

    override fun onCleared() {
        super.onCleared()
        runCatching { context.unregisterReceiver(packageReceiver); context.unregisterReceiver(themeReceiver) }
    }

    fun onHomePressed() { viewModelScope.launch { _navigationEvent.send(NavigationEvent.ScrollToHome) } }

    private suspend fun updateMicaFromBitmap(originalBitmap: Bitmap) = withContext(Dispatchers.Default) {
        try {
            // 1. FLOU OPTIMISÉ (Reste inchangé, ça marche bien)
            val blurResult = com.example.fluentui.ui.utils.BitmapHelper.createBlur(originalBitmap, radius = 25)
            val finalBitmap = if (blurResult.isSuccess) blurResult.getOrNull() else null

            // 2. EXTRACTION COULEURS (Palette intelligente)
            val paletteSource = finalBitmap ?: originalBitmap
            val p = Palette.from(paletteSource).maximumColorCount(32).generate()

            val isSystemDark = (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
            val isDark = isSystemDark

            // CORRECTION COULEUR : On cherche la couleur la plus représentative
            // En mode sombre, on veut la couleur "Vibrant" (la plus peps)
            // En mode clair, on préfère "Muted" ou "Dominant" pour ne pas agresser les yeux
            val defaultColor = if(isDark) 0xFF0078D4.toInt() else 0xFFFFFFFF.toInt()

            val seedColorInt = if (isDark) {
                p.getVibrantColor(p.getDominantColor(defaultColor))
            } else {
                p.getLightVibrantColor(p.getDominantColor(defaultColor))
            }
            val seedColor = Color(seedColorInt)

            // 3. LE FIX EST ICI : PLUS DE DÉGRADÉ MOCHE !
            // On veut une "nappe" de couleur uniforme, comme une vitre teintée.

            // Opacité de la teinte (combien de couleur on injecte)
            val tintStrength = if (isDark) 0.12f else 0.30f

            // Au lieu d'un verticalGradient qui va vers 0, on met la MÊME couleur partout.
            // On utilise SolidColor pour être plat, ou un gradient très subtil pour la lumière.
            val tintBrush = androidx.compose.ui.graphics.SolidColor(
                seedColor.copy(alpha = tintStrength)
            )

            // 4. SURFACE (Le fond de base)
            // C'est la couche qui "bouche" les trous.
            // En Dark : Gris très foncé (presque noir) avec transparence moyenne.
            // En Light : Blanc laiteux avec transparence moyenne.
            val surfaceColor = if (isDark) Color(0xFF181818) else Color(0xFFF9F9F9)
            val surfaceAlpha = if (isDark) 0.65f else 0.50f

            val noiseBrush = try { NoiseGenerator.generate(isDark) } catch(e:Exception){ null }
            val noiseAlpha = if (isDark) 0.05f else 0.12f

            val params = MicaParams(
                surfaceColor = surfaceColor,
                surfaceAlpha = surfaceAlpha,
                tintBrush = tintBrush, // <-- Maintenant c'est une couleur unie propre
                tintAlpha = 1f,
                primaryColor = if (isDark) Color.White else Color.Black,
                noiseBrush = noiseBrush,
                dominantColor = seedColor,
                noiseAlpha = noiseAlpha,
                contentColor = if (isDark) Color.White else Color.Black,
                blurredWallpaperBitmap = finalBitmap,
                isDark = isDark
            )

            withContext(Dispatchers.Main) {
                _micaParams.value = params
                WallpaperRepository.micaParams.value = params
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    fun refreshWallpaperVisuals() {
        viewModelScope.launch(Dispatchers.IO) {
            val drawable = WallpaperManager.getInstance(context).drawable
            if (drawable != null) {
                val bitmap = drawable.toBitmap()
                updateMicaFromBitmap(bitmap)
            }
        }
    }

    // ... (Le reste des méthodes setCustomWallpaper, toggleTransparency, etc. reste inchangé) ...

    fun toggleTransparency(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) { settingsManager.setTransparencyEnabled(enabled) }
    }

    fun setCustomWallpaper(uri: Uri) {
        micaCalculationJob?.cancel()
        micaCalculationJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                val inputStreamUi = context.contentResolver.openInputStream(uri)
                val bitmapUi = BitmapFactory.decodeStream(inputStreamUi)
                inputStreamUi?.close()
                if (bitmapUi != null) updateMicaFromBitmap(bitmapUi)

                settingsManager.setWallpaper(type = "URI", value = uri.toString())
                val inputStreamSys = context.contentResolver.openInputStream(uri)
                if (inputStreamSys != null) {
                    WallpaperManager.getInstance(context).setStream(inputStreamSys)
                    inputStreamSys.close()
                }
            } catch (e: Exception) { e.printStackTrace() }
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

    fun moveTile(fromIndex: Int, toIndex: Int) {
        val list = dashboardTiles.toMutableList()
        if (fromIndex in list.indices && toIndex in list.indices) {
            if (fromIndex < toIndex) for (i in fromIndex until toIndex) Collections.swap(list, i, i + 1)
            else for (i in fromIndex downTo toIndex + 1) Collections.swap(list, i, i - 1)
            dashboardTiles.clear(); dashboardTiles.addAll(list); saveDebouncer.tryEmit(Unit)
        }
    }

    fun resizeTile(tileId: String, newSize: TileSize) {
        val index = dashboardTiles.indexOfFirst { it.id == tileId }
        if (index != -1) {
            val updatedTile = dashboardTiles[index].copy(size = newSize)
            dashboardTiles[index] = updatedTile; saveDebouncer.tryEmit(Unit)
        }
    }

    fun pinApp(app: AppModel) {
        viewModelScope.launch(Dispatchers.Default) {
            val type = if (app.packageName.contains("clock")) TileType.CLOCK else TileType.APPLICATION
            var tile = TileConfig(UUID.randomUUID().toString(), app.label, null, TileSize.SMALL, type, app.packageName, 0, 0)
            if (type == TileType.APPLICATION) {
                tile = runCatching { tile.copy(icon = TileIcon.AndroidIcon(pm.getApplicationIcon(app.packageName))) }.getOrDefault(tile)
            }
            withContext(Dispatchers.Main) { dashboardTiles.add(tile); saveDebouncer.tryEmit(Unit) }
        }
    }

    fun unpinTile(id: String) { dashboardTiles.removeAll { it.id == id }; saveDebouncer.tryEmit(Unit) }

    private fun setupAutoSave() {
        viewModelScope.launch { saveDebouncer.debounce(500).collectLatest { saveTilesToStorage() } }
    }

    private suspend fun saveTilesToStorage() = withContext(Dispatchers.IO) {
        runCatching {
            val list = dashboardTiles.map { it.copy(icon = null) }
            dashboardPrefs.edit().putString("saved_tiles_data", gson.toJson(list)).apply()
        }
    }

    private suspend fun loadTilesFromStorage() = withContext(Dispatchers.IO) {
        val list = runCatching {
            val json = dashboardPrefs.getString("saved_tiles_data", null)
            if (json != null) gson.fromJson<List<TileConfig>>(json, object:TypeToken<List<TileConfig>>(){}.type) else emptyList()
        }.getOrElse { emptyList() }
        val rehydrated = list.filterNotNull().mapNotNull { tile ->
            if (tile.id == null) return@mapNotNull null
            val tileWithSize = if (tile.size == null) tile.copy(size = TileSize.SMALL) else tile
            if (tileWithSize.packageName.isNotEmpty()) {
                runCatching {
                    val d = pm.getApplicationIcon(tileWithSize.packageName)
                    tileWithSize.copy(icon = TileIcon.AndroidIcon(BitmapDrawable(context.resources, drawableToBitmap(d))))
                }.getOrDefault(tileWithSize)
            } else tileWithSize
        }
        withContext(Dispatchers.Main) { dashboardTiles.clear(); dashboardTiles.addAll(rehydrated) }
    }

    private suspend fun loadInstalledApps() = withContext(Dispatchers.IO) {
        val i = Intent(Intent.ACTION_MAIN, null).apply { addCategory(Intent.CATEGORY_LAUNCHER) }
        val list = pm.queryIntentActivities(i, 0).mapNotNull {
            runCatching {
                val p = it.activityInfo.packageName
                if (IconCache.get(p) == null) IconCache.put(p, drawableToBitmap(it.loadIcon(pm)))
                AppModel(it.loadLabel(pm).toString(), p)
            }.getOrNull()
        }.sortedBy { it.label.lowercase() }
        withContext(Dispatchers.Main) { otherApps.value = list }
    }

    private fun drawableToBitmap(d: Drawable): Bitmap {
        if (d is BitmapDrawable) return d.bitmap
        val b = Bitmap.createBitmap(1.coerceAtLeast(d.intrinsicWidth), 1.coerceAtLeast(d.intrinsicHeight), Bitmap.Config.ARGB_8888)
        val c = Canvas(b); d.setBounds(0, 0, c.width, c.height); d.draw(c); return b
    }

    fun hasNotificationPermission(): Boolean {
        val l = android.provider.Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
        return l != null && l.contains(context.packageName)
    }

    fun moveApp(f: Int, t: Int) {
        val l = otherApps.value.toMutableList()
        if (f in l.indices && t in l.indices) {
            if (f < t) for (i in f until t) Collections.swap(l, i, i + 1) else for (i in f downTo t + 1) Collections.swap(l, i, i - 1)
            otherApps.value = l
        }
    }
}