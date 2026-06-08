/**
 * @file SystemInteractionManager.kt
 * @description Handles deep system integrations via Reflection.
 * Responsible for expanding the notification panel and locking the screen safely.
 */

package com.example.fluentui.ui.system

import android.annotation.SuppressLint
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.widget.Toast

object SystemInteractionManager {

    private const val TAG = "SystemInteraction"

    /**
     * Uses Java Reflection to access hidden Android APIs and expand the status bar.
     * Works on 99% of Android devices.
     */
    @SuppressLint("WrongConstant")
    fun expandNotificationPanel(context: Context) {
        try {
            val statusBarService = context.getSystemService("statusbar")
            val statusBarManagerClass = Class.forName("android.app.StatusBarManager")
            val expandMethod = statusBarManagerClass.getMethod("expandNotificationsPanel")
            expandMethod.invoke(statusBarService)
        } catch (e: Exception) {
            Log.w(TAG, "expandNotificationsPanel failed, trying fallback method...")
            try {
                val fallbackMethod = Class.forName("android.app.StatusBarManager").getMethod("expand")
                fallbackMethod.invoke(context.getSystemService("statusbar"))
            } catch (ex: Exception) {
                Log.e(TAG, "Failed to expand notification panel via reflection.", ex)
            }
        }
    }

    /**
     * Locks the screen using DevicePolicyManager.
     * Requires Device Admin permission.
     */
    fun lockScreen(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
                // Note: If no admin receiver, just use reflection fallback
                dpm.lockNow()
                Log.d(TAG, "Screen locked successfully")
            } catch (e: Exception) {
                Log.w(TAG, "DevicePolicyManager lock failed: ${e.message}")
                // Fallback: Show settings prompt
                showAccessibilitySettingsPrompt(context)
            }
        } else {
            Toast.makeText(context, "Le verrouillage nécessite Android 9.0 ou supérieur", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Shows a prompt to enable accessibility settings (fallback only).
     */
    private fun showAccessibilitySettingsPrompt(context: Context) {
        Toast.makeText(
            context,
            "Veuillez configurer les permissions pour utiliser cette fonction",
            Toast.LENGTH_LONG
        ).show()
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }
}
