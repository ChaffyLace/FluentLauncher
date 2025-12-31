package com.example.fluentui.ui.components

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.core.graphics.drawable.toBitmap
import androidx.core.graphics.scale

fun Drawable.toCleanBitmap(size: Int): Bitmap {
    return when (this) {

        is BitmapDrawable -> {
            this.bitmap.scale(size, size)
        }

        is AdaptiveIconDrawable -> {
            val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)

            val inset = (size * 0.15f).toInt()
            setBounds(inset, inset, size - inset, size - inset)

            draw(canvas)
            bitmap
        }

        else -> this.toBitmap(size, size)
    }
}
