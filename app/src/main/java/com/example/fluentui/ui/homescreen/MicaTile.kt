package com.example.fluentui.ui.homescreen

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fluentui.ui.components.AppViewModel
import com.example.fluentui.ui.components.InternalAppIcon
import com.example.fluentui.ui.system.TileConfig
import com.example.fluentui.ui.system.TileSize
import com.example.fluentui.ui.system.TileType
import com.example.fluentui.wallpaper.MicaParams
import com.example.fluentui.wallpaper.NoiseGenerator

/**
 * Fluent Design Mica Tile
 * Implements the authentic Windows 11 material feel with specular borders and dynamic tinting.
 */
@Composable
fun MicaTile(
    config: TileConfig,
    mica: MicaParams?,
    isDark: Boolean,
    viewModel: AppViewModel,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    val context = LocalContext.current

    // --- GEOMETRY ---
    // Tiny tiles are sharper, larger tiles are softer
    val cornerRadius = if (config.size == TileSize.TINY) 8.dp else 12.dp
    val tileShape = RoundedCornerShape(cornerRadius)

    // --- ANIMATION ---
    var isPressed by remember { mutableStateOf(false) }
    // Micro-interaction: Spring effect on press
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        label = "press_scale",
        animationSpec = tween(100)
    )

    // --- LOGIC ---
    val isRichMedia = isRichMediaApp(config.packageName)
    // Only System apps get the Mica effect. Media apps get their brand color.
    val isSystemApp = config.type == TileType.APPLICATION && !isRichMedia

    // --- MICA RECIPE (The "Secret Sauce") ---

    // 1. Base Tint: Pulls color from wallpaper or falls back to system theme
    val baseColor = remember(mica, isDark) {
        mica?.surfaceColor ?: if (isDark) Color(0xFF202020) else Color(0xFFF3F3F3)
    }

    // 2. Acrylic Surface Brush (The "Glassy" look)
    // We use a subtle gradient to simulate light hitting the surface from top-left
    val surfaceBrush = remember(baseColor, isDark) {
        val startAlpha = if (isDark) 0.60f else 0.70f // More opaque at top
        val endAlpha = if (isDark) 0.30f else 0.45f   // More transparent at bottom

        Brush.linearGradient(
            colors = listOf(
                baseColor.copy(alpha = startAlpha),
                baseColor.copy(alpha = endAlpha)
            ),
            start = Offset(0f, 0f),
            end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
        )
    }

    // 3. Specular Border (The "Light Edge")
    // Windows controls have a border that is brighter on top (light source) and darker on bottom (shadow)
    val borderBrush = remember(isDark) {
        Brush.verticalGradient(
            colors = listOf(
                if (isDark) Color.White.copy(0.12f) else Color.White.copy(0.8f), // Top Highlight
                if (isDark) Color.White.copy(0.02f) else Color.Black.copy(0.05f) // Bottom Shadow
            )
        )
    }

    // 4. Noise Texture (To avoid the "plastic" look)
    val noiseBrush = remember(isDark) {
        try { NoiseGenerator.generate(isDark) } catch (e: Exception) { null }
    }

    // --- RENDER ---
    Box(
        modifier = modifier
            .scale(scale)
            // Soft shadow for depth (Elevated from background)
            .shadow(
                elevation = if (isDark) 6.dp else 4.dp,
                shape = tileShape,
                spotColor = Color.Black.copy(if (isDark) 0.4f else 0.15f),
                ambientColor = Color.Black.copy(if (isDark) 0.2f else 0.05f)
            )
            .clip(tileShape)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = { isPressed = true; tryAwaitRelease(); isPressed = false },
                    onTap = {
                        if (config.packageName.isNotEmpty()) {
                            runCatching {
                                val intent = context.packageManager.getLaunchIntentForPackage(config.packageName)
                                if (intent != null) context.startActivity(intent)
                            }
                        }
                    }
                )
            }
    ) {
        // LAYER 1: MATERIAL BACKGROUND
        if (isSystemApp) {
            // A. Glass Body
            Box(Modifier.matchParentSize().background(surfaceBrush))

            // B. Noise Texture Overlay
            if (noiseBrush != null) {
                Box(
                    Modifier
                        .matchParentSize()
                        .graphicsLayer(alpha = if (isDark) 0.04f else 0.08f)
                        .background(noiseBrush)
                )
            }

            // C. Specular Border stroke (Inside)
            Box(
                Modifier
                    .matchParentSize()
                    .border(1.dp, borderBrush, tileShape)
            )
        }

        // LAYER 2: CONTENT
        // Content gets padding, background does not.
        Box(modifier = Modifier.fillMaxSize()) {
            when (config.type) {
                TileType.CALENDAR -> CalendarWidget(config)
                TileType.MAIL -> MailWidget(config)
                TileType.CLOCK -> ClockWidget(config)
                TileType.BATTERY -> BatteryWidget(config)
                TileType.WEATHER -> WeatherWidget(config)
                else -> {
                    if (isRichMedia) {
                        RichMediaTileContent(config)
                    } else {
                        // STANDARD APP CONTENT
                        Box(Modifier.fillMaxSize().padding(12.dp)) {
                            // Responsive Icon Size
                            val iconSize = when (config.size) {
                                TileSize.TINY -> 28.dp
                                TileSize.SMALL -> 36.dp
                                else -> 52.dp
                            }

                            // Center Alignment
                            SharedAppIcon(
                                packageName = config.packageName,
                                size = when (config.size) {
                                    TileSize.TINY -> 48f; TileSize.SMALL -> 64f; else -> 96f
                                },
                                modifier = Modifier.size(iconSize).align(Alignment.Center)
                            )

                            // Label (Hidden on Tiny tiles for cleaner look)
                            if (config.size != TileSize.TINY) {
                                Text(
                                    text = config.title,
                                    fontSize = if (config.size == TileSize.SMALL) 11.sp else 12.sp,
                                    color = if (isDark) Color.White.copy(0.9f) else Color.Black.copy(0.85f),
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.align(Alignment.BottomStart),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// =================================================================================
// --- HELPER COMPONENTS (Required for compilation) ---
// =================================================================================

@Composable
fun SharedAppIcon(packageName: String, size: Float, modifier: Modifier) {
    InternalAppIcon(packageName, size, modifier)
}

fun isRichMediaApp(packageName: String): Boolean {
    val pkg = packageName.lowercase()
    return pkg.contains("netflix") || pkg.contains("spotify") ||
            pkg.contains("tiktok") || pkg.contains("youtube") ||
            pkg.contains("disney")
}

@Composable
fun RichMediaTileContent(config: TileConfig) {
    val pkg = config.packageName.lowercase()
    Box(Modifier.fillMaxSize()) {
        when {
            pkg.contains("netflix") -> {
                Box(Modifier.fillMaxSize().background(Color.Black))
                Text("N", color = Color(0xFFE50914), fontWeight = FontWeight.Bold, fontSize = 56.sp, modifier = Modifier.align(Alignment.Center))
            }
            pkg.contains("spotify") -> {
                Box(Modifier.fillMaxSize().background(Color(0xFF1DB954)))
                // Fake logo
                Box(Modifier.size(32.dp).background(Color.Black, CircleShape).align(Alignment.Center))
            }
            pkg.contains("youtube") -> {
                Box(Modifier.fillMaxSize().background(Color(0xFFFF0000)))
                Icon(Icons.Default.PlayArrow, null, tint = Color.White, modifier = Modifier.size(40.dp).align(Alignment.Center))
            }
            else -> {
                Box(Modifier.fillMaxSize().background(Color(0xFF333333)))
                Text(config.title.take(1).uppercase(), color = Color.White, fontSize = 24.sp, modifier = Modifier.align(Alignment.Center))
            }
        }
    }
}

// --- WIDGET IMPLEMENTATIONS (Visual Mocks for MVP) ---

@Composable
fun CalendarWidget(config: TileConfig) {
    FluentWidgetLayout(isDark = isSystemInDarkTheme()) {
        Icon(Icons.Default.DateRange, null, tint = Color(0xFF60CDFF), modifier = Modifier.size(22.dp))
        Spacer(Modifier.height(8.dp))
        Text("Lun 24", color = it, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Text("14:00 Réunion", color = it.copy(0.7f), fontSize = 12.sp)
    }
}

@Composable
fun MailWidget(config: TileConfig) {
    FluentWidgetLayout(isDark = isSystemInDarkTheme()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Email, null, tint = Color(0xFF0078D4), modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text("Outlook", color = it.copy(0.6f), fontSize = 11.sp)
        }
        Spacer(Modifier.weight(1f))
        Text("Projet Alpha", color = it, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
        Text("Re: Mise à jour...", color = it.copy(0.8f), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun ClockWidget(config: TileConfig) {
    FluentWidgetLayout(isDark = isSystemInDarkTheme()) {
        Text("Paris", color = it.copy(0.6f), fontSize = 11.sp)
        Spacer(Modifier.weight(1f))
        Text("14:42", color = it, fontWeight = FontWeight.Light, fontSize = 32.sp)
        Text("Lun, 24 Oct", color = it.copy(0.8f), fontSize = 12.sp)
    }
}

@Composable
fun BatteryWidget(config: TileConfig) {
    FluentWidgetLayout(isDark = isSystemInDarkTheme()) {
        Icon(Icons.Default.Info, null, tint = Color(0xFF107C10), modifier = Modifier.size(22.dp))
        Spacer(Modifier.weight(1f))
        Text("85%", color = it, fontWeight = FontWeight.Bold, fontSize = 24.sp)
        Text("~ 5h 30min", color = it.copy(0.7f), fontSize = 11.sp)
    }
}

@Composable
fun WeatherWidget(config: TileConfig) {
    val isDark = isSystemInDarkTheme()
    // Gradient background for weather specifically
    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF2F80ED), Color(0xFF56CCF2))))) {
        Column(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Icon(Icons.Default.Star, null, tint = Color.White, modifier = Modifier.size(24.dp))
            Text("21°", color = Color.White, fontWeight = FontWeight.Light, fontSize = 28.sp)
            Text("Ensoleillé", color = Color.White.copy(0.9f), fontSize = 12.sp)
        }
    }
}

// --- SHARED WIDGET LAYOUT ---
@Composable
fun FluentWidgetLayout(isDark: Boolean, content: @Composable ColumnScope.(Color) -> Unit) {
    val textColor = if (isDark) Color.White else Color.Black
    // Subtle background for widget readability vs Mica
    val bg = if (isDark) Color.White.copy(0.03f) else Color.Black.copy(0.03f)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bg)
            .padding(12.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.Start
    ) {
        content(textColor)
    }
}

// Fallback for Noise
object NoiseGenerator {
    fun generate(isDark: Boolean): Brush? = null
}