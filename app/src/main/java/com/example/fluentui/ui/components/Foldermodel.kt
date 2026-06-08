package com.example.fluentui.ui.components

import java.util.UUID

// =============================================================================
// FOLDER MODEL
// =============================================================================

data class AppFolder(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "Dossier",
    val apps: List<AppModel> = emptyList()
)

// Un item dans la grille peut être soit une app, soit un dossier
sealed class GridItem {
    data class App(val app: AppModel)       : GridItem()
    data class Folder(val folder: AppFolder) : GridItem()
}