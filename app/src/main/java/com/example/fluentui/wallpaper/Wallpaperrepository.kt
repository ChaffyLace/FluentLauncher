package com.example.fluentui.wallpaper

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

data class WallpaperBitmaps(
    val sharp: Bitmap,
    val blurred: Bitmap
)

/**
 * ==============================================================================
 * WallpaperRepository
 *
 * Responsabilité unique : charger et pré-calculer les bitmaps (nets et floutés).
 * Le résultat est exposé via un StateFlow consommé par l'UI et le ViewModel.
 *
 * Architecture "Sandbox" : Ne fait AUCUN appel au système Android (WallpaperManager),
 * garantissant 0 crash lié aux permissions (Android 14 / One UI 6).
 * ==============================================================================
 */
class WallpaperRepository(private val context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var lastKey: Pair<String, String>? = null

    private val _bitmaps = MutableStateFlow<WallpaperBitmaps?>(null)
    val bitmaps: StateFlow<WallpaperBitmaps?> = _bitmaps

    fun load(wallpaperType: String, wallpaperValue: String) {
        val key = wallpaperType to wallpaperValue
        if (key == lastKey) return
        lastKey = key

        scope.launch {
            val metrics = context.resources.displayMetrics
            val reqW = metrics.widthPixels
            val reqH = metrics.heightPixels

            // 1. Décode l'image pleine résolution selon le type
            val sharp: Bitmap? = try {
                when (wallpaperType) {
                    "FILE" -> {
                        val f = File(wallpaperValue)
                        if (f.exists()) BitmapEngine.decodeSampledBitmapFromFile(f, reqW, reqH)
                        else null
                    }
                    "RES" -> {
                        val id = wallpaperValue.toIntOrNull()
                        if (id != null) BitmapEngine.decodeSampledBitmapFromResource(context, id, reqW, reqH)
                        else null
                    }
                    "URI" -> {
                        val uri = Uri.parse(wallpaperValue)
                        BitmapEngine.decodeSampledBitmapFromUri(context, uri, reqW, reqH)
                    }
                    "SOLID" -> {
                        // Traitement unifié : une couleur unie génère un bitmap 1x1
                        val colorInt = wallpaperValue.toIntOrNull() ?: android.graphics.Color.BLACK
                        val bmp = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
                        bmp.eraseColor(colorInt)
                        bmp
                    }
                    else -> null
                }
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }

            if (sharp == null) {
                withContext(Dispatchers.Main) { _bitmaps.value = null }
                return@launch
            }

            // 2. Downscale + Blur = Traitement ultra-rapide et économe en RAM
            val blurred: Bitmap = withContext(Dispatchers.Default) {
                val thumb = BitmapEngine.createScaledThumbnail(sharp, targetWidth = 300)
                SoftwareBlur.blur(thumb, radius = 12)
            }

            // 3. Émission des Bitmaps purs.
            // (La logique Mica est déléguée au ViewModel qui écoute ce flux)
            withContext(Dispatchers.Main) {
                _bitmaps.value = WallpaperBitmaps(sharp = sharp, blurred = blurred)
            }
        }
    }
}