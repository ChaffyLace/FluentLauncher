package com.example.fluentui.ui.homescreen.widgets

import androidx.compose.animation.*
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntOffset
import com.example.fluentui.ui.system.TileConfig
import com.example.fluentui.ui.system.TileType
import com.example.fluentui.wallpaper.MicaParams

@Composable
fun TileContentDispatcher(
    config: TileConfig,
    contentColor: Color,
    accentColor: Color,
    micaParams: MicaParams?,
    isTransparencyEnabled: Boolean
) {
    val pkg = remember(config.packageName) {
        config.normalizedPackage
    }

    val showDynamic = config.isDynamic || config.type == TileType.PHOTO

    val easing = remember { CubicBezierEasing(0f, 0f, 0.2f, 1f) }
    val slideSpec = remember { tween<IntOffset>(durationMillis = 320, easing = easing) }
    val fadeSpec = remember { tween<Float>(durationMillis = 200, easing = easing) }

    AnimatedContent(
        targetState = showDynamic,
        transitionSpec = {
            (slideInVertically(slideSpec) { it } + fadeIn(fadeSpec)) togetherWith
                    (slideOutVertically(slideSpec) { -it } + fadeOut(fadeSpec))
        },
        label = "TileAnimation"
    ) { isDynamicActive ->
        if (!isDynamicActive) {
            StandardAppTile(config = config, contentColor = contentColor)
        } else {
            when {
                config.type == TileType.PHOTO -> {
                    PhotoWidget(config = config, modifier = Modifier)
                }

                config.type == TileType.WEATHER || pkg.isWeatherApp() -> {
                    val safeMica = micaParams ?: MicaParams(
                        surfaceColor = if (contentColor == Color.White) Color(0xFF181818) else Color(0xFFF3F3F3),
                        surfaceAlpha = 0.8f,
                        tintBrush = androidx.compose.ui.graphics.SolidColor(Color.Transparent),
                        tintAlpha = 0f,
                        primaryColor = contentColor,
                        noiseBrush = null,
                        dominantColor = accentColor,
                        noiseAlpha = 0f,
                        contentColor = contentColor,
                        blurredWallpaperBitmap = null,
                        isDark = contentColor == Color.White
                    )

                    WeatherWidget(
                        config = config,
                        contentColor = contentColor,
                        micaParams = safeMica,
                        modifier = Modifier
                    )
                }

                config.type == TileType.CALENDAR || pkg.isCalendarApp() -> {
                    CalendarWidget(config, contentColor, accentColor)
                }

                config.packageName == "com.netflix.mediaclient" -> {
                    NetflixCardTile()
                }

                config.type == TileType.CLOCK || pkg.isClockApp() -> {
                    ClockWidget(config, contentColor, accentColor)
                }

                config.type == TileType.MEDIA || pkg.isMusicApp() -> {
                    MediaWidget(config, contentColor, accentColor)
                }

                config.type == TileType.WIDGET || pkg.isSettingsApp() -> {
                    SystemWidget(config, contentColor, accentColor)
                }

                else -> {
                    StandardAppTile(config = config, contentColor = contentColor)
                }
            }
        }
    }
}

/* ───────── HELPERS ───────── */

private fun String.isWeatherApp() =
    contains("weather") || contains("meteo") || this == "com.google.android.apps.weather"

private fun String.isSettingsApp() =
    this == "com.android.settings" || endsWith(".settings")

private fun String.isMusicApp() =
    contains("music") || contains("spotify")

private fun String.isCalendarApp() =
    contains("calendar")

private fun String.isClockApp() =
    contains("clock")
