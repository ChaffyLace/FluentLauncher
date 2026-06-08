package com.example.fluentui.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.example.fluentui.R
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsManager(private val context: Context) {

    companion object {
        val COLUMN_COUNT = intPreferencesKey("column_count")
        val ROW_COUNT = intPreferencesKey("row_count")
        val ICON_SIZE = floatPreferencesKey("icon_size")
        val SHOW_LABELS = booleanPreferencesKey("show_labels")
        val IS_VERTICAL_SCROLL = booleanPreferencesKey("is_vertical_scroll")
        val ACCENT_COLOR = intPreferencesKey("accent_color")
        val TRANSPARENCY_ENABLED = booleanPreferencesKey("transparency_enabled")
        val FONT_SIZE = floatPreferencesKey("font_size")
        val VERTICAL_SPACING = floatPreferencesKey("vertical_spacing")

        val SHOW_DRAWER_SEARCH = booleanPreferencesKey("show_drawer_search")
        val AUTO_OPEN_KEYBOARD = booleanPreferencesKey("auto_open_keyboard")
        val SHOW_BADGES = booleanPreferencesKey("show_badges")
        val SWIPE_DOWN_ENABLED = booleanPreferencesKey("swipe_down_enabled")
        val DOUBLE_TAP_TO_SLEEP = booleanPreferencesKey("double_tap_to_sleep")

        val HIDDEN_APPS = stringSetPreferencesKey("hidden_apps")
        val SECURE_HIDDEN_APPS = booleanPreferencesKey("secure_hidden_apps")
        val SAVED_CITY = stringPreferencesKey("saved_city")
        val WALLPAPER_TYPE = stringPreferencesKey("wallpaper_type")
        val WALLPAPER_VALUE = stringPreferencesKey("wallpaper_value")

        val HOME_MODE = stringPreferencesKey("home_mode")

        // Added key for the Global Action Center overlay
        val GLOBAL_ACTION_CENTER_ENABLED = booleanPreferencesKey("global_action_center_enabled")

        val KEY_LAUNCHER_LAYOUT = stringPreferencesKey("launcher_layout")

    }

    val accentColor: Flow<Int> =
        context.dataStore.data.map { it[ACCENT_COLOR] ?: 0xFF60CDFF.toInt() }

    val transparencyEnabled: Flow<Boolean> =
        context.dataStore.data.map { it[TRANSPARENCY_ENABLED] ?: true }

    val columnCount: Flow<Int> =
        context.dataStore.data.map { it[COLUMN_COUNT] ?: 4 }

    val rowCount: Flow<Int> =
        context.dataStore.data.map { it[ROW_COUNT] ?: 6 }

    val iconSize: Flow<Float> =
        context.dataStore.data.map { it[ICON_SIZE] ?: 60f }

    val showLabels: Flow<Boolean> =
        context.dataStore.data.map { it[SHOW_LABELS] ?: true }

    val isVerticalScroll: Flow<Boolean> =
        context.dataStore.data.map { it[IS_VERTICAL_SCROLL] ?: true }

    val fontSize: Flow<Float> =
        context.dataStore.data.map { it[FONT_SIZE] ?: 13f }

    val verticalSpacing: Flow<Float> =
        context.dataStore.data.map { it[VERTICAL_SPACING] ?: 16f }

    val showDrawerSearch: Flow<Boolean> =
        context.dataStore.data.map { it[SHOW_DRAWER_SEARCH] ?: true }

    val autoOpenKeyboard: Flow<Boolean> =
        context.dataStore.data.map { it[AUTO_OPEN_KEYBOARD] ?: false }

    val showBadges: Flow<Boolean> =
        context.dataStore.data.map { it[SHOW_BADGES] ?: true }

    val swipeDownEnabled: Flow<Boolean> =
        context.dataStore.data.map { it[SWIPE_DOWN_ENABLED] ?: true }
    val secureHiddenApps: Flow<Boolean> =
        context.dataStore.data.map { it[SECURE_HIDDEN_APPS] ?: false }

    val hiddenApps: Flow<Set<String>> =
        context.dataStore.data.map { it[HIDDEN_APPS] ?: emptySet() }

    val savedCity: Flow<String?> =
        context.dataStore.data.map { it[SAVED_CITY] }

    val wallpaperType: Flow<String> =
        context.dataStore.data.map { it[WALLPAPER_TYPE] ?: "RES" }

    val wallpaperValue: Flow<String> =
        context.dataStore.data.map {
            it[WALLPAPER_VALUE] ?: R.drawable.windows_11_1_mattbirchler.toString()
        }
    val launcherLayout: Flow<String> = context.dataStore.data.map {
        it[KEY_LAUNCHER_LAYOUT] ?: "FLUENT_TILES"
    }

    val homeMode: Flow<String> =
        context.dataStore.data.map { it[HOME_MODE] ?: "TILES" }


    // Flow exposing the global action center state. Default is false.
    val globalActionCenterEnabled: Flow<Boolean> =
        context.dataStore.data.map { it[GLOBAL_ACTION_CENTER_ENABLED] ?: false }

    suspend fun setAccentColor(color: Int) =
        context.dataStore.edit { it[ACCENT_COLOR] = color }

    suspend fun setTransparencyEnabled(enabled: Boolean) =
        context.dataStore.edit { it[TRANSPARENCY_ENABLED] = enabled }

    suspend fun setColumnCount(count: Int) =
        context.dataStore.edit { it[COLUMN_COUNT] = count }

    suspend fun setRowCount(count: Int) =
        context.dataStore.edit { it[ROW_COUNT] = count }

    suspend fun setIconSize(size: Float) =
        context.dataStore.edit { it[ICON_SIZE] = size }

    suspend fun setShowLabels(show: Boolean) =
        context.dataStore.edit { it[SHOW_LABELS] = show }

    suspend fun setVerticalScroll(isVertical: Boolean) =
        context.dataStore.edit { it[IS_VERTICAL_SCROLL] = isVertical }

    suspend fun setFontSize(size: Float) =
        context.dataStore.edit { it[FONT_SIZE] = size }

    suspend fun setVerticalSpacing(spacing: Float) =
        context.dataStore.edit { it[VERTICAL_SPACING] = spacing }

    suspend fun setShowDrawerSearch(value: Boolean) =
        context.dataStore.edit { it[SHOW_DRAWER_SEARCH] = value }

    suspend fun setAutoOpenKeyboard(value: Boolean) =
        context.dataStore.edit { it[AUTO_OPEN_KEYBOARD] = value }

    suspend fun setShowBadges(value: Boolean) =
        context.dataStore.edit { it[SHOW_BADGES] = value }

    suspend fun setSwipeDownEnabled(value: Boolean) =
        context.dataStore.edit { it[SWIPE_DOWN_ENABLED] = value }

    suspend fun setDoubleTapToSleep(value: Boolean) =
        context.dataStore.edit { it[DOUBLE_TAP_TO_SLEEP] = value }

    suspend fun setSecureHiddenApps(value: Boolean) =
        context.dataStore.edit { it[SECURE_HIDDEN_APPS] = value }

    suspend fun setHomeMode(mode: String) =
        context.dataStore.edit { it[HOME_MODE] = mode }

    suspend fun setGlobalActionCenterEnabled(enabled: Boolean) =
        context.dataStore.edit { it[GLOBAL_ACTION_CENTER_ENABLED] = enabled }

    suspend fun setAppHidden(packageName: String, hidden: Boolean) {
        context.dataStore.edit { prefs ->
            val current = prefs[HIDDEN_APPS] ?: emptySet()
            prefs[HIDDEN_APPS] =
                if (hidden) current + packageName else current - packageName
        }
    }

    suspend fun setWallpaper(type: String, value: String) =
        context.dataStore.edit {
            it[WALLPAPER_TYPE] = type
            it[WALLPAPER_VALUE] = value
        }

    suspend fun saveCity(city: String) =
        context.dataStore.edit { it[SAVED_CITY] = city }

    suspend fun setTransparency(enabled: Boolean) =
        setTransparencyEnabled(enabled)

    suspend fun setHiddenApps(apps: Set<String>) =
        context.dataStore.edit { it[HIDDEN_APPS] = apps }

    suspend fun setLauncherLayout(layout: String) =
        context.dataStore.edit { it[KEY_LAUNCHER_LAYOUT] = layout }

    suspend fun setWallpaperType(type: String) =
        context.dataStore.edit { it[WALLPAPER_TYPE] = type }

    suspend fun setWallpaperValue(value: String) =
        context.dataStore.edit { it[WALLPAPER_VALUE] = value }
}