package com.example.fluentui.ui.components

import android.app.Application
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.BitmapDrawable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.core.graphics.drawable.toBitmap
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

private val Application.appDataStore by preferencesDataStore("launcher_data_final")

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val pm: PackageManager = application.packageManager

    // Le tiroir d'applications (Lecture seule, on ne touche pas)
    val otherApps = mutableStateOf<List<AppModel>>(emptyList())

    // Le Dashboard (Modifiable : contient des Apps, des Widgets ET DES VIDES)
    val dashboardTiles = mutableStateListOf<TileConfig>()

    init {
        loadInstalledApps()
        loadDashboard()
    }

    private fun loadInstalledApps() {
        viewModelScope.launch(Dispatchers.IO) {
            val intent = Intent(Intent.ACTION_MAIN, null).apply { addCategory(Intent.CATEGORY_LAUNCHER) }
            val activities = pm.queryIntentActivities(intent, 0)
            val apps = activities.map { resolveInfo ->
                AppModel(
                    resolveInfo.loadLabel(pm).toString(),
                    resolveInfo.activityInfo.packageName,
                    resolveInfo.loadIcon(pm).toBitmap()
                )
            }.sortedBy { it.label.lowercase() }
            withContext(Dispatchers.Main) { otherApps.value = apps }
        }
    }

    private fun loadDashboard() {
        viewModelScope.launch(Dispatchers.IO) {
            // ON CRÉE UNE GRILLE FIXE DE 24 CASES (4 colonnes x 6 lignes)
            // On remplit tout avec du VIDE (EMPTY) par défaut
            val tiles = MutableList(24) {
                TileConfig(UUID.randomUUID().toString(), "", size = TileSize.SMALL, type = TileType.EMPTY)
            }

            // On place les widgets par défaut au début
            val widgets = getHardcodedWidgets()
            widgets.forEachIndexed { index, widget ->
                if (index < tiles.size) tiles[index] = widget
            }

            withContext(Dispatchers.Main) {
                dashboardTiles.clear()
                dashboardTiles.addAll(tiles)
            }
        }
    }

    // --- ACTIONS WINDOWS ---

    // 1. Épingler (Remplace la première case vide trouvée)
    fun pinApp(app: AppModel) {
        val emptyIndex = dashboardTiles.indexOfFirst { it.type == TileType.EMPTY }
        if (emptyIndex != -1) {
            val iconDrawable = if (app.icon != null) BitmapDrawable(getApplication<Application>().resources, app.icon) else pm.getDefaultActivityIcon()

            dashboardTiles[emptyIndex] = TileConfig(
                id = UUID.randomUUID().toString(),
                title = app.label,
                icon = TileIcon.AndroidIcon(iconDrawable),
                size = TileSize.SMALL,
                type = TileType.APP,
                packageName = app.packageName
            )
        }
    }

    // 2. Drag & Drop (Echange deux cases, même si l'une est vide)
    fun swapTiles(fromIndex: Int, toIndex: Int) {
        if (fromIndex in dashboardTiles.indices && toIndex in dashboardTiles.indices && fromIndex != toIndex) {
            val fromItem = dashboardTiles[fromIndex]
            val toItem = dashboardTiles[toIndex]
            dashboardTiles[fromIndex] = toItem
            dashboardTiles[toIndex] = fromItem
        }
    }

    // 3. Redimensionner (Via le menu contextuel)
    fun resizeTile(id: String, newSize: TileSize) {
        val index = dashboardTiles.indexOfFirst { it.id == id }
        if (index != -1) {
            dashboardTiles[index] = dashboardTiles[index].copy(size = newSize)
        }
    }

    // 4. Désépingler (Remplace la tuile par du VIDE)
    fun unpinTile(id: String) {
        val index = dashboardTiles.indexOfFirst { it.id == id }
        if (index != -1) {
            dashboardTiles[index] = TileConfig(UUID.randomUUID().toString(), "", size = TileSize.SMALL, type = TileType.EMPTY)
        }
    }
}