package com.example.fluentui.ui.utils

import android.graphics.Bitmap
import android.util.LruCache

/**
 * Gestionnaire de mémoire pour les icônes.
 * Évite de recharger les images à chaque scroll.
 * Utilise 1/8ème de la mémoire disponible du téléphone.
 */
object IconCache {
    private val maxMemory = (Runtime.getRuntime().maxMemory() / 1024).toInt()
    private val cacheSize = maxMemory / 8

    private val cache = object : LruCache<String, Bitmap>(cacheSize) {
        override fun sizeOf(key: String, bitmap: Bitmap): Int {
            return bitmap.byteCount / 1024
        }
    }

    fun get(packageName: String): Bitmap? {
        return cache.get(packageName)
    }

    fun put(packageName: String, bitmap: Bitmap) {
        if (get(packageName) == null) {
            cache.put(packageName, bitmap)
        }
    }

    fun clear() {
        cache.evictAll()
    }
}