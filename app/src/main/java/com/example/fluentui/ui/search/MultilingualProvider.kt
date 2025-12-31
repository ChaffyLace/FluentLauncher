package com.example.fluentui.ui.search

import android.content.Context
import android.provider.Settings

class MultilingualProvider(private val context: Context) {

    fun getAppAliases(packageName: String, label: String): List<String> {
        val aliases = mutableListOf<String>()

        when (packageName) {
            "com.android.chrome" -> aliases.addAll(listOf("internet", "web", "browser", "navigateur"))
            "com.google.android.youtube" -> aliases.addAll(listOf("video", "stream"))
            "com.android.settings" -> aliases.addAll(listOf("config", "wifi", "bluetooth"))
            "com.android.calculator2" -> aliases.addAll(listOf("math", "calc"))
        }

        return aliases
    }

    fun getSettingsAliases(): Map<String, List<String>> {
        return mapOf(
            Settings.ACTION_WIFI_SETTINGS to listOf("wifi", "internet", "wlan", "connexion"),
            Settings.ACTION_BLUETOOTH_SETTINGS to listOf("bluetooth", "bt", "appairage"),
            Settings.ACTION_DISPLAY_SETTINGS to listOf("ecran", "affichage", "luminosite", "display"),
            Settings.ACTION_SOUND_SETTINGS to listOf("son", "audio", "volume", "sound"),
            Settings.ACTION_APPLICATION_SETTINGS to listOf("apps", "applications", "logiciels"),
            Settings.ACTION_BATTERY_SAVER_SETTINGS to listOf("batterie", "pile", "energie"),
            Settings.ACTION_DATE_SETTINGS to listOf("date", "heure", "time", "clock"),
            Settings.ACTION_AIRPLANE_MODE_SETTINGS to listOf("avion", "aeroplane", "flight")
        )
    }
}