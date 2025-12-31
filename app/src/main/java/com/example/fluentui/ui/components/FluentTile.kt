package com.example.fluentui.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.fluentui.ui.system.rememberHaptics
import com.example.fluentui.ui.theme.FluentMotion

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

    // 1. Scale : 0.96f quand appuyé
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = if (isPressed) FluentMotion.enterSpec(FluentMotion.DurationFaster)
        else FluentMotion.springFluent(),
        label = "TileScale"
    )

    // 2. Elevation : S'enfonce de 4dp à 2dp
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
                            haptics.performPress()
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