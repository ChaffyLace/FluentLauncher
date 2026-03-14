package com.example.fluentui.ui.homescreen

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.fluentui.ui.components.AppViewModel
import com.example.fluentui.ui.system.TileConfig
import com.example.fluentui.wallpaper.MicaParams
import com.example.fluentui.wallpaper.fluentMicaComplex
import com.example.fluentui.ui.homescreen.widgets.TileContentDispatcher

/**
 * Core Dashboard Component.
 * Exclusively handles the tile aesthetics (Mica glass effect, borders, dynamic lighting).
 */
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

    // 1. SAFE CAST ACCENT COLOR
    // We collect the raw value from the ViewModel. Since it might be inferred as 'Any', 'Int', or 'Color',
    // we explicitly cast it to a Compose Color to prevent any Type Mismatch crashes at compile time.
    val rawAccentColor by viewModel.accentColor.collectAsState()
    val resolvedAccentColor = remember(rawAccentColor) {
        when (rawAccentColor) {
            is Color -> rawAccentColor as Color
            is Int -> Color(rawAccentColor as Int)
            is Long -> Color(rawAccentColor as Long)
            is String -> try {
                Color(android.graphics.Color.parseColor(rawAccentColor as String))
            } catch (e: Exception) {
                Color(0xFF0078D4) // Fallback to Windows Blue
            }
            else -> Color(0xFF0078D4) // Ultimate fallback to prevent crashes
        }
    }

    // 2. TILE ANIMATIONS & COLORS
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

    // 3. TOP BORDER LIGHTING
    // We maintain the dominant color from the wallpaper (Mica) for the top glass reflection,
    // ensuring the glass effect looks physically accurate against the background.
    val topBorderColor = mica?.dominantColor?.copy(alpha = if (isDark) 0.50f else 0.70f)
        ?: Color.White.copy(alpha = if (isDark) 0.45f else 0.80f)

    val borderBrush = Brush.verticalGradient(
        colors = listOf(
            topBorderColor,
            Color.White.copy(alpha = if (isDark) 0.05f else 0.10f)
        )
    )

    val surfaceLight = Brush.verticalGradient(
        colors = listOf(
            Color.White.copy(alpha = if (isDark) 0.05f else 0.20f),
            Color.Transparent
        )
    )

    // 4. MAIN TILE RENDERER
    Box(
        modifier = modifier
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
        // Press feedback overlay
        if (pressOverlayAlpha > 0f) {
            Box(Modifier.matchParentSize().background(Color.Black.copy(alpha = pressOverlayAlpha)))
        }

        // Delegate internal content to the Router (Dispatcher), injecting the safely resolved accent color
        Box(modifier = Modifier.fillMaxSize()) {
            TileContentDispatcher(
                config = config,
                contentColor = contentColor,
                accentColor = resolvedAccentColor
            )
        }
    }
}