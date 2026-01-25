package com.example.fluentui.ui.system

import android.graphics.drawable.Drawable

enum class TileType {
    APPLICATION, WEATHER, CLOCK, BATTERY, CALENDAR, MAIL
}

enum class TileSize(val cols: Int, val rows: Int) {
    TINY(1, 1), SMALL(2, 2), MEDIUM(4, 2), LARGE(4, 4)
}

sealed class TileIcon {
    data class AndroidIcon(val drawable: Drawable) : TileIcon()
}

data class TileConfig(
    val id: String,
    val title: String,
    val icon: TileIcon? = null,
    val size: TileSize = TileSize.SMALL,
    val type: TileType = TileType.APPLICATION,
    val packageName: String = "",
    var col: Int = 0,
    var row: Int = 0
)