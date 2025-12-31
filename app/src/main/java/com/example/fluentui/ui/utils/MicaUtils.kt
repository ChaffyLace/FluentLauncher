package com.example.fluentui.ui.utils

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.palette.graphics.Palette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.random.Random

object MicaUtils {

    suspend fun generateDockBackground(
        originalWallpaper: Bitmap?,
        isDark: Boolean,
        isPowerSaveMode: Boolean // Nouveau paramètre
    ): Bitmap? {
        return withContext(Dispatchers.Default) {
            // Si économie d'énergie -> Pas d'image Mica (Optimisation max)
            if (isPowerSaveMode) return@withContext null
            if (originalWallpaper == null) return@withContext null

            try {
                val width = originalWallpaper.width
                val height = originalWallpaper.height
                val cropHeight = (height * 0.25).toInt()
                val startY = height - cropHeight

                val cropBitmap = Bitmap.createBitmap(originalWallpaper, 0, startY, width, cropHeight)

                val scaledWidth = width / 6
                val scaledHeight = cropHeight / 6
                val smallBitmap = Bitmap.createScaledBitmap(cropBitmap, scaledWidth, scaledHeight, true)

                val blurredBitmap = fastBlur(smallBitmap, radius = 12)

                val finalBitmap = Bitmap.createScaledBitmap(blurredBitmap, width, cropHeight, true)

                val canvas = Canvas(finalBitmap)
                val paint = Paint()

                paint.color = Color.BLACK
                paint.alpha = if (isDark) 80 else 30
                canvas.drawRect(0f, 0f, width.toFloat(), cropHeight.toFloat(), paint)

                val palette = Palette.from(originalWallpaper).generate()
                val dominantColor = if (isDark) {
                    palette.getDarkVibrantColor(Color.DKGRAY)
                } else {
                    palette.getLightVibrantColor(Color.LTGRAY)
                }
                paint.color = dominantColor
                paint.alpha = 50
                canvas.drawRect(0f, 0f, width.toFloat(), cropHeight.toFloat(), paint)

                applyNoise(finalBitmap, intensity = 10)

                if (cropBitmap != finalBitmap) cropBitmap.recycle()
                if (smallBitmap != finalBitmap) smallBitmap.recycle()
                if (blurredBitmap != finalBitmap && blurredBitmap != smallBitmap) blurredBitmap.recycle()

                return@withContext finalBitmap

            } catch (e: Exception) {
                return@withContext null
            }
        }
    }

    private fun applyNoise(bitmap: Bitmap, intensity: Int) {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        val random = Random(System.currentTimeMillis())

        for (i in pixels.indices) {
            val noise = random.nextInt(intensity * 2) - intensity
            val color = pixels[i]

            var r = (color shr 16 and 0xFF) + noise
            var g = (color shr 8 and 0xFF) + noise
            var b = (color and 0xFF) + noise

            r = r.coerceIn(0, 255)
            g = g.coerceIn(0, 255)
            b = b.coerceIn(0, 255)

            pixels[i] = (color and -0x1000000) or (r shl 16) or (g shl 8) or b
        }

        bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
    }

    private fun fastBlur(sentBitmap: Bitmap, radius: Int): Bitmap {
        val config = sentBitmap.config ?: Bitmap.Config.ARGB_8888
        val bitmap = sentBitmap.copy(config, true)

        if (radius < 1) return (null)!!
        val w = bitmap.width
        val h = bitmap.height
        val pix = IntArray(w * h)
        bitmap.getPixels(pix, 0, w, 0, 0, w, h)
        val wm = w - 1
        val hm = h - 1
        val wh = w * h
        val div = radius + radius + 1
        val r = IntArray(wh)
        val g = IntArray(wh)
        val b = IntArray(wh)
        var rsum: Int
        var gsum: Int
        var bsum: Int
        var x: Int
        var y: Int
        var i: Int
        var p: Int
        var yp: Int
        var yi: Int
        var yw: Int
        val vmin = IntArray(Math.max(w, h))
        var divsum = div + 1 shr 1
        divsum *= divsum
        val dv = IntArray(256 * divsum)
        for (i in 0 until 256 * divsum) {
            dv[i] = i / divsum
        }
        yw = 0
        yi = 0
        val stack = Array(div) { IntArray(3) }
        var stackpointer: Int
        var stackstart: Int
        var sir: IntArray
        var rbs: Int
        var r1 = radius + 1
        var routsum: Int
        var goutsum: Int
        var boutsum: Int
        var rinsum: Int
        var ginsum: Int
        var binsum: Int
        y = 0
        while (y < h) {
            bsum = 0
            gsum = 0
            rsum = 0
            boutsum = 0
            goutsum = 0
            routsum = 0
            binsum = 0
            ginsum = 0
            rinsum = 0
            i = -radius
            while (i <= radius) {
                p = pix[yi + Math.min(wm, Math.max(i, 0))]
                sir = stack[i + radius]
                sir[0] = p and 0xff0000 shr 16
                sir[1] = p and 0x00ff00 shr 8
                sir[2] = p and 0x0000ff
                rbs = r1 - Math.abs(i)
                rsum += sir[0] * rbs
                gsum += sir[1] * rbs
                bsum += sir[2] * rbs
                if (i > 0) {
                    rinsum += sir[0]
                    ginsum += sir[1]
                    binsum += sir[2]
                } else {
                    routsum += sir[0]
                    goutsum += sir[1]
                    boutsum += sir[2]
                }
                i++
            }
            stackpointer = radius
            x = 0
            while (x < w) {
                r[yi] = dv[rsum]
                g[yi] = dv[gsum]
                b[yi] = dv[bsum]
                rsum -= routsum
                gsum -= goutsum
                bsum -= boutsum
                stackstart = stackpointer - radius + div
                sir = stack[stackstart % div]
                routsum -= sir[0]
                goutsum -= sir[1]
                boutsum -= sir[2]
                if (y == 0) {
                    vmin[x] = Math.min(x + radius + 1, wm)
                }
                p = pix[yw + vmin[x]]
                sir[0] = p and 0xff0000 shr 16
                sir[1] = p and 0x00ff00 shr 8
                sir[2] = p and 0x0000ff
                rinsum += sir[0]
                ginsum += sir[1]
                binsum += sir[2]
                rsum += rinsum
                gsum += ginsum
                bsum += binsum
                stackpointer = (stackpointer + 1) % div
                sir = stack[stackpointer % div]
                routsum += sir[0]
                goutsum += sir[1]
                boutsum += sir[2]
                rinsum -= sir[0]
                ginsum -= sir[1]
                binsum -= sir[2]
                yi++
                x++
            }
            yw += w
            y++
        }
        x = 0
        while (x < w) {
            bsum = 0
            gsum = 0
            rsum = 0
            boutsum = 0
            goutsum = 0
            routsum = 0
            binsum = 0
            ginsum = 0
            rinsum = 0
            yp = -radius * w
            i = -radius
            while (i <= radius) {
                yi = Math.max(0, yp) + x
                sir = stack[i + radius]
                sir[0] = r[yi]
                sir[1] = g[yi]
                sir[2] = b[yi]
                rbs = r1 - Math.abs(i)
                rsum += sir[0] * rbs
                gsum += sir[1] * rbs
                bsum += sir[2] * rbs
                if (i > 0) {
                    rinsum += sir[0]
                    ginsum += sir[1]
                    binsum += sir[2]
                } else {
                    routsum += sir[0]
                    goutsum += sir[1]
                    boutsum += sir[2]
                }
                if (i < hm) {
                    yp += w
                }
                i++
            }
            yi = x
            stackpointer = radius
            y = 0
            while (y < h) {
                pix[yi] = -0x1000000 or (dv[rsum] shl 16) or (dv[gsum] shl 8) or dv[bsum]
                rsum -= routsum
                gsum -= goutsum
                bsum -= boutsum
                stackstart = stackpointer - radius + div
                sir = stack[stackstart % div]
                routsum -= sir[0]
                goutsum -= sir[1]
                boutsum -= sir[2]
                if (x == 0) {
                    vmin[y] = Math.min(y + r1, hm) * w
                }
                p = x + vmin[y]
                sir[0] = r[p]
                sir[1] = g[p]
                sir[2] = b[p]
                rinsum += sir[0]
                ginsum += sir[1]
                binsum += sir[2]
                rsum += rinsum
                gsum += ginsum
                bsum += binsum
                stackpointer = (stackpointer + 1) % div
                sir = stack[stackpointer % div]
                routsum += sir[0]
                goutsum += sir[1]
                boutsum += sir[2]
                rinsum -= sir[0]
                ginsum -= sir[1]
                binsum -= sir[2]
                yi += w
                y++
            }
            x++
        }
        bitmap.setPixels(pix, 0, w, 0, 0, w, h)
        return bitmap
    }
}