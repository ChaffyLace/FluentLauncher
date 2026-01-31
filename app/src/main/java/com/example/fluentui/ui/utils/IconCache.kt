package com.example.fluentui.ui.utils

import android.graphics.Bitmap
import androidx.collection.LruCache
import kotlin.math.max

object IconCache {
    // 1. On récupère la RAM allouée en KiloOctets (KB)
    private val maxMemory = (Runtime.getRuntime().maxMemory() / 1024).toInt()

    // 2. STRATÉGIE AGRESSIVE POUR LAUNCHER :
    // - On essaie de prendre 25% (1/4) de la RAM (Standard Launcher).
    // - MAIS on force un minimum de 40MB (40 * 1024 KB) pour être sûr que ça ne lag pas.
    // - On plafonne à 150MB pour ne pas tuer le téléphone si c'est un monstre de puissance.
    private val cacheSizeInKB = (maxMemory / 4).coerceIn(40 * 1024, 150 * 1024)

    // 3. Création du cache
    private val cache = object : LruCache<String, Bitmap>(cacheSizeInKB * 1024) { // Conversion en Octets pour LruCache
        override fun sizeOf(key: String, value: Bitmap): Int {
            // Le cache calcule la taille réelle en octets de l'image
            return value.byteCount
        }
    }

    fun get(packageName: String): Bitmap? {
        return cache.get(packageName)
    }

    fun put(packageName: String, bitmap: Bitmap) {
        // Sécurité : On ne met pas en cache si l'image est plus grosse que le cache lui-même (improbable mais safe)
        if (bitmap.byteCount < cache.maxSize()) {
            cache.put(packageName, bitmap)
        }
    }

    fun clear() {
        cache.evictAll()
    }
}