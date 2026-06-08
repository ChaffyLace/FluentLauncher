/**
 * @file HomeScreen.kt
 * @description Core dashboard UI for the launcher.
 * Manages the grid layout, drag-and-drop mechanics, Live Tiles,
 * and deep system gesture integrations via NestedScroll.
 * Integrates the ActionCenterOverlay at the root presentation layer.
 */

package com.example.fluentui.ui.homescreen

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.fluentui.R
import com.example.fluentui.ui.components.AppViewModel
import com.example.fluentui.ui.motion.FluentDashboardMotion
import com.example.fluentui.ui.motion.FluentMotion
import com.example.fluentui.ui.motion.FluentTile
import com.example.fluentui.ui.system.SystemInteractionManager
import com.example.fluentui.ui.system.TileConfig
import com.example.fluentui.ui.system.TileSize
import com.example.fluentui.ui.system.TileType
import com.example.fluentui.ui.system.rememberHaptics
import com.example.fluentui.wallpaper.MicaParams
import com.example.fluentui.wallpaper.fluentMicaComplex
import fluent.ui.system.icons.FluentIcons
import fluent.ui.system.icons.regular.Delete
import fluent.ui.system.icons.regular.Info
import fluent.ui.system.icons.regular.PinOff
import fluent.ui.system.icons.regular.StarEmphasis
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

// =============================================================================
// CONSTANTS & UTILS
// =============================================================================

private const val COLUMNS               = 6
private const val ROWS                  = 20
private val   CELL_SPACING              = 4.dp
private val   HORIZONTAL_PADDING        = 16.dp
private val   TOP_PADDING               = 60.dp
private val   BOTTOM_PADDING            = 100.dp
private val   TILE_CORNER_RADIUS        = 16.dp
private const val LAUNCH_DELAY_MS       = 150L
private const val LAUNCH_RESET_MS       = 500L
private const val DRAG_THRESHOLD_PX     = 15f
private const val SCROLL_TRIGGER_PX     = 150f
private const val SCROLL_STEP_PX        = 35f
private const val SWIPE_DOWN_THRESHOLD  = 100f

private val PACKAGE_NAME_REGEX = Regex("^[a-zA-Z][a-zA-Z0-9_]*(\\.[a-zA-Z][a-zA-Z0-9_]*)+$")
private fun String.isSafePackageName() = PACKAGE_NAME_REGEX.matches(this)

/**
 * Checks if a tile is eligible for dynamic content transformation.
 */
private fun TileConfig.hasDynamicContent(): Boolean {
    val pkg = packageName.lowercase()
    return type == TileType.PHOTO    ||
            type == TileType.MEDIA    ||
            type == TileType.CALENDAR ||
            type == TileType.WEATHER  ||
            type == TileType.CLOCK    ||
            type == TileType.WIDGET   ||
            pkg.contains("spotify")  ||
            pkg.contains("music")    ||
            pkg.contains("calendar") ||
            pkg.contains("clock")    ||
            pkg.contains("deskclock")||
            pkg.endsWith(".settings")
}

// =============================================================================
// GRID LAYOUT MANAGER (Core Logic)
// =============================================================================

internal data class GridPosition(val col: Int, val row: Int) {
    fun isPlaced() = col >= 0 && row >= 0
    companion object { val UNPLACED = GridPosition(-1, -1) }
}

internal data class GridMetrics(
    val cellSizePx: Int,
    val spacingPx:  Int,
    val paddingPx:  Int,
    val columns:    Int,
    val rows:       Int
)

internal data class TilePixelPosition(
    val x: Int, val y: Int,
    val width: Int, val height: Int
)

private var TileConfig.gridPosition: GridPosition
    get()      = GridPosition(gridX, gridY)
    set(value) { gridX = value.col; gridY = value.row }

internal object GridLayoutManager {

    fun colSpan(size: TileSize): Int = when (size) {
        TileSize.TINY   -> 1
        TileSize.SMALL  -> 2
        TileSize.MEDIUM -> 4
        TileSize.LARGE  -> 4
    }

    fun rowSpan(size: TileSize): Int = when (size) {
        TileSize.TINY   -> 1
        TileSize.SMALL  -> 2
        TileSize.MEDIUM -> 2
        TileSize.LARGE  -> 4
    }

    /**
     * Calculates pixel dimensions for the grid based on available screen width.
     */
    fun buildMetrics(containerWidthPx: Int, paddingPx: Int, spacingPx: Int): GridMetrics? {
        if (containerWidthPx == 0) return null
        val usable = containerWidthPx - paddingPx * 2 - spacingPx * (COLUMNS - 1)
        if (usable <= 0) return null
        return GridMetrics(usable / COLUMNS, spacingPx, paddingPx, COLUMNS, ROWS)
    }

    /**
     * Maps a grid coordinate to absolute pixel positions.
     */
    fun pixelPosition(tile: TileConfig, m: GridMetrics): TilePixelPosition {
        val step = m.cellSizePx + m.spacingPx
        val cols = colSpan(tile.size)
        val rows = rowSpan(tile.size)
        return TilePixelPosition(
            x      = m.paddingPx + tile.gridX * step,
            y      = tile.gridY * step,
            width  = cols * m.cellSizePx + (cols - 1) * m.spacingPx,
            height = rows * m.cellSizePx + (rows - 1) * m.spacingPx
        )
    }

    fun pixelToGrid(pxX: Float, pxY: Float, m: GridMetrics): GridPosition {
        val step = (m.cellSizePx + m.spacingPx).toFloat()
        return GridPosition(
            col = ((pxX - m.paddingPx) / step).roundToInt().coerceIn(0, m.columns - 1),
            row = (pxY / step).roundToInt().coerceIn(0, m.rows - 1)
        )
    }

    fun clampPosition(col: Int, row: Int, size: TileSize, m: GridMetrics): GridPosition =
        GridPosition(
            col = col.coerceIn(0, m.columns - colSpan(size)),
            row = row.coerceIn(0, m.rows    - rowSpan(size))
        )

    fun overlaps(
        x1: Int, y1: Int, w1: Int, h1: Int,
        x2: Int, y2: Int, w2: Int, h2: Int
    ) = x1 < x2 + w2 && x1 + w1 > x2 && y1 < y2 + h2 && y1 + h1 > y2

    fun isValid(
        tile: TileConfig,
        pos: GridPosition,
        all: List<TileConfig>,
        m: GridMetrics
    ): Boolean {
        val w = colSpan(tile.size)
        val h = rowSpan(tile.size)
        if (pos.col < 0 || pos.row < 0 || pos.col + w > m.columns || pos.row + h > m.rows) return false
        return all.none { o ->
            o.id != tile.id && o.gridPosition.isPlaced() &&
                    overlaps(pos.col, pos.row, w, h, o.gridX, o.gridY, colSpan(o.size), rowSpan(o.size))
        }
    }

    /**
     * Finds the nearest empty slot to avoid tile overlapping.
     */
    fun findFreeSlotNear(
        tile: TileConfig,
        preferredRow: Int,
        preferredCol: Int,
        all: List<TileConfig>,
        m: GridMetrics
    ): GridPosition? {
        val w      = colSpan(tile.size)
        val maxCol = m.columns - w
        if (maxCol < 0) return null

        val colsByProximity = (0..maxCol).sortedBy { abs(it - preferredCol) }

        for (r in preferredRow until m.rows) {
            for (c in colsByProximity) {
                val pos = GridPosition(c, r)
                if (isValid(tile, pos, all, m)) return pos
            }
        }
        for (r in (0 until preferredRow).reversed()) {
            for (c in colsByProximity) {
                val pos = GridPosition(c, r)
                if (isValid(tile, pos, all, m)) return pos
            }
        }
        return null
    }

    /**
     * Core collision resolver: Pushes tiles away when an anchor moves or resizes.
     */
    fun resolveCollisions(
        anchorId: String,
        anchorX: Int, anchorY: Int, anchorW: Int, anchorH: Int,
        working: MutableList<TileConfig>,
        m: GridMetrics
    ) {
        val displaced = working
            .filter { tile ->
                tile.id != anchorId &&
                        tile.gridPosition.isPlaced() &&
                        overlaps(anchorX, anchorY, anchorW, anchorH,
                            tile.gridX, tile.gridY, colSpan(tile.size), rowSpan(tile.size))
            }
            .sortedBy { abs(it.gridY - anchorY) + abs(it.gridX - anchorX) }

        for (tile in displaced) {
            val idx = working.indexOfFirst { it.id == tile.id }
            if (idx == -1) continue
            working[idx] = tile.copy(gridX = -1, gridY = -1)
            val pos = findFreeSlotNear(
                tile         = working[idx],
                preferredRow = tile.gridY,
                preferredCol = tile.gridX,
                all          = working,
                m            = m
            )
            working[idx] = if (pos != null)
                tile.copy(gridX = pos.col, gridY = pos.row)
            else
                tile.copy(gridX = -1, gridY = -1)
        }
    }

    fun applyResize(
        tile: TileConfig,
        newSize: TileSize,
        working: MutableList<TileConfig>,
        m: GridMetrics
    ): GridPosition {
        val clamped = clampPosition(tile.gridX, tile.gridY, newSize, m)
        val resized = tile.copy(size = newSize, gridX = clamped.col, gridY = clamped.row)

        val idx = working.indexOfFirst { it.id == tile.id }
        if (idx != -1) working[idx] = resized

        resolveCollisions(
            anchorId = tile.id,
            anchorX  = clamped.col, anchorY = clamped.row,
            anchorW  = colSpan(newSize), anchorH = rowSpan(newSize),
            working  = working, m = m
        )
        return clamped
    }

    fun applyDrag(
        tile: TileConfig,
        desiredCol: Int, desiredRow: Int,
        working: MutableList<TileConfig>,
        m: GridMetrics
    ): GridPosition {
        val clamped = clampPosition(desiredCol, desiredRow, tile.size, m)
        val moved   = tile.copy(gridX = clamped.col, gridY = clamped.row)

        val idx = working.indexOfFirst { it.id == tile.id }
        if (idx != -1) working[idx] = moved

        resolveCollisions(
            anchorId = tile.id,
            anchorX  = clamped.col, anchorY = clamped.row,
            anchorW  = colSpan(tile.size), anchorH = rowSpan(tile.size),
            working  = working, m = m
        )
        return clamped
    }

    fun contentRows(tiles: List<TileConfig>): Int =
        (tiles.maxOfOrNull { it.gridY + rowSpan(it.size) } ?: 5) + 2
}

// =============================================================================
// MAIN DASHBOARD COMPONENT
// =============================================================================

@SuppressLint("LocalContextGetResourceValueCall")
@Composable
fun DashboardPage(viewModel: AppViewModel) {
    val context          = LocalContext.current
    val scope            = rememberCoroutineScope()
    var launchingTileId by remember { mutableStateOf<String?>(null) }
    val micaParams      by viewModel.micaParams.collectAsState()
    val tiles           = viewModel.dashboardTiles.toList()
    val isDark          = isSystemInDarkTheme()

    // Action Center global state references
    val showActionCenter by viewModel.isActionCenterOpen.collectAsState()
    val accentColor      by viewModel.accentColor.collectAsState()

    LaunchedEffect(isDark) {
        viewModel.refreshWallpaperVisuals(isDarkOverride = isDark)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        FluentDashboardMotion(isAppLaunching = launchingTileId != null) {
            Box(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
                DashboardGrid(
                    tiles           = tiles,
                    micaParams      = micaParams,
                    isDark          = isDark,
                    viewModel       = viewModel,
                    launchingTileId = launchingTileId,
                    onTileClick     = { tile ->
                        when {
                            tile.type == TileType.PHOTO -> Unit

                            tile.type == TileType.APPLICATION
                                    && launchingTileId == null
                                    && tile.packageName.isSafePackageName() -> {
                                val intent = context.packageManager
                                    .getLaunchIntentForPackage(tile.packageName)
                                if (intent != null) {
                                    launchingTileId = tile.id
                                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    scope.launch {
                                        delay(LAUNCH_DELAY_MS)
                                        context.startActivity(intent)
                                        delay(LAUNCH_RESET_MS)
                                        launchingTileId = null
                                    }
                                } else {
                                    Toast.makeText(context, context.getString(R.string.hs_err_not_found), Toast.LENGTH_SHORT).show()
                                }
                            }

                            else -> Unit
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun DashboardGrid(
    tiles:           List<TileConfig>,
    micaParams:      MicaParams?,
    isDark:          Boolean,
    viewModel:       AppViewModel,
    launchingTileId: String?  = null,
    onTileClick:     (TileConfig) -> Unit
) {
    val context  = LocalContext.current
    val density  = LocalDensity.current
    val haptics  = rememberHaptics()
    val scope    = rememberCoroutineScope()

    var draggingTileId    by remember { mutableStateOf<String?>(null) }
    var draggingStartPos  by remember { mutableStateOf<TilePixelPosition?>(null) }
    var dragOffset        by remember { mutableStateOf(Offset.Zero) }
    var targetGridPos     by remember { mutableStateOf(GridPosition.UNPLACED) }
    val isDragging         = draggingTileId != null

    var autoScrollJob by remember { mutableStateOf<Job?>(null) }
    DisposableEffect(Unit) { onDispose { autoScrollJob?.cancel() } }

    var currentTiles by remember { mutableStateOf(tiles) }
    LaunchedEffect(tiles) { if (!isDragging) currentTiles = tiles }

    var containerWidthPx  by remember { mutableIntStateOf(0) }
    var containerHeightPx by remember { mutableIntStateOf(0) }
    val hPaddingPx = remember(density) { with(density) { HORIZONTAL_PADDING.roundToPx() } }
    val spacingPx  = remember(density) { with(density) { CELL_SPACING.roundToPx() } }

    val gridMetrics by remember(containerWidthPx, hPaddingPx, spacingPx) {
        derivedStateOf {
            GridLayoutManager.buildMetrics(containerWidthPx, hPaddingPx, spacingPx)
        }
    }

    val tilePositions by remember(currentTiles, gridMetrics) {
        derivedStateOf {
            val m = gridMetrics ?: return@derivedStateOf emptyMap<String, TilePixelPosition>()
            buildMap {
                currentTiles.forEach { tile ->
                    if (tile.gridPosition.isPlaced())
                        put(tile.id, GridLayoutManager.pixelPosition(tile, m))
                }
            }
        }
    }

    // Auto-placement for new tiles
    LaunchedEffect(currentTiles, gridMetrics) {
        val m = gridMetrics ?: return@LaunchedEffect
        if (isDragging) return@LaunchedEffect
        val unplaced = currentTiles.filter { !it.gridPosition.isPlaced() }
        if (unplaced.isEmpty()) return@LaunchedEffect

        val working = currentTiles.toMutableList()
        unplaced.forEach { tile ->
            val pos = GridLayoutManager.findFreeSlotNear(
                tile         = tile,
                preferredRow = 0,
                preferredCol = 0,
                all          = working,
                m            = m
            ) ?: return@forEach
            val idx = working.indexOfFirst { it.id == tile.id }
            if (idx != -1) working[idx] = tile.copy(gridX = pos.col, gridY = pos.row)
        }
        currentTiles = working
        viewModel.updateTilesOrder(working)
    }

    val contentHeightPx by remember(currentTiles, gridMetrics) {
        derivedStateOf {
            val m = gridMetrics ?: return@derivedStateOf 300
            (GridLayoutManager.contentRows(currentTiles) * (m.cellSizePx + m.spacingPx))
                .coerceAtLeast(300)
        }
    }

    val scrollState = rememberScrollState()

    val swipeDownEnabled by viewModel.swipeDownEnabled.collectAsState(initial = true)

    // Handling Swipe Down gesture for system integrations
    val nestedScrollConnection = remember(swipeDownEnabled) {
        object : NestedScrollConnection {
            var accumulatedSwipe = 0f

            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (swipeDownEnabled && available.y > 0 && scrollState.value == 0) {
                    accumulatedSwipe += available.y
                    if (accumulatedSwipe > SWIPE_DOWN_THRESHOLD) {
                        viewModel.toggleActionCenter(true)
                        accumulatedSwipe = 0f
                        return available
                    }
                } else {
                    accumulatedSwipe = 0f
                }
                return Offset.Zero
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { containerWidthPx = it.width; containerHeightPx = it.height }
            .nestedScroll(nestedScrollConnection)
            .verticalScroll(scrollState, enabled = !isDragging)
            .padding(top = TOP_PADDING, bottom = BOTTOM_PADDING)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(with(density) { contentHeightPx.toDp() } + 100.dp)
        ) {

            val newlyPinnedIds = remember { mutableStateOf(emptySet<String>()) }
            LaunchedEffect(currentTiles) {
                val currentIds  = currentTiles.map { it.id }.toSet()
                val previousIds = newlyPinnedIds.value
                val added = if (previousIds.isEmpty()) emptySet() else currentIds - previousIds
                newlyPinnedIds.value = currentIds
                if (added.isNotEmpty()) {
                    delay(800)
                    newlyPinnedIds.value = currentIds - added
                }
            }

            currentTiles.forEach { tile ->
                val tilePos = tilePositions[tile.id] ?: return@forEach
                key(tile.id) {
                    DashboardTile(
                        tile               = tile,
                        tilePos            = tilePos,
                        isNewlyPinned      = tile.id in (newlyPinnedIds.value),
                        dragStartPos       = if (tile.id == draggingTileId) draggingStartPos else null,
                        isDragging         = tile.id == draggingTileId,
                        isLaunching        = launchingTileId == tile.id,
                        dragOffsetProvider = { dragOffset },
                        micaParams         = micaParams,
                        isDark             = isDark,
                        viewModel          = viewModel,
                        onTap              = { if (!isDragging) onTileClick(tile) },

                        onResizeTile = { newSize ->
                            val metrics = gridMetrics ?: return@DashboardTile
                            haptics.performTick()
                            val working = currentTiles.toMutableList()
                            GridLayoutManager.applyResize(tile, newSize, working, metrics)
                            currentTiles = working
                            viewModel.updateTilesOrder(working)
                            viewModel.resizeTile(tile.id, newSize)
                        },

                        onUnpinTile = {
                            val updated = currentTiles.filter { it.id != tile.id }
                            currentTiles = updated
                            viewModel.updateTilesOrder(updated)
                            viewModel.unpinTile(tile.id)
                        },

                        onDragStart = {
                            draggingTileId   = tile.id
                            dragOffset       = Offset.Zero
                            draggingStartPos = tilePositions[tile.id]
                            targetGridPos    = GridPosition(tile.gridX, tile.gridY)
                            haptics.performLongPress()
                        },

                        onDrag = { dragAmount ->
                            dragOffset += dragAmount
                            val metrics = gridMetrics ?: return@DashboardTile
                            val startPx = draggingStartPos ?: return@DashboardTile
                            val w       = GridLayoutManager.colSpan(tile.size)
                            val h       = GridLayoutManager.rowSpan(tile.size)

                            val raw = GridLayoutManager.pixelToGrid(
                                pxX = startPx.x + dragOffset.x,
                                pxY = startPx.y + dragOffset.y,
                                m   = metrics
                            )
                            val clamped = GridPosition(
                                col = raw.col.coerceIn(0, metrics.columns - w),
                                row = raw.row.coerceIn(0, metrics.rows    - h)
                            )
                            if (clamped != targetGridPos) {
                                targetGridPos = clamped
                                haptics.performTick()
                            }

                            // Auto-scrolling logic while dragging
                            val visualTop    = startPx.y + dragOffset.y - scrollState.value
                            val visualBottom = visualTop + startPx.height
                            val scrollDir = when {
                                visualTop    < SCROLL_TRIGGER_PX && scrollState.value > 0       -> -1f
                                visualBottom > containerHeightPx - SCROLL_TRIGGER_PX
                                        && scrollState.value < scrollState.maxValue             ->  1f
                                else                                                             ->  0f
                            }
                            if (scrollDir == 0f) {
                                autoScrollJob?.cancel(); autoScrollJob = null
                            } else if (autoScrollJob?.isActive != true) {
                                autoScrollJob = scope.launch {
                                    while (true) {
                                        scrollState.scrollBy(scrollDir * SCROLL_STEP_PX)
                                        delay(16)
                                    }
                                }
                            }
                        },

                        onDragEnd = {
                            autoScrollJob?.cancel(); autoScrollJob = null
                            val metrics = gridMetrics
                            if (metrics != null && targetGridPos.isPlaced()) {
                                val working = currentTiles.toMutableList()
                                GridLayoutManager.applyDrag(
                                    tile       = tile,
                                    desiredCol = targetGridPos.col,
                                    desiredRow = targetGridPos.row,
                                    working    = working,
                                    m          = metrics
                                )
                                currentTiles = working
                                viewModel.updateTilesOrder(working)
                            } else {
                                viewModel.updateTilesOrder(currentTiles)
                            }
                            draggingTileId   = null
                            dragOffset       = Offset.Zero
                            targetGridPos    = GridPosition.UNPLACED
                            draggingStartPos = null
                            haptics.performRelease()
                        },

                        onDragCancel = {
                            autoScrollJob?.cancel(); autoScrollJob = null
                            currentTiles     = tiles
                            draggingTileId   = null
                            dragOffset       = Offset.Zero
                            targetGridPos    = GridPosition.UNPLACED
                            draggingStartPos = null
                            haptics.performRelease()
                        }
                    )
                }
            }
        }
    }
}

// =============================================================================
// MENU COMPONENTS
// =============================================================================

@Composable
private fun FluentAnimatedMenuItem(
    text: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    val haptics = rememberHaptics()
    var isPressed by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1f,
        animationSpec = tween(150),
        label = "menuItemScale"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        haptics.performTick()
                        tryAwaitRelease()
                        isPressed = false
                    },
                    onTap = { onClick() }
                )
            }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(14.dp))
        Text(text = text, color = color, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun FluentResizeRow(
    currentSize: TileSize,
    color: Color,
    onResize: (TileSize) -> Unit
) {
    val haptics = rememberHaptics()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val sizes = listOf(
            TileSize.TINY to "1×1",
            TileSize.SMALL to "2×2",
            TileSize.MEDIUM to "4×2",
            TileSize.LARGE to "4×4"
        )

        sizes.forEach { (size, label) ->
            val isSelected = size == currentSize
            val bgColor by animateColorAsState(if (isSelected) color.copy(alpha = 0.15f) else Color.Transparent, label = "bg")
            val borderColor = if (isSelected) color.copy(alpha = 0.3f) else color.copy(alpha = 0.1f)

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(bgColor)
                    .border(1.dp, borderColor, RoundedCornerShape(6.dp))
                    .clickable {
                        haptics.performTick()
                        onResize(size)
                    }
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    color = color,
                    fontSize = 12.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                )
            }
        }
    }
}

// =============================================================================
// DASHBOARD TILE (Individual Tile Logic)
// =============================================================================

@Composable
private fun DashboardTile(
    tile:              TileConfig,
    tilePos:           TilePixelPosition,
    isNewlyPinned:     Boolean = false,
    dragStartPos:      TilePixelPosition?,
    isDragging:        Boolean,
    isLaunching:       Boolean,
    dragOffsetProvider: () -> Offset,
    micaParams:        MicaParams?,
    isDark:            Boolean,
    viewModel:         AppViewModel,
    onTap:             () -> Unit,
    onResizeTile:      (TileSize) -> Unit,
    onUnpinTile:       () -> Unit,
    onDragStart:       () -> Unit,
    onDrag:            (Offset) -> Unit,
    onDragEnd:         () -> Unit,
    onDragCancel:      () -> Unit
) {
    val context  = LocalContext.current
    val density  = LocalDensity.current
    val scope    = rememberCoroutineScope()
    val haptics  = rememberHaptics()

    val isTransparencyEnabled by viewModel.transparencyEnabled.collectAsState(initial = true)

    val currentOnTap         by rememberUpdatedState(onTap)
    val currentOnResizeTile  by rememberUpdatedState(onResizeTile)
    val currentOnUnpinTile   by rememberUpdatedState(onUnpinTile)
    val currentOnDragStart   by rememberUpdatedState(onDragStart)
    val currentOnDrag        by rememberUpdatedState(onDrag)
    val currentOnDragEnd     by rememberUpdatedState(onDragEnd)
    val currentOnDragCancel  by rememberUpdatedState(onDragCancel)
    val currentDragStartPos  by rememberUpdatedState(dragStartPos)

    var isPressed      by remember { mutableStateOf(false) }
    var dropPosition   by remember { mutableStateOf<Offset?>(null) }
    var dragDistance   by remember { mutableFloatStateOf(0f) }
    var showMenu       by remember { mutableStateOf(false) }
    var isUnpinning    by remember { mutableStateOf(false) }

    var isAppearing by remember { mutableStateOf(isNewlyPinned) }
    LaunchedEffect(isNewlyPinned) {
        if (isNewlyPinned) {
            withFrameNanos {}
            isAppearing = false
        }
    }

    val animWidth  by animateDpAsState(with(density) { tilePos.width.toDp()  }, FluentMotion.standardSpec(), "w_${tile.id}")
    val animHeight by animateDpAsState(with(density) { tilePos.height.toDp() }, FluentMotion.standardSpec(), "h_${tile.id}")

    val animOffset = remember(tile.id) {
        Animatable(Offset(tilePos.x.toFloat(), tilePos.y.toFloat()), Offset.VectorConverter)
    }
    val currentTilePos    by rememberUpdatedState(tilePos)
    val currentIsDragging by rememberUpdatedState(isDragging)

    LaunchedEffect(tile.id) {
        animOffset.snapTo(Offset(currentTilePos.x.toFloat(), currentTilePos.y.toFloat()))
        snapshotFlow { Triple(currentTilePos.x, currentTilePos.y, currentIsDragging) }
            .collect { (x, y, dragging) ->
                if (!dragging) {
                    dropPosition?.let { pos -> animOffset.snapTo(pos); dropPosition = null }
                    val target = Offset(x.toFloat(), y.toFloat())
                    if (animOffset.value != target)
                        animOffset.animateTo(target, FluentMotion.standardSpec())
                }
            }
    }

    LaunchedEffect(isDragging) {
        if (isDragging) {
            animOffset.stop()
            animOffset.snapTo(Offset(currentTilePos.x.toFloat(), currentTilePos.y.toFloat()))
        }
    }

    val lifecycleScale by animateFloatAsState(
        targetValue = when {
            isAppearing -> 0.70f
            isUnpinning -> 0.85f
            isDragging  -> 1.05f
            else        -> 1f
        },
        animationSpec = when {
            isAppearing -> FluentMotion.enterSpec()
            isUnpinning -> FluentMotion.exitSpec()
            else        -> FluentMotion.standardSpec()
        },
        label = "scale_${tile.id}"
    )
    val animAlpha by animateFloatAsState(
        targetValue = when {
            isAppearing -> 0f
            isUnpinning -> 0f
            isDragging  -> 0.85f
            else        -> 1f
        },
        animationSpec = when {
            isAppearing -> FluentMotion.enterSpec()
            isUnpinning -> FluentMotion.exitSpec()
            else        -> FluentMotion.standardSpec()
        },
        label = "alpha_${tile.id}"
    )
    val animZIndex by animateFloatAsState(
        targetValue   = if (isDragging || showMenu) 100f else 0f,
        animationSpec = FluentMotion.standardSpec(),
        label         = "z_${tile.id}"
    )

    @SuppressLint("LocalContextGetResourceValueCall")
    fun openAppSettings() {
        if (!tile.packageName.isSafePackageName()) return
        try {
            context.startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", tile.packageName, null)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            )
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, context.getString(R.string.hs_err_settings), Toast.LENGTH_SHORT).show()
        }
    }

    @SuppressLint("LocalContextGetResourceValueCall")
    fun uninstallApp() {
        if (!tile.packageName.isSafePackageName()) return
        try {
            context.startActivity(
                Intent(Intent.ACTION_DELETE).apply {
                    data = Uri.fromParts("package", tile.packageName, null)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            )
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, context.getString(R.string.hs_err_uninstall), Toast.LENGTH_SHORT).show()
        }
    }

    Box(
        modifier = Modifier
            .size(animWidth, animHeight)
            .graphicsLayer {
                val startPos = currentDragStartPos
                val pos = if (isDragging && startPos != null) {
                    val off = dragOffsetProvider()
                    Offset(startPos.x + off.x, startPos.y + off.y)
                } else {
                    animOffset.value
                }
                translationX = pos.x
                translationY = pos.y
                scaleX       = lifecycleScale
                scaleY       = lifecycleScale
                alpha        = animAlpha
                clip         = false
            }
            .zIndex(animZIndex)
    ) {
        FluentTile(
            modifier      = Modifier.fillMaxSize(),
            isPressed     = isPressed && !isDragging,
            isDragging    = isDragging,
            isLaunching   = isLaunching,
            dragProgress  = 0f,
            cornerRadius  = TILE_CORNER_RADIUS
        ) {
            MicaTile(
                config    = tile,
                mica      = micaParams,
                isDark    = isDark,
                viewModel = viewModel,
                modifier  = Modifier
                    .fillMaxSize()
                    .pointerInput(tile.id, "tap") {
                        if (tile.type != TileType.PHOTO) {
                            detectTapGestures(
                                onPress = {
                                    if (!isUnpinning && !isAppearing) {
                                        isPressed = true
                                        try { tryAwaitRelease() } finally { isPressed = false }
                                    }
                                },
                                onTap = {
                                    if (!showMenu && !isUnpinning) currentOnTap()
                                }
                            )
                        }
                    }
                    .pointerInput(tile.id, "drag") {
                        detectDragGesturesAfterLongPress(
                            onDragStart = {
                                if (!isUnpinning && !isAppearing) {
                                    dragDistance = 0f
                                    showMenu     = false
                                }
                            },
                            onDrag = { change, amount ->
                                if (!isUnpinning) {
                                    val wasIdle = dragDistance < DRAG_THRESHOLD_PX
                                    dragDistance += amount.getDistance()
                                    if (wasIdle && dragDistance >= DRAG_THRESHOLD_PX) {
                                        currentOnDragStart()
                                    }
                                    change.consume()
                                    if (dragDistance >= DRAG_THRESHOLD_PX) {
                                        currentOnDrag(amount)
                                    }
                                }
                            },
                            onDragEnd = {
                                if (!isUnpinning) {
                                    if (dragDistance >= DRAG_THRESHOLD_PX) {
                                        currentDragStartPos?.let { start ->
                                            val off = dragOffsetProvider()
                                            dropPosition = Offset(start.x + off.x, start.y + off.y)
                                        }
                                        currentOnDragEnd()
                                    } else {
                                        haptics.performLongPress()
                                        showMenu = true
                                    }
                                    dragDistance = 0f
                                }
                            },
                            onDragCancel = {
                                if (!isUnpinning) {
                                    if (dragDistance >= DRAG_THRESHOLD_PX) {
                                        currentDragStartPos?.let { start ->
                                            val off = dragOffsetProvider()
                                            dropPosition = Offset(start.x + off.x, start.y + off.y)
                                        }
                                        currentOnDragCancel()
                                    }
                                    dragDistance = 0f
                                }
                            }
                        )
                    },
                isTransparencyEnabled = isTransparencyEnabled,
                isPressed = isPressed || isDragging
            )
        }

        // Context menu execution block
        val menuShape         = RoundedCornerShape(12.dp)
        val solidWindowsColor = if (isDark) Color(0xFF1E1E1E) else Color(0xFFFAFAFA)
        val borderColor       = if (isDark) Color.White.copy(0.12f) else Color.Black.copy(0.08f)

        DropdownMenu(
            expanded         = showMenu,
            onDismissRequest = { showMenu = false },
            offset           = DpOffset(0.dp, (-20).dp),
            shape            = menuShape,
            containerColor   = Color.Transparent,
            shadowElevation  = 0.dp,
            tonalElevation   = 0.dp,
            modifier         = Modifier
                .width(240.dp)
                .then(
                    if (isTransparencyEnabled) {
                        Modifier.fluentMicaComplex(
                            micaParams = micaParams,
                            enabled    = true,
                            elevation  = 0.dp,
                            shape      = menuShape
                        )
                    } else {
                        Modifier.background(solidWindowsColor, menuShape)
                    }
                )
                .border(1.dp, borderColor, menuShape)
        ) {
            Column(modifier = Modifier.animateContentSize(FluentMotion.standardSpec())) {
                val textColor = if (isDark) Color.White else Color.Black

                FluentAnimatedMenuItem(
                    text  = stringResource(R.string.hs_unpin),
                    icon  = FluentIcons.Regular.PinOff,
                    color = textColor,
                    onClick = {
                        showMenu    = false
                        isUnpinning = true
                        scope.launch { delay(150); currentOnUnpinTile() }
                    }
                )

                if (tile.hasDynamicContent()) {
                    FluentAnimatedMenuItem(
                        text  = if (tile.isDynamic) stringResource(R.string.hs_dynamic_off)
                        else stringResource(R.string.hs_dynamic_on),
                        icon  = FluentIcons.Regular.StarEmphasis,
                        color = textColor,
                        onClick = {
                            showMenu = false
                            viewModel.toggleDynamicState(tile.id)
                        }
                    )
                }

                if (tile.packageName.isSafePackageName()) {
                    FluentAnimatedMenuItem(
                        text  = stringResource(R.string.hs_app_info),
                        icon  = FluentIcons.Regular.Info,
                        color = textColor,
                        onClick = { showMenu = false; openAppSettings() }
                    )
                    FluentAnimatedMenuItem(
                        text  = stringResource(R.string.hs_uninstall),
                        icon  = FluentIcons.Regular.Delete,
                        color = Color(0xFFE53935),
                        onClick = { showMenu = false; uninstallApp() }
                    )
                }

                HorizontalDivider(
                    color    = textColor.copy(0.1f),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )

                Text(
                    text       = stringResource(R.string.hs_resize),
                    color      = textColor.copy(0.5f),
                    fontSize   = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier   = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )

                FluentResizeRow(
                    currentSize = tile.size,
                    color       = textColor,
                    onResize    = { newSize ->
                        showMenu = false
                        currentOnResizeTile(newSize)
                    }
                )
            }
        }
    }
}