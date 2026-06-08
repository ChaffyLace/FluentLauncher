package com.example.fluentui.ui.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FolderDao {
    @Query("SELECT * FROM drawer_folders")
    fun observeAll(): Flow<List<FolderEntity>>

    @Query("SELECT id FROM drawer_folders")
    suspend fun getAllIds(): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(folders: List<FolderEntity>)

    @Query("DELETE FROM drawer_folders WHERE id = :id")
    suspend fun deleteById(id: String)
}