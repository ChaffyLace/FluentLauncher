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
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fluentui.R
import com.example.fluentui.ui.motion.FluentMotion
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

/** Accepted states for a widget to remain visible. Pausing does not dismiss it. */
private val ACTIVE_STATES = setOf(
    PlaybackState.STATE_PLAYING,
    PlaybackState.STATE_PAUSED,
    PlaybackState.STATE_BUFFERING,
    PlaybackState.STATE_CONNECTING
)

/** Polling interval — 500ms for a reactive play/pause without overloading the CPU. */
private const val POLL_INTERVAL_MS = 500L

// ============================================================================
// DATA MODEL
// ============================================================================

/**
 * Immutable player state.
 *
 * [hasSession] = true as long as a session exists for the target app, whether playing or paused.
 * Only the absence of a session reverts the widget to a "normal icon" mode.
 *
 * [albumArt] is EXCLUDED from [visualKey]: Android recreates a Bitmap object on every
 * getBitmap() even for the same image, so its hashCode changes on every poll
 * and would trigger AnimatedContent every 500ms -> constant flashing.
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
// DATA LAYER  —  no suspend, called from LaunchedEffect(implicit IO)
// ============================================================================

/**
 * Resolves the appropriate controller based on the target package.
 *
 * - [targetPackage] not empty -> strict filter on the pinned app.
 * Spotify never affects Samsung Music and vice versa.
 * - [targetPackage] empty -> first session in an active state.
 *
 * Priority: STATE_PLAYING > STATE_PAUSED/others in both modes.
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
            // Exclusive mode — only the tile's package
            sessions.firstOrNull { it.packageName == targetPackage }
        } else {
            // General mode — priority to what is currently playing
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
 * Sends a transport command.
 * Fails silently — must never crash the UI.
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
// MAIN WIDGET
// ============================================================================

@Composable
fun MediaWidget(
    config:       TileConfig,
    contentColor: Color,
    accentColor:  Color,
    modifier:     Modifier = Modifier
) {
    val context       = LocalContext.current
    val targetPackage = config.packageName  // empty = general mode

    // ── Granular State ────────────────────────────────────────────────────────
    // Each field is a separate State rather than a single object.
    // Advantage: Compose only recomposes components that read the changed field.
    // Toggling isPlaying does not recompose the title.
    var title      by remember { mutableStateOf("") }
    var artist     by remember { mutableStateOf("") }
    var appName    by remember { mutableStateOf("") }
    var appColor   by remember { mutableStateOf(Color(0xFF1DB954)) }
    var albumArt   by remember { mutableStateOf<Bitmap?>(null) }
    var isPlaying  by remember { mutableStateOf(false) }
    var hasSession by remember { mutableStateOf(false) }
    // Last known title — albumArt is only reloaded upon song change,
    // never on every poll (prevents flash related to Bitmap object recreation)
    var lastTitle  by remember { mutableStateOf("") }

    LaunchedEffect(targetPackage) {
        while (isActive) {
            val s = readMediaState(context, targetPackage)

            hasSession = s.hasSession
            isPlaying  = s.isPlaying
            appName    = s.appName
            appColor   = s.appColor

            // Reload albumArt if:
            // - the song changed (new title)
            // - or albumArt is null while we have a cover available
            //   (cold start case where lastTitle="" = title="" on first tick)
            if (s.title != lastTitle || (albumArt == null && s.albumArt != null)) {
                albumArt  = s.albumArt
                lastTitle = s.title
            }

            // title/artist last: their change triggers AnimatedContent
            // after albumArt is already synchronized -> no frame where the image
            // lags behind the text
            title  = s.title
            artist = s.artist

            delay(POLL_INTERVAL_MS)
        }
    }

    // ── Routing ───────────────────────────────────────────────────────────────
    // showPlayer: starts at false as soon as a session appears,
    // switches to true after entrance animation — never switches back.
    var showPlayer by remember { mutableStateOf(false) }
    LaunchedEffect(hasSession) {
        if (hasSession && !showPlayer) {
            // 2.5s on the icon before triggering the animation
            // — gives the user time to see that Spotify is playing
            delay(2_500L)
            showPlayer = true
        } else if (!hasSession) {
            showPlayer = false
        }
    }

    // Windows slide animation — once per music launch.
    // tween with FastOutSlowInEasing = fluid deceleration, no stutter.
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
            // Normal icon — briefly visible on launch
            StandardAppTile(config = config, contentColor = contentColor)
        } else {
            // Player — remains visible as long as the session is active
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
// TINY (1x1) — Background cover + app color dot
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
// SMALL (2x2) — Background cover · info bottom-left · play bottom-right
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
        // Full background cover
        if (albumArt != null) {
            Image(albumArt.asImageBitmap(), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        }
        // Subtle gradient only at the bottom for the button
        Box(Modifier.fillMaxSize().background(
            Brush.verticalGradient(
                0.5f to Color.Transparent,
                1.0f to Color.Black.copy(0.55f)
            )
        ))

        // Centered Play/Pause at the bottom — no text, cover is enough
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
// MEDIUM (4x2) — Text left · play right · background cover horiz gradient
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
        // Horizontal gradient — text readable on left, cover visible on right
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

            // Play/Pause only — prev/next reserved for LARGE
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
// LARGE (4x4) — Header · cover + info · prev/play/next
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


            // ── Left cover (compact) + right title/artist ─────────────
            // Cover reduced to 64dp to leave room for text
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

            // ── Controls ─────────────────────────────────────────────────────
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                // Previous — mini
                IconButton(
                    onClick  = { sendAction(context, pkg, PlaybackState.ACTION_SKIP_TO_PREVIOUS) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(FluentIcons.Regular.Previous, null, Modifier.size(16.dp), tint = Color.White.copy(0.65f))
                }

                Spacer(Modifier.width(16.dp))

                // Main Play/Pause
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

                // Next — mini
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
// ATOMIC COMPONENT — play/pause button reused in SMALL and MEDIUM
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
            // AnimatedContent on play/pause state — fluid transition of the icon
            AnimatedContent(
                targetState  = isPlaying,
                transitionSpec = { fadeIn(tween(150)) togetherWith fadeOut(tween(150)) },
                label        = "PlayPauseIcon"
            ) { playing ->
                Icon(
                    imageVector        = if (playing) FluentIcons.Regular.Pause else FluentIcons.Regular.Play,
                    contentDescription = if (playing) stringResource(R.string.media_cd_pause) else stringResource(R.string.media_cd_play),
                    modifier           = Modifier.size(iconSize),
                    tint               = Color.White
                )
            }
        }
    }
}