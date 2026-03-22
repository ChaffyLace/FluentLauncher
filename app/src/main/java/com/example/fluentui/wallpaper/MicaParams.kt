package com.example.fluentui.wallpaper

import android.graphics.Bitmap
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

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

object MicaState {
    val micaParams: MutableState<MicaParams?> = mutableStateOf(null)
}