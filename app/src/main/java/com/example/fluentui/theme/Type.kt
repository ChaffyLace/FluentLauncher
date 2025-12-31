package com.example.fluentui.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val FluentBlue = Color(0xFF0078D4)
private val DarkBackground = Color(0xFF202020)
private val LightBackground = Color(0xFFF3F3F3)

private val DarkColorScheme = darkColorScheme(
    primary = FluentBlue,
    secondary = FluentBlue,
    tertiary = Color(0xFF60CDFF),
    background = DarkBackground,
    surface = Color(0xFF2D2D2D)
)

private val LightColorScheme = lightColorScheme(
    primary = FluentBlue,
    secondary = FluentBlue,
    tertiary = Color(0xFF005A9E),
    background = LightBackground,
    surface = Color(0xFFFFFFFF)
)

@Composable
fun FluentUITheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = FluentTypography,
        content = content
    )
}