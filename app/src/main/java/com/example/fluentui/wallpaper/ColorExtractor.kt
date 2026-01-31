package com.example.fluentui.wallpaper

import android.graphics.Bitmap
import androidx.compose.ui.graphics.Color
import androidx.core.graphics.ColorUtils
import androidx.palette.graphics.Palette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object ColorExtractor {

    /**
     * Analyse le bitmap et renvoie la couleur "Vibrante" ou "Dominante".
     * C'est cette couleur brute qu'on passera à MicaParams.
     */
    suspend fun extractDominantColor(bitmap: Bitmap?): Color = withContext(Dispatchers.Default) {
        if (bitmap == null) {
            // Bleu Windows 11 par défaut si pas de wallpaper
            return@withContext Color(0xFF0078D4)
        }

        try {
            // 1. On réduit l'image pour aller ultra-vite (performance)
            val p = Palette.from(bitmap)
                .resizeBitmapArea(100 * 100)
                .clearFilters() // On accepte tout (même le sombre/clair)
                .generate()

            // 2. On essaie de choper la couleur "Vibrant" (la plus saturée)
            // Si pas dispo, on prend la "Dominante".
            // Si toujours rien, on prend le bleu Windows.
            val fallbackColor = 0xFF0078D4.toInt()
            val seedColorInt = p.getVibrantColor(p.getDominantColor(fallbackColor))

            // 3. Petite correction de saturation (si c'est trop gris, on force le bleu)
            val hsv = FloatArray(3)
            android.graphics.Color.colorToHSV(seedColorInt, hsv)
            if (hsv[1] < 0.1f) { // Saturation trop faible (< 10%)
                return@withContext Color(0xFF0078D4)
            }

            return@withContext Color(seedColorInt)

        } catch (e: Exception) {
            return@withContext Color(0xFF0078D4)
        }
    }

    /**
     * Détermine si le fond d'écran est sombre ou clair.
     * Utile pour forcer le mode sombre/clair des textes si l'utilisateur est en mode "Auto".
     */
    suspend fun isDarkWallpaper(bitmap: Bitmap?): Boolean = withContext(Dispatchers.Default) {
        if (bitmap == null) return@withContext true // Par sécurité, on considère sombre par défaut

        try {
            // On analyse juste un tout petit carré pour la luminosité moyenne
            val small = Bitmap.createScaledBitmap(bitmap, 40, 40, true)
            val p = Palette.from(small).generate()
            val dom = p.getDominantColor(0xFF000000.toInt())

            // Si la luminosité est inférieure à 0.5 (50%), c'est sombre
            ColorUtils.calculateLuminance(dom) < 0.5
        } catch (e: Exception) {
            true
        }
    }
}