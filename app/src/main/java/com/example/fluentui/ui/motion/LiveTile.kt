package com.example.fluentui.ui.motion

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import kotlinx.coroutines.delay

@Composable
fun FluentLiveTile(
    modifier: Modifier = Modifier,
    frontContent: @Composable () -> Unit,
    backContent: @Composable () -> Unit,
    animationInterval: Long = 5000,
    initialDelay: Long = 0
) {
    var isFront by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        delay(initialDelay)
        while (true) {
            delay(animationInterval)
            isFront = !isFront
        }
    }

    AnimatedContent(
        targetState = isFront,
        modifier = modifier,
        transitionSpec = {
            // L'effet "Slide Up" (Glissement vers le haut) typique de Windows
            // Toujours du bas vers le haut, jamais l'inverse
            slideInVertically(
                initialOffsetY = { height -> height }, // Arrive du bas
                animationSpec = FluentMotion.enterSpec(800) // Durée 0.8s
            ) togetherWith slideOutVertically(
                targetOffsetY = { height -> -height }, // Part vers le haut
                animationSpec = FluentMotion.enterSpec(800)
            )
        },
        label = "LiveTile"
    ) { showFront ->
        if (showFront) {
            frontContent()
        } else {
            backContent()
        }
    }
}