package com.example.fluentui.ui.components

import android.os.Process
import android.os.UserHandle

/**
 * Lightweight data class.
 * STRICTLY NO BITMAPS HERE to prevent OutOfMemory errors.
 * The icon is loaded asynchronously via AppIconLoader.
 */
data class AppModel(
    val label: String,
    val packageName: String,
    val userHandle: UserHandle = Process.myUserHandle(),
    val isSystemApp: Boolean = false
)