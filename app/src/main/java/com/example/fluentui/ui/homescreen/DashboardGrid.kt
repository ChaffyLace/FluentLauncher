package com.example.fluentui.ui.homescreen

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.fluentui.ui.components.AppViewModel
import com.example.fluentui.ui.motion.FluentDashboardMotion
import com.example.fluentui.ui.motion.FluentMotion
import com.example.fluentui.ui.motion.FluentTile
import com.example.fluentui.ui.system.TileConfig
import com.example.fluentui.ui.system.TileSize
import com.example.fluentui.ui.system.TileType
import com.example.fluentui.ui.system.rememberHaptics
import com.example.fluentui.wallpaper.MicaParams
import fluent.ui.system.icons.FluentIcons
import fluent.ui.system.icons.regular.Delete
import fluent.ui.system.icons.regular.Info
import fluent.ui.system.icons.regular.PinOff
import fluent.ui.system.icons.regular.StarEmphasis
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

// ─────────────────────────────────────────────────────────────────────────────
// CONSTANTES DE LA GRILLE
// ─────────────────────────────────────────────────────────────────────────────

private const val COLUMNS = 6
private const val ROWS = 20
private val CELL_SPACING = 4.dp
private val HORIZONTAL_PADDING = 16.dp
private val TOP_PADDING = 60.dp
private val BOTTOM_PADDING = 100.dp

private val TILE_CORNER_RADIUS = 16.dp

private const val LAUNCH_DELAY_MS = 150L
private const val LAUNCH_RESET_MS = 500L

private const val DRAG_THRESHOLD_PX = 15f
private const val SCROLL_TRIGGER_ZONE_PX = 150f
private const val SCROLL_STEP_PX = 35f

// FIX #12 — Regex stricte pour valider les package names Android avant tout usage dans Uri/Intent.
// Format attendu : com.example.app (au moins deux segments, alphanum + underscore uniquement)
private val PACKAGE_NAME_REGEX = Regex("^[a-zA-Z][a-zA-Z0-9_]*(\\.[a-zA-Z][a-zA-Z0-9_]*)+$")
private fun String.isSafePackageName(): Boolean = PACKAGE_NAME_REGEX.matches(this)

// ─────────────────────────────────────────────────────────────────────────────

internal data class GridPosition(val col: Int, val row: Int) {
    fun isPlaced() = col >= 0 && row >= 0
    companion object { val UNPLACED = GridPosition(-1, -1) }
}

internal data class GridMetrics(
    val cellSizePx: Int, val spacingPx: Int,
    val paddingPx: Int, val columns: Int, val rows: Int
)

internal data class TilePixelPosition(val x: Int, val y: Int, val width: Int, val height: Int)

private var TileConfig.gridPosition: GridPosition
    get() = GridPosition(gridX, gridY)
    set(value) { gridX = value.col; gridY = value.row }

internal object GridLayoutManager {

    fun colSpan(size: TileSize): Int = when (size) {
        TileSize.TINY -> 1; TileSize.SMALL -> 2
        TileSize.MEDIUM -> 4; TileSize.LARGE -> 4; else -> 2
    }

    fun rowSpan(size: TileSize): Int = when (size) {
        TileSize.TINY -> 1; TileSize.SMALL -> 2
        TileSize.MEDIUM -> 2; TileSize.LARGE -> 4; else -> 2
    }

    fun buildMetrics(containerWidthPx: Int, paddingPx: Int, spacingPx: Int): GridMetrics? {
        if (containerWidthPx == 0) return null
        val usable = containerWidthPx - paddingPx * 2 - spacingPx * (COLUMNS - 1)
        if (usable <= 0) return null
        return GridMetrics(usable / COLUMNS, spacingPx, paddingPx, COLUMNS, ROWS)
    }

    fun pixelPosition(tile: TileConfig, m: GridMetrics): TilePixelPosition {
        val step = m.cellSizePx + m.spacingPx
        val cols = colSpan(tile.size); val rows = rowSpan(tile.size)
        return TilePixelPosition(
            x = m.paddingPx + tile.gridX * step, y = tile.gridY * step,
            width = cols * m.cellSizePx + (cols - 1) * m.spacingPx,
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

    fun overlaps(x1: Int, y1: Int, w1: Int, h1: Int, x2: Int, y2: Int, w2: Int, h2: Int) =
        x1 < x2 + w2 && x1 + w1 > x2 && y1 < y2 + h2 && y1 + h1 > y2

    fun isValid(tile: TileConfig, pos: GridPosition, all: List<TileConfig>, m: GridMetrics): Boolean {
        val w = colSpan(tile.size); val h = rowSpan(tile.size)
        if (pos.col < 0 || pos.row < 0 || pos.col + w > m.columns || pos.row + h > m.rows) return false
        return all.none { o ->
            o.id != tile.id && o.gridPosition.isPlaced() &&
                    overlaps(pos.col, pos.row, w, h, o.gridX, o.gridY, colSpan(o.size), rowSpan(o.size))
        }
    }

    fun findFreeSlot(tile: TileConfig, all: List<TileConfig>, m: GridMetrics): GridPosition? {
        val w = colSpan(tile.size)
        for (r in 0 until m.rows) for (c in 0..(m.columns - w)) {
            val pos = GridPosition(c, r)
            if (isValid(tile, pos, all, m)) return pos
        }
        return null
    }

    fun contentRows(tiles: List<TileConfig>): Int =
        (tiles.maxOfOrNull { it.gridY + rowSpan(it.size) } ?: 5) + 2
}

@Composable
fun DashboardPage(viewModel: AppViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var launchingTileId by remember { mutableStateOf<String?>(null) }

    val micaParams by viewModel.micaParams.collectAsState()
    val tiles = viewModel.dashboardTiles.toList()
    val isDark = isSystemInDarkTheme()

    FluentDashboardMotion(isAppLaunching = launchingTileId != null) {
        Box(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
            DashboardGrid(
                tiles = tiles,
                micaParams = micaParams,
                isDark = isDark,
                viewModel = viewModel,
                launchingTileId = launchingTileId,
                onTileClick = { tile ->
                    // GUARD: We only launch an Intent if it's a real APPLICATION
                    if (tile.type == TileType.APPLICATION && launchingTileId == null && tile.packageName.isSafePackageName()) {
                        val intent = context.packageManager.getLaunchIntentForPackage(tile.packageName)
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
                            Toast.makeText(context, "App not found", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            )
        }
    }
}

@Composable
fun DashboardGrid(
    tiles: List<TileConfig>,
    micaParams: MicaParams?,
    isDark: Boolean,
    viewModel: AppViewModel,
    launchingTileId: String? = null,
    onTileClick: (TileConfig) -> Unit
) {
    val density = LocalDensity.current
    val haptics = rememberHaptics()
    val scope = rememberCoroutineScope()

    var draggingTileId by remember { mutableStateOf<String?>(null) }
    var draggingTileStartPos by remember { mutableStateOf<TilePixelPosition?>(null) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    var targetGridPos by remember { mutableStateOf(GridPosition.UNPLACED) }
    val isDragging = draggingTileId != null

    // FIX #11 — autoScrollJob géré via DisposableEffect pour garantir l'annulation même si le
    // composable quitte la composition pendant un scroll actif (ex : navigation, reconfiguration).
    var autoScrollJob by remember { mutableStateOf<Job?>(null) }
    DisposableEffect(Unit) {
        onDispose { autoScrollJob?.cancel() }
    }

    var currentTiles by remember { mutableStateOf(tiles) }
    LaunchedEffect(tiles) { if (!isDragging) currentTiles = tiles }

    var containerWidthPx by remember { mutableIntStateOf(0) }
    var containerHeightPx by remember { mutableIntStateOf(0) }
    val hPaddingPx = remember(density) { with(density) { HORIZONTAL_PADDING.roundToPx() } }
    val spacingPx = remember(density) { with(density) { CELL_SPACING.roundToPx() } }

    val gridMetrics by remember(containerWidthPx, hPaddingPx, spacingPx) {
        derivedStateOf { GridLayoutManager.buildMetrics(containerWidthPx, hPaddingPx, spacingPx) }
    }

    val tilePositions by remember(currentTiles, gridMetrics) {
        derivedStateOf {
            val m = gridMetrics ?: return@derivedStateOf emptyMap<String, TilePixelPosition>()
            buildMap {
                currentTiles.forEach { tile ->
                    if (tile.gridPosition.isPlaced()) put(tile.id, GridLayoutManager.pixelPosition(tile, m))
                }
            }
        }
    }

    LaunchedEffect(currentTiles, gridMetrics) {
        val m = gridMetrics ?: return@LaunchedEffect
        if (isDragging) return@LaunchedEffect
        val unplaced = currentTiles.filter { !it.gridPosition.isPlaced() }
        if (unplaced.isEmpty()) return@LaunchedEffect
        val working = currentTiles.toMutableList()
        unplaced.forEach { tile ->
            val pos = GridLayoutManager.findFreeSlot(tile, working, m) ?: return@forEach
            val idx = working.indexOfFirst { it.id == tile.id }
            if (idx != -1) working[idx] = tile.copy(gridX = pos.col, gridY = pos.row)
        }
        currentTiles = working
        viewModel.updateTilesOrder(working)
    }

    // FIX #7 — contentHeightPx : garde un minimum de 300px pour éviter l'effondrement du container
    // quand gridMetrics est null (première frame avant onSizeChanged).
    val contentHeightPx by remember(currentTiles, gridMetrics) {
        derivedStateOf {
            val m = gridMetrics ?: return@derivedStateOf 300
            (GridLayoutManager.contentRows(currentTiles) * (m.cellSizePx + m.spacingPx))
                .coerceAtLeast(300)
        }
    }

    val scrollState = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { containerWidthPx = it.width; containerHeightPx = it.height }
            .verticalScroll(scrollState, enabled = !isDragging)
            .padding(top = TOP_PADDING, bottom = BOTTOM_PADDING)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(with(density) { contentHeightPx.toDp() } + 100.dp)
        ) {

            // ─── GHOST BOX ────────────────────────────────────────────────────
            // FIX #5 — Le ghost utilise désormais un seul bloc graphicsLayer pour synchroniser
            // X, Y, width, height dans la même passe de dessin → plus de décalage d'un frame.
            val m = gridMetrics
            if (isDragging && targetGridPos.isPlaced() && m != null) {
                val draggedTile = currentTiles.find { it.id == draggingTileId }
                if (draggedTile != null) {
                    val ghostPos by remember(draggedTile, targetGridPos, m) {
                        derivedStateOf {
                            GridLayoutManager.pixelPosition(
                                draggedTile.copy(gridX = targetGridPos.col, gridY = targetGridPos.row), m
                            )
                        }
                    }

                    val animGhostX by animateFloatAsState(
                        targetValue = ghostPos.x.toFloat(),
                        animationSpec = FluentMotion.standardSpec(), label = "ghost_x"
                    )
                    val animGhostY by animateFloatAsState(
                        targetValue = ghostPos.y.toFloat(),
                        animationSpec = FluentMotion.standardSpec(), label = "ghost_y"
                    )
                    val animGhostW by animateFloatAsState(
                        targetValue = ghostPos.width.toFloat(),
                        animationSpec = FluentMotion.standardSpec(), label = "ghost_w"
                    )
                    val animGhostH by animateFloatAsState(
                        targetValue = ghostPos.height.toFloat(),
                        animationSpec = FluentMotion.standardSpec(), label = "ghost_h"
                    )

                    // Tout se passe dans un seul graphicsLayer → rendu atomique, zéro décalage.
                    Box(
                        modifier = Modifier
                            .graphicsLayer {
                                translationX = animGhostX
                                translationY = animGhostY
                            }
                            .size(
                                with(density) { animGhostW.toDp() },
                                with(density) { animGhostH.toDp() }
                            )
                            .background(Color.White.copy(0.15f), RoundedCornerShape(TILE_CORNER_RADIUS))
                            .border(2.dp, Color.White.copy(0.8f), RoundedCornerShape(TILE_CORNER_RADIUS))
                    )
                }
            }

            currentTiles.forEach { tile ->
                val tilePos = tilePositions[tile.id] ?: return@forEach
                key(tile.id) {
                    DashboardTile(
                        tile = tile,
                        tilePos = tilePos,
                        dragStartPos = if (tile.id == draggingTileId) draggingTileStartPos else null,
                        isDragging = tile.id == draggingTileId,
                        isLaunching = launchingTileId == tile.id,
                        dragOffsetProvider = { dragOffset },
                        micaParams = micaParams, isDark = isDark, viewModel = viewModel,
                        onTap = { if (!isDragging) onTileClick(tile) },
                        onResizeTile = { newSize ->
                            haptics.performTick()
                            val metrics = gridMetrics ?: return@DashboardTile
                            val updatedTile = tile.copy(size = newSize)
                            val w1 = GridLayoutManager.colSpan(newSize)
                            val h1 = GridLayoutManager.rowSpan(newSize)
                            val workingList = currentTiles.toMutableList()

                            val tIdx = workingList.indexOfFirst { it.id == tile.id }
                            if (tIdx != -1) workingList[tIdx] = updatedTile

                            for (i in workingList.indices) {
                                val other = workingList[i]
                                if (other.id != tile.id && other.gridPosition.isPlaced()) {
                                    val w2 = GridLayoutManager.colSpan(other.size)
                                    val h2 = GridLayoutManager.rowSpan(other.size)
                                    if (GridLayoutManager.overlaps(updatedTile.gridX, updatedTile.gridY, w1, h1, other.gridX, other.gridY, w2, h2)) {
                                        workingList[i] = other.copy(gridX = -1, gridY = -1)
                                    }
                                }
                            }
                            for (i in workingList.indices) {
                                val other = workingList[i]
                                if (!other.gridPosition.isPlaced()) {
                                    val pos = GridLayoutManager.findFreeSlot(other, workingList, metrics)
                                    if (pos != null) workingList[i] = other.copy(gridX = pos.col, gridY = pos.row)
                                }
                            }

                            currentTiles = workingList
                            viewModel.updateTilesOrder(workingList)
                            viewModel.resizeTile(tile.id, newSize)
                        },
                        onUnpinTile = {
                            val newTilesList = currentTiles.filter { it.id != tile.id }
                            currentTiles = newTilesList
                            viewModel.updateTilesOrder(newTilesList)
                        },
                        onDragStart = {
                            draggingTileId = tile.id
                            dragOffset = Offset.Zero
                            draggingTileStartPos = tilePositions[tile.id]
                            targetGridPos = GridPosition(tile.gridX, tile.gridY)
                            haptics.performLongPress()
                        },
                        onDrag = { dragAmount ->
                            dragOffset += dragAmount
                            val metrics = gridMetrics ?: return@DashboardTile
                            val startPx = draggingTileStartPos ?: return@DashboardTile

                            val topX = startPx.x + dragOffset.x
                            val topY = startPx.y + dragOffset.y

                            val w1 = GridLayoutManager.colSpan(tile.size)
                            val h1 = GridLayoutManager.rowSpan(tile.size)
                            val raw = GridLayoutManager.pixelToGrid(topX, topY, metrics)
                            val clamped = GridPosition(
                                raw.col.coerceIn(0, metrics.columns - w1),
                                raw.row.coerceIn(0, metrics.rows - h1)
                            )

                            if (clamped != targetGridPos) {
                                targetGridPos = clamped
                                haptics.performTick()

                                val updatedTile = tile.copy(gridX = clamped.col, gridY = clamped.row)
                                val workingList = currentTiles.toMutableList()

                                val tIdx = workingList.indexOfFirst { it.id == tile.id }
                                if (tIdx != -1) workingList[tIdx] = updatedTile

                                for (i in workingList.indices) {
                                    val other = workingList[i]
                                    if (other.id != tile.id && other.gridPosition.isPlaced()) {
                                        val w2 = GridLayoutManager.colSpan(other.size)
                                        val h2 = GridLayoutManager.rowSpan(other.size)
                                        if (GridLayoutManager.overlaps(clamped.col, clamped.row, w1, h1, other.gridX, other.gridY, w2, h2)) {
                                            workingList[i] = other.copy(gridX = -1, gridY = -1)
                                        }
                                    }
                                }
                                for (i in workingList.indices) {
                                    val other = workingList[i]
                                    if (!other.gridPosition.isPlaced()) {
                                        val pos = GridLayoutManager.findFreeSlot(other, workingList, metrics)
                                        if (pos != null) workingList[i] = other.copy(gridX = pos.col, gridY = pos.row)
                                    }
                                }
                                currentTiles = workingList
                            }

                            val visualTopY = startPx.y + dragOffset.y - scrollState.value
                            val visualBottomY = visualTopY + startPx.height
                            val scrollDir = when {
                                visualTopY < SCROLL_TRIGGER_ZONE_PX && scrollState.value > 0 -> -1f
                                visualBottomY > containerHeightPx - SCROLL_TRIGGER_ZONE_PX
                                        && scrollState.value < scrollState.maxValue -> 1f
                                else -> 0f
                            }
                            if (scrollDir == 0f) {
                                autoScrollJob?.cancel(); autoScrollJob = null
                            } else if (autoScrollJob?.isActive != true) {
                                autoScrollJob = scope.launch {
                                    while (true) { scrollState.scrollBy(scrollDir * SCROLL_STEP_PX); delay(16) }
                                }
                            }
                        },
                        onDragEnd = {
                            autoScrollJob?.cancel(); autoScrollJob = null
                            viewModel.updateTilesOrder(currentTiles)
                            draggingTileId = null
                            dragOffset = Offset.Zero
                            targetGridPos = GridPosition.UNPLACED
                            draggingTileStartPos = null
                            haptics.performRelease()
                        },
                        onDragCancel = {
                            autoScrollJob?.cancel(); autoScrollJob = null
                            currentTiles = tiles
                            draggingTileId = null
                            dragOffset = Offset.Zero
                            targetGridPos = GridPosition.UNPLACED
                            draggingTileStartPos = null
                            haptics.performRelease()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun DashboardTile(
    tile: TileConfig,
    tilePos: TilePixelPosition,
    dragStartPos: TilePixelPosition?,
    isDragging: Boolean,
    isLaunching: Boolean,
    dragOffsetProvider: () -> Offset,
    micaParams: MicaParams?,
    isDark: Boolean,
    viewModel: AppViewModel,
    onTap: () -> Unit,
    onResizeTile: (TileSize) -> Unit,
    onUnpinTile: () -> Unit,
    onDragStart: () -> Unit,
    onDrag: (dragAmount: Offset) -> Unit,
    onDragEnd: () -> Unit,
    onDragCancel: () -> Unit
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()

    val currentOnTap by rememberUpdatedState(onTap)
    val currentOnResizeTile by rememberUpdatedState(onResizeTile)
    val currentOnUnpinTile by rememberUpdatedState(onUnpinTile)
    val currentOnDragStart by rememberUpdatedState(onDragStart)
    val currentOnDrag by rememberUpdatedState(onDrag)
    val currentOnDragEnd by rememberUpdatedState(onDragEnd)
    val currentOnDragCancel by rememberUpdatedState(onDragCancel)
    val currentDragStartPos by rememberUpdatedState(dragStartPos)

    var isPressed by remember { mutableStateOf(false) }
    var dropPosition by remember { mutableStateOf<Offset?>(null) }
    var dragDistance by remember { mutableFloatStateOf(0f) }
    var showContextMenu by remember { mutableStateOf(false) }
    var isUnpinning by remember { mutableStateOf(false) }

    // FIX #4 — isAppearing démarre à true seulement pour les nouvelles tuiles.
    // On passe à false dès le premier frame rendu pour éviter l'animation d'entrée
    // sur les tuiles déjà présentes au lancement de l'écran.
    // La frame-skip via `withFrameNanos` garantit que la valeur initiale (0.85f / 0f)
    // est bien dessinée avant de transitionner vers l'état final.
    var isAppearing by remember { mutableStateOf(true) }
    LaunchedEffect(tile.id) {
        // Attendre exactement une frame pour que l'état initial soit rendu
        withFrameNanos {}
        isAppearing = false
    }

    val animWidth by animateDpAsState(
        targetValue = with(density) { tilePos.width.toDp() },
        animationSpec = FluentMotion.standardSpec(),
        label = "width_${tile.id}"
    )
    val animHeight by animateDpAsState(
        targetValue = with(density) { tilePos.height.toDp() },
        animationSpec = FluentMotion.standardSpec(),
        label = "height_${tile.id}"
    )

    // FIX #1 — animOffset : on snap immédiatement à la position initiale à la première composition
    // pour éviter le "téléport" depuis Offset(0,0) vers la vraie position.
    val animOffset = remember(tile.id) {
        Animatable(Offset(tilePos.x.toFloat(), tilePos.y.toFloat()), Offset.VectorConverter)
    }

    // FIX #9 — snapshotFlow ne peut tracker QUE des objets Compose State.
    // tilePos et isDragging sont des paramètres ordinaires → snapshotFlow ne les voit pas changer.
    // On les wrappe avec rememberUpdatedState pour obtenir un State<T> observable,
    // ce qui permet au flow de se déclencher à chaque changement de position sans recréer
    // le LaunchedEffect (et donc sans relancer toutes les tuiles à chaque frame de drag).
    val currentTilePos by rememberUpdatedState(tilePos)
    val currentIsDragging by rememberUpdatedState(isDragging)

    LaunchedEffect(tile.id) {
        // Snap initial synchrone : couvre le cas où tilePos change avant la 1ère composition
        animOffset.snapTo(Offset(currentTilePos.x.toFloat(), currentTilePos.y.toFloat()))
        snapshotFlow { Triple(currentTilePos.x, currentTilePos.y, currentIsDragging) }
            .collect { (x, y, dragging) ->
                if (!dragging) {
                    // FIX #3 — Snap vers dropPosition d'abord, puis animation vers la grille
                    dropPosition?.let { pos -> animOffset.snapTo(pos); dropPosition = null }
                    // FIX #8 — Guard : pas d'animation inutile si déjà à destination
                    val target = Offset(x.toFloat(), y.toFloat())
                    if (animOffset.value != target) {
                        animOffset.animateTo(target, FluentMotion.standardSpec())
                    }
                }
            }
    }

    val lifecycleScale by animateFloatAsState(
        targetValue = when {
            isAppearing -> 0.85f
            isUnpinning -> 0.85f
            isDragging -> 1.05f
            else -> 1f
        },
        animationSpec = when {
            isAppearing -> FluentMotion.enterSpec()
            isUnpinning -> FluentMotion.exitSpec()
            else -> FluentMotion.standardSpec()
        },
        label = "lifecycle_scale_${tile.id}"
    )

    val animAlpha by animateFloatAsState(
        targetValue = when {
            isAppearing -> 0f
            isUnpinning -> 0f
            isDragging -> 0.85f
            else -> 1f
        },
        animationSpec = when {
            isAppearing -> FluentMotion.enterSpec()
            isUnpinning -> FluentMotion.exitSpec()
            else -> FluentMotion.standardSpec()
        },
        label = "alpha_${tile.id}"
    )

    // FIX #2 — zIndex animé pour éviter le hard-jump visuel 0→100.
    // La tuile draguée / avec menu contextuel monte progressivement au premier plan.
    val animZIndex by animateFloatAsState(
        targetValue = if (isDragging || showContextMenu) 100f else 0f,
        animationSpec = FluentMotion.standardSpec(),
        label = "zIndex_${tile.id}"
    )

    // ─── ACTIONS SYSTÈME ──────────────────────────────────────────────────────

    fun openAppSettings() {
        // FIX #12 — Validation stricte du package name avant de construire l'URI
        if (tile.packageName.isSafePackageName()) {
            try {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", tile.packageName, null)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (e: ActivityNotFoundException) {
                Toast.makeText(context, "Impossible d'ouvrir les paramètres", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun uninstallApp() {
        // FIX #12 — Validation stricte du package name + Uri.fromParts au lieu de Uri.parse
        if (tile.packageName.isSafePackageName()) {
            try {
                val intent = Intent(Intent.ACTION_DELETE).apply {
                    data = Uri.fromParts("package", tile.packageName, null)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (e: ActivityNotFoundException) {
                Toast.makeText(context, "Impossible de désinstaller", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Box(
        modifier = Modifier
            .size(animWidth, animHeight)
            .graphicsLayer {
                // FIX #6 — Safe access : on ne lit currentDragStartPos qu'une seule fois
                // dans un let, ce qui élimine le !! et la race condition associée.
                val currentOffset = dragOffsetProvider()
                val startPos = currentDragStartPos
                val pos = if (isDragging && startPos != null)
                    Offset(startPos.x + currentOffset.x, startPos.y + currentOffset.y)
                else
                    animOffset.value

                translationX = pos.x
                translationY = pos.y
                scaleX = lifecycleScale
                scaleY = lifecycleScale
                alpha = animAlpha
                this.clip = false
            }
            .zIndex(animZIndex) // FIX #2 — zIndex animé
    ) {
        FluentTile(
            modifier = Modifier.fillMaxSize(),
            isPressed = isPressed && !isDragging,
            isDragging = isDragging,
            isLaunching = isLaunching,
            dragProgress = 0f,
            cornerRadius = TILE_CORNER_RADIUS
        ) {
            val isTransparencyEnabled by viewModel.transparencyEnabled.collectAsState()
            MicaTile(
                config = tile, mica = micaParams, isDark = isDark,
                viewModel = viewModel,
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(tile.id, "tap") {
                        detectTapGestures(
                            onPress = {
                                if (!isUnpinning && !isAppearing) {
                                    isPressed = true
                                    try { tryAwaitRelease() } finally { isPressed = false }
                                }
                            },
                            onTap = {
                                if (!showContextMenu && !isUnpinning) currentOnTap()
                            },
                            onDoubleTap = {
                                if (!showContextMenu && !isUnpinning) {
                                    val next = when (tile.size) {
                                        TileSize.TINY   -> TileSize.SMALL
                                        TileSize.SMALL  -> TileSize.MEDIUM
                                        TileSize.MEDIUM -> TileSize.LARGE
                                        TileSize.LARGE  -> TileSize.TINY
                                        else            -> TileSize.SMALL
                                    }
                                    currentOnResizeTile(next)
                                }
                            }
                        )
                    }
                    .pointerInput(tile.id, "drag") {
                        detectDragGesturesAfterLongPress(
                            onDragStart = {
                                if (!isUnpinning && !isAppearing) {
                                    dragDistance = 0f
                                    currentOnDragStart()
                                }
                            },
                            onDrag = { change, dragAmount ->
                                if (!isUnpinning) {
                                    dragDistance += dragAmount.getDistance()
                                    if (dragDistance > DRAG_THRESHOLD_PX && showContextMenu)
                                        showContextMenu = false
                                    change.consume()
                                    currentOnDrag(dragAmount)
                                }
                            },
                            onDragEnd = {
                                if (!isUnpinning) {
                                    if (dragDistance > DRAG_THRESHOLD_PX) {
                                        // FIX #6 — Safe let : on évite le !! sur currentDragStartPos
                                        currentDragStartPos?.let { start ->
                                            val currentOffset = dragOffsetProvider()
                                            dropPosition = Offset(
                                                start.x + currentOffset.x,
                                                start.y + currentOffset.y
                                            )
                                        }
                                        currentOnDragEnd()
                                    } else {
                                        showContextMenu = true
                                        currentOnDragCancel()
                                    }
                                    dragDistance = 0f
                                }
                            },
                            onDragCancel = {
                                if (!isUnpinning) {
                                    dragDistance = 0f
                                    // FIX #6 — Safe let
                                    currentDragStartPos?.let { start ->
                                        val currentOffset = dragOffsetProvider()
                                        dropPosition = Offset(
                                            start.x + currentOffset.x,
                                            start.y + currentOffset.y
                                        )
                                    }
                                    currentOnDragCancel()
                                }
                            }
                        )
                    },
                isTransparencyEnabled = isTransparencyEnabled,
                isPressed = isPressed || isDragging
            )
        }

        DropdownMenu(
            expanded = showContextMenu,
            onDismissRequest = { showContextMenu = false },
            shape = RoundedCornerShape(8.dp),
            containerColor = if (isDark) Color(0xFF202020) else Color(0xFFF3F3F3),
            shadowElevation = 12.dp, tonalElevation = 0.dp,
            modifier = Modifier
                .width(220.dp)
                .border(
                    0.5.dp,
                    if (isDark) Color.White.copy(0.1f) else Color.Black.copy(0.1f),
                    RoundedCornerShape(8.dp)
                )
        ) {
            Column(modifier = Modifier.animateContentSize(FluentMotion.standardSpec())) {
                val textColor = if (isDark) Color.White else Color.Black

                DropdownMenuItem(
                    text = { Text("Désépingler", color = textColor, fontSize = 14.sp) },
                    leadingIcon = { Icon(FluentIcons.Regular.PinOff, null, tint = textColor, modifier = Modifier.size(20.dp)) },
                    onClick = {
                        showContextMenu = false
                        isUnpinning = true
                        scope.launch {
                            delay(150)
                            currentOnUnpinTile()
                        }
                    }
                )
                DropdownMenuItem(
                    text = { Text("Tuile dynamique", color = textColor, fontSize = 14.sp) },
                    leadingIcon = { Icon(FluentIcons.Regular.StarEmphasis, null, tint = textColor, modifier = Modifier.size(20.dp)) },
                    onClick = { showContextMenu = false }
                )

                // FIX #12 — On n'affiche les options système que si le package name est valide
                if (tile.packageName.isSafePackageName()) {
                    DropdownMenuItem(
                        text = { Text("Infos sur l'application", color = textColor, fontSize = 14.sp) },
                        leadingIcon = { Icon(FluentIcons.Regular.Info, null, tint = textColor, modifier = Modifier.size(20.dp)) },
                        onClick = {
                            showContextMenu = false
                            openAppSettings()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Désinstaller", color = Color(0xFFE53935), fontSize = 14.sp) },
                        leadingIcon = { Icon(FluentIcons.Regular.Delete, null, tint = Color(0xFFE53935), modifier = Modifier.size(20.dp)) },
                        onClick = {
                            showContextMenu = false
                            uninstallApp()
                        }
                    )
                }

                HorizontalDivider(color = textColor.copy(0.1f), modifier = Modifier.padding(vertical = 4.dp))

                Text(
                    "Redimensionner", color = textColor.copy(0.5f), fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
                DropdownMenuItem(
                    text = { Text("Petite (1x1)", color = textColor, fontSize = 14.sp) },
                    onClick = { showContextMenu = false; currentOnResizeTile(TileSize.TINY) }
                )
                DropdownMenuItem(
                    text = { Text("Moyenne (2x2)", color = textColor, fontSize = 14.sp) },
                    onClick = { showContextMenu = false; currentOnResizeTile(TileSize.SMALL) }
                )
                DropdownMenuItem(
                    text = { Text("Large (4x2)", color = textColor, fontSize = 14.sp) },
                    onClick = { showContextMenu = false; currentOnResizeTile(TileSize.MEDIUM) }
                )
                DropdownMenuItem(
                    text = { Text("Grand (4x4)", color = textColor, fontSize = 14.sp) },
                    onClick = { showContextMenu = false; currentOnResizeTile(TileSize.LARGE) }
                )
            }
        }
    }
}