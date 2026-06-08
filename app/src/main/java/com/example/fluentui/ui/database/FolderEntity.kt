package com.example.fluentui.ui.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.fluentui.ui.components.AppModel

@Entity(tableName = "drawer_folders")
data class FolderEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val apps: List<AppModel>
)