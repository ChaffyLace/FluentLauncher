package com.example.fluentui.ui.homescreen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.example.fluentui.ui.components.AppViewModel
import com.example.fluentui.ui.components.FluentBadge
import com.example.fluentui.ui.system.TileConfig
import com.example.fluentui.ui.system.TileType
import com.example.fluentui.wallpaper.MicaParams
import com.example.fluentui.wallpaper.fluentMicaComplex
import com.example.fluentui.ui.homescreen.widgets.TileContentDispatcher

@Composable
fun MicaTile(
    config: TileConfig,
    mica: MicaParams?,
    isDark: Boolean,
    viewModel: AppViewModel,
    modifier: Modifier = Modifier,
    isTransparencyEnabled: Boolean,
    isPressed: Boolean = false
) {
    val cornerRadius = 16.dp
    val borderWidth = 1.dp

    val outerShape = RoundedCornerShape(cornerRadius)
    val innerShape = RoundedCornerShape(cornerRadius - borderWidth)

    val rawAccentColor by viewModel.accentColor.collectAsState()
    val resolvedAccentColor = remember(rawAccentColor) {
        when (rawAccentColor) {
            is Color  -> rawAccentColor as Color
            is Int    -> Color(rawAccentColor as Int)
            is Long   -> Color(rawAccentColor as Long)
            is String -> try {
                Color(android.graphics.Color.parseColor(rawAccentColor as String))
            } catch (e: Exception) { Color(0xFF0078D4) }
            else -> Color(0xFF0078D4)
        }
    }

    val notificationCounts by viewModel.notificationCounts.collectAsState()
    // TileType.MEDIA : la "notification" est le lecteur système, pas une vraie notif.
    // On masque le badge pour éviter le "1" permanent et trompeur.
    val badgeCount = remember(config.packageName, config.type, notificationCounts) {
        if (config.type == TileType.MEDIA) 0
        else notificationCounts[config.packageName] ?: 0
    }

    val pressOverlayAlpha by animateFloatAsState(
        targetValue = if (isPressed) 0.15f else 0f,
        animationSpec = tween(durationMillis = 75),
        label = "tile_overlay_alpha"
    )

    val contentColor = if (isDark) Color.White else Color(0xFF1A1A1A)

    val tileSurface = when {
        isTransparencyEnabled && isDark  -> Color(0xFF111111).copy(alpha = 0.50f)
        isTransparencyEnabled && !isDark -> Color.White.copy(alpha = 0.40f)
        isDark                           -> Color(0xFF242424)
        else                             -> Color(0xFFF3F3F3)
    }

    // Bordure neutre — dégradé blanc/transparent sans teinte wallpaper
    val borderBrush = Brush.verticalGradient(
        colors = listOf(
            Color.White.copy(alpha = if (isDark) 0.18f else 0.70f),
            Color.White.copy(alpha = if (isDark) 0.04f else 0.10f)
        )
    )

    val surfaceLight = Brush.verticalGradient(
        colors = listOf(
            Color.White.copy(alpha = if (isDark) 0.05f else 0.20f),
            Color.Transparent
        )
    )

    Box(modifier = modifier.graphicsLayer { clip = false }) {

        // Tile — zIndex 0
        Box(
            modifier = Modifier
                .fillMaxSize()
                .zIndex(0f)
                .fluentMicaComplex(
                    micaParams = mica,
                    enabled = isTransparencyEnabled,
                    elevation = 0.dp,
                    shape = outerShape
                )
                .then(
                    if (isTransparencyEnabled) Modifier.border(borderWidth, borderBrush, outerShape)
                    else Modifier
                )
                .padding(if (isTransparencyEnabled) borderWidth else 0.dp)
                .background(tileSurface, innerShape)
                .background(surfaceLight, innerShape)
                .clip(innerShape)
        ) {
            if (pressOverlayAlpha > 0f) {
                Box(Modifier.matchParentSize().background(Color.Black.copy(alpha = pressOverlayAlpha)))
            }
            Box(modifier = Modifier.fillMaxSize()) {
                TileContentDispatcher(
                    config = config,
                    contentColor = contentColor,
                    accentColor = resolvedAccentColor
                )
            }
        }

        // Badge — zIndex 1, toujours devant
        // Disparaît immédiatement au début du drag (fadeOut 80ms)
        // Réapparaît avec un délai de 200ms après le relâché pour laisser
        // la grille finir son animation de replacement avant que le badge pop
        if (badgeCount > 0) {
            AnimatedVisibility(
                visible = !isPressed,
                enter = fadeIn(animationSpec = tween(durationMillis = 150, delayMillis = 200)),
                exit  = fadeOut(animationSpec = tween(durationMillis = 80)),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 8.dp, y = (-8).dp)
                    .zIndex(1f)
            ) {
                FluentBadge(
                    count = badgeCount,
                    badgeColor = resolvedAccentColor,
                    strokeColor = if (isDark) Color(0xFF1E1E1E) else Color(0xFFFAFAFA)
                )
            }
        }
    }
}