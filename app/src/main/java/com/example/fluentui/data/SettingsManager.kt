package com.example.fluentui.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.example.fluentui.R // Imported to access default app resources
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Create a single instance of DataStore for the application to prevent memory leaks
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/**
 * SettingsManager
 *
 * Handles persistent local storage of user preferences using Jetpack DataStore.
 * Provides reactive data streams (Flow) for UI observation and suspend functions for asynchronous writes.
 */
class SettingsManager(private val context: Context) {

    companion object {
        // Define DataStore keys
        val COLUMN_COUNT = intPreferencesKey("column_count")
        val ROW_COUNT = intPreferencesKey("row_count")
        val ICON_SIZE = floatPreferencesKey("icon_size")
        val SHOW_LABELS = booleanPreferencesKey("show_labels")
        val IS_VERTICAL_SCROLL = booleanPreferencesKey("is_vertical_scroll")
        val ACCENT_COLOR = intPreferencesKey("accent_color")
        val TRANSPARENCY_ENABLED = booleanPreferencesKey("transparency_enabled")
        val FONT_SIZE = floatPreferencesKey("font_size")
        val VERTICAL_SPACING = floatPreferencesKey("vertical_spacing")
        val HIDDEN_APPS = stringSetPreferencesKey("hidden_apps")
        val SAVED_CITY = stringPreferencesKey("saved_city")
        val WALLPAPER_TYPE = stringPreferencesKey("wallpaper_type")
        val WALLPAPER_VALUE = stringPreferencesKey("wallpaper_value")
    }

    // --- FLOWS (Read Operations) ---
    // Expose preferences as flows with robust production-ready fallback values (OOBE defaults)

    val accentColor: Flow<Int> = context.dataStore.data.map { it[ACCENT_COLOR] ?: 0xFF60CDFF.toInt() }
    val transparencyEnabled: Flow<Boolean> = context.dataStore.data.map { it[TRANSPARENCY_ENABLED] ?: true }
    val columnCount: Flow<Int> = context.dataStore.data.map { it[COLUMN_COUNT] ?: 4 }
    val rowCount: Flow<Int> = context.dataStore.data.map { it[ROW_COUNT] ?: 6 }
    val iconSize: Flow<Float> = context.dataStore.data.map { it[ICON_SIZE] ?: 60f }
    val showLabels: Flow<Boolean> = context.dataStore.data.map { it[SHOW_LABELS] ?: true }

    // FIX: DEFAULT TO TRUE. Ensures the app drawer opens in a vertical list mode during the first launch.
    val isVerticalScroll: Flow<Boolean> = context.dataStore.data.map { it[IS_VERTICAL_SCROLL] ?: true }

    val fontSize: Flow<Float> = context.dataStore.data.map { it[FONT_SIZE] ?: 13f }
    val verticalSpacing: Flow<Float> = context.dataStore.data.map { it[VERTICAL_SPACING] ?: 16f }
    val hiddenApps: Flow<Set<String>> = context.dataStore.data.map { it[HIDDEN_APPS] ?: emptySet() }
    val savedCity: Flow<String?> = context.dataStore.data.map { it[SAVED_CITY] }

    // FIX: DEFAULTS TO RES. Ensures a premium built-in wallpaper is shown instead of a black screen.
    val wallpaperType: Flow<String> = context.dataStore.data.map { it[WALLPAPER_TYPE] ?: "RES" }
    val wallpaperValue: Flow<String> = context.dataStore.data.map {
        it[WALLPAPER_VALUE] ?: R.drawable.windows_11_1_mattbirchler.toString()
    }

    // --- SETTERS (Write Operations) ---

    suspend fun setAccentColor(color: Int) { context.dataStore.edit { it[ACCENT_COLOR] = color } }
    suspend fun setTransparencyEnabled(enabled: Boolean) { context.dataStore.edit { it[TRANSPARENCY_ENABLED] = enabled } }
    suspend fun setColumnCount(count: Int) { context.dataStore.edit { it[COLUMN_COUNT] = count } }
    suspend fun setRowCount(count: Int) { context.dataStore.edit { it[ROW_COUNT] = count } }
    suspend fun setIconSize(size: Float) { context.dataStore.edit { it[ICON_SIZE] = size } }
    suspend fun setShowLabels(show: Boolean) { context.dataStore.edit { it[SHOW_LABELS] = show } }
    suspend fun setVerticalScroll(isVertical: Boolean) { context.dataStore.edit { it[IS_VERTICAL_SCROLL] = isVertical } }
    suspend fun setFontSize(size: Float) { context.dataStore.edit { it[FONT_SIZE] = size } }
    suspend fun setVerticalSpacing(spacing: Float) { context.dataStore.edit { it[VERTICAL_SPACING] = spacing } }

    suspend fun setAppHidden(packageName: String, hidden: Boolean) {
        context.dataStore.edit { prefs ->
            val current = prefs[HIDDEN_APPS] ?: emptySet()
            if (hidden) {
                prefs[HIDDEN_APPS] = current + packageName
            } else {
                prefs[HIDDEN_APPS] = current - packageName
            }
        }
    }

    suspend fun setWallpaper(type: String, value: String) {
        context.dataStore.edit {
            it[WALLPAPER_TYPE] = type
            it[WALLPAPER_VALUE] = value
        }
    }

    suspend fun saveCity(city: String) { context.dataStore.edit { it[SAVED_CITY] = city } }

    // Aliases for consistency
    suspend fun setTransparency(enabled: Boolean) { setTransparencyEnabled(enabled) }
    suspend fun setHiddenApps(apps: Set<String>) { context.dataStore.edit { it[HIDDEN_APPS] = apps } }
    suspend fun setWallpaperType(type: String) { context.dataStore.edit { it[WALLPAPER_TYPE] = type } }
    suspend fun setWallpaperValue(value: String) { context.dataStore.edit { it[WALLPAPER_VALUE] = value } }
}