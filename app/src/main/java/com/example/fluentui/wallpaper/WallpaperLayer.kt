package com.example.fluentui.wallpaper

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.graphics.RenderEffect
import android.graphics.Shader
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
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

    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var blurredBitmap by remember { mutableStateOf<Bitmap?>(null) }

    val vibrantColorFilter = remember(isDark) {
        val matrix = ColorMatrix()
        if (isDark) matrix.setToSaturation(1.6f) else matrix.setToSaturation(1.3f)
        ColorFilter.colorMatrix(matrix)
    }

    val noiseBrush = remember(isDark) {
        try { NoiseGenerator.generate(isDark) } catch (e: Exception) { null }
    }

    val displayMetrics = remember { context.resources.displayMetrics }

    // =========================================================================
    // CHARGEMENT
    // =========================================================================
    LaunchedEffect(settings.wallpaperType, settings.wallpaperValue) {
        withContext(Dispatchers.IO) {
            val loadedBitmap = try {
                when (settings.wallpaperType) {
                    "FILE" -> {
                        val file = File(settings.wallpaperValue)
                        if (file.exists()) BitmapEngine.decodeSampledBitmapFromFile(
                            file, displayMetrics.widthPixels, displayMetrics.heightPixels
                        ) else null
                    }
                    "RES" -> {
                        val id = settings.wallpaperValue.toIntOrNull()
                        if (id != null) BitmapEngine.decodeSampledBitmapFromResource(
                            context, id,
                            displayMetrics.widthPixels, displayMetrics.heightPixels
                        ) else null
                    }
                    "URI" -> {
                        val uri = Uri.parse(settings.wallpaperValue)
                        BitmapEngine.decodeSampledBitmapFromUri(
                            context, uri,
                            displayMetrics.widthPixels, displayMetrics.heightPixels
                        )
                    }
                    else -> null
                }
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }

            val blurBmp = loadedBitmap?.let {
                BitmapEngine.createBlurryThumbnail(it, width = 300)
            }

            withContext(Dispatchers.Main) {
                bitmap = loadedBitmap
                blurredBitmap = blurBmp
            }
        }
    }

    // Parallax subtil — 2% max, spring pour coller au doigt sans effet PowerPoint
    val parallaxOffset by animateFloatAsState(
        targetValue = scrollProgress * -0.02f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "ParallaxOffset"
    )

    // =========================================================================
    // RENDU — TON DESIGN ORIGINAL
    // =========================================================================
    Box(modifier = modifier.fillMaxSize().clipToBounds()) {

        // --- A. FOND NET avec Parallax ---
        Crossfade(targetState = bitmap, animationSpec = tween(600), label = "Wp") { bmp ->
            if (bmp != null) {
                Box(modifier = Modifier.fillMaxSize().clipToBounds()) {
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                scaleX = 1.08f
                                scaleY = 1.08f
                                translationY = size.height * parallaxOffset
                            }
                    )
                }
            } else {
                val solidColor = try {
                    Color(settings.wallpaperValue.toInt())
                } catch (e: Exception) { Color(0xFF202020) }
                Box(Modifier.fillMaxSize().background(solidColor))
            }
        }

        // --- B. EFFET FLOU au scroll ---
        if (scrollProgress > 0.01f) {

            val renderEffect = if (Build.VERSION.SDK_INT >= 31) {
                RenderEffect.createBlurEffect(80f, 80f, Shader.TileMode.CLAMP)
                    .asComposeRenderEffect()
            } else null

            Box(modifier = Modifier.fillMaxSize().clipToBounds().graphicsLayer {
                alpha = scrollProgress.coerceIn(0f, 1f)
                if (renderEffect != null) this.renderEffect = renderEffect
            }) {
                Box(Modifier.fillMaxSize().background(
                    if (isDark) Color(0xFF181818) else Color(0xFFF3F3F3)
                ))

                Crossfade(targetState = blurredBitmap, animationSpec = tween(600), label = "BlurWp") { blurBmp ->
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

                if (isDark) Box(Modifier.fillMaxSize().background(Color.Black.copy(0.2f)))
            }

            // --- C. GRAIN MICA ---
            if (noiseBrush != null) {
                Box(Modifier.fillMaxSize().graphicsLayer { alpha = 0.06f }.background(noiseBrush))
            }
        }
    }
}

// ==============================================================================
// BITMAP ENGINE
// Règle EXIF :
//   API 28+ → ImageDecoder corrige automatiquement → PAS de rotation manuelle
//   API <28 → BitmapFactory ignore l'EXIF → rotation manuelle obligatoire
// ==============================================================================
object BitmapEngine {
    private const val MAX_TEXTURE_SIZE = 2048

    fun decodeSampledBitmapFromUri(
        context: Context, uri: Uri, reqWidth: Int, reqHeight: Int
    ): Bitmap? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                // ImageDecoder corrige l'EXIF seul — AUCUNE rotation manuelle
                val source = ImageDecoder.createSource(context.contentResolver, uri)
                val bmp = ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                    decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                    decoder.isMutableRequired = true
                    var sampleSize = 1
                    while (info.size.width / sampleSize > reqWidth &&
                        info.size.height / sampleSize > reqHeight) {
                        sampleSize *= 2
                    }
                    decoder.setTargetSampleSize(sampleSize)
                }
                checkTextureSize(bmp)
            } else {
                // API < 28 : rotation manuelle obligatoire
                val rotation = context.contentResolver.openInputStream(uri)?.use {
                    getRotationFromExif(ExifInterface(it))
                } ?: 0f

                val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                context.contentResolver.openInputStream(uri)?.use {
                    BitmapFactory.decodeStream(it, null, options)
                }
                options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)
                options.inJustDecodeBounds = false
                options.inPreferredConfig = Bitmap.Config.ARGB_8888

                var decoded: Bitmap? = null
                context.contentResolver.openInputStream(uri)?.use {
                    decoded = BitmapFactory.decodeStream(it, null, options)
                }
                applyRotationAndTextureLimit(decoded, rotation)
            }
        } catch (e: Exception) { e.printStackTrace(); null }
    }

    fun decodeSampledBitmapFromFile(
        file: File, reqWidth: Int, reqHeight: Int
    ): Bitmap? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                // ImageDecoder corrige l'EXIF seul — AUCUNE rotation manuelle
                val source = ImageDecoder.createSource(file)
                val bmp = ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                    decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                    decoder.isMutableRequired = true
                    var sampleSize = 1
                    while (info.size.width / sampleSize > reqWidth &&
                        info.size.height / sampleSize > reqHeight) {
                        sampleSize *= 2
                    }
                    decoder.setTargetSampleSize(sampleSize)
                }
                checkTextureSize(bmp)
            } else {
                // API < 28 : rotation manuelle obligatoire
                val rotation = try {
                    getRotationFromExif(ExifInterface(file.absolutePath))
                } catch (e: Exception) { 0f }

                val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(file.absolutePath, options)
                options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)
                options.inJustDecodeBounds = false
                options.inPreferredConfig = Bitmap.Config.ARGB_8888

                applyRotationAndTextureLimit(
                    BitmapFactory.decodeFile(file.absolutePath, options), rotation
                )
            }
        } catch (e: Exception) { e.printStackTrace(); null }
    }

    fun decodeSampledBitmapFromResource(
        context: Context, resId: Int, reqWidth: Int, reqHeight: Int
    ): Bitmap? {
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
            Bitmap.createScaledBitmap(original, width, height, true)
        } catch (e: Exception) { original }
    }

    private fun getRotationFromExif(exif: ExifInterface): Float =
        when (exif.getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL
        )) {
            ExifInterface.ORIENTATION_ROTATE_90,
            ExifInterface.ORIENTATION_TRANSVERSE    -> 90f
            ExifInterface.ORIENTATION_ROTATE_180,
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> 180f
            ExifInterface.ORIENTATION_ROTATE_270,
            ExifInterface.ORIENTATION_TRANSPOSE     -> 270f
            else                                    -> 0f
        }

    private fun applyRotationAndTextureLimit(bitmap: Bitmap?, rotation: Float): Bitmap? {
        if (bitmap == null) return null
        val result = if (rotation != 0f) {
            val matrix = Matrix().apply { postRotate(rotation) }
            val rotated = Bitmap.createBitmap(
                bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true
            )
            if (rotated != bitmap) bitmap.recycle()
            rotated
        } else bitmap
        return checkTextureSize(result)
    }

    private fun calculateInSampleSize(
        options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int
    ): Int {
        val (height, width) = options.run { outHeight to outWidth }
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2
            while ((halfHeight / inSampleSize) >= reqHeight &&
                (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    private fun checkTextureSize(bitmap: Bitmap?): Bitmap? {
        if (bitmap == null) return null
        if (bitmap.width <= MAX_TEXTURE_SIZE && bitmap.height <= MAX_TEXTURE_SIZE) return bitmap
        val ratio = min(
            MAX_TEXTURE_SIZE.toFloat() / bitmap.width,
            MAX_TEXTURE_SIZE.toFloat() / bitmap.height
        )
        return try {
            val scaled = Bitmap.createScaledBitmap(
                bitmap,
                (bitmap.width * ratio).toInt().coerceAtLeast(1),
                (bitmap.height * ratio).toInt().coerceAtLeast(1),
                true
            )
            if (scaled != bitmap) bitmap.recycle()
            scaled
        } catch (e: Exception) { bitmap }
    }
}