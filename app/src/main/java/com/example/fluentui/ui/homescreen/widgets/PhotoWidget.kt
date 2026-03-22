package com.example.fluentui.ui.homescreen.widgets

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.fluentui.ui.system.TileConfig
import com.example.fluentui.ui.system.TileSize
import fluent.ui.system.icons.FluentIcons
import fluent.ui.system.icons.color.Image
import fluent.ui.system.icons.regular.Add
import kotlinx.coroutines.delay

// ============================================================================
// PERSISTANCE
// ============================================================================

private const val PREFS_NAME   = "photo_widget_prefs"
private const val MAX_PHOTOS   = 10
private const val SLIDESHOW_MS = 5_000L

private fun saveUris(context: Context, tileId: String, uris: List<Uri>) {
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .edit()
        .putString("photos_$tileId", uris.joinToString("|") { it.toString() })
        .apply()
}

private fun loadUris(context: Context, tileId: String): List<Uri> {
    val raw = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .getString("photos_$tileId", null) ?: return emptyList()
    return raw.split("|").filter { it.isNotBlank() }.map { Uri.parse(it) }
}

// ============================================================================
// WIDGET
// ============================================================================

@Composable
fun PhotoWidget(
    config:   TileConfig,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val tileId  = config.id.ifBlank { "photo_widget" }

    var photos       by remember(tileId) { mutableStateOf(loadUris(context, tileId)) }
    var currentIndex by remember { mutableIntStateOf(0) }

    // ── ACTION_OPEN_DOCUMENT avec EXTRA_ALLOW_MULTIPLE ────────────────────────
    // Plus fiable que PickMultipleVisualMedia qui tombe en fallback galerie sur
    // certains OEM. ACTION_OPEN_DOCUMENT retourne des URIs persistants que
    // takePersistableUriPermission peut toujours accepter.
    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            val persistent = uris.mapNotNull { uri ->
                runCatching {
                    context.contentResolver.takePersistableUriPermission(
                        uri,
                        android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                    uri
                }.getOrNull()
            }
            if (persistent.isNotEmpty()) {
                val merged = (photos + persistent)
                    .distinctBy { it.toString() }
                    .take(MAX_PHOTOS)
                photos       = merged
                currentIndex = 0
                saveUris(context, tileId, merged)
            }
        }
    }

    fun openPicker() {
        // "image/*" — toutes les images, le sélecteur système s'ouvre
        // avec multi-sélection activée via OpenMultipleDocuments
        picker.launch(arrayOf("image/*"))
    }

    // ── Slideshow ─────────────────────────────────────────────────────────────
    LaunchedEffect(photos.size) {
        if (photos.size > 1) {
            while (true) {
                delay(SLIDESHOW_MS)
                currentIndex = (currentIndex + 1) % photos.size
            }
        }
    }

    val cornerRadius = when (config.size) {
        TileSize.TINY  -> 12.dp
        TileSize.SMALL -> 16.dp
        else           -> 20.dp
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(cornerRadius))
            .background(Color(0xFF1A1A1A))
    ) {
        if (photos.isEmpty()) {
            EmptyState(size = config.size, onPickPhotos = ::openPicker)
        } else {
            // ── Photos ────────────────────────────────────────────────────────
            AnimatedContent(
                targetState  = currentIndex,
                transitionSpec = { fadeIn(tween(800)) togetherWith fadeOut(tween(800)) },
                label        = "PhotoSlide",
                modifier     = Modifier.fillMaxSize()
            ) { idx ->
                val uri = photos.getOrNull(idx)
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(uri)
                        .crossfade(true)
                        .memoryCacheKey(uri?.toString())
                        .diskCacheKey(uri?.toString())
                        .build(),
                    contentDescription = null,
                    modifier           = Modifier.fillMaxSize(),
                    contentScale       = ContentScale.Crop,
                    alignment          = Alignment.Center
                )
            }

            // ── Indicateurs ───────────────────────────────────────────────────
            if (config.size != TileSize.TINY && photos.size > 1) {
                Row(
                    modifier              = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    photos.indices.take(8).forEach { i ->
                        Box(
                            modifier = Modifier
                                .size(if (i == currentIndex) 6.dp else 4.dp)
                                .clip(CircleShape)
                                .background(
                                    if (i == currentIndex) Color.White
                                    else Color.White.copy(alpha = 0.4f)
                                )
                        )
                    }
                }
            }

            // ── Bouton + ──────────────────────────────────────────────────────
            if (config.size != TileSize.TINY) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.45f))
                        .clickable { openPicker() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(FluentIcons.Regular.Add, null, Modifier.size(16.dp), tint = Color.White)
                }
            }
        }
    }
}

// ============================================================================
// ÉTAT VIDE
// ============================================================================

@Composable
private fun EmptyState(size: TileSize, onPickPhotos: () -> Unit) {
    Box(
        modifier         = Modifier.fillMaxSize().clickable { onPickPhotos() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                FluentIcons.Color.Image, null,
                Modifier.size(when (size) {
                    TileSize.TINY  -> 24.dp
                    TileSize.SMALL -> 32.dp
                    else           -> 42.dp
                }),
                tint = Color.Unspecified
            )
            if (size != TileSize.TINY) {
                Text(
                    text       = "Appuyer pour choisir",
                    color      = Color.White.copy(alpha = 0.6f),
                    fontSize   = if (size == TileSize.SMALL) 11.sp else 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}