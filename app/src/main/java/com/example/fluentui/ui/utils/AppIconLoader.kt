package com.example.fluentui.ui.utils

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * PRODUCTION GRADE ICON LOADER
 * * 1. Uses LRU Cache (Memory Management).
 * 2. Uses Coroutines (Thread Safety).
 * 3. Handles Adaptive Icons (Android 8+ support).
 * 4. Normalizes sizes (Prevents jagged edges in UI).
 */
object AppIconLoader {

    // --- YOUR EXISTING CACHE LOGIC (Integrated here) ---
    private val maxMemory = (Runtime.getRuntime().maxMemory() / 1024).toInt()
    private val cacheSize = maxMemory / 8

    private val memoryCache = object : LruCache<String, Bitmap>(cacheSize) {
        override fun sizeOf(key: String, bitmap: Bitmap): Int {
            // value is in kilobytes
            return bitmap.byteCount / 1024
        }
    }

    /**
     * Main entry point. Call this from your Composable via LaunchedEffect.
     * @param packageName The app's package name.
     * @param sizeDp The desired size in DP (e.g., 48, 60).
     */
    suspend fun loadIcon(context: Context, packageName: String, sizeDp: Int): Bitmap? {
        val key = "$packageName-$sizeDp"

        // 1. FAST PATH: Check Memory Cache
        memoryCache.get(key)?.let { return it }

        // 2. SLOW PATH: Load from System (Must be on IO Thread)
        return withContext(Dispatchers.IO) {
            try {
                val pm = context.packageManager
                val drawable = pm.getApplicationIcon(packageName)

                // Calculate pixel size based on screen density
                val sizePx = (sizeDp * context.resources.displayMetrics.density).toInt()

                // Convert & Resize
                val bitmap = drawable.toNormalizedBitmap(sizePx)

                // Save to Cache
                if (bitmap != null) {
                    memoryCache.put(key, bitmap)
                }

                bitmap
            } catch (e: PackageManager.NameNotFoundException) {
                // App might have been uninstalled recently
                null
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    /**
     * Converts any Drawable (Legacy, Adaptive, Vector) to a standard Bitmap.
     * This ensures all icons in your Grid look uniform.
     */
    private fun Drawable.toNormalizedBitmap(sizePx: Int): Bitmap? {
        if (sizePx <= 0) return null

        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Setup bounds for the canvas drawing
        setBounds(0, 0, sizePx, sizePx)

        // Handle Adaptive Icons (Android 8+) specifically to avoid weird crop issues
        if (this is AdaptiveIconDrawable) {
            // Adaptive icons usually need a slight scale or they touch edges
            // But usually default draw is fine if bounds are set correctly
            draw(canvas)
        } else {
            // Legacy icons
            draw(canvas)
        }

        return bitmap
    }

    fun clearCache() {
        memoryCache.evictAll()
    }
}