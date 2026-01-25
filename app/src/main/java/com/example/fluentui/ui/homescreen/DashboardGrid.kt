package com.example.fluentui.ui.homescreen

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.fluentui.ui.components.AppViewModel
import com.example.fluentui.ui.system.TileConfig
import com.example.fluentui.ui.system.TileSize
import com.example.fluentui.wallpaper.MicaParams
import kotlin.math.roundToInt

private const val COLUMN_COUNT = 6
private val GAP_SIZE = 8.dp

@Composable
fun DashboardGrid(
    tiles: List<TileConfig>,
    micaParams: MicaParams?,
    isDark: Boolean,
    viewModel: AppViewModel,
    onTileClick: (TileConfig) -> Unit,
    isLaunchingState: Boolean
) {
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current

    // État du Drag
    var draggingTileId by remember { mutableStateOf<String?>(null) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    var initialDragPosition by remember { mutableStateOf(Offset.Zero) } // Position absolue départ
    var draggingTileSize by remember { mutableStateOf(androidx.compose.ui.geometry.Size.Zero) }

    var menuTileId by remember { mutableStateOf<String?>(null) }

    // --- CARTE DES POSITIONS (LA CLÉ DE LA RÉUSSITE) ---
    // On stocke la zone exacte de chaque tuile sur l'écran
    val itemBounds = remember { mutableMapOf<String, Rect>() }

    // Liste locale pour réactivité immédiate
    var currentTiles by remember { mutableStateOf(tiles) }
    LaunchedEffect(tiles) {
        if (draggingTileId == null) currentTiles = tiles
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val gridWidth = maxWidth
        val paddingHorizontal = 24.dp
        val totalGap = GAP_SIZE * (COLUMN_COUNT - 1)
        val unitWidth = (gridWidth - paddingHorizontal - totalGap) / COLUMN_COUNT

        Box(modifier = Modifier.fillMaxSize()) {

            LazyVerticalGrid(
                columns = GridCells.Fixed(COLUMN_COUNT),
                contentPadding = PaddingValues(top = 60.dp, bottom = 100.dp, start = 12.dp, end = 12.dp),
                verticalArrangement = Arrangement.spacedBy(GAP_SIZE),
                horizontalArrangement = Arrangement.spacedBy(GAP_SIZE),
                modifier = Modifier.fillMaxSize()
            ) {
                itemsIndexed(
                    items = currentTiles,
                    key = { _, tile -> tile.id },
                    span = { _, tile ->
                        val colSpan = when (tile.size) {
                            TileSize.TINY -> 1
                            TileSize.SMALL -> 2
                            TileSize.MEDIUM -> 4
                            TileSize.LARGE -> 4
                        }
                        GridItemSpan(colSpan)
                    }
                ) { index, tile ->

                    val isDragging = tile.id == draggingTileId
                    val tileHeight = calculateTileHeight(tile.size, unitWidth)

                    Box(
                        modifier = Modifier
                            .height(tileHeight)
                            .onGloballyPositioned { coords ->
                                // ON ENREGISTRE LA POSITION EN TEMPS RÉEL
                                if (coords.isAttached) {
                                    itemBounds[tile.id] = coords.boundsInWindow()
                                }
                            }
                            .alpha(if (isDragging) 0f else 1f) // L'original disparaît
                            .pointerInput(tile.id) {
                                detectDragGesturesAfterLongPress(
                                    onDragStart = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        draggingTileId = tile.id

                                        // On capture d'où on part (absolu)
                                        val bounds = itemBounds[tile.id] ?: Rect.Zero
                                        initialDragPosition = bounds.topLeft
                                        draggingTileSize = bounds.size

                                        dragOffset = Offset.Zero
                                        menuTileId = null
                                    },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        dragOffset += dragAmount
                                    },
                                    onDragEnd = {
                                        // Clic ou Déplacement ?
                                        if (dragOffset.getDistance() < 10f) {
                                            menuTileId = tile.id
                                        } else {
                                            // --- DÉTECTION DU LÂCHER (DROP) ---

                                            // 1. Où est le doigt (le centre de la tuile déplacée) ?
                                            val dropCenterX = initialDragPosition.x + dragOffset.x + (draggingTileSize.width / 2)
                                            val dropCenterY = initialDragPosition.y + dragOffset.y + (draggingTileSize.height / 2)
                                            val dropPoint = Offset(dropCenterX, dropCenterY)

                                            // 2. Qui est dessous ?
                                            val targetId = itemBounds.entries.find { (id, rect) ->
                                                id != tile.id && rect.contains(dropPoint)
                                            }?.key

                                            // 3. Exécution de l'échange
                                            if (targetId != null) {
                                                val fromIndex = currentTiles.indexOfFirst { it.id == tile.id }
                                                val toIndex = currentTiles.indexOfFirst { it.id == targetId }

                                                if (fromIndex != -1 && toIndex != -1) {
                                                    viewModel.moveTile(fromIndex, toIndex)
                                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                }
                                            }
                                        }
                                        draggingTileId = null
                                        dragOffset = Offset.Zero
                                    },
                                    onDragCancel = {
                                        draggingTileId = null
                                        dragOffset = Offset.Zero
                                    }
                                )
                            }
                    ) {
                        MicaTile(
                            config = tile,
                            mica = micaParams,
                            isDark = isDark,
                            viewModel = viewModel,
                            modifier = Modifier.fillMaxSize(),
                            onClick = { if (menuTileId == null) onTileClick(tile) }
                        )
                    }
                }
            }

            // --- FANTÔME (VISUEL) ---
            if (draggingTileId != null) {
                val tile = currentTiles.find { it.id == draggingTileId }
                if (tile != null) {
                    val w = calculateWidth(tile.size, unitWidth)
                    val h = calculateTileHeight(tile.size, unitWidth)

                    // Position Absolue sur l'écran
                    val currentX = initialDragPosition.x + dragOffset.x
                    val currentY = initialDragPosition.y + dragOffset.y

                    Box(
                        modifier = Modifier
                            .offset { IntOffset(currentX.roundToInt(), currentY.roundToInt()) }
                            .size(w, h)
                            .zIndex(1000f)
                            .scale(1.05f)
                            .alpha(0.9f)
                    ) {
                        MicaTile(tile, micaParams, isDark, viewModel, Modifier.fillMaxSize(), {})
                    }
                }
            }

            // --- MENU ---
            if (menuTileId != null) {
                val tile = tiles.find { it.id == menuTileId }
                if (tile != null) {
                    FluentContextMenu(true, { menuTileId = null }, tile, viewModel)
                }
            }
        }
    }
}

// Helpers
private fun calculateTileHeight(size: TileSize, unit: Dp): Dp {
    return when(size) {
        TileSize.TINY -> unit
        TileSize.SMALL -> (unit * 2) + GAP_SIZE
        TileSize.MEDIUM -> (unit * 2) + GAP_SIZE
        TileSize.LARGE -> (unit * 4) + (GAP_SIZE * 3)
    }
}

private fun calculateWidth(size: TileSize, unit: Dp): Dp {
    return when(size) {
        TileSize.TINY -> unit
        TileSize.SMALL -> (unit * 2) + GAP_SIZE
        TileSize.MEDIUM -> (unit * 4) + (GAP_SIZE * 3)
        TileSize.LARGE -> (unit * 4) + (GAP_SIZE * 3)
    }
}

@Composable
private fun FluentContextMenu(
    expanded: Boolean, onDismiss: () -> Unit, tile: TileConfig, viewModel: AppViewModel
) {
    MaterialTheme(
        shapes = MaterialTheme.shapes.copy(extraSmall = RoundedCornerShape(8.dp)),
        colorScheme = MaterialTheme.colorScheme.copy(surface = Color(0xFF2C2C2C))
    ) {
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = onDismiss,
            modifier = Modifier.background(Color(0xFF202020).copy(0.95f)).border(1.dp, Color.White.copy(0.08f), RoundedCornerShape(8.dp))
        ) {
            DropdownMenuItem(text = { Text("Désépingler", color = Color.White) }, onClick = { viewModel.unpinTile(tile.id); onDismiss() })
            HorizontalDivider(color = Color.White.copy(0.1f), modifier = Modifier.padding(vertical = 4.dp))
            Text("Taille", color = Color.Gray, fontSize = 12.sp, modifier = Modifier.padding(start = 12.dp, top = 4.dp))
            TileSize.values().forEach { size ->
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (tile.size == size) Box(Modifier.size(6.dp).background(Color(0xFF60CDFF), RoundedCornerShape(50))) else Spacer(Modifier.width(6.dp))
                            Spacer(Modifier.width(8.dp))
                            val name = when(size.name) { "SMALL" -> "Petit"; "MEDIUM" -> "Moyen"; "WIDE" -> "Large"; "LARGE" -> "Grand"; else -> size.name }
                            Text(name, color = if (tile.size == size) Color(0xFF60CDFF) else Color.White, fontWeight = if (tile.size == size) FontWeight.Bold else FontWeight.Normal)
                        }
                    },
                    onClick = { viewModel.resizeTile(tile.id, size); onDismiss() },
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    modifier = Modifier.height(36.dp)
                )
            }
        }
    }
}