package com.example.fluentui.data

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore by preferencesDataStore(name = "fluent_launcher_prefs")

class SettingsManager(private val context: Context) {

    companion object {
        val COLUMN_COUNT = intPreferencesKey("column_count")
        val ROW_COUNT = intPreferencesKey("row_count") // NOUVEAU
        val ICON_SIZE = floatPreferencesKey("icon_size")
        val SHOW_LABELS = booleanPreferencesKey("show_labels")
        val ACCENT_COLOR = intPreferencesKey("accent_color")
        val IS_VERTICAL_SCROLL = booleanPreferencesKey("is_vertical_scroll")
    }

    val columnCount: Flow<Int> = context.dataStore.data.map { it[COLUMN_COUNT] ?: 4 }
    val rowCount: Flow<Int> = context.dataStore.data.map { it[ROW_COUNT] ?: 6 }
    val iconSize: Flow<Float> = context.dataStore.data.map { it[ICON_SIZE] ?: 60f }
    val showLabels: Flow<Boolean> = context.dataStore.data.map { it[SHOW_LABELS] ?: true }
    val accentColor: Flow<Int> = context.dataStore.data.map { it[ACCENT_COLOR] ?: 0xFF0078D4.toInt() }
    val isVerticalScroll: Flow<Boolean> = context.dataStore.data.map { it[IS_VERTICAL_SCROLL] ?: false }

    suspend fun setColumnCount(count: Int) { context.dataStore.edit { it[COLUMN_COUNT] = count } }
    suspend fun setRowCount(count: Int) { context.dataStore.edit { it[ROW_COUNT] = count } }
    suspend fun setIconSize(size: Float) { context.dataStore.edit { it[ICON_SIZE] = size } }
    suspend fun setShowLabels(show: Boolean) { context.dataStore.edit { it[SHOW_LABELS] = show } }
    suspend fun setAccentColor(color: Int) { context.dataStore.edit { it[ACCENT_COLOR] = color } }
    suspend fun setVerticalScroll(isVertical: Boolean) { context.dataStore.edit { it[IS_VERTICAL_SCROLL] = isVertical } }
}