package com.example.fluentui.wallpaper

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.RenderEffect
import android.graphics.Shader
import android.net.Uri
import android.os.Build
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import com.example.fluentui.ui.motion.FluentParallax
import com.example.fluentui.ui.settings.SettingsState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.min

@Composable
fun WallpaperLayer(
    settings: SettingsState,
    isDark: Boolean,
    scrollProgress: Float,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // États graphiques
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var blurredBitmap by remember { mutableStateOf<Bitmap?>(null) }

    // Filtres et Effets
    val vibrantColorFilter = remember(isDark) {
        val matrix = ColorMatrix()
        if (isDark) matrix.setToSaturation(1.6f) else matrix.setToSaturation(1.3f)
        ColorFilter.colorMatrix(matrix)
    }

    // Le fameux NoiseGenerator (si le fichier existe, sinon null)
    val noiseBrush = remember(isDark) {
        try { NoiseGenerator.generate(isDark) } catch (e: Exception) { null }
    }

    val displayMetrics = remember { context.resources.displayMetrics }

    // ========================================================================
    // 1. CHARGEMENT COMPLEXE (Avec la correction FILE intégrée)
    // ========================================================================
    LaunchedEffect(settings.wallpaperType, settings.wallpaperValue) {
        withContext(Dispatchers.IO) {
            val loadedBitmap = try {
                when (settings.wallpaperType) {
                    "FILE" -> {
                        // --- C'EST ICI LA CORRECTION ---
                        // On décode directement le fichier copié localement
                        val file = File(settings.wallpaperValue)
                        if (file.exists()) {
                            BitmapFactory.decodeFile(file.absolutePath)
                        } else null
                    }
                    "RES" -> {
                        val id = settings.wallpaperValue.toIntOrNull()
                        if (id != null) BitmapEngine.decodeSampledBitmapFromResource(context, id, displayMetrics.widthPixels, displayMetrics.heightPixels) else null
                    }
                    "URI" -> {
                        val uri = Uri.parse(settings.wallpaperValue)
                        BitmapEngine.decodeSampledBitmapFromUri(context, uri, displayMetrics.widthPixels, displayMetrics.heightPixels)
                    }
                    else -> null
                }
            } catch (e: Exception) { null }

            // Création de la version floue pour l'animation
            val blurBmp = if (loadedBitmap != null) {
                // On garde une qualité suffisante (300px) pour éviter les gros pixels
                BitmapEngine.createBlurryThumbnail(loadedBitmap, width = 300)
            } else null

            withContext(Dispatchers.Main) {
                bitmap = loadedBitmap
                blurredBitmap = blurBmp
            }
        }
    }

    // ========================================================================
    // 2. RENDU GRAPHIQUE (Le code "Premium")
    // ========================================================================
    Box(modifier = modifier.fillMaxSize()) {

        // --- A. FOND NET (Sharp) ---
        Crossfade(targetState = bitmap, animationSpec = tween(600), label = "Wp") { bmp ->
            if (bmp != null) {
                FluentParallax(scrollProgress = scrollProgress) {
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            } else {
                // Fallback Couleur
                val solidColor = try { Color(settings.wallpaperValue.toInt()) } catch (e: Exception) { Color(0xFF202020) }
                Box(Modifier.fillMaxSize().background(solidColor))
            }
        }

        // --- B. EFFET FLOU (Blur) au scroll ---
        // On ne l'affiche que si on commence à scroller
        if (scrollProgress > 0.01f) {

            // RenderEffect (Android 12+)
            val renderEffect = if (Build.VERSION.SDK_INT >= 31) {
                RenderEffect.createBlurEffect(80f, 80f, Shader.TileMode.CLAMP).asComposeRenderEffect()
            } else null

            Box(modifier = Modifier.fillMaxSize().graphicsLayer {
                alpha = scrollProgress.coerceIn(0f, 1f)
                if (renderEffect != null) this.renderEffect = renderEffect
            }) {
                // Fond semi-opaque pour cacher l'image nette dessous
                Box(Modifier.fillMaxSize().background(if(isDark) Color(0xFF181818) else Color(0xFFF3F3F3)))

                // Image floue manuelle (Fallback Android <12 ou style artistique)
                Crossfade(targetState = blurredBitmap, animationSpec = tween(600)) { blurBmp ->
                    if (blurBmp != null) {
                        Image(
                            bitmap = blurBmp.asImageBitmap(),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                            alpha = 0.6f,
                            colorFilter = vibrantColorFilter,
                            filterQuality = FilterQuality.Medium
                        )
                    }
                }

                // Voile sombre supplémentaire en mode sombre
                if (isDark) Box(Modifier.fillMaxSize().background(Color.Black.copy(0.2f)))
            }

            // --- C. GRAIN MICA (Noise) ---
            if (noiseBrush != null) {
                Box(Modifier.fillMaxSize().graphicsLayer { alpha = 0.06f }.background(noiseBrush))
            }
        }
    }
}

// ========================================================================
// 3. MOTEUR BITMAP (Ton code d'origine)
// ========================================================================
object BitmapEngine {
    private const val MAX_TEXTURE_SIZE = 2048 // Limite OpenGL safe

    fun decodeSampledBitmapFromUri(context: Context, uri: Uri, reqWidth: Int, reqHeight: Int): Bitmap? {
        return try {
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }

            options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)
            options.inJustDecodeBounds = false
            options.inPreferredConfig = Bitmap.Config.ARGB_8888

            var result: Bitmap? = null
            context.contentResolver.openInputStream(uri)?.use { result = BitmapFactory.decodeStream(it, null, options) }
            checkTextureSize(result)
        } catch (e: Exception) { null }
    }

    fun decodeSampledBitmapFromResource(context: Context, resId: Int, reqWidth: Int, reqHeight: Int): Bitmap? {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeResource(context.resources, resId, options)

        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)
        options.inJustDecodeBounds = false

        return checkTextureSize(BitmapFactory.decodeResource(context.resources, resId, options))
    }

    fun createBlurryThumbnail(original: Bitmap, width: Int): Bitmap {
        val ratio = width.toFloat() / original.width
        val height = (original.height * ratio).toInt()
        return try {
            // filter = true pour un lissage bilinéaire (moins de pixels visibles)
            Bitmap.createScaledBitmap(original, width, height, true)
        } catch (e: Exception) { original }
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val (height: Int, width: Int) = options.run { outHeight to outWidth }
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    private fun checkTextureSize(bitmap: Bitmap?): Bitmap? {
        if (bitmap == null) return null
        if (bitmap.width > MAX_TEXTURE_SIZE || bitmap.height > MAX_TEXTURE_SIZE) {
            val ratio = min(MAX_TEXTURE_SIZE.toFloat() / bitmap.width, MAX_TEXTURE_SIZE.toFloat() / bitmap.height)
            return try {
                Bitmap.createScaledBitmap(bitmap, (bitmap.width * ratio).toInt(), (bitmap.height * ratio).toInt(), true)
            } catch (e: Exception) { bitmap }
        }
        return bitmap
    }
}