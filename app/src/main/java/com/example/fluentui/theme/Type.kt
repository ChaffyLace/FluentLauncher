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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

data class FluentColors(
    val background: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val accent: Color,
    val border: Color
)

val LightFluentColors = FluentColors(
    background = Color(0xFFF3F3F3),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFF9F9F9),
    textPrimary = Color(0xFF1E1E1E),
    textSecondary = Color(0xFF5E5E5E),
    accent = Color(0xFF0078D4),
    border = Color(0x14000000)
)

val DarkFluentColors = FluentColors(
    background = Color(0xFF202020),
    surface = Color(0xFF2C2C2C),
    surfaceVariant = Color(0xFF383838),
    textPrimary = Color(0xFFFFFFFF),
    textSecondary = Color(0xFFD0D0D0),
    accent = Color(0xFF60CDFF),
    border = Color(0x14FFFFFF)
)

val LocalFluentColors = staticCompositionLocalOf { LightFluentColors }

@Composable
fun FluentUITheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val fluentColors = if (darkTheme) DarkFluentColors else LightFluentColors

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> darkColorScheme(
            primary = fluentColors.accent,
            onPrimary = Color.Black,
            background = fluentColors.background,
            surface = fluentColors.surface,
            onSurface = fluentColors.textPrimary
        )
        else -> lightColorScheme(
            primary = fluentColors.accent,
            onPrimary = Color.White,
            background = fluentColors.background,
            surface = fluentColors.surface,
            onSurface = fluentColors.textPrimary
        )
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    CompositionLocalProvider(LocalFluentColors provides fluentColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = FluentTypography,
            content = content
        )
    }
}

object FluentTheme {
    val colors: FluentColors
        @Composable
        get() = LocalFluentColors.current
    }