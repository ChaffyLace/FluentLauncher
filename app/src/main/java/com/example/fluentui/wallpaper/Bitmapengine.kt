package com.example.fluentui.wallpaper

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import kotlin.math.min

// ==============================================================================
// BitmapEngine v2
//
// Changement par rapport à v1 :
//   • createBlurryThumbnail() renommé createScaledThumbnail() — il ne floutait
//     pas, et son ancien nom était trompeur. Le flou est maintenant géré par
//     SoftwareBlur, appelé par WallpaperRepository après ce redimensionnement.
// ==============================================================================
object BitmapEngine {
    private const val MAX_TEXTURE_SIZE = 2048

    fun decodeSampledBitmapFromUri(
        context: Context, uri: Uri, reqWidth: Int, reqHeight: Int
    ): Bitmap? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
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
        file: java.io.File, reqWidth: Int, reqHeight: Int
    ): Bitmap? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
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

    // Redimensionne vers targetWidth px de large — le flou est appliqué APRÈS
    // par SoftwareBlur, dans WallpaperRepository.
    fun createScaledThumbnail(original: Bitmap, targetWidth: Int): Bitmap {
        val ratio = targetWidth.toFloat() / original.width
        val targetHeight = (original.height * ratio).toInt().coerceAtLeast(1)
        return try {
            Bitmap.createScaledBitmap(original, targetWidth, targetHeight, true)
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