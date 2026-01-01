package com.example.fluentui.ui.utils

import androidx.compose.ui.graphics.vector.ImageVector
import fluent.ui.system.icons.FluentIcons
import fluent.ui.system.icons.color.*
import fluent.ui.system.icons.filled.Call

object FluentIconMapper {
    private val iconMap = mapOf(
        // === SYSTÈME ===
        "com.android.settings" to FluentIcons.Color.Settings,
        "com.android.camera" to FluentIcons.Color.Camera,
        "com.google.android.dialer" to FluentIcons.Filled.Call, // Gardé en Filled comme tu l'as mis
        "com.google.android.deskclock" to FluentIcons.Color.Clock,
        "com.android.chrome" to FluentIcons.Color.Globe,
        "com.google.android.gm" to FluentIcons.Color.Mail,
        "com.google.android.calendar" to FluentIcons.Color.Calendar, // Souvent CalendarLtr dans la lib
        "com.google.android.apps.photos" to FluentIcons.Color.Image,

        // === MESSAGERIE (CORRECTION) ===
        // Google Messages (SMS "Normaux") -> C'est celle-ci qui prend l'icône Chat
        "com.google.android.apps.messaging" to FluentIcons.Color.Chat,
        "com.android.mms" to FluentIcons.Color.Chat, // Pour certains téléphones Samsung/AOSP
    )

    fun getFluentIcon(packageName: String): ImageVector? {
        return iconMap[packageName]
    }
}