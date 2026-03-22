package com.example.fluentui.wallpaper

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import com.example.fluentui.ui.settings.SettingsState

// ==============================================================================
// WallpaperLayer v2
//
// Ce composable ne fait plus QUE du rendu.
// Tout le travail lourd (I/O, calcul de flou) est délégué au WallpaperRepository.
//
// Résultat :
//   • Aucun freeze au changement de thème dark/light (le repository n'est pas
//     invalidé par isDark, seul wallpaperType/wallpaperValue le sont).
//   • Aucun GPU saturé : l'image floutée est un Bitmap pré-calculé, affiché
//     comme une Image normale — pas de RenderEffect live.
//   • Le composable est résilient : si le repository n'a pas encore chargé,
//     on affiche le fond couleur unie sans attendre.
// ==============================================================================

// Instancier une fois au niveau de l'Activity ou d'un ViewModel,
// puis passer en paramètre ou via CompositionLocal.
// Exemple dans Activity :
//   val wallpaperRepo = remember { WallpaperRepository(applicationContext) }
//   WallpaperLayer(settings, isDark, scrollProgress, repo = wallpaperRepo)

@Composable
fun WallpaperLayer(
    settings: SettingsState,
    isDark: Boolean,
    scrollProgress: Float,
    repo: WallpaperRepository,
    modifier: Modifier = Modifier
) {
    // Déclenche le chargement uniquement si la SOURCE a changé, pas le thème
    LaunchedEffect(settings.wallpaperType, settings.wallpaperValue) {
        repo.load(settings.wallpaperType, settings.wallpaperValue)
    }

    // Collecte le résultat pré-calculé — jamais de calcul lourd ici
    val bitmaps by repo.bitmaps.collectAsState()

    // ColorFilter recalculé uniquement si isDark change (pas au scroll)
    val vibrantColorFilter = remember(isDark) {
        val matrix = ColorMatrix()
        if (isDark) matrix.setToSaturation(1.6f) else matrix.setToSaturation(1.3f)
        ColorFilter.colorMatrix(matrix)
    }

    // Grain Mica recalculé uniquement si isDark change
    val noiseBrush = remember(isDark) {
        try { NoiseGenerator.generate(isDark) } catch (e: Exception) { null }
    }

    // Parallax : spring haute raideur, amortissement total → colle au doigt
    val parallaxOffset by animateFloatAsState(
        targetValue = scrollProgress * -0.02f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "ParallaxOffset"
    )

    Box(modifier = modifier.fillMaxSize().clipToBounds()) {

        // ── Couche A : Image nette avec parallax ──────────────────────────────
        Crossfade(
            targetState = bitmaps?.sharp,
            animationSpec = tween(600),
            label = "WallpaperSharp"
        ) { sharp ->
            if (sharp != null) {
                Box(Modifier.fillMaxSize().clipToBounds()) {
                    Image(
                        bitmap = sharp.asImageBitmap(),
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
                // Fallback couleur unie pendant le chargement
                val solidColor = try {
                    Color(settings.wallpaperValue.toInt())
                } catch (e: Exception) { Color(0xFF202020) }
                Box(Modifier.fillMaxSize().background(solidColor))
            }
        }

        // ── Couche B : Effet Mica au scroll ───────────────────────────────────
        // N'est rendu que si l'utilisateur a scrollé au-delà d'un seuil minimal.
        // L'image floutée est un Bitmap pré-calculé : AUCUN RenderEffect live,
        // AUCUN calcul GPU par frame.
        if (scrollProgress > 0.01f) {

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clipToBounds()
                    .graphicsLayer { alpha = scrollProgress.coerceIn(0f, 1f) }
            ) {
                // Fond couleur de base (absorbe les zones transparentes du blur)
                Box(
                    Modifier.fillMaxSize().background(
                        if (isDark) Color(0xFF181818) else Color(0xFFF3F3F3)
                    )
                )

                // Image pré-floutée — affichée comme n'importe quel Bitmap
                Crossfade(
                    targetState = bitmaps?.blurred,
                    animationSpec = tween(600),
                    label = "WallpaperBlurred"
                ) { blurred ->
                    if (blurred != null) {
                        Image(
                            bitmap = blurred.asImageBitmap(),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                            alpha = 0.6f,
                            colorFilter = vibrantColorFilter,
                            filterQuality = FilterQuality.Medium
                        )
                    }
                }

                // Assombrissement léger en mode sombre
                if (isDark) {
                    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.2f)))
                }
            }

            // Grain Mica — texture noise très subtile
            if (noiseBrush != null) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .graphicsLayer { alpha = 0.06f }
                        .background(noiseBrush)
                )
            }
        }
    }
}