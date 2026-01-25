package com.example.fluentui.wallpaper

import android.graphics.Bitmap
import androidx.compose.ui.graphics.Color
import androidx.core.graphics.ColorUtils
import androidx.palette.graphics.Palette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object ColorExtractor {
    suspend fun generateMicaTint(bitmap: Bitmap?, isDark: Boolean): Color = withContext(Dispatchers.Default) {
        if (bitmap == null) {
            return@withContext if (isDark) Color(0xFF202020) else Color(0xFFF3F3F3)
        }

        val p = Palette.from(bitmap).resizeBitmapArea(200 * 200).generate()

        val fallbackSeed = 0xFF808080.toInt()
        var seedColorInt = p.getVibrantColor(p.getDominantColor(fallbackSeed))

        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(seedColorInt, hsv)
        if (hsv[1] < 0.1f) {
            seedColorInt = 0xFF0078D4.toInt()
        }

        val baseGrey = if (isDark) 0xFF151515.toInt() else 0xFFF9F9F9.toInt()
        val blendRatio = if (isDark) FluentTokens.Mica.COLOR_BLEND_RATIO_DARK else FluentTokens.Mica.COLOR_BLEND_RATIO_LIGHT

        var finalArgb = ColorUtils.blendARGB(baseGrey, seedColorInt, blendRatio)

        val luminance = ColorUtils.calculateLuminance(finalArgb)
        if (isDark && luminance > FluentTokens.Mica.LUMINOSITY_THRESHOLD_DARK) {
            finalArgb = ColorUtils.blendARGB(finalArgb, 0xFF000000.toInt(), 0.3f)
        } else if (!isDark && luminance < FluentTokens.Mica.LUMINOSITY_THRESHOLD_LIGHT) {
            finalArgb = ColorUtils.blendARGB(finalArgb, 0xFFFFFFFF.toInt(), 0.3f)
        }

        return@withContext Color(finalArgb)
    }

    // On garde celle-ci pour savoir si on écrit en Noir ou Blanc
    fun isDarkWallpaper(bitmap: Bitmap?): Boolean {
        if (bitmap == null) return true
        val small = Bitmap.createScaledBitmap(bitmap, 40, 40, true)
        val p = Palette.from(small).generate()
        val dom = p.getDominantColor(0xFF000000.toInt())
        return ColorUtils.calculateLuminance(dom) < 0.5
    }
}