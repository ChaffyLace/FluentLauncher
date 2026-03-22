package com.example.fluentui.ui.motion

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import kotlinx.coroutines.delay

/**
 * FluentLiveTile — animation Live Tile Windows 10 exacte.
 *
 * Comportement :
 *  • [enabled] = true  → cycle automatique entre [frontContent] et [backContent]
 *  • [enabled] = false → reste sur [frontContent], aucune animation, aucune coroutine
 *
 * Animation : slide vertical du bas vers le haut + fade simultané.
 * Easing CubicBezier(0.0, 0.0, 0.2, 1.0) = départ instantané, freinage doux.
 * C'est l'easing mesuré sur les vraies Live Tiles Windows 10 Start Menu.
 *
 * @param enabled           Active/désactive le cycle live. Quand false, isFront
 *                          repasse à true via une dernière animation propre.
 * @param animationInterval Durée d'affichage de chaque face (ms). Défaut 5000ms.
 * @param initialDelay      Délai avant le premier flip. Permet de décaler les tuiles
 *                          entre elles pour éviter qu'elles flippent toutes en même temps.
 * @param frontContent      Face principale — toujours affichée quand disabled.
 * @param backContent       Face secondaire — affichée en alternance quand enabled.
 */
@Composable
fun FluentLiveTile(
    modifier:          Modifier  = Modifier,
    enabled:           Boolean   = true,
    animationInterval: Long      = 5_000L,
    initialDelay:      Long      = 0L,
    frontContent:      @Composable () -> Unit,
    backContent:       @Composable () -> Unit
) {
    var isFront by remember { mutableStateOf(true) }

    LaunchedEffect(enabled, animationInterval, initialDelay) {
        if (!enabled) {
            // Désactivation : retour à la face principale proprement
            isFront = true
            return@LaunchedEffect
        }
        delay(initialDelay)
        while (true) {
            delay(animationInterval)
            isFront = !isFront
        }
    }

    // Easing Windows 10 Live Tile exact :
    // Démarre en pleine vitesse (0.0, 0.0), freine progressivement (0.2, 1.0).
    // Aucun spring, aucun rebond — mouvement propre et mécanique comme Windows 10.
    val windowsEasing = CubicBezierEasing(0.0f, 0.0f, 0.2f, 1.0f)

    // 320ms = durée mesurée sur les Live Tiles Windows 10.
    // 800ms (ancienne valeur) rendait l'animation trop lente et molle.
    val slideSpec = tween<IntOffset>(durationMillis = 320, easing = windowsEasing)
    // Fade légèrement plus court — l'entrée est nette, pas de fondu traînant
    val fadeSpec  = tween<Float>(durationMillis = 200, easing = windowsEasing)

    AnimatedContent(
        targetState  = if (enabled) isFront else true,
        modifier     = modifier,
        transitionSpec = {
            // Toujours du bas vers le haut, jamais l'inverse — Windows 10 exact.
            // Contenu entrant : arrive du bas (initialOffsetY = +height)
            // Contenu sortant : part vers le haut (targetOffsetY = -height)
            // Le fade simultané masque le bord visible en bas de la tuile.
            (slideInVertically(animationSpec = slideSpec) { height -> height } +
                    fadeIn(animationSpec = fadeSpec)) togetherWith
                    (slideOutVertically(animationSpec = slideSpec) { height -> -height } +
                            fadeOut(animationSpec = fadeSpec))
        },
        label = "LiveTile"
    ) { showFront ->
        if (showFront) frontContent() else backContent()
    }
}