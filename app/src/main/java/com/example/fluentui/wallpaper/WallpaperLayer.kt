package com.example.fluentui.wallpaper

import android.app.WallpaperManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.palette.graphics.Palette
import com.example.fluentui.R
import com.example.fluentui.ui.motion.FluentParallax
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.random.Random

// ============================================================================================
// COMPONENT: WALLPAPER LAYER (ENGINE)
// ============================================================================================

/**
 * Moteur de rendu du fond d'écran "OEM-Grade".
 *
 * Architecture des couches (Z-Index croissant) :
 * 1. Sharp Bitmap (Image nette) + Parallaxe
 * 2. Dimming Layer (Ombre permanente pour contraste texte)
 * 3. Black Curtain (Rideau noir pour transition Dashboard -> Grid)
 * 4. Mica Layer (Flou + Saturation + Noise + Tint)
 *
 * @param sharpBitmap Bitmap haute résolution pour le fond statique.
 * @param blurredBitmap Bitmap basse résolution pré-calculé pour l'effet Mica.
 * @param motionProgress Valeur normalisée [0.0, 1.0] pilotant l'ensemble des animations.
 * @param isDark Thème système actuel.
 */
@Composable
fun WallpaperLayer(
    sharpBitmap: Bitmap,
    blurredBitmap: Bitmap?,
    motionProgress: Float,
    isDark: Boolean
) {
    // Calcul des alphas basés sur la physique du mouvement.
    // Curtain : Devient opaque à 85% lors du passage au Grid.
    val curtainAlpha = (0.85f * motionProgress).coerceIn(0f, 0.95f)
    // Mica : Apparition linéaire.
    val micaAlpha = motionProgress.coerceIn(0f, 1f)

    // LAYER 1: BASE & PARALLAXE
    FluentParallax(scrollProgress = motionProgress) {
        Box(modifier = Modifier.fillMaxSize()) {
            Image(
                bitmap = sharpBitmap.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                filterQuality = FilterQuality.High // Priorité à la netteté ici
            )

            // LAYER 2: DIMMING (Contraste)
            if (isDark) {
                Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.2f)))
            }
        }
    }

    // LAYER 3: BLACK CURTAIN (Transition)
    // Optimisation GPU : On utilise graphicsLayer pour éviter le redessin de la Box
    if (curtainAlpha > 0f) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = curtainAlpha }
                .background(Color.Black)
        )
    }

    // LAYER 4: MICA EFFECT (Blur/Noise/Sat)
    // Condition de sortie rapide : ne pas rendre si invisible ou bitmap manquant
    if (micaAlpha > 0.01f && blurredBitmap != null) {
        MicaEffectLayer(
            blurredBitmap = blurredBitmap,
            alpha = micaAlpha,
            isDark = isDark
        )
    }
}

/**
 * Sous-composant gérant l'effet "Mica" (Acrylique) de Windows 11.
 * Combine : Blur + Saturation Boost + Noise Texture + Color Tint.
 */
@Composable
private fun MicaEffectLayer(
    blurredBitmap: Bitmap,
    alpha: Float,
    isDark: Boolean
) {
    // Memoization des configurations coûteuses pour éviter le recalcul à chaque frame (60fps target)
    val config = remember(isDark) { EffectConfig.from(isDark) }
    val dominantColor = remember(blurredBitmap) { ColorExtractor.extractSmartColor(blurredBitmap, isDark) }
    val noiseBrush = remember(isDark) { NoiseGenerator.generate(isDark) }
    val saturationMatrix = remember { ColorMatrix().apply { setToSaturation(1.4f) } }

    // Utilisation de RenderEffect (Android 12+) pour un flou GPU natif si disponible
    val blurRenderEffect = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // MODIFICATION : Augmentation du rayon de flou (50f -> 90f)
            RenderEffect.createBlurEffect(90f, 90f, Shader.TileMode.CLAMP).asComposeRenderEffect()
        } else null
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                this.alpha = alpha
                // Effet de profondeur : Zoom In léger (105% -> 100%) lors de l'apparition
                val scale = 1.05f - (0.05f * alpha)
                scaleX = scale
                scaleY = scale
                if (blurRenderEffect != null) renderEffect = blurRenderEffect
            }
    ) {
        // 1. Base Floue & Saturée
        Image(
            bitmap = blurredBitmap.asImageBitmap(),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            colorFilter = ColorFilter.colorMatrix(saturationMatrix)
        )

        // 2. Surface Tint (Uniformisation)
        Box(Modifier.fillMaxSize().background(config.surfaceColor.copy(alpha = config.surfaceAlpha)))

        // 3. Dynamic Tint (Colorisation basée sur le wallpaper)
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    listOf(dominantColor.copy(alpha = config.tintAlpha), dominantColor.copy(alpha = 0f))
                )
            )
        )

        // 4. Noise (Grain de texture)
        Box(Modifier.fillMaxSize().background(noiseBrush, alpha = config.noiseAlpha))
    }
}

// ============================================================================================
// SYSTEM HOOKS & HELPERS
// ============================================================================================

/**
 * Hook d'état pour charger et surveiller le fond d'écran système.
 * Implémente un BroadcastReceiver pour le rechargement à chaud (Hot-Reload).
 *
 * @return Pair(SharpBitmap?, BlurredBitmap?)
 */
@Composable
fun rememberSystemWallpaper(context: Context): Pair<Bitmap?, Bitmap?> {
    var bitmaps by remember { mutableStateOf<Pair<Bitmap?, Bitmap?>>(null to null) }

    // Compteur de version pour forcer la recomposition du LaunchedEffect
    var reloadTrigger by remember { mutableIntStateOf(0) }

    // Lifecycle : Enregistrement du Receiver
    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                reloadTrigger++ // Signal de rechargement
            }
        }
        // Écoute les changements de wallpaper système
        val filter = IntentFilter(Intent.ACTION_WALLPAPER_CHANGED)
        context.registerReceiver(receiver, filter)
        onDispose { context.unregisterReceiver(receiver) }
    }

    // Worker : Chargement asynchrone (IO Thread)
    LaunchedEffect(reloadTrigger) {
        withContext(Dispatchers.IO) {
            val manager = WallpaperManager.getInstance(context)

            // Gestion de compatibilité API 34 (UpsideDownCake) vs Legacy
            val drawable = try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE &&
                    context.checkSelfPermission(android.Manifest.permission.READ_MEDIA_IMAGES) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    manager.getDrawable(WallpaperManager.FLAG_SYSTEM)
                } else {
                    manager.drawable
                }
            } catch (e: SecurityException) {
                // Fallback sécurisé en cas de permission refusée
                ContextCompat.getDrawable(context, R.drawable.windows_11_dark)
            } catch (e: Exception) {
                null
            }

            val original = drawable?.toBitmap()

            if (original != null) {
                // OPTIMISATION RAM : Downsampling intelligent
                // On limite la largeur à 2560px (QHD+) pour éviter les OOM sur les images 4K/8K
                val sharp = if (original.width > 2560) {
                    val ratio = 2560f / original.width
                    Bitmap.createScaledBitmap(original, 2560, (original.height * ratio).toInt(), true)
                } else original

                // OPTIMISATION PERF : Pre-Blur Downsampling
                // MODIFICATION : Réduction encore plus forte de la taille (400 -> 200)
                // pour un flou par étirement plus prononcé sur les anciens appareils.
                val blurRatio = 200f / sharp.width
                val smallForBlur = Bitmap.createScaledBitmap(sharp, 200, (sharp.height * blurRatio).toInt(), true)

                bitmaps = sharp to smallForBlur
            }
        }
    }
    return bitmaps
}

/**
 * Singleton utilitaire pour l'extraction de palette (Material You / Fluent logic).
 */
private object ColorExtractor {
    fun extractSmartColor(bitmap: Bitmap, isDark: Boolean): Color {
        return runCatching {
            val small = Bitmap.createScaledBitmap(bitmap, 50, 50, false)
            val palette = Palette.from(small).generate()
            val fallback = if (isDark) 0xFF000000.toInt() else 0xFFFFFFFF.toInt()

            // Préférence pour les couleurs vibrantes en mode sombre pour le "pop"
            val color = if (isDark) palette.getDarkVibrantColor(palette.getDominantColor(fallback))
            else palette.getLightVibrantColor(palette.getDominantColor(fallback))

            Color(color)
        }.getOrElse { Color.Transparent }
    }
}

/**
 * Générateur de texture de bruit procédurale (Noise).
 * Utilise un cache pour éviter de régénérer le Shader à chaque appel.
 */
private object NoiseGenerator {
    private val shaderCache = mutableMapOf<Boolean, ShaderBrush>()

    fun generate(isDark: Boolean): ShaderBrush {
        if (shaderCache.containsKey(isDark)) return shaderCache[isDark]!!

        val size = 64
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(size * size)
        val rnd = Random(42) // Seed fixe pour consistance
        val base = if (isDark) 0xFFFFFF else 0x000000

        // Génération pixel par pixel
        for (i in pixels.indices) {
            val alpha = rnd.nextInt(40) + 10 // Alpha aléatoire [10, 50]
            pixels[i] = (alpha shl 24) or base
        }

        val brush = ShaderBrush(BitmapShader(bmp, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT))
        shaderCache[isDark] = brush
        return brush
    }
}

/**
 * Configuration des constantes visuelles pour l'effet Mica.
 */
private data class EffectConfig(
    val surfaceColor: Color,
    val surfaceAlpha: Float,
    val tintAlpha: Float,
    val noiseAlpha: Float
) {
    companion object {
        fun from(isDark: Boolean) = if (isDark)
        // MODIFICATION : noiseAlpha augmenté de 0.04f à 0.08f
            EffectConfig(Color(0xFF181818), 0.60f, 0.30f, 0.08f)
        else
        // MODIFICATION : noiseAlpha augmenté de 0.03f à 0.06f
            EffectConfig(Color(0xFFF5F5F5), 0.55f, 0.40f, 0.06f)
    }
}