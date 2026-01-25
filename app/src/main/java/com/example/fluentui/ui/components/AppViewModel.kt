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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import java.io.File
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

    // --- STATE ---
    val notificationCounts = NotificationRepo.counts
    val otherApps = mutableStateOf<List<AppModel>>(emptyList())
    // Liste simple : L'ordre dans la liste détermine la position à l'écran
    val dashboardTiles = mutableStateListOf<TileConfig>()

    private val _navigationEvent = Channel<NavigationEvent>(Channel.BUFFERED)
    val navigationEvent = _navigationEvent.receiveAsFlow()

    // --- MICA PARAMS (Fix Argument Mismatch) ---
    private val defaultMica = MicaParams(
        surfaceColor = Color(0xFFFFFFFF),
        primaryColor = Color(0xFF000000),
        tintBrush = Brush.verticalGradient(listOf(Color.Transparent, Color.Transparent)),
        tintAlpha = 0.0f,
        noiseBrush = null,
        noiseAlpha = 0.0f,
        surfaceAlpha = 0.5f
    )

    private val _micaParams = MutableStateFlow(defaultMica)
    val micaParams = _micaParams.asStateFlow()

    val transparencyEnabled = settingsManager.transparencyEnabled.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = true
    )

    private val saveDebouncer = MutableSharedFlow<Unit>(replay = 0, extraBufferCapacity = 1, onBufferOverflow = kotlinx.coroutines.channels.BufferOverflow.DROP_OLDEST)

    // --- RECEIVERS ---
    private val packageReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) { viewModelScope.launch { loadInstalledApps() } }
    }
    private val themeReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
            viewModelScope.launch { calculateMicaFromWallpaper(settingsManager.wallpaperType.first(), settingsManager.wallpaperValue.first()) }
        }
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
            context.registerReceiver(themeReceiver, IntentFilter(Intent.ACTION_CONFIGURATION_CHANGED))
        }

        viewModelScope.launch {
            loadInstalledApps()
            loadTilesFromStorage()
            observeWallpaperSettings()
        }
        setupAutoSave()
    }

    override fun onCleared() {
        super.onCleared()
        runCatching { context.unregisterReceiver(packageReceiver); context.unregisterReceiver(themeReceiver) }
    }

    fun onHomePressed() { viewModelScope.launch { _navigationEvent.send(NavigationEvent.ScrollToHome) } }

    // =================================================================================
    // GESTION SIMPLE ET ROBUSTE (Style LazyGrid)
    // =================================================================================

    /**
     * Déplace une tuile d'un index à un autre dans la liste.
     * C'est LazyVerticalGrid qui gère l'affichage ensuite.
     */
    fun moveTile(fromIndex: Int, toIndex: Int) {
        val list = dashboardTiles.toMutableList()
        if (fromIndex in list.indices && toIndex in list.indices) {
            if (fromIndex < toIndex) {
                for (i in fromIndex until toIndex) Collections.swap(list, i, i + 1)
            } else {
                for (i in fromIndex downTo toIndex + 1) Collections.swap(list, i, i - 1)
            }

            dashboardTiles.clear()
            dashboardTiles.addAll(list)
            saveDebouncer.tryEmit(Unit)
        }
    }

    /**
     * Change la taille et laisse la grille se réorganiser toute seule.
     */
    fun resizeTile(tileId: String, newSize: TileSize) {
        val index = dashboardTiles.indexOfFirst { it.id == tileId }
        if (index != -1) {
            val updatedTile = dashboardTiles[index].copy(size = newSize)
            dashboardTiles[index] = updatedTile
            saveDebouncer.tryEmit(Unit)
        }
    }

    /**
     * Ajoute à la fin de la liste.
     */
    fun pinApp(app: AppModel) {
        viewModelScope.launch(Dispatchers.Default) {
            val type = if (app.packageName.contains("clock")) TileType.CLOCK else TileType.APPLICATION

            // On ajoute simplement à la fin
            var tile = TileConfig(
                id = UUID.randomUUID().toString(),
                title = app.label,
                icon = null,
                size = TileSize.SMALL,
                type = type,
                packageName = app.packageName,
                col = 0, row = 0
            )

            if (type == TileType.APPLICATION) {
                tile = runCatching { tile.copy(icon = TileIcon.AndroidIcon(pm.getApplicationIcon(app.packageName))) }.getOrDefault(tile)
            }

            withContext(Dispatchers.Main) {
                dashboardTiles.add(tile)
                saveDebouncer.tryEmit(Unit)
            }
        }
    }

    fun unpinTile(id: String) {
        dashboardTiles.removeAll { it.id == id }
        saveDebouncer.tryEmit(Unit)
    }

    // =================================================================================
    // UTILS & PERSISTENCE
    // =================================================================================

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

        val valid = list.filterNotNull().map { if (it.size == null) it.copy(size = TileSize.SMALL) else it }
        val rehydrated = valid.map {
            if (it.packageName.isNotEmpty()) runCatching { it.copy(icon = TileIcon.AndroidIcon(pm.getApplicationIcon(it.packageName))) }.getOrDefault(it) else it
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

    fun setCustomWallpaper(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val r = context.contentResolver
                context.openFileOutput("custom_wallpaper.jpg", Context.MODE_PRIVATE).use { o -> r.openInputStream(uri)?.use { i -> i.copyTo(o) } }
                settingsManager.setWallpaper("FILE", context.getFileStreamPath("custom_wallpaper.jpg").absolutePath)
                r.openInputStream(uri)?.use { WallpaperManager.getInstance(context).setStream(it) }
            }
        }
    }

    private fun observeWallpaperSettings() {
        viewModelScope.launch(Dispatchers.IO) {
            settingsManager.wallpaperType.combine(settingsManager.wallpaperValue) { t, v -> Pair(t, v) }
                .collectLatest { (t, v) -> calculateMicaFromWallpaper(t, v) }
        }
    }

    private suspend fun calculateMicaFromWallpaper(type: String, value: String) {
        val b = withContext(Dispatchers.IO) {
            runCatching {
                when (type) {
                    "FILE" -> if (File(value).exists()) decodeSampledBitmap(value) else null
                    "RES" -> value.toIntOrNull()?.let { BitmapFactory.decodeResource(context.resources, it) }
                    "URI" -> context.contentResolver.openInputStream(Uri.parse(value))?.use { BitmapFactory.decodeStream(it) }
                    else -> null
                }
            }.getOrNull()
        }

        if (b != null) {
            withContext(Dispatchers.Default) {
                val p = Palette.from(b).maximumColorCount(16).generate()
                val isDark = (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
                val tint = if (isDark) Color(p.getVibrantColor(p.getMutedColor(0xFFFFFFFF.toInt()))) else Color(p.getLightVibrantColor(p.getDominantColor(0xFFCCCCCC.toInt())))

                val params = MicaParams(
                    surfaceColor = if (isDark) Color(0xFF202020) else Color(0xFFFFFFFF),
                    primaryColor = if (isDark) Color(0xFFF6F6F6) else Color(0xFF1E1E1E),
                    tintBrush = Brush.verticalGradient(
                        listOf(tint.copy(if(isDark) 0.12f else 0.05f), if(isDark) tint.copy(0.02f) else Color.Transparent)
                    ),
                    tintAlpha = 1f,
                    noiseBrush = try { NoiseGenerator.generate(isDark) } catch(e:Exception){null},
                    noiseAlpha = if (isDark) 0.04f else 0.02f,
                    surfaceAlpha = if (isDark) 0.25f else 0.55f
                )
                withContext(Dispatchers.Main) {
                    _micaParams.value = params
                    WallpaperRepository.micaParams.value = params
                }
            }
        }
    }

    private fun decodeSampledBitmap(path: String): Bitmap? {
        return runCatching {
            val o = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(path, o)
            o.inSampleSize = 2; o.inJustDecodeBounds = false
            BitmapFactory.decodeFile(path, o)
        }.getOrNull()
    }

    fun moveApp(f: Int, t: Int) {
        val l = otherApps.value.toMutableList()
        if (f in l.indices && t in l.indices) {
            if (f < t) for (i in f until t) Collections.swap(l, i, i + 1) else for (i in f downTo t + 1) Collections.swap(l, i, i - 1)
            otherApps.value = l
        }
    }
}