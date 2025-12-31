package com.example.fluentui.ui.motion

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.fluentui.ui.system.rememberHaptics
import com.example.fluentui.ui.theme.FluentMotion

// ==============================================================================
// 1. FLUENT TILE (Gestion du clic, scale, elevation)
// ==============================================================================
@Composable
fun FluentTile(
    modifier: Modifier = Modifier,
    isClickable: Boolean = true,
    onClick: () -> Unit,
    cornerRadius: Dp = 12.dp,
    content: @Composable () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    val haptics = rememberHaptics()

    // Animation Scale : Rétrécit vite (83ms)
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = if (isPressed)
            FluentMotion.enterSpec(FluentMotion.DurationFaster)
        else
            FluentMotion.springFluent(),
        label = "TileScale"
    )

    // Animation Elevation : S'enfonce de 4dp à 2dp
    val elevation by animateDpAsState(
        targetValue = if (isPressed) 2.dp else 4.dp,
        animationSpec = FluentMotion.springFluent(),
        label = "TileElevation"
    )

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                shadowElevation = elevation.toPx()
                shape = RoundedCornerShape(cornerRadius)
                clip = true
            }
            .pointerInput(isClickable) {
                if (isClickable) {
                    detectTapGestures(
                        onPress = {
                            isPressed = true
                            haptics.performPress() // Petit choc haptique
                            val success = tryAwaitRelease()
                            isPressed = false
                            if (success) {
                                haptics.performRelease()
                                onClick()
                            }
                        }
                    )
                }
            }
    ) {
        content()
    }
}

// ==============================================================================
// 2. FLUENT REVEAL (Pour l'ouverture Recherche / Menu Démarrer)
// ==============================================================================
@Composable
fun FluentReveal(
    visible: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    // Opacité
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = FluentMotion.enterSpec(FluentMotion.DurationNormal),
        label = "RevealAlpha"
    )

    // Flou d'entrée (0dp -> 15dp -> 0dp selon l'effet voulu, ici on fait simple)
    // Astuce : On floute quand c'est invisible pour faire un effet de mise au point
    val blurRadius by animateDpAsState(
        targetValue = if (visible) 0.dp else 15.dp,
        animationSpec = FluentMotion.enterSpec(FluentMotion.DurationNormal),
        label = "RevealBlur"
    )

    if (alpha > 0f) {
        Box(
            modifier = modifier
                .graphicsLayer {
                    this.alpha = alpha
                    // Petit effet de Zoom In (0.95 -> 1.0) typique Windows
                    val scale = 0.95f + (0.05f * alpha)
                    scaleX = scale
                    scaleY = scale
                }
                .blur(blurRadius)
        ) {
            content()
        }
    }
}

// ==============================================================================
// 3. FLUENT PARALLAX (Pour le fond d'écran)
// ==============================================================================
@Composable
fun FluentParallax(
    scrollProgress: Float, // 0f (Dashboard) -> 1f (Grid)
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier.graphicsLayer {
            translationX = -scrollProgress * 40f
            scaleX = 1.05f
            scaleY = 1.05f
        }
    ) {
        content()
    }
}

// ==============================================================================
// 4. FLUENT OVERSCROLL (Effet élastique, optionnel)
// ==============================================================================
@Composable
fun FluentOverscroll(
    overscrollOffset: Float,
    content: @Composable () -> Unit
) {
    val animatedOffset by animateFloatAsState(
        targetValue = overscrollOffset,
        animationSpec = FluentMotion.springFluent(),
        label = "Overscroll"
    )

    Box(
        modifier = Modifier.graphicsLayer {
            translationY = animatedOffset * 0.3f
        }
    ) {
        content()
    }
}