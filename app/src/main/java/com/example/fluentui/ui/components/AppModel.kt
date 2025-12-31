package com.example.fluentui.ui.components

import android.graphics.Bitmap
import android.os.Process
import android.os.UserHandle

data class AppModel(
    val label: String,
    val packageName: String,
    val icon: Bitmap?,
    val userHandle: UserHandle = Process.myUserHandle(),
    val isSystemApp: Boolean = false
)
