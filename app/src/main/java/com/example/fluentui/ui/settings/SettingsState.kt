package com.example.fluentui.ui.settings

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import com.example.fluentui.R

data class SettingsState(
    val columnCount: Int = 4,
    val rowCount: Int = 6,
    val iconSize: Float = 60f,
    val fontSize: Float = 13f,
    val verticalSpacing: Float = 16f,
    val showLabels: Boolean = true,
    val accentColor: Color = Color(0xFF60CDFF),
    val transparencyEnabled: Boolean = true,
    val isVertical: Boolean = true,
    val wallpaperType: String = "RES",
    val wallpaperValue: String = R.drawable.windows_11_1_mattbirchler.toString(),
    val currentWallpaperBitmap: ImageBitmap? = null,
    val isLoadingWallpaper: Boolean = false,
    val hiddenApps: Set<String> = emptySet()
)

enum class SettingsPageType {
    HOME, BACKGROUND, COLOR, LAYOUT
}