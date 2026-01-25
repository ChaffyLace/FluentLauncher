package com.example.fluentui.ui.motion

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.fluentui.ui.system.rememberHaptics
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

// =================================================================================
// 1. CONSTANTES PHYSIQUES (MICROSOFT FLUENT)
// =================================================================================
object FluentMotion {
    // Courbes de Bézier précises de Windows 11
    val DecelerateEasing = CubicBezierEasing(0.1f, 0.9f, 0.2f, 1.0f) // Entrée douce
    val AccelerateEasing = CubicBezierEasing(0.7f, 0.0f, 1.0f, 0.5f) // Sortie rapide

    // Durées standard
    const val DurationFast = 150
    const val DurationNormal = 300
    const val DurationSlow = 500

    // Specs physiques (Spring) pour un feeling "Organique"
    fun <T> springFluent(
        stiffness: Float = Spring.StiffnessMediumLow, // 400f - Un peu mou mais réactif
        dampingRatio: Float = 0.85f // Pas trop de rebond, juste ce qu'il faut
    ) = spring<T>(dampingRatio, stiffness)

    fun <T> pageScrollSpec() = spring<T>(stiffness = Spring.StiffnessLow, dampingRatio = 1f)
    fun <T> enterSpec(duration: Int) = tween<T>(duration, easing = DecelerateEasing)
}

// =================================================================================
// 2. FLUENT TILE (BOUTON PREMIUM)
// =================================================================================
@Composable
fun FluentTile(
    modifier: Modifier = Modifier,
    isClickable: Boolean = true,
    onClick: () -> Unit = {},
    cornerRadius: Dp = 12.dp,
    primaryColor: Color? = null,
    // Ajout d'un paramètre pour gérer l'animation de lancement
    isLaunching: Boolean = false,
    content: @Composable BoxScope.() -> Unit
) {
    // État de la pression (Click)
    var isPressed by remember { mutableStateOf(false) }
    val haptics = rememberHaptics()

    // 1. Animation d'échelle (Le "Squish" au clic)
    // Si on lance l'app, la tuile s'agrandit légèrement avant de disparaître (effet zoom in)
    // Sinon, elle rétrécit au clic (effet bouton)
    val targetScale = if (isLaunching) 1.5f else if (isPressed) 0.94f else 1f
    val scaleAnim by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = if (isLaunching) tween(400, easing = FluentMotion.DecelerateEasing) else spring(dampingRatio = 0.7f, stiffness = 600f),
        label = "TileScale"
    )

    // 2. Animation d'élévation/Ombre
    val elevation by animateDpAsState(
        targetValue = if (isPressed) 1.dp else if (isLaunching) 0.dp else 4.dp,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "TileElevation"
    )

    // 3. Animation d'inclinaison (Tilt) - Optionnel mais très premium
    // Pour l'instant on reste simple pour la perf, mais le graphicsLayer est prêt.

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scaleAnim
                scaleY = scaleAnim
                shadowElevation = elevation.toPx()
                shape = RoundedCornerShape(cornerRadius)
                clip = true
                // Point de pivot au centre pour un zoom équilibré
                transformOrigin = TransformOrigin.Center
            }
            .then(if (primaryColor != null) Modifier.background(primaryColor) else Modifier)
            .pointerInput(isClickable) {
                if (isClickable) {
                    detectTapGestures(
                        onPress = {
                            if (!isLaunching) { // Bloque le clic si l'anim de lancement est en cours
                                isPressed = true
                                haptics.performPress()
                                tryAwaitRelease()
                                isPressed = false
                                haptics.performRelease()
                            }
                        },
                        onTap = {
                            if (!isLaunching) onClick()
                        }
                    )
                }
            }
    ) {
        content()
    }
}

// =================================================================================
// 3. LAUNCHER EXIT ANIMATION (L'EFFET "RECUL")
// =================================================================================
// Utilise ce wrapper autour de ton Grid de tuiles dans le DashboardPage
@Composable
fun FluentDashboardMotion(
    isAppLaunching: Boolean,
    content: @Composable () -> Unit
) {
    // Quand une app se lance, le reste du dashboard recule et devient flou
    val scale by animateFloatAsState(
        targetValue = if (isAppLaunching) 0.90f else 1f,
        animationSpec = tween(400, easing = FluentMotion.DecelerateEasing),
        label = "DashboardScale"
    )

    val alpha by animateFloatAsState(
        targetValue = if (isAppLaunching) 0f else 1f,
        animationSpec = tween(300), // Disparaît un peu plus vite que le zoom
        label = "DashboardAlpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                this.alpha = alpha
            }
    ) {
        content()
    }
}

// =================================================================================
// 4. LES AUTRES TUILES ANIMÉES (PEEK & FLIP)
// =================================================================================
@Composable
fun FluentAnimLiveTile(
    modifier: Modifier = Modifier,
    frontContent: @Composable () -> Unit,
    backContent: @Composable () -> Unit,
    animationInterval: Long = 5000,
    initialDelay: Long = Random.nextLong(0, 3000)
) {
    var isFront by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        delay(initialDelay)
        while (true) {
            delay(animationInterval)
            isFront = !isFront
        }
    }
    Box(modifier = modifier.clip(RoundedCornerShape(12.dp))) { // CornerRadius unifié
        AnimatedContent(
            targetState = isFront,
            transitionSpec = {
                // Animation de glissement fluide style Windows
                slideInVertically(tween(600, easing = FluentMotion.DecelerateEasing)) { it } togetherWith
                        slideOutVertically(tween(600, easing = FluentMotion.DecelerateEasing)) { -it }
            },
            label = "LiveTileAnim"
        ) { showFront ->
            if (showFront) frontContent() else backContent()
        }
    }
}

@Composable
fun FluentAnimFlipTile(
    modifier: Modifier = Modifier,
    frontContent: @Composable () -> Unit,
    backContent: @Composable () -> Unit,
    flipInterval: Long = 6000,
    initialDelay: Long = Random.nextLong(0, 4000)
) {
    var isFront by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        delay(initialDelay)
        while (true) {
            delay(flipInterval)
            isFront = !isFront
        }
    }
    // Flip 3D avec perspective
    val rotation by animateFloatAsState(
        targetValue = if (isFront) 0f else 180f,
        animationSpec = spring(stiffness = Spring.StiffnessLow, dampingRatio = 0.7f), // Rebond agréable
        label = "FlipAnim"
    )

    Box(
        modifier = modifier
            .graphicsLayer {
                rotationX = rotation
                cameraDistance = 12f * density // Ajoute de la profondeur 3D
            }
    ) {
        // Gestion de la visibilité des faces (Backface Culling manuel)
        if (rotation <= 90f || rotation >= 270f) {
            Box(Modifier.fillMaxSize()) { frontContent() }
        } else {
            Box(Modifier.fillMaxSize().graphicsLayer { rotationX = 180f }) { backContent() }
        }
    }
}

// =================================================================================
// 5. ANIMATIONS GLOBALES (REVEAL, OVERSCROLL)
// =================================================================================

@Composable
fun FluentReveal(visible: Boolean, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    // Entrée : Zoom In + Fade In
    // Sortie : Zoom Out + Fade Out
    val transition = updateTransition(visible, label = "Reveal")

    val alpha by transition.animateFloat(
        transitionSpec = { tween(FluentMotion.DurationNormal) },
        label = "Alpha"
    ) { if (it) 1f else 0f }

    val scale by transition.animateFloat(
        transitionSpec = {
            if (targetState) spring(dampingRatio = 0.8f) else tween(FluentMotion.DurationFast)
        },
        label = "Scale"
    ) { if (it) 1f else 0.92f } // 0.92 -> 1.0 (Effet Pop)

    if (alpha > 0f) {
        Box(
            modifier = modifier.graphicsLayer {
                this.alpha = alpha
                scaleX = scale
                scaleY = scale
            }
        ) {
            content()
        }
    }
}

@Composable
fun FluentParallax(scrollProgress: Float, content: @Composable () -> Unit) {
    // Effet Parallaxe subtil sur l'axe X
    Box(
        modifier = Modifier.graphicsLayer {
            translationX = -scrollProgress * 60f // Un peu plus prononcé
            // On agrandit un peu le fond pour éviter les bords noirs lors du mouvement
            scaleX = 1.1f
            scaleY = 1.1f
        }
    ) {
        content()
    }
}

@Composable
fun FluentScrollView(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    // Overscroll élastique "Rubber Band"
    var overscrollOffset by remember { mutableStateOf(0f) }
    val animatedOverscroll by animateFloatAsState(
        targetValue = overscrollOffset,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 400f), // Très élastique
        label = "RubberBand"
    )

    // Remise à zéro automatique
    LaunchedEffect(overscrollOffset) {
        if (overscrollOffset != 0f) {
            delay(50) // Petit délai pour laisser l'utilisateur voir l'étirement
            overscrollOffset = 0f
        }
    }

    val connection = remember {
        object : androidx.compose.ui.input.nestedscroll.NestedScrollConnection {
            override fun onPostScroll(consumed: androidx.compose.ui.geometry.Offset, available: androidx.compose.ui.geometry.Offset, source: androidx.compose.ui.input.nestedscroll.NestedScrollSource): androidx.compose.ui.geometry.Offset {
                // Facteur 0.2f pour une résistance forte (sensation lourde)
                if (source == androidx.compose.ui.input.nestedscroll.NestedScrollSource.Drag) {
                    overscrollOffset += available.y * 0.2f
                    return androidx.compose.ui.geometry.Offset(0f, available.y)
                }
                return androidx.compose.ui.geometry.Offset.Zero
            }
        }
    }

    Box(
        modifier = modifier
            .nestedScroll(connection)
            .graphicsLayer { translationY = animatedOverscroll }
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(contentPadding)
        ) {
            content()
        }
    }
}