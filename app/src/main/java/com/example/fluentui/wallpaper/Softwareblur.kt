package com.example.fluentui.wallpaper

import android.graphics.Bitmap

// ==============================================================================
// SoftwareBlur — flou par boîte itératif (3 passes horizontale + verticale)
//
// Pourquoi pas RenderScript ?  Déprécié API 31, supprimé API 33+.
// Pourquoi pas RenderEffect ?  Ne peut pas écrire le résultat dans un Bitmap ;
//                               il floute en temps réel sur le GPU à chaque frame.
//
// Ce que fait cet algorithme :
//   • Un flou box (moyenne des pixels voisins) appliqué 3 fois de suite donne
//     un résultat quasi gaussien (théorème des boîtes itérées).
//   • La complexité est O(W × H) quelle que soit la taille du radius — pas O(r²).
//   • Sur un thumbnail 300px de large, 3 passes s'exécutent en ~5-10 ms.
//   • radius 12 sur 300px ≈ effet visuel de RenderEffect(80f, 80f) sur plein écran.
// ==============================================================================
object SoftwareBlur {

    fun blur(src: Bitmap, radius: Int): Bitmap {
        require(radius in 1..250) { "radius doit être entre 1 et 250" }

        // Copie mutable pour ne pas modifier l'original
        val out = src.copy(Bitmap.Config.ARGB_8888, true) ?: return src
        val w = out.width
        val h = out.height
        val pixels = IntArray(w * h)
        out.getPixels(pixels, 0, w, 0, 0, w, h)

        // 3 passes box → approximation gaussienne
        repeat(3) {
            boxBlurH(pixels, w, h, radius)
            boxBlurV(pixels, w, h, radius)
        }

        out.setPixels(pixels, 0, w, 0, 0, w, h)
        return out
    }

    // --------------------------------------------------------------------------
    // Passe horizontale — moyenne glissante sur chaque ligne
    // --------------------------------------------------------------------------
    private fun boxBlurH(pixels: IntArray, w: Int, h: Int, r: Int) {
        val scale = 1f / (2 * r + 1)
        for (y in 0 until h) {
            val rowStart = y * w
            var rSum = 0; var gSum = 0; var bSum = 0

            // Initialise la fenêtre sur le bord gauche (réplication de bord)
            val firstPx = pixels[rowStart]
            rSum = red(firstPx) * (r + 1)
            gSum = green(firstPx) * (r + 1)
            bSum = blue(firstPx) * (r + 1)
            for (x in 0 until r) {
                val p = pixels[rowStart + x.coerceIn(0, w - 1)]
                rSum += red(p); gSum += green(p); bSum += blue(p)
            }

            for (x in 0 until w) {
                val pIn  = pixels[rowStart + (x + r).coerceIn(0, w - 1)]
                val pOut = pixels[rowStart + (x - r - 1).coerceIn(0, w - 1)]
                rSum += red(pIn)   - red(pOut)
                gSum += green(pIn) - green(pOut)
                bSum += blue(pIn)  - blue(pOut)
                pixels[rowStart + x] = pack(
                    (rSum * scale).toInt().coerceIn(0, 255),
                    (gSum * scale).toInt().coerceIn(0, 255),
                    (bSum * scale).toInt().coerceIn(0, 255)
                )
            }
        }
    }

    // --------------------------------------------------------------------------
    // Passe verticale — même logique, colonnes
    // --------------------------------------------------------------------------
    private fun boxBlurV(pixels: IntArray, w: Int, h: Int, r: Int) {
        val scale = 1f / (2 * r + 1)
        for (x in 0 until w) {
            var rSum = 0; var gSum = 0; var bSum = 0

            val firstPx = pixels[x]
            rSum = red(firstPx) * (r + 1)
            gSum = green(firstPx) * (r + 1)
            bSum = blue(firstPx) * (r + 1)
            for (y in 0 until r) {
                val p = pixels[y.coerceIn(0, h - 1) * w + x]
                rSum += red(p); gSum += green(p); bSum += blue(p)
            }

            for (y in 0 until h) {
                val pIn  = pixels[(y + r).coerceIn(0, h - 1) * w + x]
                val pOut = pixels[(y - r - 1).coerceIn(0, h - 1) * w + x]
                rSum += red(pIn)   - red(pOut)
                gSum += green(pIn) - green(pOut)
                bSum += blue(pIn)  - blue(pOut)
                pixels[y * w + x] = pack(
                    (rSum * scale).toInt().coerceIn(0, 255),
                    (gSum * scale).toInt().coerceIn(0, 255),
                    (bSum * scale).toInt().coerceIn(0, 255)
                )
            }
        }
    }

    // Extracteurs de canaux inline pour éviter les allocations
    private inline fun red(c: Int)   = (c shr 16) and 0xFF
    private inline fun green(c: Int) = (c shr 8)  and 0xFF
    private inline fun blue(c: Int)  =  c          and 0xFF
    private inline fun pack(r: Int, g: Int, b: Int): Int =
        (0xFF shl 24) or (r shl 16) or (g shl 8) or b
}