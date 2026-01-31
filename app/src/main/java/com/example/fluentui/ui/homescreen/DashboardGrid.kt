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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.fluentui.ui.components.AppViewModel
import com.example.fluentui.ui.system.TileConfig
import com.example.fluentui.ui.system.TileSize
import com.example.fluentui.wallpaper.MicaParams
import kotlin.math.roundToInt

// CONFIGURATION
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

    // 1. RÉCUPÉRATION DU PARAMÈTRE MANQUANT
    // Nécessaire pour MicaTile
    val isTransparencyEnabled by viewModel.transparencyEnabled.collectAsState()

    // État du Drag
    var draggingTileId by remember { mutableStateOf<String?>(null) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    var startGridOffset by remember { mutableStateOf(IntOffset.Zero) }

    var draggingItemSize by remember { mutableStateOf(IntSize.Zero) }

    var menuTileId by remember { mutableStateOf<String?>(null) }

    val lazyGridState = rememberLazyGridState()

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

            // 1. GRILLE PRINCIPALE
            LazyVerticalGrid(
                columns = GridCells.Fixed(COLUMN_COUNT),
                state = lazyGridState,
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
                            TileSize.MEDIUM -> 4 // Assure-toi que MEDIUM existe dans ton enum TileSize
                            TileSize.LARGE -> 4
                            else -> 2 // Fallback
                        }
                        GridItemSpan(colSpan)
                    }
                ) { index, tile ->

                    val isDragging = tile.id == draggingTileId
                    val alpha by animateFloatAsState(if (isDragging) 0f else 1f, label = "alpha")
                    val tileHeight = calculateTileHeight(tile.size, unitWidth)

                    Box(
                        modifier = Modifier
                            .height(tileHeight)
                            .graphicsLayer { this.alpha = alpha }
                            .zIndex(if (isDragging) 100f else 0f)
                            .pointerInput(tile.id) {
                                detectDragGesturesAfterLongPress(
                                    onDragStart = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)

                                        val itemInfo = lazyGridState.layoutInfo.visibleItemsInfo.find { it.key == tile.id }
                                        if (itemInfo != null) {
                                            startGridOffset = itemInfo.offset
                                            draggingItemSize = itemInfo.size
                                            draggingTileId = tile.id
                                            dragOffset = Offset.Zero
                                            menuTileId = null
                                        }
                                    },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        dragOffset += dragAmount
                                    },
                                    onDragEnd = {
                                        if (dragOffset.getDistance() < 10f) {
                                            menuTileId = tile.id
                                        } else {
                                            // --- DÉTECTION ---
                                            val centerX = startGridOffset.x + dragOffset.x + (draggingItemSize.width / 2)
                                            val centerY = startGridOffset.y + dragOffset.y + (draggingItemSize.height / 2)

                                            val targetItem = lazyGridState.layoutInfo.visibleItemsInfo.find { item ->
                                                val x = item.offset.x
                                                val y = item.offset.y
                                                val w = item.size.width
                                                val h = item.size.height

                                                centerX > x && centerX < x + w &&
                                                        centerY > y && centerY < y + h
                                            }

                                            if (targetItem != null && targetItem.key != tile.id) {
                                                val fromIdx = currentTiles.indexOfFirst { it.id == tile.id }
                                                val toIdx = currentTiles.indexOfFirst { it.id == targetItem.key }

                                                if (fromIdx != -1 && toIdx != -1) {
                                                    viewModel.moveTile(fromIdx, toIdx)
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
                        // CORRECTION 1 : Ajout du paramètre isTransparencyEnabled
                        MicaTile(
                            config = tile,
                            mica = micaParams,
                            isDark = isDark,
                            viewModel = viewModel,
                            modifier = Modifier.fillMaxSize(),
                            onClick = { if (menuTileId == null) onTileClick(tile) },
                            isTransparencyEnabled = isTransparencyEnabled
                        )
                    }
                }
            }

            // 2. FANTÔME (VISUEL)
            if (draggingTileId != null) {
                val tile = currentTiles.find { it.id == draggingTileId }
                if (tile != null) {
                    val w = calculateWidth(tile.size, unitWidth)
                    val h = calculateTileHeight(tile.size, unitWidth)

                    val finalX = startGridOffset.x + dragOffset.x + with(density) { 12.dp.toPx() }
                    val finalY = startGridOffset.y + dragOffset.y + with(density) { 60.dp.toPx() }

                    Box(
                        modifier = Modifier
                            .offset { IntOffset(finalX.roundToInt(), finalY.roundToInt()) }
                            .size(w, h)
                            .zIndex(1000f)
                            .scale(1.05f)
                            .alpha(0.9f)
                    ) {
                        // CORRECTION 2 : Ajout du paramètre isTransparencyEnabled ici aussi
                        MicaTile(
                            config = tile,
                            mica = micaParams,
                            isDark = isDark,
                            viewModel = viewModel,
                            modifier = Modifier.fillMaxSize(),
                            isTransparencyEnabled = isTransparencyEnabled,
                            onClick = {}
                        )
                    }
                }
            }

            // 3. MENU
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
        else -> unit * 2 // Fallback
    }
}

private fun calculateWidth(size: TileSize, unit: Dp): Dp {
    return when(size) {
        TileSize.TINY -> unit
        TileSize.SMALL -> (unit * 2) + GAP_SIZE
        TileSize.MEDIUM -> (unit * 2) + GAP_SIZE
        TileSize.LARGE -> (unit * 4) + (GAP_SIZE * 3)
        else -> unit * 2 // Fallback
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