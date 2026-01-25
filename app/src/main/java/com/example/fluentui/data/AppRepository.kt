package com.example.fluentui.data

import android.content.Context
import android.content.pm.LauncherApps
import android.os.UserManager
import androidx.core.content.getSystemService
import com.example.fluentui.ui.components.AppModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.util.Collections

class AppRepository(private val context: Context) {

    private val launcherApps: LauncherApps? = context.getSystemService()
    private val userManager: UserManager? = context.getSystemService()

    fun getInstalledApps(): Flow<List<AppModel>> = flow {
        val apps = mutableListOf<AppModel>()

        // Support pour les profils pro (Work Profiles)
        val profiles = userManager?.userProfiles ?: emptyList()

        for (user in profiles) {
            val activities = launcherApps?.getActivityList(null, user) ?: emptyList()

            for (info in activities) {
                // CORRECTION CRITIQUE :
                // On ne charge plus l'icône ici. On ne fait que référencer l'app.
                // Cela rend cette boucle 50x plus rapide.

                val app = AppModel(
                    label = info.label.toString(),
                    packageName = info.applicationInfo.packageName,
                    userHandle = user
                )
                apps.add(app)
            }
        }

        Collections.sort(apps) { a, b -> a.label.compareTo(b.label, ignoreCase = true) }

        emit(apps)
    }.flowOn(Dispatchers.IO)

}