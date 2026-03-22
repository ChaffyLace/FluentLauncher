package com.example.fluentui.ui.system

import android.graphics.drawable.Drawable

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
// TILE SIZE
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
    // Icône système Android — ViewModel charge le Drawable via PackageManager
    data class AndroidIcon(val drawable: Drawable) : TileIcon()
    // Ressource locale (R.drawable.xxx)
    data class ResourceIcon(val resId: Int) : TileIcon()
    // URI externe (image choisie par l'utilisateur)
    data class UriIcon(val uri: String) : TileIcon()
}

// ============================================================================
// TILE CONFIG
//
// IMPORTANT — pas @Immutable ici.
// @Immutable dit à Compose de skip la recomposition si la référence n'a pas
// changé. Or gridX et gridY sont mutés directement par DashboardGrid via une
// extension property — Compose ne verrait jamais les changements et les tuiles
// ne se repositionneraient pas après un drag.
//
// On utilise @Stable à la place : Compose sait que les lectures de champs
// notifient les snapshots, mais reste libre de recomposer quand les valeurs
// changent. C'est le contrat correct pour un objet mutable-mais-observé.
// ============================================================================

@androidx.compose.runtime.Stable
data class TileConfig(
    val id:          String,
    val title:       String,
    val icon:        TileIcon? = null,
    val size:        TileSize  = TileSize.SMALL,
    val type:        TileType  = TileType.APPLICATION,
    val packageName: String    = "",
    var gridX:       Int       = -1,
    var gridY:       Int       = -1,
    val isDynamic:   Boolean   = true
) {
    fun move(x: Int, y: Int): TileConfig = copy(gridX = x, gridY = y)

    val label: String get() = title
}