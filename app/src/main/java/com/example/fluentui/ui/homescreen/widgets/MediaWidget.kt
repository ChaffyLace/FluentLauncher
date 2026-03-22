package com.example.fluentui.ui.homescreen.widgets

import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.util.Log
import androidx.compose.animation.AnimatedContent
import com.example.fluentui.ui.motion.FluentMotion
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fluentui.ui.system.TileConfig
import com.example.fluentui.ui.system.TileSize
import fluent.ui.system.icons.FluentIcons
import fluent.ui.system.icons.regular.Next
import fluent.ui.system.icons.regular.Pause
import fluent.ui.system.icons.regular.Play
import fluent.ui.system.icons.regular.Previous
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

private const val TAG = "MediaWidget"

// ============================================================================
// CONSTANTS
// ============================================================================

/** États acceptés pour qu'un widget reste affiché. La pause ne fait pas disparaître. */
private val ACTIVE_STATES = setOf(
    PlaybackState.STATE_PLAYING,
    PlaybackState.STATE_PAUSED,
    PlaybackState.STATE_BUFFERING,
    PlaybackState.STATE_CONNECTING
)

/** Intervalle de poll — 500ms pour un play/pause réactif sans surcharger le CPU. */
private const val POLL_INTERVAL_MS = 500L

// ============================================================================
// DATA MODEL
// ============================================================================

/**
 * État immutable du lecteur.
 *
 * [hasSession] = true tant qu'une session existe pour l'app cible, qu'elle joue ou soit
 * en pause. Seule l'absence de session remet le widget en mode "icône normale".
 *
 * [albumArt] est EXCLU de [visualKey] : Android recrée un Bitmap object à chaque
 * getBitmap() même pour la même image, donc son hashCode change à chaque poll
 * et déclencherait AnimatedContent toutes les 500ms → flash constant.
 */
private data class MediaState(
    val title:      String  = "",
    val artist:     String  = "",
    val appName:    String  = "",
    val appColor:   Color   = Color(0xFF1DB954),
    val albumArt:   Bitmap? = null,
    val isPlaying:  Boolean = false,
    val hasSession: Boolean = false
) {
    val visualKey: String get() = "$title|$artist|$isPlaying"
}

// ============================================================================
// DATA LAYER  —  pas de suspend, appelé depuis LaunchedEffect(IO implicite)
// ============================================================================

/**
 * Résout le contrôleur approprié selon le package cible.
 *
 * - [targetPackage] non vide → filtre strict sur l'app épinglée.
 *   Spotify n'affecte jamais Samsung Music et inversement.
 * - [targetPackage] vide → première session dans un état actif.
 *
 * Priorité : STATE_PLAYING > STATE_PAUSED/autres dans les deux modes.
 */
private fun resolveController(context: Context, targetPackage: String): MediaController? {
    return runCatching {
        val mgr = context.getSystemService(Context.MEDIA_SESSION_SERVICE)
                as MediaSessionManager
        val cn  = ComponentName(
            context,
            "${context.packageName}.service.NotificationListener"
        )
        val sessions = mgr.getActiveSessions(cn)
            .filter { it.playbackState?.state in ACTIVE_STATES }

        if (targetPackage.isNotBlank()) {
            // Mode exclusif — uniquement le package de la tuile
            sessions.firstOrNull { it.packageName == targetPackage }
        } else {
            // Mode général — priorité à ce qui joue
            sessions.firstOrNull { it.playbackState?.state == PlaybackState.STATE_PLAYING }
                ?: sessions.firstOrNull()
        }
    }.getOrElse { e ->
        Log.e(TAG, "resolveController failed", e)
        null
    }
}

private fun readMediaState(context: Context, targetPackage: String): MediaState {
    val ctrl = resolveController(context, targetPackage) ?: return MediaState()
    val meta = ctrl.metadata                            ?: return MediaState()

    val title    = meta.getString(MediaMetadata.METADATA_KEY_TITLE).orEmpty()
    val artist   = meta.getString(MediaMetadata.METADATA_KEY_ARTIST)
        ?: meta.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST).orEmpty()
    val albumArt = meta.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
        ?: meta.getBitmap(MediaMetadata.METADATA_KEY_ART)
    val isPlaying = ctrl.playbackState?.state == PlaybackState.STATE_PLAYING

    val appName = runCatching {
        context.packageManager
            .getApplicationLabel(context.packageManager.getApplicationInfo(ctrl.packageName, 0))
            .toString()
    }.getOrDefault(ctrl.packageName)

    return MediaState(
        title      = title,
        artist     = artist,
        appName    = appName,
        albumArt   = albumArt,
        isPlaying  = isPlaying,
        hasSession = title.isNotBlank()
    )
}

/**
 * Envoie une commande de transport.
 * Silencieux en cas d'erreur — ne doit jamais crasher l'UI.
 */
private fun sendAction(context: Context, targetPackage: String, action: Long) {
    runCatching {
        val ctrl = resolveController(context, targetPackage) ?: return
        val t    = ctrl.transportControls               ?: return
        when (action) {
            PlaybackState.ACTION_PLAY             -> t.play()
            PlaybackState.ACTION_PAUSE            -> t.pause()
            PlaybackState.ACTION_SKIP_TO_NEXT     -> t.skipToNext()
            PlaybackState.ACTION_SKIP_TO_PREVIOUS -> t.skipToPrevious()
        }
    }.getOrElse { e -> Log.e(TAG, "sendAction failed", e) }
}

// ============================================================================
// WIDGET PRINCIPAL
// ============================================================================

@Composable
fun MediaWidget(
    config:       TileConfig,
    contentColor: Color,
    accentColor:  Color,
    modifier:     Modifier = Modifier
) {
    val context       = LocalContext.current
    val targetPackage = config.packageName  // vide = mode général

    // ── State granulaire ──────────────────────────────────────────────────────
    // Chaque champ est un State séparé plutôt qu'un seul objet.
    // Avantage : Compose ne recompose que les composants qui lisent le champ
    // qui a changé. isPlaying qui toggle ne recompose pas le titre.
    var title      by remember { mutableStateOf("") }
    var artist     by remember { mutableStateOf("") }
    var appName    by remember { mutableStateOf("") }
    var appColor   by remember { mutableStateOf(Color(0xFF1DB954)) }
    var albumArt   by remember { mutableStateOf<Bitmap?>(null) }
    var isPlaying  by remember { mutableStateOf(false) }
    var hasSession by remember { mutableStateOf(false) }
    // Dernier titre connu — albumArt n'est rechargé que lors d'un changement de chanson,
    // jamais à chaque poll (évite le flash lié à la recréation du Bitmap object)
    var lastTitle  by remember { mutableStateOf("") }

    LaunchedEffect(targetPackage) {
        while (isActive) {
            val s = readMediaState(context, targetPackage)

            hasSession = s.hasSession
            isPlaying  = s.isPlaying
            appName    = s.appName
            appColor   = s.appColor

            // Recharge albumArt si :
            // - la chanson a changé (nouveau titre)
            // - ou albumArt est null alors qu'on a une pochette disponible
            //   (cas du démarrage à froid où lastTitle="" = title="" au premier tick)
            if (s.title != lastTitle || (albumArt == null && s.albumArt != null)) {
                albumArt  = s.albumArt
                lastTitle = s.title
            }

            // title/artist en dernier : leur changement déclenche AnimatedContent
            // après que albumArt est déjà synchronisé → pas de frame où l'image
            // est en retard par rapport au texte
            title  = s.title
            artist = s.artist

            delay(POLL_INTERVAL_MS)
        }
    }

    // ── Routing ───────────────────────────────────────────────────────────────
    // showPlayer : démarre à false dès qu'une session apparaît,
    // passe à true après l'animation d'entrée — ne rebascule jamais.
    var showPlayer by remember { mutableStateOf(false) }
    LaunchedEffect(hasSession) {
        if (hasSession && !showPlayer) {
            // 2.5s sur l'icône avant de déclencher l'animation
            // — laisse le temps à l'utilisateur de voir que c'est Spotify qui joue
            delay(2_500L)
            showPlayer = true
        } else if (!hasSession) {
            showPlayer = false
        }
    }

    // Animation slide Windows — une seule fois au lancement de la musique.
    // tween avec FastOutSlowInEasing = décélération fluide, pas de saccade.
    AnimatedContent(
        targetState  = showPlayer,
        transitionSpec = {
            slideInVertically(
                initialOffsetY = { it },
                animationSpec  = tween(durationMillis = 500, easing = FastOutSlowInEasing)
            ) togetherWith slideOutVertically(
                targetOffsetY  = { -it },
                animationSpec  = tween(durationMillis = 500, easing = FastOutSlowInEasing)
            )
        },
        label    = "MediaEntrance",
        modifier = modifier.fillMaxSize()
    ) { isPlayer ->
        if (!isPlayer) {
            // Icône normale — visible brièvement au lancement
            StandardAppTile(config = config, contentColor = contentColor)
        } else {
            // Lecteur — reste affiché tant que la session est active
            AnimatedContent(
                targetState  = "$title|$artist|$isPlaying",
                transitionSpec = { fadeIn(tween(250)) togetherWith fadeOut(tween(250)) },
                label        = "MediaRoot",
                modifier     = Modifier.fillMaxSize()
            ) { currentKey ->
                val keyIsPlaying = currentKey.endsWith("|true")
                when (config.size) {
                    TileSize.TINY   -> TinyLayout(albumArt, appColor)
                    TileSize.SMALL  -> SmallLayout(title, artist, appName, appColor, albumArt, keyIsPlaying, context, targetPackage)
                    TileSize.MEDIUM -> MediumLayout(title, artist, appName, appColor, albumArt, keyIsPlaying, context, targetPackage)
                    TileSize.LARGE  -> LargeLayout(title, artist, appName, appColor, albumArt, keyIsPlaying, context, targetPackage)
                }
            }
        }
    }
}

// ============================================================================
// TINY (1×1) — pochette fond + point couleur app
// ============================================================================

@Composable
private fun TinyLayout(albumArt: Bitmap?, appColor: Color) {
    Box(
        modifier         = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF1C1C1C)),
        contentAlignment = Alignment.Center
    ) {
        if (albumArt != null) {
            Image(
                bitmap             = albumArt.asImageBitmap(),
                contentDescription = null,
                modifier           = Modifier.fillMaxSize(),
                contentScale       = ContentScale.Crop
            )
        }
    }
}

// ============================================================================
// SMALL (2×2) — pochette fond · info bas-gauche · play bas-droite
// ============================================================================

@Composable
private fun SmallLayout(
    title: String, artist: String, appName: String,
    appColor: Color, albumArt: Bitmap?,
    isPlaying: Boolean, context: Context, pkg: String
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF1C1C1C))
    ) {
        // Pochette plein fond
        if (albumArt != null) {
            Image(albumArt.asImageBitmap(), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        }
        // Dégradé subtil uniquement en bas pour le bouton
        Box(Modifier.fillMaxSize().background(
            Brush.verticalGradient(
                0.5f to Color.Transparent,
                1.0f to Color.Black.copy(0.55f)
            )
        ))

        // Play/Pause centré en bas — aucun texte, la pochette suffit
        MediaButton(
            isPlaying = isPlaying,
            size      = 36.dp,
            iconSize  = 16.dp,
            onClick   = { sendAction(context, pkg, if (isPlaying) PlaybackState.ACTION_PAUSE else PlaybackState.ACTION_PLAY) },
            modifier  = Modifier.align(Alignment.BottomCenter).padding(bottom = 10.dp)
        )
    }
}

// ============================================================================
// MEDIUM (4×2) — texte gauche · play droite · pochette fond dégradé horiz.
// ============================================================================

@Composable
private fun MediumLayout(
    title: String, artist: String, appName: String,
    appColor: Color, albumArt: Bitmap?,
    isPlaying: Boolean, context: Context, pkg: String
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF1C1C1C))
    ) {
        if (albumArt != null) {
            Image(albumArt.asImageBitmap(), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        }
        // Dégradé horizontal — texte lisible à gauche, pochette visible à droite
        Box(Modifier.fillMaxSize().background(
            Brush.horizontalGradient(listOf(Color.Black.copy(0.92f), Color.Black.copy(0.18f)))
        ))

        Row(
            modifier          = Modifier.fillMaxSize().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {

                Text(title,  color = Color.White,             fontSize = 16.sp, fontWeight = FontWeight.Bold,   maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(artist, color = Color.White.copy(0.55f), fontSize = 13.sp, fontWeight = FontWeight.Normal, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }

            Spacer(Modifier.width(12.dp))

            // Play/Pause uniquement — prev/next réservés à LARGE
            MediaButton(
                isPlaying = isPlaying,
                size      = 44.dp,
                iconSize  = 22.dp,
                onClick   = { sendAction(context, pkg, if (isPlaying) PlaybackState.ACTION_PAUSE else PlaybackState.ACTION_PLAY) }
            )
        }
    }
}

// ============================================================================
// LARGE (4×4) — header · pochette + info · prev/play/next
// ============================================================================

@Composable
private fun LargeLayout(
    title: String, artist: String, appName: String,
    appColor: Color, albumArt: Bitmap?,
    isPlaying: Boolean, context: Context, pkg: String
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF1C1C1C))
    ) {
        if (albumArt != null) {
            Image(albumArt.asImageBitmap(), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop, alpha = 0.20f)
        }
        Box(Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(Color.Black.copy(0.22f), Color.Black.copy(0.94f)))
        ))

        Column(
            modifier            = Modifier.fillMaxSize().padding(18.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {


            // ── Pochette gauche (compact) + titre/artiste droite ─────────────
            // Pochette réduite à 64dp pour laisser de la place au texte
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.White.copy(0.06f))
                ) {
                    if (albumArt != null) {
                        Image(albumArt.asImageBitmap(), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    }
                }
                Column(
                    modifier            = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text       = title,
                        color      = Color.White,
                        fontSize   = 17.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines   = 2,
                        overflow   = TextOverflow.Ellipsis,
                        lineHeight = 22.sp
                    )
                    Text(
                        text     = artist,
                        color    = Color.White.copy(0.60f),
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // ── Contrôles ─────────────────────────────────────────────────────
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                // Précédent — mini
                IconButton(
                    onClick  = { sendAction(context, pkg, PlaybackState.ACTION_SKIP_TO_PREVIOUS) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(FluentIcons.Regular.Previous, null, Modifier.size(16.dp), tint = Color.White.copy(0.65f))
                }

                Spacer(Modifier.width(16.dp))

                // Play/Pause principal
                Box(
                    modifier         = Modifier.size(52.dp).clip(CircleShape).background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    IconButton(
                        onClick  = { sendAction(context, pkg, if (isPlaying) PlaybackState.ACTION_PAUSE else PlaybackState.ACTION_PLAY) },
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            if (isPlaying) FluentIcons.Regular.Pause else FluentIcons.Regular.Play,
                            null, Modifier.size(24.dp), tint = Color.Black
                        )
                    }
                }

                Spacer(Modifier.width(16.dp))

                // Suivant — mini
                IconButton(
                    onClick  = { sendAction(context, pkg, PlaybackState.ACTION_SKIP_TO_NEXT) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(FluentIcons.Regular.Next, null, Modifier.size(16.dp), tint = Color.White.copy(0.65f))
                }
            }
        }
    }
}

// ============================================================================
// COMPOSANT ATOMIQUE — bouton play/pause réutilisé dans SMALL et MEDIUM
// ============================================================================

@Composable
private fun MediaButton(
    isPlaying: Boolean,
    size:      androidx.compose.ui.unit.Dp,
    iconSize:  androidx.compose.ui.unit.Dp,
    onClick:   () -> Unit,
    modifier:  Modifier = Modifier
) {
    Box(
        modifier         = modifier
            .size(size)
            .clip(CircleShape)
            .background(Color.White.copy(0.18f)),
        contentAlignment = Alignment.Center
    ) {
        IconButton(onClick = onClick, modifier = Modifier.fillMaxSize()) {
            // AnimatedContent sur l'état play/pause — transition fluide de l'icône
            AnimatedContent(
                targetState  = isPlaying,
                transitionSpec = { fadeIn(tween(150)) togetherWith fadeOut(tween(150)) },
                label        = "PlayPauseIcon"
            ) { playing ->
                Icon(
                    imageVector        = if (playing) FluentIcons.Regular.Pause else FluentIcons.Regular.Play,
                    contentDescription = if (playing) "Pause" else "Lecture",
                    modifier           = Modifier.size(iconSize),
                    tint               = Color.White
                )
            }
        }
    }
}