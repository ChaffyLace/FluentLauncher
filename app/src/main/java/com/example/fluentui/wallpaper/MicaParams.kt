package com.example.fluentui.wallpaper

import android.graphics.Bitmap
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Ce fichier sert de "mémoire partagée" pour l'effet Mica.
 * Il permet à n'importe quel bouton de savoir quelle couleur prendre
 * sans avoir besoin de recalculer l'analyse du fond d'écran à chaque fois.
 */

data class MicaParams(
    val surfaceColor: Color,
    val surfaceAlpha: Float,
    val tintBrush: Brush,
    val tintAlpha: Float,
    val primaryColor: Color,
    val noiseBrush: Brush?,
    val dominantColor: Color,
    val noiseAlpha: Float,
    val contentColor: Color = Color.White,
    val blurredWallpaperBitmap: Bitmap? = null,
    val isDark: Boolean = true

)

object WallpaperRepository {
    val micaParams: MutableState<MicaParams?> = mutableStateOf(null)
}