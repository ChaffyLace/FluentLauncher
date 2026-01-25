package com.example.fluentui.wallpaper

import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.ImageShader
import kotlin.random.Random

/**
 * Générateur de texture de bruit procédurale (Noise).
 * Utilise un cache pour éviter de régénérer le Shader à chaque appel.
 */
object NoiseGenerator {
    private var cachedNoise: Brush? = null
    private var lastIsDark: Boolean? = null

    fun generate(isDark: Boolean): Brush {
        if (cachedNoise != null && lastIsDark == isDark) return cachedNoise!!

        val width = 256
        val height = 256
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        for (x in 0 until width) {
            for (y in 0 until height) {
                val noise = Random.nextInt(0, 30)
                val alpha = (noise * 0.3f).toInt()

                val color = if (isDark) {
                    AndroidColor.argb(alpha, 255, 255, 255)
                } else {
                    AndroidColor.argb(alpha, 0, 0, 0)
                }

                bitmap.setPixel(x, y, color)
            }
        }

        val brush = ShaderBrush(ImageShader(bitmap.asImageBitmap(), TileMode.Repeated, TileMode.Repeated))
        cachedNoise = brush
        lastIsDark = isDark
        return brush
    }
}