/**
 * @file TileContentDispatcher.kt
 * @description Centralized dispatcher for Fluent UI tiles.
 * Routes to specialized widgets based on TileType, or falls back to a
 * static standard app icon. Uses the native Windows 10 Slide-Up animation
 * strictly on state change (activation/deactivation).
 */

package com.example.fluentui.ui.homescreen.widgets

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntOffset
import com.example.fluentui.ui.system.TileConfig
import com.example.fluentui.ui.system.TileType

@Composable
fun TileContentDispatcher(
    config: TileConfig,
    contentColor: Color,
    accentColor: Color
) {
    // Normalize package name for safe matching
    val pkg = remember(config.packageName) { config.packageName.lowercase().trim() }

    // Photos are inherently dynamic, otherwise rely on user preference
    val showDynamic = config.isDynamic || config.type == TileType.PHOTO

    // ========================================================================
    // WINDOWS 10 EXACT EASING
    // P1(0.0, 0.0) -> P2(0.2, 1.0) = Fast out, slow in. No bounce.
    // ========================================================================
    val windowsEasing = remember { CubicBezierEasing(0.0f, 0.0f, 0.2f, 1.0f) }

    // CORRECTION ICI : Remplacement de <Int> par <IntOffset> pour le slide
    val slideSpec = remember { tween<IntOffset>(durationMillis = 320, easing = windowsEasing) }
    val fadeSpec = remember { tween<Float>(durationMillis = 200, easing = windowsEasing) }

    // ========================================================================
    // STATE-DRIVEN ANIMATION ENGINE
    // Animates strictly once when 'showDynamic' toggles.
    // Always slides from bottom to top to respect Windows design guidelines.
    // ========================================================================
    AnimatedContent(
        targetState = showDynamic,
        modifier = Modifier.fillMaxSize(),
        transitionSpec = {
            (slideInVertically(animationSpec = slideSpec) { height -> height } +
                    fadeIn(animationSpec = fadeSpec)) togetherWith
                    (slideOutVertically(animationSpec = slideSpec) { height -> -height } +
                            fadeOut(animationSpec = fadeSpec))
        },
        label = "LiveTileToggleAnimation"
    ) { isDynamicActive ->

        if (!isDynamicActive) {
            // ── Face A: Static App Icon ───────────────────────────────────────
            StandardAppTile(config, contentColor)
        } else {
            // ── Face B: Live Widget ───────────────────────────────────────────
            when {
                config.type == TileType.PHOTO -> {
                    PhotoWidget(config = config, modifier = Modifier.fillMaxSize())
                }
                config.type == TileType.CALENDAR || isSystemCalendar(pkg) -> {
                    CalendarWidget(config, contentColor, accentColor)
                }
                config.type == TileType.CLOCK || isSystemClock(pkg) -> {
                    ClockWidget(config, contentColor, accentColor)
                }
                (config.type == TileType.WIDGET && pkg == "fluent.internal.system") || isSettingsApp(pkg) -> {
                    SystemWidget(config, contentColor, accentColor)
                }
                config.type == TileType.MEDIA || isMusicApp(pkg) -> {
                    MediaWidget(config, contentColor, accentColor)
                }
                config.type == TileType.WEATHER || pkg.contains("weather") -> {
                    StandardAppTile(config, contentColor)
                }
                else -> {
                    StandardAppTile(config, contentColor)
                }
            }
        }
    }
}

// ============================================================================
// DETECTION HELPERS
// ============================================================================

private fun isSettingsApp(pkg: String): Boolean {
    return pkg == "com.android.settings" ||
            pkg == "com.samsung.android.settings" ||
            pkg == "com.google.android.settings" ||
            pkg == "com.oneplus.settings" ||
            pkg == "com.huawei.settings" ||
            pkg == "com.miui.settings" ||
            pkg == "com.oppo.settings" ||
            pkg.endsWith(".settings")
}

private fun isMusicApp(pkg: String): Boolean {
    return pkg.contains("spotify") ||
            pkg.contains("samsung.android.app.music") ||
            pkg.contains("sec.android.app.music") ||
            pkg.contains("music") ||
            pkg.contains("deezer") ||
            pkg.contains("tidal") ||
            pkg.contains("soundcloud") ||
            pkg.contains("player")
}

private fun isSystemCalendar(pkg: String): Boolean {
    return pkg.contains("android.calendar") ||
            pkg.contains("com.google.android.calendar") ||
            pkg.contains("com.samsung.android.calendar")
}

private fun isSystemClock(pkg: String): Boolean {
    return pkg.contains("android.deskclock") ||
            pkg.contains("com.google.android.deskclock") ||
            pkg.contains("com.sec.android.app.clockpackage") ||
            pkg.contains("com.huawei.android.totemweather")
}