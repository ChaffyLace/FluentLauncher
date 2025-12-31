package com.example.fluentui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

data class FluentColors(
    val background: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val accent: Color,
    val accentVariant: Color,
    )

val lightFluentColors = FluentColors(
    background = Color(0xFFF3F3F3),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFF9F9F9),
    textPrimary = Color(0xFF1E1E1E),
    textSecondary = Color(0xFF5E5E5E),
    accent = Color(0xFF0078D4),
    accentVariant = Color(0xFF005A9E)
)

val darkFluentColors = FluentColors(
    background = Color(0xFF1B1B1B),
    surface = Color(0xFF2A2A2A),
    surfaceVariant = Color(0xFF3C3C3C),
    textPrimary = Color(0xFFFFFFFF),
    textSecondary = Color(0xFFB3B3B3),
    accent = Color(0xFF4CC2FF),
    accentVariant = Color(0xFF0078D4)
)

val LocalFluentColors = staticCompositionLocalOf { lightFluentColors }

@Composable
fun FluentTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) darkFluentColors else lightFluentColors

    CompositionLocalProvider(LocalFluentColors provides colors) {
        content()
    }
}

object FluentTheme {
    val colors: FluentColors
        @Composable
        get() = LocalFluentColors.current
}