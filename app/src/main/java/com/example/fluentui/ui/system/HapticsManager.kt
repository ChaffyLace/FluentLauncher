package com.example.fluentui.ui.system

import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView

class HapticsManager(private val view: View) {
    fun performPress() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
        } else {
            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        }
    }

    fun performRelease() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
        }
    }
}

@Composable
fun rememberHaptics(): HapticsManager {
    val view = LocalView.current
    return remember(view) { HapticsManager(view) }
}