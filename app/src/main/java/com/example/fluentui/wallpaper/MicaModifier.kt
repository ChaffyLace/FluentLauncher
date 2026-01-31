package com.example.fluentui.wallpaper

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlin.math.max

/**
 * Modifier Mica "Zéro Transparence Accidentelle".
 * Garantit que le fond est toujours opaque par rapport aux icônes en dessous.
 */
fun Modifier.fluentMicaComplex(
    micaParams: MicaParams?,
    enabled: Boolean = true,
    elevation: Dp = 0.dp,
    shape: Shape
): Modifier = composed {
    // Couleurs de secours (Opaque) : Gris Sombre ou Gris Clair
    // Utilisées si le Mica est désactivé ou si le bitmap flou n'est pas encore chargé.
    val fallbackColor = if (micaParams?.isDark == true) Color(0xFF202020) else Color(0xFFF3F3F3)

    if (!enabled || micaParams == null) {
        // CAS 1 : MICA DÉSACTIVÉ -> On met du SOLIDE
        this
            .shadow(elevation, shape)
            .background(fallbackColor, shape) // <-- BACKGROUND OPAQUE
            .border(1.dp, Color.White.copy(0.1f), shape)
    } else {
        // CAS 2 : MICA ACTIVÉ
        this
            .shadow(elevation, shape)
            .clip(shape)
            // On dessine le fond manuellement
            .drawBehind {
                // ÉTAPE 0 : LE "BOUCHE-TROU" (CRUCIAL)
                // On peint d'abord en noir/blanc opaque pour cacher les icônes du Grid.
                // Même si l'image floue met 100ms à charger, l'utilisateur ne verra pas le grid.
                drawRect(color = fallbackColor, alpha = 1f)

                // ÉTAPE 1 : L'IMAGE FLOUTÉE (WALLPAPER)
                micaParams.blurredWallpaperBitmap?.let { bitmap ->
                    try {
                        val imageBitmap = bitmap.asImageBitmap()

                        // Calcul CenterCrop
                        val srcWidth = imageBitmap.width
                        val srcHeight = imageBitmap.height
                        val dstWidth = size.width
                        val dstHeight = size.height

                        val scale = max(dstWidth / srcWidth, dstHeight / srcHeight)
                        val scaledWidth = srcWidth * scale
                        val scaledHeight = srcHeight * scale

                        val left = (dstWidth - scaledWidth) / 2
                        val top = (dstHeight - scaledHeight) / 2

                        // On dessine l'image (qui va recouvrir le couche "Bouche-Trou")
                        drawImage(
                            image = imageBitmap,
                            dstOffset = IntOffset(left.toInt(), top.toInt()),
                            dstSize = IntSize(scaledWidth.toInt(), scaledHeight.toInt())
                        )
                    } catch (e: Exception) {
                        // Si le dessin échoue, on a toujours l'ÉTAPE 0 qui assure l'opacité.
                    }
                }

                // ÉTAPE 2 : LA TEINTE DE SURFACE (Translucide)
                drawRect(color = micaParams.surfaceColor, alpha = micaParams.surfaceAlpha)

                // ÉTAPE 3 : LA COULEUR DOMINANTE (Gradient)
                drawRect(brush = micaParams.tintBrush, alpha = micaParams.tintAlpha)

                // ÉTAPE 4 : LE GRAIN (Noise)
                micaParams.noiseBrush?.let { brush ->
                    drawRect(brush = brush, alpha = micaParams.noiseAlpha)
                }
            }
            .border(1.dp, Color.White.copy(0.08f), shape)
    }
}