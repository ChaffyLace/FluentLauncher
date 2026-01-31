package com.example.fluentui.wallpaper

import android.graphics.Bitmap
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.asImageBitmap // Important pour la conversion
import kotlin.math.sin

object NoiseGenerator {
    private var cachedDarkNoise: Brush? = null
    private var cachedLightNoise: Brush? = null

    fun generate(isDark: Boolean): Brush {
        val cached = if (isDark) cachedDarkNoise else cachedLightNoise
        if (cached != null) return cached

        val size = 64
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)

        for (x in 0 until size) {
            for (y in 0 until size) {
                // Algorithme de bruit Perlin simplifié
                val noise = generatePerlinNoise(x.toFloat() / size, y.toFloat() / size)

                // On mappe le bruit vers l'alpha (Transparence)
                // 50f = Intensité du grain
                val alpha = ((noise + 1f) * 0.5f * 50f).toInt().coerceIn(0, 255)

                val pixelColor = if (isDark) {
                    android.graphics.Color.argb(alpha, 255, 255, 255)
                } else {
                    android.graphics.Color.argb(alpha, 200, 200, 200)
                }
                bitmap.setPixel(x, y, pixelColor)
            }
        }

        val imageBitmap = bitmap.asImageBitmap()
        val shader = ImageShader(
            image = imageBitmap,
            tileModeX = TileMode.Repeated,
            tileModeY = TileMode.Repeated
        )
        val brush = ShaderBrush(shader)

        if (isDark) cachedDarkNoise = brush else cachedLightNoise = brush
        return brush
    }

    private fun generatePerlinNoise(x: Float, y: Float): Float {
        var result = 0f
        var amplitude = 1f
        var frequency = 1f

        for (octave in 0 until 2) {
            val sampleX = x * frequency * 8f
            val sampleY = y * frequency * 8f
            val noise = sin(sampleX * 12.9898f + sampleY * 78.233f) * 43758.5453f
            val fractional = noise - noise.toInt()
            result += fractional * amplitude
            amplitude *= 0.5f
            frequency *= 2f
        }
        return result.coerceIn(-1f, 1f)
    }
}