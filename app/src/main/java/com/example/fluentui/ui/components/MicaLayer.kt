package com.example.fluentui.ui.components

import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer

@Composable
fun MicaLayer() {
    val isDark = isSystemInDarkTheme()

    // Couleur de fond Mica (semi-transparente)
    val micaColor = if (isDark) {
        Color(0xFF202020).copy(alpha = 0.80f) // Gris profond en mode sombre
    } else {
        Color(0xFFF3F3F3).copy(alpha = 0.85f) // Gris clair en mode clair
    }

    // Flou fort pour adoucir le fond
    val blurModifier = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        Modifier.graphicsLayer {
            renderEffect = RenderEffect
                .createBlurEffect(100f, 100f, Shader.TileMode.CLAMP)
                .asComposeRenderEffect()
        }
    } else Modifier

    // Gradient subtil pour simuler la lumière et la matière
    val micaGradient = Brush.verticalGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.05f), // Lumière en haut
            Color.Transparent,               // Milieu neutre
            Color.Black.copy(alpha = 0.10f)  // Ombre en bas
        )
    )

    // Overlay de bruit visuel simulé (texture légère)
    val noiseOverlay = Brush.linearGradient(
        colors = listOf(
            Color.Black.copy(alpha = 0.02f),
            Color.White.copy(alpha = 0.02f)
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .then(blurModifier)
            .background(micaColor)
            .background(micaGradient)
            .background(noiseOverlay)
    )
}