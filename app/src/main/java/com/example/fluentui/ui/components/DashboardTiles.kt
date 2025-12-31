package com.example.fluentui.ui.components

import android.graphics.Bitmap
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fluentui.ui.theme.AppTheme
import com.example.fluentui.ui.theme.ThemeColors
import com.example.fluentui.ui.theme.getColors
import fluent.ui.system.icons.FluentIcons
import fluent.ui.system.icons.regular.*
import java.util.*
import kotlin.math.roundToInt
import java.text.SimpleDateFormat

// ==============================================================================
// DATA MODELS
// ==============================================================================

enum class TileSize(val span: Int) {
    SMALL(1),    // 1x1
    MEDIUM(2),   // 2x1
    SQUARE(2),   // 2x2
    LARGE(4)     // 4x1
}

enum class TileType {
    APP, WEATHER, CALENDAR, FOLDER, NEWS, MUSIC, PHOTO, GAME
}

sealed class TileIcon {
    data class Vector(val vector: ImageVector) : TileIcon()
    data class Bitmap(val bitmap: android.graphics.Bitmap) : TileIcon()
}

data class TileConfig(
    val id: String,
    val title: String,
    val subtitle: String = "",
    val icon: TileIcon? = null,
    val size: TileSize,
    val type: TileType,
    val packageName: String = "",
    val showLabel: Boolean = true,
    val badge: Int? = null, // Pour notifications
    val accentColor: Color? = null, // Couleur custom
    val action: (() -> Unit)? = null
)

// ==============================================================================
// DASHBOARD PAGE PRINCIPALE
// ==============================================================================

@Composable
fun DashboardPage(
    viewModel: AppViewModel,
    blurredWallpaper: Bitmap?,
    theme: AppTheme = AppTheme.WINDOWS_DARK
) {
    val context = LocalContext.current
    val isDark = isSystemInDarkTheme()
    val colors = theme.getColors(isDark)
    val pinnedApps by viewModel.pinnedApps

    // Configuration de la grille
    val columnCount = 4
    val spacing = 12.dp
    val horizontalPadding = 20.dp

    // CORRECTION ICI : On calcule les tuiles AVANT d'entrer dans le LazyVerticalGrid
    val tiles = rememberTiles(pinnedApps)

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .padding(top = 80.dp, bottom = 120.dp)
    ) {
        val availableWidth = maxWidth - (horizontalPadding * 2)
        val baseUnitSize = (availableWidth - (spacing * (columnCount - 1))) / columnCount

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = horizontalPadding)
        ) {
            // Greeting
            val greeting = remember {
                val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
                when (hour) {
                    in 5..11 -> "Bonjour"
                    in 12..17 -> "Bon après-midi"
                    else -> "Bonsoir"
                }
            }

            Text(
                text = greeting,
                style = MaterialTheme.typography.headlineLarge,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .padding(bottom = 20.dp)
                    .graphicsLayer {
                        shadowElevation = 8f
                        shape = RoundedCornerShape(8.dp)
                    }
            )

            // Grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(columnCount),
                verticalArrangement = Arrangement.spacedBy(spacing),
                horizontalArrangement = Arrangement.spacedBy(spacing),
                contentPadding = PaddingValues(bottom = 32.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                // CORRECTION : On utilise la liste calculée plus haut
                items(
                    items = tiles,
                    key = { it.id },
                    span = { tile -> GridItemSpan(tile.size.span) }
                ) { tile ->
                    val tileHeight = when (tile.size) {
                        TileSize.SMALL -> baseUnitSize
                        TileSize.MEDIUM -> baseUnitSize
                        TileSize.SQUARE -> (baseUnitSize * 2) + spacing
                        TileSize.LARGE -> (baseUnitSize * 2) + spacing
                    }

                    GlassmorphismTile(
                        tile = tile,
                        colors = colors,
                        blurredWallpaper = blurredWallpaper,
                        forcedHeight = tileHeight,
                        isDark = isDark,
                        onClick = {
                            if (tile.packageName.isNotEmpty()) {
                                context.packageManager
                                    .getLaunchIntentForPackage(tile.packageName)
                                    ?.let { context.startActivity(it) }
                            } else {
                                tile.action?.invoke()
                            }
                        }
                    )
                }
            }
        }
    }
}

// ==============================================================================
// GLASSMORPHISM TILE - LA MAGIE OPÈRE ICI
// ==============================================================================

@Composable
private fun GlassmorphismTile(
    tile: TileConfig,
    colors: ThemeColors,
    blurredWallpaper: Bitmap?,
    forcedHeight: androidx.compose.ui.unit.Dp,
    isDark: Boolean,
    onClick: () -> Unit
) {
    var tilePosition by remember { mutableStateOf(Offset.Zero) }
    var tileSize by remember { mutableStateOf(IntSize.Zero) }
    var isPressed by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "scale"
    )

    val shape = RoundedCornerShape(16.dp)
    val contentPadding = if (tile.size == TileSize.SMALL) 12.dp else 16.dp

    Box(
        modifier = Modifier
            .height(forcedHeight)
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .onGloballyPositioned { coordinates ->
                tilePosition = coordinates.positionInWindow()
                tileSize = IntSize(
                    coordinates.size.width,
                    coordinates.size.height
                )
            }
            .clip(shape)
            // ═══════════════════════════════════════════════════════════════
            // GLASSMORPHISM EFFECT
            // ═══════════════════════════════════════════════════════════════
            .drawWithContent {
                // 1. Dessiner le morceau de wallpaper flouté
                if (blurredWallpaper != null) {
                    val imageBitmap = blurredWallpaper.asImageBitmap()
                    val srcX = tilePosition.x.roundToInt().coerceAtLeast(0)
                    val srcY = tilePosition.y.roundToInt().coerceAtLeast(0)

                    if (srcX < blurredWallpaper.width && srcY < blurredWallpaper.height) {
                        try {
                            drawImage(
                                image = imageBitmap,
                                srcOffset = IntOffset(srcX, srcY),
                                srcSize = IntSize(
                                    tileSize.width.coerceAtMost(blurredWallpaper.width - srcX),
                                    tileSize.height.coerceAtMost(blurredWallpaper.height - srcY)
                                ),
                                dstSize = tileSize
                            )
                        } catch (e: Exception) {
                            // Fallback si erreur
                            drawRect(
                                color = if (isDark) Color(0xFF1A1A1A) else Color(0xFFFFFFFF),
                                alpha = 0.3f
                            )
                        }
                    }
                }

                // 2. Overlay translucide (donne la couleur de fond)
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            if (isDark) Color(0xFF000000).copy(alpha = 0.4f)
                            else Color(0xFFFFFFFF).copy(alpha = 0.5f),
                            if (isDark) Color(0xFF000000).copy(alpha = 0.3f)
                            else Color(0xFFFFFFFF).copy(alpha = 0.4f)
                        )
                    )
                )

                // 3. Accent color si définie (comme le bleu de Minecraft)
                if (tile.accentColor != null) {
                    drawRect(
                        color = tile.accentColor,
                        alpha = 0.15f
                    )
                }

                // 4. Noise texture subtile
                drawRect(
                    color = if (isDark) Color.White else Color.Black,
                    alpha = 0.02f
                )

                // 5. Dessiner le contenu (icônes, texte)
                drawContent()
            }
            // Bordure lumineuse
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = if (isDark) 0.2f else 0.4f),
                        Color.Transparent,
                        Color.Black.copy(alpha = 0.1f)
                    )
                ),
                shape = shape
            )
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        tryAwaitRelease()
                        isPressed = false
                    },
                    onTap = { onClick() }
                )
            }
            .padding(contentPadding)
    ) {
        // Contenu de la tuile
        val textColor = Color.White.copy(0.95f)
        val textShadow = Shadow(
            color = Color.Black.copy(0.6f),
            offset = Offset(0f, 2f),
            blurRadius = 4f
        )

        when (tile.type) {
            TileType.APP -> AppTileContent(tile, textColor, textShadow)
            TileType.GAME -> GameTileContent(tile, textColor, textShadow)
            TileType.WEATHER -> WeatherTileContent(textColor, textShadow)
            TileType.CALENDAR -> CalendarTileContent(textColor, textShadow)
            TileType.FOLDER -> FolderTileContent(tile, textColor, textShadow)
            TileType.NEWS -> NewsTileContent(textColor, textShadow)
            TileType.MUSIC -> MusicTileContent(textColor, textShadow)
            TileType.PHOTO -> PhotoTileContent(textColor, textShadow)
        }

        // Badge notification
        if (tile.badge != null && tile.badge > 0) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 4.dp, y = (-4).dp)
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE74C3C))
                    .border(2.dp, Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (tile.badge > 99) "99+" else tile.badge.toString(),
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// ==============================================================================
// TILE CONTENTS
// ==============================================================================

@Composable
private fun AppTileContent(
    tile: TileConfig,
    textColor: Color,
    textShadow: Shadow
) {
    if (tile.size == TileSize.SMALL) {
        // Small tile: Icône centrée + label en bas
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.weight(1f))

            // Icon
            if (tile.icon != null) {
                when (tile.icon) {
                    is TileIcon.Vector -> Icon(
                        imageVector = tile.icon.vector,
                        contentDescription = null,
                        tint = textColor,
                        modifier = Modifier.size(32.dp)
                    )
                    is TileIcon.Bitmap -> Image(
                        bitmap = tile.icon.bitmap.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                        contentScale = ContentScale.Fit
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Label
            if (tile.showLabel) {
                Text(
                    text = tile.title,
                    style = MaterialTheme.typography.labelSmall.copy(
                        shadow = textShadow
                    ),
                    color = textColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    } else {
        // Medium/Wide tile: Icône + label côte à côte
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (tile.icon != null) {
                when (tile.icon) {
                    is TileIcon.Vector -> Icon(
                        imageVector = tile.icon.vector,
                        contentDescription = null,
                        tint = textColor,
                        modifier = Modifier.size(36.dp)
                    )
                    is TileIcon.Bitmap -> Image(
                        bitmap = tile.icon.bitmap.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        contentScale = ContentScale.Fit
                    )
                }
            }

            if (tile.showLabel) {
                Text(
                    text = tile.title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        shadow = textShadow
                    ),
                    color = textColor,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun GameTileContent(
    tile: TileConfig,
    textColor: Color,
    textShadow: Shadow
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Icon (Game cover)
        if (tile.icon != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                when (tile.icon) {
                    is TileIcon.Bitmap -> Image(
                        bitmap = tile.icon.bitmap.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    is TileIcon.Vector -> Icon(
                        imageVector = tile.icon.vector,
                        contentDescription = null,
                        tint = textColor,
                        modifier = Modifier.size(64.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Title
        Text(
            text = tile.title,
            style = MaterialTheme.typography.titleMedium.copy(
                shadow = textShadow
            ),
            color = textColor,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun WeatherTileContent(textColor: Color, textShadow: Shadow) {
    Row(
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.weight(1f)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = FluentIcons.Regular.Location,
                    contentDescription = null,
                    tint = textColor.copy(0.8f),
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Paris",
                    style = MaterialTheme.typography.labelSmall.copy(shadow = textShadow),
                    color = textColor.copy(0.8f),
                    fontSize = 11.sp
                )
            }

            Text(
                text = "19°",
                style = MaterialTheme.typography.displayLarge.copy(shadow = textShadow),
                color = textColor,
                fontWeight = FontWeight.Light,
                fontSize = 48.sp
            )

            Text(
                text = "Ensoleillé",
                style = MaterialTheme.typography.bodyMedium.copy(shadow = textShadow),
                color = textColor
            )
        }

        Icon(
            imageVector = FluentIcons.Regular.WeatherSunny,
            contentDescription = null,
            tint = Color(0xFFFFC107),
            modifier = Modifier.size(48.dp)
        )
    }
}

@Composable
private fun CalendarTileContent(textColor: Color, textShadow: Shadow) {
    val calendar = Calendar.getInstance()
    val dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH)
    val dayName = SimpleDateFormat("EEEE", Locale.getDefault()).format(calendar.time)
    val month = SimpleDateFormat("MMMM", Locale.getDefault()).format(calendar.time)

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = month.uppercase(),
            style = MaterialTheme.typography.labelMedium.copy(shadow = textShadow),
            color = textColor.copy(0.8f),
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )

        Text(
            text = dayOfMonth.toString(),
            style = MaterialTheme.typography.displayLarge.copy(shadow = textShadow),
            color = textColor,
            fontWeight = FontWeight.Bold,
            fontSize = 64.sp,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        Text(
            text = dayName,
            style = MaterialTheme.typography.bodyLarge.copy(shadow = textShadow),
            color = textColor,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.align(Alignment.End)
        )
    }
}

@Composable
private fun FolderTileContent(tile: TileConfig, textColor: Color, textShadow: Shadow) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Mini app grid
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                MiniAppIcon(FluentIcons.Regular.Chat, textColor)
                MiniAppIcon(FluentIcons.Regular.Mail, textColor)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                MiniAppIcon(FluentIcons.Regular.People, textColor)
                MiniAppIcon(FluentIcons.Regular.CalendarLtr, textColor)
            }
        }

        Text(
            text = tile.title,
            style = MaterialTheme.typography.labelMedium.copy(shadow = textShadow),
            color = textColor,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun MiniAppIcon(icon: ImageVector, tint: Color) {
    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(tint.copy(alpha = 0.2f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
private fun MusicTileContent(textColor: Color, textShadow: Shadow) {
    Row(
        modifier = Modifier.fillMaxSize(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Album cover
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.DarkGray),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = FluentIcons.Regular.MusicNote1,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(32.dp)
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Midnight City",
                style = MaterialTheme.typography.titleSmall.copy(shadow = textShadow),
                color = textColor,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            Text(
                text = "M83",
                style = MaterialTheme.typography.bodySmall.copy(shadow = textShadow),
                color = textColor.copy(0.7f),
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun NewsTileContent(textColor: Color, textShadow: Shadow) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE74C3C))
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "ACTUALITÉS",
                style = MaterialTheme.typography.labelSmall.copy(shadow = textShadow),
                color = textColor.copy(0.8f),
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }

        Text(
            text = "Microsoft lance Windows 12 avec intelligence artificielle intégrée.",
            style = MaterialTheme.typography.bodyMedium.copy(shadow = textShadow),
            color = textColor,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 18.sp
        )

        Text(
            text = "Il y a 2h • The Verge",
            style = MaterialTheme.typography.labelSmall.copy(shadow = textShadow),
            color = textColor.copy(0.6f),
            fontSize = 10.sp
        )
    }
}

@Composable
private fun PhotoTileContent(textColor: Color, textShadow: Shadow) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(8.dp))
    ) {
        Icon(
            imageVector = FluentIcons.Regular.Image,
            contentDescription = null,
            tint = textColor.copy(0.4f),
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = 1.5f
                    scaleY = 1.5f
                    rotationZ = -15f
                }
        )

        Text(
            text = "Souvenirs",
            style = MaterialTheme.typography.labelLarge.copy(shadow = textShadow),
            color = textColor,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(8.dp)
        )
    }
}

// ==============================================================================
// TILES FACTORY
// ==============================================================================

@Composable
private fun rememberTiles(pinnedApps: List<AppModel>): List<TileConfig> {
    return remember(pinnedApps) {
        buildList {
            // ROW 1
            add(TileConfig(
                id = "settings",
                title = "Paramètres",
                icon = TileIcon.Vector(FluentIcons.Regular.Settings),
                size = TileSize.SMALL,
                type = TileType.APP,
                packageName = "com.android.settings"
            ))

            add(TileConfig(
                id = "files",
                title = "Fichiers",
                icon = TileIcon.Vector(FluentIcons.Regular.Folder),
                size = TileSize.SMALL,
                type = TileType.APP,
                packageName = "com.google.android.documentsui"
            ))

            add(TileConfig(
                id = "email",
                title = "Gmail",
                icon = TileIcon.Vector(FluentIcons.Regular.Mail),
                size = TileSize.SMALL,
                type = TileType.APP,
                packageName = "com.google.android.gm",
                badge = 99
            ))

            add(TileConfig(
                id = "word",
                title = "Word",
                icon = TileIcon.Vector(FluentIcons.Regular.Document),
                size = TileSize.SMALL,
                type = TileType.APP,
                packageName = "com.microsoft.office.word",
                accentColor = Color(0xFF2B579A)
            ))

            // ROW 2
            add(TileConfig(
                id = "minecraft",
                title = "Minecraft: Java Edition",
                subtitle = "Mojang Studios",
                icon = null, // Tu mettras l'icône Minecraft ici
                size = TileSize.SQUARE,
                type = TileType.GAME,
                packageName = "com.mojang.minecraftpe",
                accentColor = Color(0xFF62A850)
            ))

            add(TileConfig(
                id = "weather",
                title = "Weather",
                icon = null,
                size = TileSize.MEDIUM,
                type = TileType.WEATHER,
                showLabel = false
            ))

            // ROW 3
            add(TileConfig(
                id = "music",
                title = "Music",
                icon = null,
                size = TileSize.MEDIUM,
                type = TileType.MUSIC,
                showLabel = false
            ))

            add(TileConfig(
                id = "calendar",
                title = "Calendar",
                icon = null,
                size = TileSize.SQUARE,
                type = TileType.CALENDAR,
                showLabel = false
            ))

            // ROW 4
            add(TileConfig(
                id = "news",
                title = "News",
                icon = null,
                size = TileSize.MEDIUM,
                type = TileType.NEWS,
                showLabel = false
            ))

            add(TileConfig(
                id = "folder_social",
                title = "Social",
                icon = null,
                size = TileSize.SMALL,
                type = TileType.FOLDER
            ))

            add(TileConfig(
                id = "camera",
                title = "Appareil photo",
                icon = TileIcon.Vector(FluentIcons.Regular.Camera),
                size = TileSize.SMALL,
                type = TileType.APP,
                packageName = "com.android.camera"
            ))
        }
    }
}