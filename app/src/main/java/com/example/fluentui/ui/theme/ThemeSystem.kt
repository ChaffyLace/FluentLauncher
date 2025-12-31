package com.example.fluentui.ui.theme

import androidx.compose.ui.graphics.Color

enum class AppTheme { WINDOWS_DARK, WINDOWS_LIGHT, OLED_BLACK }

data class ThemeColors(
    val background: Color,
    val tileBackground: Color,
    val tileBorder: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val accent: Color
)

fun AppTheme.getColors(isSystemDark: Boolean): ThemeColors {
    val actualTheme = if (isSystemDark) AppTheme.WINDOWS_DARK else AppTheme.WINDOWS_LIGHT

    return when (actualTheme) {
        AppTheme.WINDOWS_DARK -> ThemeColors(
            background = Color.Transparent,
            tileBackground = Color(0xFF202020),
            tileBorder = Color.White.copy(alpha = 0.1f),
            textPrimary = Color.White.copy(0.95f),
            textSecondary = Color.White.copy(0.7f),
            textTertiary = Color.White.copy(0.5f),
            accent = Color(0xFF60CDFF)
        )
        AppTheme.WINDOWS_LIGHT -> ThemeColors(
            background = Color.Transparent,
            tileBackground = Color(0xFFF3F3F3),
            tileBorder = Color.Black.copy(alpha = 0.1f),
            textPrimary = Color.Black.copy(0.9f),
            textSecondary = Color.Black.copy(0.7f),
            textTertiary = Color.Black.copy(0.5f),
            accent = Color(0xFF0078D4)
        )
        AppTheme.OLED_BLACK -> ThemeColors(
            background = Color.Black,
            tileBackground = Color(0xFF101010),
            tileBorder = Color(0xFF333333),
            textPrimary = Color.White,
            textSecondary = Color.Gray,
            textTertiary = Color.DarkGray,
            accent = Color(0xFF60CDFF)
        )
    }
}