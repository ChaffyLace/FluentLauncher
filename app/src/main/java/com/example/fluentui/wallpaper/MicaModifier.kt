package com.example.fluentui.wallpaper

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlin.math.max

/**
 * Applies a high-performance Windows 11 Mica/Acrylic material effect.
 * Features a Persistent Cache to prevent black screens caused by Android's aggressive Bitmap recycling.
 */
fun Modifier.fluentMicaComplex(
    micaParams: MicaParams?,
    enabled: Boolean = true,
    elevation: Dp = 0.dp,
    shape: Shape
): Modifier = composed {
    val isDarkTheme = micaParams?.isDark == true
    val fallbackColor = if (isDarkTheme) Color(0xFF202020) else Color(0xFFF3F3F3)

    // Fast return for static UI (Battery saver or missing parameters)
    if (!enabled || micaParams == null) {
        return@composed this
            .shadow(elevation, shape)
            .background(fallbackColor, shape)
            .border(1.dp, Color.White.copy(0.1f), shape)
    }

    // =========================================================================
    // BOUCLIER ANTI-RECYCLAGE (Persistent Cache)
    // Au lieu de lire le bitmap à la volée, on le stocke dans un état persistant.
    // Si l'OS ou le ViewModel détruit le bitmap source 1 seconde plus tard,
    // l'interface graphique gardera cette copie intacte et l'écran ne deviendra pas noir.
    // =========================================================================
    var persistentBitmap by remember { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(micaParams.blurredWallpaperBitmap) {
        val bmp = micaParams.blurredWallpaperBitmap
        if (bmp != null && !bmp.isRecycled) {
            try {
                // On verrouille la conversion en ImageBitmap dans la RAM de Compose
                persistentBitmap = bmp.asImageBitmap()
            } catch (e: Exception) {
                // Fail silently
            }
        }
    }

    this
        .shadow(elevation, shape)
        .clip(shape)
        .drawBehind {
            // 1. Couche de base opaque (cache les icônes qui passent en dessous)
            drawRect(color = fallbackColor, alpha = 1f)

            // 2. Rendu de l'image mise en cache (ne disparaîtra plus)
            persistentBitmap?.let { imageBitmap ->
                val dstWidth = size.width
                val dstHeight = size.height

                if (dstWidth > 0f && dstHeight > 0f) {
                    val srcWidth = imageBitmap.width.toFloat()
                    val srcHeight = imageBitmap.height.toFloat()

                    // CenterCrop logic
                    val scale = max(dstWidth / srcWidth, dstHeight / srcHeight)
                    val scaledWidth = srcWidth * scale
                    val scaledHeight = srcHeight * scale

                    val left = (dstWidth - scaledWidth) / 2f
                    val top = (dstHeight - scaledHeight) / 2f

                    try {
                        drawImage(
                            image = imageBitmap,
                            dstOffset = IntOffset(left.toInt(), top.toInt()),
                            dstSize = IntSize(scaledWidth.toInt(), scaledHeight.toInt())
                        )
                    } catch (e: Exception) {
                        // Tolérance aux pannes OpenGL
                    }
                }
            }

            // 3. Application des teintes Windows 11
            drawRect(color = micaParams.surfaceColor, alpha = micaParams.surfaceAlpha)
            drawRect(brush = micaParams.tintBrush, alpha = micaParams.tintAlpha)

            // 4. Grain
            micaParams.noiseBrush?.let { brush ->
                drawRect(brush = brush, alpha = micaParams.noiseAlpha)
            }
        }
        .border(1.dp, Color.White.copy(0.08f), shape)
}