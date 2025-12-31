package com.example.fluentui.ui.components

import android.app.Application
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.core.graphics.drawable.toBitmap
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Collections

// Extension pour DataStore
private val Application.appOrderDataStore by preferencesDataStore("app_order")

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val pm: PackageManager = application.packageManager
    private val dataStore = application.appOrderDataStore

    private val _otherApps = mutableStateOf<List<AppModel>>(emptyList())
    val otherApps: State<List<AppModel>> = _otherApps

    private val _pinnedApps = mutableStateOf<List<AppModel>>(emptyList())
    val pinnedApps: State<List<AppModel>> = _pinnedApps

    init {
        loadApps()
    }

    /**
     * Charge toutes les apps depuis le PackageManager
     * et applique l'ordre sauvegardé si disponible
     */
    private fun loadApps() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // 1. Récupérer toutes les apps
                val intent = Intent(Intent.ACTION_MAIN, null).apply {
                    addCategory(Intent.CATEGORY_LAUNCHER)
                }
                val activities = pm.queryIntentActivities(intent, 0)

                // 2. Charger l'ordre sauvegardé
                val savedOrder = loadAppOrder()

                // 3. Convertir en AppModel
                val appsMap = activities.associate { resolveInfo ->
                    val pkg = resolveInfo.activityInfo.packageName
                    pkg to toAppModel(resolveInfo)
                }

                // 4. Appliquer l'ordre sauvegardé OU ordre alphabétique par défaut
                val orderedApps = if (savedOrder.isNotEmpty()) {
                    // Utiliser l'ordre sauvegardé
                    val orderedList = savedOrder.mapNotNull { pkg -> appsMap[pkg] }

                    // Ajouter les nouvelles apps à la fin (pas dans l'ordre sauvegardé)
                    val newApps = appsMap.values.filter { app ->
                        app.packageName !in savedOrder
                    }.sortedBy { it.label.lowercase() }

                    orderedList + newApps
                } else {
                    // Premier lancement : ordre alphabétique
                    appsMap.values.sortedBy { it.label.lowercase() }
                }

                // 5. Sauvegarder l'ordre initial si c'était vide
                if (savedOrder.isEmpty()) {
                    saveAppOrder(orderedApps.map { it.packageName })
                }

                withContext(Dispatchers.Main) {
                    _otherApps.value = orderedApps
                    _pinnedApps.value = orderedApps.take(8)
                }

                Log.d("AppViewModel", "Loaded ${orderedApps.size} apps")
            } catch (e: Exception) {
                Log.e("AppViewModel", "Error loading apps", e)
            }
        }
    }

    /**
     * Déplace une app de fromIndex vers toIndex
     * et sauvegarde immédiatement le nouvel ordre
     */
    fun moveApp(fromIndex: Int, toIndex: Int) {
        val currentList = _otherApps.value.toMutableList()

        if (fromIndex !in currentList.indices || toIndex !in currentList.indices) {
            Log.w("AppViewModel", "Invalid indices: from=$fromIndex, to=$toIndex, size=${currentList.size}")
            return
        }

        // Déplacement intelligent (comme iOS)
        if (fromIndex < toIndex) {
            for (i in fromIndex until toIndex) {
                Collections.swap(currentList, i, i + 1)
            }
        } else {
            for (i in fromIndex downTo toIndex + 1) {
                Collections.swap(currentList, i, i - 1)
            }
        }

        // Mise à jour du state
        _otherApps.value = currentList

        // Sauvegarde asynchrone
        viewModelScope.launch(Dispatchers.IO) {
            saveAppOrder(currentList.map { it.packageName })
            Log.d("AppViewModel", "App order saved: moved $fromIndex -> $toIndex")
        }
    }

    /**
     * Réinitialise l'ordre alphabétique
     */
    fun resetOrder() {
        viewModelScope.launch(Dispatchers.IO) {
            val sorted = _otherApps.value.sortedBy { it.label.lowercase() }
            withContext(Dispatchers.Main) {
                _otherApps.value = sorted
            }
            saveAppOrder(sorted.map { it.packageName })
            Log.d("AppViewModel", "Order reset to alphabetical")
        }
    }

    /**
     * Sauvegarde l'ordre des apps dans DataStore
     */
    private suspend fun saveAppOrder(packageNames: List<String>) {
        try {
            dataStore.edit { prefs ->
                prefs[APP_ORDER_KEY] = packageNames.joinToString(",")
            }
        } catch (e: Exception) {
            Log.e("AppViewModel", "Error saving app order", e)
        }
    }

    /**
     * Charge l'ordre sauvegardé depuis DataStore
     */
    private suspend fun loadAppOrder(): List<String> {
        return try {
            val prefs = dataStore.data.first()
            val orderString = prefs[APP_ORDER_KEY] ?: ""
            if (orderString.isBlank()) emptyList()
            else orderString.split(",")
        } catch (e: Exception) {
            Log.e("AppViewModel", "Error loading app order", e)
            emptyList()
        }
    }

    private fun toAppModel(resolveInfo: ResolveInfo): AppModel {
        val packageName = resolveInfo.activityInfo.packageName
        val label = resolveInfo.loadLabel(pm).toString()

        // --- CORRECTION ICI ---
        // On convertit le Drawable en Bitmap
        val icon = resolveInfo.loadIcon(pm).toBitmap()

        return AppModel(label, packageName, icon)
    }

    companion object {
        private val APP_ORDER_KEY = stringPreferencesKey("app_order")
    }
}