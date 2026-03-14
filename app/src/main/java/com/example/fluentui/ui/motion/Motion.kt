package com.example.fluentui.ui.motion

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

// =================================================================================
// 1. CONSTANTES PHYSIQUES (MICROSOFT FLUENT DESIGN)
// =================================================================================
object FluentMotion {
    // Courbes de Bézier officielles Windows 11 (Aucun rebond)
    val EntranceEasing = CubicBezierEasing(0.1f, 0.9f, 0.2f, 1.0f) // Arrivée rapide, freinage doux
    val ExitEasing = CubicBezierEasing(0.7f, 0.0f, 1.0f, 0.5f)     // Départ lent, accélération rapide
    val StandardEasing = FastOutSlowInEasing                       // Déplacements classiques

    // Durées standard de l'OS
    const val DurationFast = 150
    const val DurationNormal = 250
    const val DurationSlow = 400

    // Remplacement des Springs par des Tweens paramétrés pour Fluent
    fun <T> enterSpec(duration: Int = DurationNormal) = tween<T>(duration, easing = EntranceEasing)
    fun <T> exitSpec(duration: Int = DurationFast) = tween<T>(duration, easing = ExitEasing)
    fun <T> standardSpec(duration: Int = DurationNormal) = tween<T>(duration, easing = StandardEasing)
}

// =================================================================================
// 2. FLUENT TILE (BOUTON PREMIUM) - CORRIGÉ POUR LE DRAG & DROP
// =================================================================================
@Composable
fun FluentTile(
    modifier: Modifier = Modifier,
    isPressed: Boolean = false, // L'état est maintenant géré par le parent (ex: DashboardGrid)
    isLaunching: Boolean = false,
    isDragging: Boolean = false,
    dragProgress: Float = 0f,
    cornerRadius: Dp = 16.dp,   // Aligné sur le standard défini dans MicaTile
    primaryColor: Color? = null,
    content: @Composable BoxScope.() -> Unit
) {
    // SCALES COMBINÉES (Lancement > Glissement > Pression)
    val targetScale = when {
        isLaunching -> 1.5f
        isDragging -> 1.02f // Subtil soulèvement au lieu d'un gros zoom
        isPressed -> 0.97f  // Valeur exacte de la doc Microsoft (pas 0.94)
        else -> 1f
    }

    val scaleAnim by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = when {
            isLaunching -> FluentMotion.enterSpec(400)
            isPressed -> tween(100, easing = LinearOutSlowInEasing) // Descente ultra-rapide
            else -> FluentMotion.standardSpec() // Remontée fluide
        },
        label = "TileScale"
    )

    // ÉLÉVATION (Le glissement a la priorité)
    val targetElevation = when {
        isLaunching -> 0.dp
        isDragging -> 8.dp // Ombre plus douce
        isPressed -> 0.dp
        else -> 0.dp
    }

    val elevation by animateDpAsState(
        targetValue = targetElevation,
        animationSpec = FluentMotion.standardSpec(),
        label = "TileElevation"
    )

    // ROTATION SUBTILE pendant le glissement
    val rotation by animateFloatAsState(
        targetValue = if (isDragging) dragProgress * 3f else 0f,
        animationSpec = FluentMotion.standardSpec(150),
        label = "TileRotation"
    )

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scaleAnim
                scaleY = scaleAnim
                shadowElevation = elevation.toPx()
                rotationZ = rotation
                shape = RoundedCornerShape(cornerRadius)
                clip = false // Laisse le contenu gérer sa propre coupe pour ne pas abîmer les bordures
                transformOrigin = TransformOrigin.Center
            }
            .then(if (primaryColor != null) Modifier.background(primaryColor, RoundedCornerShape(cornerRadius)) else Modifier)
        // Supprimé : pointerInput et detectTapGestures.
        // La détection des gestes doit être faite par la grille pour ne pas bloquer le Drag & Drop.
    ) {
        content()
    }
}

// =================================================================================
// 3. DRAG PLACEHOLDER (Zone fantôme)
// =================================================================================
@Composable
fun FluentDragPlaceholder(
    isVisible: Boolean,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 16.dp
) {
    val alpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = FluentMotion.exitSpec(),
        label = "PlaceholderAlpha"
    )

    val scale by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0.85f,
        animationSpec = FluentMotion.enterSpec(),
        label = "PlaceholderScale"
    )

    if (alpha > 0.01f) {
        Box(
            modifier = modifier
                .graphicsLayer {
                    this.alpha = alpha
                    scaleX = scale
                    scaleY = scale
                }
                .background(
                    color = Color.White.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(cornerRadius)
                )
                .border(
                    width = 2.dp,
                    color = Color.White.copy(alpha = 0.80f),
                    shape = RoundedCornerShape(cornerRadius)
                )
        )
    }
}

// =================================================================================
// 4. LAUNCHER EXIT ANIMATION (Effet d'enfoncement de l'écran d'accueil)
// =================================================================================
@Composable
fun FluentDashboardMotion(
    isAppLaunching: Boolean,
    content: @Composable () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (isAppLaunching) 0.92f else 1f,
        animationSpec = FluentMotion.enterSpec(400),
        label = "DashboardScale"
    )

    val alpha by animateFloatAsState(
        targetValue = if (isAppLaunching) 0f else 1f,
        animationSpec = FluentMotion.exitSpec(300),
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
// 5. TUILES DYNAMIQUES (LIVE TILES)
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

    Box(modifier = modifier.clip(RoundedCornerShape(16.dp))) {
        AnimatedContent(
            targetState = isFront,
            transitionSpec = {
                slideInVertically(FluentMotion.enterSpec(500)) { it } togetherWith
                        slideOutVertically(FluentMotion.exitSpec(500)) { -it }
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

    val rotation by animateFloatAsState(
        targetValue = if (isFront) 0f else 180f,
        animationSpec = FluentMotion.standardSpec(600),
        label = "FlipAnim"
    )

    Box(
        modifier = modifier
            .graphicsLayer {
                rotationX = rotation
                cameraDistance = 12f * density
            }
    ) {
        if (rotation <= 90f || rotation >= 270f) {
            Box(Modifier.fillMaxSize()) { frontContent() }
        } else {
            Box(Modifier.fillMaxSize().graphicsLayer { rotationX = 180f }) { backContent() }
        }
    }
}

// =================================================================================
// 6. ANIMATIONS GLOBALES (REVEAL, OVERSCROLL)
// =================================================================================
@Composable
fun FluentReveal(visible: Boolean, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val transition = updateTransition(visible, label = "Reveal")

    val alpha by transition.animateFloat(
        transitionSpec = { if (targetState) FluentMotion.enterSpec() else FluentMotion.exitSpec() },
        label = "Alpha"
    ) { if (it) 1f else 0f }

    val scale by transition.animateFloat(
        transitionSpec = { if (targetState) FluentMotion.enterSpec() else FluentMotion.exitSpec() },
        label = "Scale"
    ) { if (it) 1f else 0.95f }

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
    Box(
        modifier = Modifier.graphicsLayer {
            translationX = -scrollProgress * 60f
            scaleX = 1.05f // Réduit pour éviter la distorsion excessive
            scaleY = 1.05f
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
    // Note: L'overscroll elastique est une pratique iOS. Sur Windows, c'est généralement
    // un hard stop visuel, mais on garde une très légère inertie ici si tu y tiens.
    var overscrollOffset by remember { mutableFloatStateOf(0f) }

    val animatedOverscroll by animateFloatAsState(
        targetValue = overscrollOffset,
        animationSpec = FluentMotion.standardSpec(150),
        label = "Overscroll"
    )

    LaunchedEffect(overscrollOffset) {
        if (overscrollOffset != 0f) {
            delay(50)
            overscrollOffset = 0f
        }
    }

    val connection = remember {
        object : androidx.compose.ui.input.nestedscroll.NestedScrollConnection {
            override fun onPostScroll(
                consumed: androidx.compose.ui.geometry.Offset,
                available: androidx.compose.ui.geometry.Offset,
                source: androidx.compose.ui.input.nestedscroll.NestedScrollSource
            ): androidx.compose.ui.geometry.Offset {
                if (source == androidx.compose.ui.input.nestedscroll.NestedScrollSource.Drag) {
                    overscrollOffset += available.y * 0.1f // Très résistif
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