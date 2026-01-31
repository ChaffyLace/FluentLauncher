package com.example.fluentui.ui.utils

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

object BitmapHelper {

    // On limite drastiquement la taille de travail.
    // 150px est largement suffisant pour un flou d'arrière-plan.
    // Au-delà, on gaspille de la RAM pour rien car le flou détruit les détails.
    private const val MAX_PROCESSING_SIZE = 150

    /**
     * Crée un flou optimisé sans allocations mémoire inutiles.
     */
    suspend fun createBlur(original: Bitmap, radius: Int): Result<Bitmap> = withContext(Dispatchers.Default) {
        try {
            if (radius < 1) return@withContext Result.failure(IllegalArgumentException("Radius must be >= 1"))

            // 1. CALCUL DU RATIO DE RÉDUCTION
            // On vise une largeur/hauteur max de 150px.
            val widthRatio = MAX_PROCESSING_SIZE.toFloat() / original.width
            val heightRatio = MAX_PROCESSING_SIZE.toFloat() / original.height
            val ratio = min(widthRatio, heightRatio)

            // Si l'image est déjà petite, on garde le ratio 1, sinon on réduit.
            val finalRatio = if (ratio < 1f) ratio else 1f

            val width = (original.width * finalRatio).roundToInt().coerceAtLeast(10)
            val height = (original.height * finalRatio).roundToInt().coerceAtLeast(10)

            // 2. CRÉATION DU BITMAP RÉDUIT (Low Memory)
            // filter = true pour lisser les pixels lors de la réduction
            val smallBitmap = Bitmap.createScaledBitmap(original, width, height, true)

            // 3. FLOU
            val blurred = stackBlur(smallBitmap, radius)

            if (blurred != null) {
                Result.success(blurred)
            } else {
                Result.failure(Exception("Blur failed"))
            }

        } catch (e: OutOfMemoryError) {
            // Sécurité ultime : on force le GC et on échoue proprement
            System.gc()
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun stackBlur(sentBitmap: Bitmap, radius: Int): Bitmap? {
        // Copie mutable sécurisée
        val config = sentBitmap.config ?: Bitmap.Config.ARGB_8888
        val bitmap = try {
            sentBitmap.copy(config, true)
        } catch (e: OutOfMemoryError) {
            return null
        }

        if (radius < 1) return null

        val w = bitmap.width
        val h = bitmap.height
        val pix = IntArray(w * h)

        try {
            bitmap.getPixels(pix, 0, w, 0, 0, w, h)
        } catch (e: Exception) { return null }

        val wm = w - 1
        val hm = h - 1
        val wh = w * h
        val div = radius + radius + 1

        val r = IntArray(wh)
        val g = IntArray(wh)
        val b = IntArray(wh)
        var rsum: Int; var gsum: Int; var bsum: Int
        var x: Int; var y: Int; var i: Int; var p: Int; var yp: Int; var yi: Int; var yw: Int
        val vmin = IntArray(max(w, h))

        var divsum = (div + 1) shr 1
        divsum *= divsum
        val dv = IntArray(256 * divsum)
        for (i in 0 until 256 * divsum) dv[i] = i / divsum

        yw = 0
        yi = 0

        val stack = Array(div) { IntArray(3) }
        var stackpointer: Int; var stackstart: Int; var sir: IntArray; var rbs: Int
        var r1 = radius + 1
        var routsum: Int; var goutsum: Int; var boutsum: Int
        var rinsum: Int; var ginsum: Int; var binsum: Int

        // --- PASSE 1 (Horizontale) ---
        for (y in 0 until h) {
            rinsum = 0; ginsum = 0; binsum = 0; routsum = 0; goutsum = 0; boutsum = 0
            rsum = 0; gsum = 0; bsum = 0
            for (i in -radius..radius) {
                p = pix[yi + min(wm, max(i, 0))]
                sir = stack[i + radius]
                sir[0] = (p and 0xff0000) shr 16
                sir[1] = (p and 0x00ff00) shr 8
                sir[2] = (p and 0x0000ff)
                rbs = r1 - abs(i)
                rsum += sir[0] * rbs; gsum += sir[1] * rbs; bsum += sir[2] * rbs
                if (i > 0) { rinsum += sir[0]; ginsum += sir[1]; binsum += sir[2] }
                else { routsum += sir[0]; goutsum += sir[1]; boutsum += sir[2] }
            }
            stackpointer = radius
            for (x in 0 until w) {
                r[yi] = dv[rsum]; g[yi] = dv[gsum]; b[yi] = dv[bsum]
                rsum -= routsum; gsum -= goutsum; bsum -= boutsum
                stackstart = stackpointer - radius + div
                sir = stack[stackstart % div]
                routsum -= sir[0]; goutsum -= sir[1]; boutsum -= sir[2]
                if (y == 0) vmin[x] = min(x + radius + 1, wm)
                p = pix[yw + vmin[x]]
                sir[0] = (p and 0xff0000) shr 16; sir[1] = (p and 0x00ff00) shr 8; sir[2] = (p and 0x0000ff)
                rinsum += sir[0]; ginsum += sir[1]; binsum += sir[2]
                rsum += rinsum; gsum += ginsum; bsum += binsum
                stackpointer = (stackpointer + 1) % div
                sir = stack[stackpointer % div]
                routsum += sir[0]; goutsum += sir[1]; boutsum += sir[2]
                rinsum -= sir[0]; ginsum -= sir[1]; binsum -= sir[2]
                yi++
            }
            yw += w
        }

        // --- PASSE 2 (Verticale) ---
        for (x in 0 until w) {
            rinsum = 0; ginsum = 0; binsum = 0; routsum = 0; goutsum = 0; boutsum = 0
            rsum = 0; gsum = 0; bsum = 0
            yp = -radius * w
            for (i in -radius..radius) {
                yi = max(0, yp) + x
                sir = stack[i + radius]
                rbs = r1 - abs(i)
                rsum += r[yi] * rbs; gsum += g[yi] * rbs; bsum += b[yi] * rbs
                if (i > 0) { rinsum += r[yi]; ginsum += g[yi]; binsum += b[yi] }
                else { routsum += r[yi]; goutsum += g[yi]; boutsum += b[yi] }
                if (i < hm) yp += w
            }
            yi = x
            stackpointer = radius
            for (y in 0 until h) {
                pix[yi] = (0xff000000.toInt() or (dv[rsum] shl 16) or (dv[gsum] shl 8) or dv[bsum])
                rsum -= routsum; gsum -= goutsum; bsum -= boutsum
                stackstart = stackpointer - radius + div
                sir = stack[stackstart % div]
                routsum -= sir[0]; goutsum -= sir[1]; boutsum -= sir[2]
                if (x == 0) vmin[y] = min(y + r1, hm) * w
                p = x + vmin[y]
                sir[0] = r[p]; sir[1] = g[p]; sir[2] = b[p]
                rinsum += sir[0]; ginsum += sir[1]; binsum += sir[2]
                rsum += rinsum; gsum += ginsum; bsum += binsum
                stackpointer = (stackpointer + 1) % div
                sir = stack[stackpointer]
                routsum += sir[0]; goutsum += sir[1]; boutsum += sir[2]
                rinsum -= sir[0]; ginsum -= sir[1]; binsum -= sir[2]
                yi += w
            }
        }

        bitmap.setPixels(pix, 0, w, 0, 0, w, h)
        return bitmap
    }
}