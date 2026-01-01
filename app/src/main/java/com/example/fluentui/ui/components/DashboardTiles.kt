package com.example.fluentui.ui.components

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.zIndex
import androidx.core.graphics.ColorUtils
import androidx.palette.graphics.Palette
import com.example.fluentui.ui.theme.AppTheme
import com.example.fluentui.ui.utils.FluentIconMapper
import fluent.ui.system.icons.FluentIcons
import fluent.ui.system.icons.regular.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*
import kotlin.math.roundToInt

// --- MODÈLES DE DONNÉES ---
enum class FluentMaterial { MICA_ALT, ACRYLIC, SOLID }
enum class TileSize(val span: Int) { SMALL(1), MEDIUM(2), SQUARE(2), LARGE(4) }
enum class TileType { APP, WEATHER, CALENDAR, FOLDER, NEWS, MUSIC, EMPTY } // EMPTY est la clé du succès

sealed class TileIcon {
    data class Vector(val vector: ImageVector) : TileIcon()
    data class AndroidIcon(val drawable: Drawable) : TileIcon()
}

data class TileConfig(
    val id: String, // ID UNIQUE (UUID)
    val title: String,
    val icon: TileIcon? = null,
    val size: TileSize,
    val type: TileType,
    val material: FluentMaterial = FluentMaterial.MICA_ALT,
    val packageName: String = "",
    val accentColor: Color? = null,
    val action: (() -> Unit)? = null
)

@Composable
fun DashboardPage(
    viewModel: AppViewModel,
    blurredWallpaper: Bitmap?,
    theme: AppTheme = AppTheme.WINDOWS_DARK
) {
    val context = LocalContext.current
    val isDark = isSystemInDarkTheme()

    // On observe la liste en direct
    val tiles = viewModel.dashboardTiles

    var showAppPicker by remember { mutableStateOf(false) }
    var smartTint by remember { mutableStateOf(if (isDark) Color(0xFF202020) else Color(0xFFF0F0F0)) }

    // --- ETATS DU DRAG & DROP ---
    var draggingItemIndex by remember { mutableStateOf<Int?>(null) }
    var draggingOffset by remember { mutableStateOf(Offset.Zero) }

    LaunchedEffect(blurredWallpaper, isDark) {
        if (blurredWallpaper != null) {
            smartTint = withContext(Dispatchers.Default) { SmartColorEngine.analyze(blurredWallpaper, isDark) }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {

        // BOUTON FLOTTANT "AJOUTER" (En bas à droite, style Windows 11)
        FloatingActionButton(
            onClick = { showAppPicker = true },
            modifier = Modifier.align(Alignment.BottomEnd).padding(32.dp),
            containerColor = smartTint,
            contentColor = if(isDark) Color.White else Color.Black
        ) {
            Icon(Icons.Default.Add, "Ajouter")
        }

        Column(modifier = Modifier.fillMaxSize().padding(top = 60.dp, start = 16.dp, end = 16.dp)) {
            Text(
                text = "Accueil",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = if (isDark) Color.White else Color.Black,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 100.dp)
            ) {
                // LA CLÉ (KEY) EST CRUCIALE POUR EVITER LES BUGS VISUELS
                itemsIndexed(items = tiles, key = { _, item -> item.id }, span = { _, item -> GridItemSpan(item.size.span) }) { index, tile ->

                    val isDragging = index == draggingItemIndex
                    val zIndex = if (isDragging) 10f else 1f

                    Box(
                        modifier = Modifier
                            .zIndex(zIndex)
                            .graphicsLayer {
                                translationX = if (isDragging) draggingOffset.x else 0f
                                translationY = if (isDragging) draggingOffset.y else 0f
                                scaleX = if (isDragging) 1.1f else 1f
                                scaleY = if (isDragging) 1.1f else 1f
                                shadowElevation = if(isDragging) 16f else 0f
                                alpha = if(isDragging) 0.9f else 1f
                            }
                            .pointerInput(Unit) {
                                detectDragGesturesAfterLongPress(
                                    onDragStart = {
                                        draggingItemIndex = index
                                        draggingOffset = Offset.Zero
                                    },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        draggingOffset += dragAmount

                                        // LOGIQUE DE SWAP
                                        val threshold = 100f // Pixels avant de déclencher un swap
                                        val offsetX = draggingOffset.x
                                        val offsetY = draggingOffset.y

                                        // Calcul de l'index cible
                                        val targetIndex = when {
                                            offsetX > threshold -> index + 1
                                            offsetX < -threshold -> index - 1
                                            offsetY > threshold -> index + 4
                                            offsetY < -threshold -> index - 4
                                            else -> null
                                        }

                                        if (targetIndex != null && targetIndex in tiles.indices) {
                                            viewModel.swapTiles(index, targetIndex)
                                            draggingItemIndex = targetIndex
                                            draggingOffset = Offset.Zero
                                        }
                                    },
                                    onDragEnd = { draggingItemIndex = null; draggingOffset = Offset.Zero },
                                    onDragCancel = { draggingItemIndex = null; draggingOffset = Offset.Zero }
                                )
                            }
                    ) {
                        if (tile.type == TileType.EMPTY) {
                            // CASE VIDE (Zone transparente, mais qui prend la place)
                            Box(modifier = Modifier.height(80.dp).fillMaxWidth().background(Color.White.copy(0.02f), RoundedCornerShape(8.dp)))
                        } else {
                            // TUILE RÉELLE
                            FluentTile(tile, smartTint, isDark, viewModel)
                        }
                    }
                }
            }
        }
    }

    // LE TIROIR D'APPS (Utilise ton AppGrid original)
    if (showAppPicker) {
        Dialog(onDismissRequest = { showAppPicker = false }) {
            Surface(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                color = if (isDark) Color(0xFF1F1F1F) else Color(0xFFF3F3F3),
                shadowElevation = 24.dp
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Toutes les applications", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        IconButton(onClick = { showAppPicker = false }) { Icon(Icons.Default.Close, null) }
                    }

                    // ON APPELLE TON APPGRID ICI POUR CHOISIR L'APP
                    AppGrid(
                        viewModel = viewModel,
                        state = rememberLazyGridState(),
                        columnCount = 4, rowCount = 6, iconSize = 60f, showLabels = true, searchQuery = "", accentColor = smartTint, isVertical = true,
                        onPinRequest = { app ->
                            viewModel.pinApp(app)
                            showAppPicker = false // Ferme le tiroir après choix
                        },
                        onClose = { showAppPicker = false }
                    )
                }
            }
        }
    }
}

// --- TUILE AVEC MENU CONTEXTUEL WINDOWS ---
@Composable
private fun FluentTile(config: TileConfig, smartTint: Color, isDark: Boolean, viewModel: AppViewModel) {
    val context = LocalContext.current
    var showMenu by remember { mutableStateOf(false) }

    val height = when (config.size) {
        TileSize.SMALL, TileSize.MEDIUM -> 80.dp
        TileSize.SQUARE, TileSize.LARGE -> 170.dp
    }

    Box(
        modifier = Modifier
            .height(height)
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectTapGestures(
                    onLongPress = { showMenu = true }, // CLIC DROIT
                    onTap = {
                        if (config.packageName.isNotEmpty()) {
                            try { context.startActivity(context.packageManager.getLaunchIntentForPackage(config.packageName)) } catch (e: Exception) {}
                        } else {
                            config.action?.invoke()
                        }
                    }
                )
            }
    ) {
        // Fond Mica/Acrylic
        val bg = when (config.material) {
            FluentMaterial.MICA_ALT -> if (isDark) smartTint.copy(alpha = 0.2f).compositeOver(Color(0xFF202020)) else smartTint.copy(alpha = 0.8f).compositeOver(Color.White)
            FluentMaterial.ACRYLIC -> if(isDark) Color(0xFF333333) else Color(0xFFEEEEEE)
            else -> smartTint
        }
        Box(Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp)).background(bg).border(1.dp, Color.White.copy(0.1f), RoundedCornerShape(8.dp)))

        // Contenu
        val color = if (isDark) Color.White else Color.Black
        Box(Modifier.padding(12.dp).fillMaxSize()) {
            if (config.type == TileType.APP) {
                Box(Modifier.align(Alignment.Center)) {
                    if (config.icon is TileIcon.AndroidIcon) Image(bitmap = (config.icon.drawable as BitmapDrawable).bitmap.asImageBitmap(), contentDescription = null, modifier = Modifier.size(32.dp))
                }
                Text(config.title, modifier = Modifier.align(Alignment.BottomStart), fontSize = 12.sp, color = color, maxLines = 1, overflow = TextOverflow.Ellipsis)
            } else {
                Text(config.title, modifier = Modifier.align(Alignment.TopStart), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = color)
                if (config.type == TileType.WEATHER) Icon(FluentIcons.Regular.WeatherSunny, null, modifier = Modifier.align(Alignment.Center).size(32.dp), tint = Color(0xFFFFC107))
            }
        }

        // LE MENU CONTEXTUEL (Désépingler / Redimensionner)
        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }, modifier = Modifier.background(if(isDark) Color(0xFF2D2D2D) else Color(0xFFF0F0F0))) {
            DropdownMenuItem(text = { Text("Petit") }, onClick = { viewModel.resizeTile(config.id, TileSize.SMALL); showMenu = false })
            DropdownMenuItem(text = { Text("Large") }, onClick = { viewModel.resizeTile(config.id, TileSize.MEDIUM); showMenu = false })
            DropdownMenuItem(text = { Text("Grand") }, onClick = { viewModel.resizeTile(config.id, TileSize.SQUARE); showMenu = false })
            HorizontalDivider()
            DropdownMenuItem(text = { Text("Désépingler", color = Color.Red) }, onClick = { viewModel.unpinTile(config.id); showMenu = false })
        }
    }
}

// Helpers
fun getHardcodedWidgets(): List<TileConfig> {
    return listOf(
        TileConfig(UUID.randomUUID().toString(), "Météo", size = TileSize.MEDIUM, type = TileType.WEATHER),
        TileConfig(UUID.randomUUID().toString(), "Musique", size = TileSize.MEDIUM, type = TileType.MUSIC),
        TileConfig(UUID.randomUUID().toString(), "Calendrier", size = TileSize.SQUARE, type = TileType.CALENDAR)
    )
}
object SmartColorEngine { fun analyze(bitmap: Bitmap, isDark: Boolean): Color { return if(isDark) Color(0xFF0078D4) else Color(0xFF005A9E) } }
fun Color.compositeOver(background: Color): Color { val alpha = this.alpha; val r = (this.red * alpha) + (background.red * (1 - alpha)); val g = (this.green * alpha) + (background.green * (1 - alpha)); val b = (this.blue * alpha) + (background.blue * (1 - alpha)); return Color(r, g, b, 1f) }
private fun getGreetingMessage(): String { val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY); return when (hour) { in 5..11 -> "Bonjour"; in 12..17 -> "Bon après-midi"; else -> "Bonsoir" } }