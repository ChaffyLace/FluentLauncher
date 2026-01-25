package com.example.fluentui.wallpaper

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun FluentGlassTile(
    modifier: Modifier = Modifier,
    micaParams: MicaParams?,
    isDark: Boolean,
    cornerRadius: Dp = 8.dp,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }

    // Animation de clic (Scale)
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f, // 0.96 est plus subtil que 0.98
        animationSpec = tween(durationMillis = 100),
        label = "scale"
    )

    // Couleurs & Dégradés
    val baseColor = micaParams?.surfaceColor ?: if (isDark) Color(0xFF202020) else Color(0xFFF3F3F3)
    val shape = RoundedCornerShape(cornerRadius)

    val surfaceGradient = remember(baseColor, isDark) {
        val start = if(isDark) baseColor.copy(alpha = 0.6f) else baseColor.copy(alpha = 0.85f)
        val end = if(isDark) baseColor.copy(alpha = 0.3f) else baseColor.copy(alpha = 0.5f)
        Brush.linearGradient(
            colors = listOf(start, end),
            start = Offset(0f, 0f),
            end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
        )
    }

    val borderGradient = remember(isDark) {
        val topColor = if (isDark) Color.White.copy(0.15f) else Color.White.copy(0.6f)
        val bottomColor = if (isDark) Color.Black.copy(0.1f) else Color.Black.copy(0.05f)
        Brush.verticalGradient(listOf(topColor, bottomColor))
    }

    val noiseBrush = remember(isDark) { NoiseGenerator.generate(isDark) }

    // --- STRUCTURE ---
    Box(
        modifier = modifier
            .scale(scale)
            // Gestion des clics
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        tryAwaitRelease()
                        isPressed = false
                    },
                    onTap = { onClick?.invoke() },
                    onLongPress = { onLongClick?.invoke() }
                )
            }
    ) {
        // COUCHE 1 : Fond Dégradé
        // CORRECTION : On passe 'shape' au background pour éviter les coins carrés qui dépassent
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(surfaceGradient, shape)
        )

        // COUCHE 2 : Bruit (Noise)
        // CORRECTION : On passe 'shape' ici aussi
        Box(
            modifier = Modifier
                .matchParentSize()
                .graphicsLayer(alpha = if (isDark) 0.03f else 0.08f)
                .background(noiseBrush, shape)
        )

        // COUCHE 3 : Bordure
        // La bordure se pose proprement sur la forme
        Box(
            modifier = Modifier
                .matchParentSize()
                .border(1.dp, borderGradient, shape)
        )

        // COUCHE 4 : Le contenu (icône, texte)
        // Pas de padding ici, c'est géré par le parent (DashboardTile)
        content()
    }
}

// Extension utile pour l'opacité simplifiée (si pas déjà présente)
fun Modifier.alpha(value: Float) = this.then(Modifier.graphicsLayer(alpha = value))