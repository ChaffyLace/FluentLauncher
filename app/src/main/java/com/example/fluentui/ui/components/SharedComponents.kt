package com.example.fluentui.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import com.example.fluentui.ui.utils.AppIconLoader

/**
 * Ce composant est indispensable.
 * Il remplace l'ancien "Image(bitmap = app.icon)" qui faisait crasher l'app.
 * Il demande à AppIconLoader de chercher l'image sans bloquer l'écran.
 */
@Composable
fun SharedAppIcon(
    packageName: String,
    size: Float,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(packageName) {
        bitmap = AppIconLoader.loadIcon(context, packageName, size.toInt())
    }

    if (bitmap != null) {
        Image(
            bitmap = bitmap!!.asImageBitmap(),
            contentDescription = null,
            modifier = modifier,
            contentScale = ContentScale.Fit,
            filterQuality = FilterQuality.Medium // Lissage propre
        )
    } else {
        Box(modifier = modifier.background(Color.Gray.copy(0.2f), CircleShape))
    }
}