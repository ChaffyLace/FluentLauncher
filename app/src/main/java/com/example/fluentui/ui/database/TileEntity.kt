package com.example.fluentui.ui.database

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Database entity representing a Tile on the dashboard.
 */
@Entity(tableName = "tiles")
data class TileEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val packageName: String,
    val type: String,
    val size: String,
    val gridX: Int,
    val gridY: Int,
    val isDynamic: Boolean = true
)