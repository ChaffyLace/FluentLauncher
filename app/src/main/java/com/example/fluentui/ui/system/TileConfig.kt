package com.example.fluentui.ui.system

import android.graphics.drawable.Drawable
import androidx.compose.runtime.Immutable

enum class TileType {
    APPLICATION, WEATHER, CLOCK, BATTERY, CALENDAR, MAIL, WIDGET, SHORTCUT, FOLDER, TIMER, STOPWATCH
}

enum class TileSize(val cols: Int, val rows: Int) {
    TINY(1, 1),
    SMALL(2, 2),
    MEDIUM(4, 2),
    LARGE(4, 4)
}

sealed class TileIcon {
    data class AndroidIcon(val drawable: Drawable) : TileIcon()
}

@Immutable // Optimisation pour Jetpack Compose
data class TileConfig(
    val id: String,
    val title: String,
    val icon: TileIcon? = null,
    val size: TileSize = TileSize.SMALL,
    val type: TileType = TileType.APPLICATION,
    val packageName: String = "",
    var gridX: Int = -1,
    var gridY: Int = -1
) {

    val label: String
        get() = title
}