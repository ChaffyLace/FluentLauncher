package com.example.fluentui.ui.system

import android.graphics.drawable.Drawable
import androidx.compose.runtime.Stable

// ============================================================================
// TILE TYPE
// ============================================================================

enum class TileType {
    APPLICATION,
    WEATHER,
    CLOCK,
    BATTERY,
    CALENDAR,
    MAIL,
    WIDGET,
    SHORTCUT,
    FOLDER,
    PHOTO,
    MEDIA
}

// ============================================================================
// TILE SIZE (GRID BASED)
// ============================================================================

enum class TileSize(val cols: Int, val rows: Int) {
    TINY(1, 1),
    SMALL(2, 2),
    MEDIUM(4, 2),
    LARGE(4, 4)
}

// ============================================================================
// TILE ICON
// ============================================================================

sealed class TileIcon {
    data class AndroidIcon(val drawable: Drawable) : TileIcon()
    data class ResourceIcon(val resId: Int) : TileIcon()
    data class UriIcon(val uri: String) : TileIcon()
}


@Stable
data class TileConfig(
    val id: String,
    val title: String,

    val icon: TileIcon? = null,
    val type: TileType = TileType.APPLICATION,

    val packageName: String = "",

    val size: TileSize = TileSize.SMALL,

    var gridX: Int = -1,
    var gridY: Int = -1,
    val zone: String = "dashboard",

    val isDynamic: Boolean = true
) {

    // ───────── GRID HELPERS ─────────

    val width: Int
        get() = size.cols

    val height: Int
        get() = size.rows

    val isSmall: Boolean
        get() = width == 1 && height == 1

    val isMedium: Boolean
        get() = width == 2 && height == 2

    val isLarge: Boolean
        get() = width >= 4 || height >= 4

    // ───────── DISPLAY ─────────

    val label: String
        get() = title

    val normalizedPackage: String
        get() = packageName.lowercase().trim()

    // ───────── ACTIONS ─────────

    fun move(x: Int, y: Int): TileConfig = copy(
        gridX = x,
        gridY = y
    )
}