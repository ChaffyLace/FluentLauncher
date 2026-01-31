package com.example.fluentui.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast

object AppActions {

    /**
     * Lance la pop-up système pour demander la désinstallation
     */
    fun uninstallApp(context: Context, packageName: String) {
        try {
            // On empêche de se désinstaller soi-même (sécurité)
            if (packageName == context.packageName) {
                Toast.makeText(context, "Impossible de désinstaller le Launcher lui-même !", Toast.LENGTH_SHORT).show()
                return
            }

            // L'Intent magique pour désinstaller
            val intent = Intent(Intent.ACTION_DELETE)
            intent.data = Uri.parse("package:$packageName")
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(intent)

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Erreur lors de la désinstallation", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Ouvre la page "Infos sur l'application" dans les paramètres Android
     */
    fun openAppInfo(context: Context, packageName: String) {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            intent.data = Uri.parse("package:$packageName")
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}