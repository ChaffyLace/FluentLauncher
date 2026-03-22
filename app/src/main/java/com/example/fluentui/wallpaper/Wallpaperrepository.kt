package com.example.fluentui.wallpaper

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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

// ==============================================================================
// WallpaperRepository
//
// Responsabilité unique : charger et pré-calculer les bitmaps UNE SEULE FOIS.
// Le résultat est exposé via un StateFlow consommé par WallpaperLayer.
//
// Survie aux changements de thème : comme la clé (type + value) n'est PAS liée
// au thème, un passage dark→light n'invalidera JAMAIS le StateFlow.
// ==============================================================================
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

            // 1. Décode l'image pleine résolution
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
                    else -> null
                }
            } catch (e: Exception) { e.printStackTrace(); null }

            if (sharp == null) {
                withContext(Dispatchers.Main) { _bitmaps.value = null }
                return@launch
            }

            val blurred: Bitmap = withContext(Dispatchers.Default) {
                val thumb = BitmapEngine.createScaledThumbnail(sharp, targetWidth = 300)
                SoftwareBlur.blur(thumb, radius = 12)
            }

            withContext(Dispatchers.Main) {
                _bitmaps.value = WallpaperBitmaps(sharp = sharp, blurred = blurred)

                val currentMica = MicaState.micaParams.value

                if (currentMica != null) {
                    MicaState.micaParams.value = currentMica.copy(blurredWallpaperBitmap = blurred)
                } else {

                    MicaState.micaParams.value = MicaParams(
                        surfaceColor = Color(0xFFFAFAFA),
                        surfaceAlpha = 0.5f,
                        tintBrush = Brush.linearGradient(listOf(Color.Transparent, Color.Transparent)),
                        tintAlpha = 0f,
                        primaryColor = Color.Black,
                        noiseBrush = null,
                        dominantColor = Color.Gray,
                        noiseAlpha = 0f,
                        contentColor = Color.Black,
                        blurredWallpaperBitmap = blurred,
                        isDark = false
                    )
                }
            }
        }
    }
}