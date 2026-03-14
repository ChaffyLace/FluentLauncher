package com.example.fluentui.ui.system

import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView

class HapticsManager(private val view: View) {


    fun performPress() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            view.performHapticFeedback(HapticFeedbackConstants.TEXT_HANDLE_MOVE)
        } else {
            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
        }
    }

    fun performRelease() {
    }

    fun performSliderTick() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
        }
    }


    /**
     * Long press détecté (début du drag)
     */
    fun performLongPress() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            view.performHapticFeedback(
                HapticFeedbackConstants.LONG_PRESS,
                HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING
            )
        } else {
            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        }
    }

    /**
     * Swap de tuiles (réutilise CLOCK_TICK si dispo, sinon fallback)
     */
    fun performTick() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        }
    }

    /**
     * Confirmation d'action (lancement d'app)
     */
    fun performConfirm() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            view.performHapticFeedback(
                HapticFeedbackConstants.CONFIRM,
                HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING
            )
        } else {
            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
        }
    }

    /**
     * Rejection (annulation)
     */
    fun performReject() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            view.performHapticFeedback(
                HapticFeedbackConstants.REJECT,
                HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING
            )
        } else {
            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        }
    }
}

@Composable
fun rememberHaptics(): HapticsManager {
    val view = LocalView.current
    return remember(view) { HapticsManager(view) }
}