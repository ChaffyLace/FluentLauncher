package com.example.fluentui.ui.utils

import com.example.fluentui.R

object LocalIconPack {

    private val icons = mapOf(

        "com.android.settings" to R.drawable.ic_settings,

        "com.android.camera" to R.drawable.ic_camera,
        "com.sec.android.app.camera" to R.drawable.ic_camera,
        "com.google.android.GoogleCamera" to R.drawable.ic_camera,

        "com.google.android.calculator" to R.drawable.ic_calculator,
        "com.sec.android.app.popupcalculator" to R.drawable.ic_calculator,

        "com.google.android.deskclock" to R.drawable.ic_clock,
        "com.sec.android.app.clockpackage" to R.drawable.ic_clock,

        "com.google.android.dialer" to R.drawable.ic_phone,
        "com.samsung.android.dialer" to R.drawable.ic_phone,
        "com.android.dialer" to R.drawable.ic_phone,

        "com.google.android.apps.messaging" to R.drawable.ic_message,
        "com.samsung.android.messaging" to R.drawable.ic_message,
        "com.android.mms" to R.drawable.ic_message,

        "com.google.android.contacts" to R.drawable.ic_contact,
        "com.samsung.android.app.contacts" to R.drawable.ic_contact,
        "com.android.contacts" to R.drawable.ic_contact,

        "com.sec.android.app.myfiles" to R.drawable.ic_files,
        "com.google.android.apps.nbu.files" to R.drawable.ic_files,

        // --- NOUVEAUX AJOUTS ---

        // Calendrier
        "com.google.android.calendar" to R.drawable.ic_calendar,
        "com.samsung.android.calendar" to R.drawable.ic_calendar,
        "com.android.calendar" to R.drawable.ic_calendar,

        // Galerie / Photos
        "com.google.android.apps.photos" to R.drawable.ic_gallery,
        "com.sec.android.gallery3d" to R.drawable.ic_gallery,
        "com.android.gallery3d" to R.drawable.ic_gallery,

        // Musique
        "com.google.android.apps.youtube.music" to R.drawable.ic_music,
        "com.sec.android.app.music" to R.drawable.ic_music,
        "com.spotify.music" to R.drawable.ic_music,
        "com.android.music" to R.drawable.ic_music
    )

    fun getIcon(packageName: String): Int? {
        return icons[packageName]
    }
}