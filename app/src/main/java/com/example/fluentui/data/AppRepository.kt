package com.example.fluentui.data

import android.content.Context
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.UserManager
import androidx.core.content.getSystemService
import com.example.fluentui.ui.components.AppModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.util.Collections

class AppRepository(private val context: Context) {

    private val packageManager: PackageManager = context.packageManager
    private val launcherApps: LauncherApps? = context.getSystemService()
    private val userManager: UserManager? = context.getSystemService()

    fun getInstalledApps(): Flow<List<AppModel>> = flow {
        val apps = mutableListOf<AppModel>()

        val profiles = userManager?.userProfiles ?: emptyList()

        for (user in profiles) {
            val activities = launcherApps?.getActivityList(null, user) ?: emptyList()

            for (info in activities) {
                val icon = getIconBitmap(info.getIcon(0))

                val app = AppModel(
                    label = info.label.toString(),
                    packageName = info.applicationInfo.packageName,
                    icon = icon,
                    userHandle = user
                )
                apps.add(app)
            }
        }

        Collections.sort(apps) { a, b -> a.label.compareTo(b.label, ignoreCase = true) }

        emit(apps)
    }.flowOn(Dispatchers.IO)

    private fun getIconBitmap(drawable: Drawable): Bitmap {
        if (drawable is BitmapDrawable) return drawable.bitmap

        val bitmap = Bitmap.createBitmap(
            drawable.intrinsicWidth.coerceAtLeast(1),
            drawable.intrinsicHeight.coerceAtLeast(1),
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }
}