package com.example.fluentui.ui.database

import com.example.fluentui.ui.components.AppFolder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class FolderRepository(private val dao: FolderDao) {

    // Transforme le flux de la base de données en flux pour l'UI
    val foldersFlow: Flow<List<AppFolder>> = dao.observeAll().map { entities ->
        entities.map { it.toAppFolder() }
    }

    // Synchronisation intelligente : ajoute les nouveaux, met à jour les modifiés, supprime les anciens
    suspend fun syncFolders(currentFolders: List<AppFolder>) {
        val dbIds = dao.getAllIds()
        val currentIds = currentFolders.map { it.id }

        // 1. Supprimer ceux qui ne sont plus dans l'UI (Dossier dissout ou supprimé)
        val toDelete = dbIds.filter { it !in currentIds }
        toDelete.forEach { dao.deleteById(it) }

        // 2. Sauvegarder l'état actuel
        dao.upsertAll(currentFolders.map { it.toEntity() })
    }

    private fun AppFolder.toEntity() = FolderEntity(id = id, name = name, apps = apps)
    private fun FolderEntity.toAppFolder() = AppFolder(id = id, name = name, apps = apps)
}