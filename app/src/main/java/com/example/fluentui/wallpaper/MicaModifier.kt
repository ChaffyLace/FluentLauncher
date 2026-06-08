package com.example.fluentui.wallpaper

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlin.math.max

/**
 * Applies a high-performance Windows 11 Mica/Acrylic material effect.
 * Features a Persistent Cache to prevent black screens caused by Android's aggressive Bitmap recycling,
 * and uses Global Space Mapping to ensure texture continuity across the entire screen.
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
    // =========================================================================
    var persistentBitmap by remember { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(micaParams.blurredWallpaperBitmap) {
        val bmp = micaParams.blurredWallpaperBitmap
        if (bmp != null && !bmp.isRecycled) {
            try {
                persistentBitmap = bmp.asImageBitmap()
            } catch (e: Exception) {
                // Fail silently
            }
        }
    }

    // =========================================================================
    // GLOBAL SPACE MAPPING (Continuité spatiale)
    // =========================================================================
    var globalPosition by remember { mutableStateOf(Offset.Zero) }
    val context = LocalContext.current
    val displayMetrics = context.resources.displayMetrics
    val screenWidth = displayMetrics.widthPixels.toFloat()
    val screenHeight = displayMetrics.heightPixels.toFloat()

    this
        .shadow(elevation, shape)
        .onGloballyPositioned { coordinates ->
            // Capture la position absolue de ce composant sur l'écran
            globalPosition = coordinates.positionInWindow()
        }
        .clip(shape)
        .drawBehind {
            // 1. Couche de base opaque (cache les éléments qui scrollent en dessous)
            drawRect(color = fallbackColor, alpha = 1f)

            // 2. Rendu de l'image mise en cache, mappée sur l'écran global
            persistentBitmap?.let { imageBitmap ->
                if (screenWidth > 0f && screenHeight > 0f) {
                    val srcWidth = imageBitmap.width.toFloat()
                    val srcHeight = imageBitmap.height.toFloat()

                    // CenterCrop logic calculée par rapport à l'ÉCRAN ENTIER, pas au composant
                    val scale = max(screenWidth / srcWidth, screenHeight / srcHeight)
                    val scaledWidth = srcWidth * scale
                    val scaledHeight = srcHeight * scale

                    val left = (screenWidth - scaledWidth) / 2f
                    val top = (screenHeight - scaledHeight) / 2f

                    // On applique la translation inverse pour que le (0,0) du pinceau
                    // corresponde au (0,0) de l'écran du téléphone.
                    translate(left = -globalPosition.x, top = -globalPosition.y) {
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
            }

            // 3. Application des teintes Windows 11 (Mica Color Tints)
            drawRect(color = micaParams.surfaceColor, alpha = micaParams.surfaceAlpha)
            drawRect(brush = micaParams.tintBrush, alpha = micaParams.tintAlpha)

            // 4. Grain (Noise)
            micaParams.noiseBrush?.let { brush ->
                drawRect(brush = brush, alpha = micaParams.noiseAlpha)
            }
        }
        .border(1.dp, Color.White.copy(0.08f), shape)
}