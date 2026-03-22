package com.example.fluentui.ui.database

import com.example.fluentui.ui.system.TileConfig
import com.example.fluentui.ui.system.TileSize
import com.example.fluentui.ui.system.TileType
import kotlinx.coroutines.flow.Flow

/**
 * Abstraction layer between ViewModel and Room.
 * Handles the conversion between UI models (TileConfig) and database entities (TileEntity).
 */
class TileRepository(private val dao: TileDao) {

    val tilesFlow: Flow<List<TileEntity>> = dao.observeAll()

    suspend fun upsert(tile: TileConfig) {
        dao.upsert(tile.toEntity())
    }

    suspend fun upsertAll(tiles: List<TileConfig>) {
        dao.upsertAll(tiles.map { it.toEntity() })
    }

    suspend fun delete(id: String) {
        dao.deleteById(id)
    }

    suspend fun deleteAll() {
        dao.deleteAll()
    }

    suspend fun count(): Int = dao.count()

    // ── Conversion TileConfig → TileEntity ───────────────────────────────────
    private fun TileConfig.toEntity() = TileEntity(
        id          = id,
        title       = title,
        packageName = packageName,
        type        = type.name,
        size        = size.name,
        gridX       = gridX,
        gridY       = gridY,
        isDynamic   = isDynamic
    )

    // ── Conversion TileEntity → TileConfig ───────────────────────────────────
    fun entityToConfig(entity: TileEntity): TileConfig? {
        val tileType = try {
            TileType.valueOf(entity.type)
        } catch (e: Exception) {
            return null
        }

        val tileSize = try {
            TileSize.valueOf(entity.size)
        } catch (e: Exception) {
            TileSize.SMALL
        }

        return TileConfig(
            id          = entity.id,
            title       = entity.title,
            packageName = entity.packageName,
            type        = tileType,
            size        = tileSize,
            icon        = null,
            gridX       = entity.gridX,
            gridY       = entity.gridY,
            isDynamic   = entity.isDynamic
        )
    }
}