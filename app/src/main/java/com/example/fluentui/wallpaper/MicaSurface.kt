package com.example.fluentui.wallpaper

import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun MicaSurface(
    modifier: Modifier = Modifier,
    mica: MicaParams?, // Peut être null
    cornerRadius: Dp = 12.dp,
    isDark: Boolean = true,
    content: @Composable () -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)

    // 1. Configuration du Flou (Blur)
    // On ne l'applique PAS au parent pour ne pas flouter l'icône
    val blurEffect = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            RenderEffect.createBlurEffect(40f, 40f, Shader.TileMode.CLAMP)
                .asComposeRenderEffect()
        } else null
    }

    Box(
        modifier = modifier
            .clip(shape) // Coupe tout ce qui dépasse
        // Pas de background ici, sinon on perd la transparence
    ) {
        if (mica != null) {
            // --- COUCHE 1 : LE FOND FLOUTÉ ---
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .graphicsLayer {
                        if (blurEffect != null) renderEffect = blurEffect
                        alpha = 0.6f // Transparence globale du verre
                    }
                    .background(mica.surfaceColor) // Couleur de base
            )

            // --- COUCHE 2 : LA TEINTE (TINT) ---
            Box(
                Modifier
                    .matchParentSize()
                    .background(mica.tintBrush, alpha = mica.tintAlpha)
            )

            // --- COUCHE 3 : LE BRUIT (NOISE) ---
            mica.noiseBrush?.let {
                Box(
                    Modifier
                        .matchParentSize()
                        .graphicsLayer(alpha = mica.noiseAlpha) // Appliquer l'alpha ici est plus sûr
                        .background(it)
                )
            }

            // --- COUCHE 4 : BORDURE ---
            Box(
                Modifier
                    .matchParentSize()
                    .border(1.dp, Color.White.copy(if (isDark) 0.12f else 0.4f), shape)
            )
        } else {
            // Fallback si pas de Mica (Couleur unie)
            Box(
                Modifier
                    .matchParentSize()
                    .background(if (isDark) Color(0xFF202020) else Color(0xFFF3F3F3))
            )
        }

        content()
    }
}