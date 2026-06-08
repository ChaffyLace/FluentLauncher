package com.example.fluentui.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import com.example.fluentui.R // N'oublie pas d'importer ton R

object AppActions {

    fun uninstallApp(context: Context, packageName: String) {
        try {
            if (packageName == context.packageName) {
                // MODIFIÉ : Utilisation de la ressource string
                Toast.makeText(
                    context,
                    context.getString(R.string.toast_uninstall_self_error),
                    Toast.LENGTH_SHORT
                ).show()
                return
            }

            val intent = Intent(Intent.ACTION_DELETE)
            intent.data = Uri.parse("package:$packageName")
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(intent)

        } catch (e: Exception) {
            e.printStackTrace()
            // MODIFIÉ : Utilisation de la ressource string
            Toast.makeText(
                context,
                context.getString(R.string.toast_uninstall_error),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

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