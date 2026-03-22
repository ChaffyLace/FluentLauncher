package com.example.fluentui.ui.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for TileEntity.
 */
@Dao
interface TileDao {
    @Query("SELECT * FROM tiles")
    fun observeAll(): Flow<List<TileEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(tile: TileEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(tiles: List<TileEntity>)

    @Query("DELETE FROM tiles WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM tiles")
    suspend fun deleteAll()

    @Query("SELECT COUNT(id) FROM tiles")
    suspend fun count(): Int
}