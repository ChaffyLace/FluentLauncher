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
import com.example.fluentui.wallpaper.MicaState.micaParams

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
    val borderWidth  = 1.dp
    val outerShape   = RoundedCornerShape(cornerRadius)
    val innerShape   = RoundedCornerShape(cornerRadius - borderWidth)

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

    // ── FIX : on observe showBadges depuis le SettingsViewModel ──────────────
    // transparencyEnabled est déjà dans AppViewModel, showBadges est dans
    // SettingsViewModel — on le lit depuis AppViewModel.settingsManager via
    // un StateFlow dédié (voir note ci-dessous).
    val showBadges by viewModel.showBadges.collectAsState(initial = true)

    // Calcul du badge : 0 si MEDIA (lecteur système = pas une vraie notif)
    // ou si showBadges est désactivé dans les settings.
    val badgeCount = remember(config.packageName, config.type, notificationCounts, showBadges) {
        if (!showBadges || config.type == TileType.MEDIA) 0
        else notificationCounts[config.packageName] ?: 0
    }

    val pressOverlayAlpha by animateFloatAsState(
        targetValue    = if (isPressed) 0.15f else 0f,
        animationSpec  = tween(durationMillis = 75),
        label          = "tile_overlay_alpha"
    )

    val contentColor = if (isDark) Color.White else Color(0xFF1A1A1A)

    val tileSurface = when {
        isTransparencyEnabled && isDark  -> Color(0xFF111111).copy(alpha = 0.50f)
        isTransparencyEnabled && !isDark -> Color.White.copy(alpha = 0.40f)
        isDark                           -> Color(0xFF242424)
        else                             -> Color(0xFFF3F3F3)
    }

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

        // ── Tile surface ──────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxSize()
                .zIndex(0f)
                .fluentMicaComplex(
                    micaParams = mica,
                    enabled    = isTransparencyEnabled,
                    elevation  = 0.dp,
                    shape      = outerShape
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
                    accentColor = resolvedAccentColor,
                    micaParams = mica, // <-- AJOUTÉ
                    isTransparencyEnabled = isTransparencyEnabled // <-- AJOUTÉ
                )
            }
        }

        // ── Badge de notification ─────────────────────────────────────────────
        if (badgeCount > 0) {
            AnimatedVisibility(
                visible  = !isPressed,
                enter    = fadeIn(animationSpec  = tween(durationMillis = 150, delayMillis = 200)),
                exit     = fadeOut(animationSpec = tween(durationMillis = 80)),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 8.dp, y = (-8).dp)
                    .zIndex(1f)
            ) {
                FluentBadge(
                    count       = badgeCount,
                    badgeColor  = resolvedAccentColor,
                    strokeColor = if (isDark) Color(0xFF1E1E1E) else Color(0xFFFAFAFA)
                )
            }
        }
    }
}