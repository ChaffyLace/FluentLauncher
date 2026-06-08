/**
 * @file AppGrid.kt
 * @description Core UI component for displaying and managing applications in a grid layout.
 * Features production-grade drag-and-drop physics, strict collision detection for folder creation,
 * and high-fidelity Mica material rendering.
 */

package com.example.fluentui.ui.components

import android.content.Context
import android.content.Intent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fluentui.R
import com.example.fluentui.ui.utils.IconCache
import com.example.fluentui.wallpaper.MicaParams
import com.example.fluentui.wallpaper.fluentMicaComplex
import fluent.ui.system.icons.FluentIcons
import fluent.ui.system.icons.regular.Delete
import fluent.ui.system.icons.regular.Folder
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.Collator
import java.util.UUID
import kotlin.math.roundToInt


// =============================================================================
// UTILS
// =============================================================================

private val AppIconShape = RoundedCornerShape(12.dp)

/**
 * Calcule une zone de collision optimisée.
 * Un ratio de 0.05f garde la fluidité tout en évitant les fusions accidentelles
 * quand on frôle à peine l'icône voisine.
 */
private fun shrinkRect(rect: Rect, ratio: Float = 0.05f): Rect {
    val widthShrink = rect.width * ratio
    val heightShrink = rect.height * ratio
    return Rect(
        left = rect.left + widthShrink,
        top = rect.top + heightShrink,
        right = rect.right - widthShrink,
        bottom = rect.bottom - heightShrink
    )
}
// =============================================================================
// INTERNAL APP ICON
// =============================================================================

@Composable
fun InternalAppIcon(packageName: String, modifier: Modifier = Modifier) {
    val bitmap = remember(packageName) {
        try { IconCache.get(packageName) } catch (e: SecurityException) { null }
    }
    if (bitmap != null) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = null,
            modifier = modifier,
            contentScale = ContentScale.Fit
        )
    } else {
        Box(modifier = modifier.background(Color.Gray.copy(0.2f), AppIconShape))
    }
}

// =============================================================================
// MAIN APP GRID
// =============================================================================

@Composable
fun AppGrid(
    viewModel: AppViewModel = viewModel(),
    state: LazyGridState = rememberLazyGridState(),
    columnCount: Int = 4,
    rowCount: Int = 6,
    iconSize: Float = 60f,
    showLabels: Boolean = true,
    searchQuery: String = "",
    hiddenApps: Set<String> = emptySet(),
    fontSize: Float = 13f,
    verticalSpacing: Float = 16f,
    accentColor: Color = Color(0xFF0078D4),
    isVertical: Boolean = false,
    onPinRequest: (AppModel) -> Unit = { viewModel.pinApp(it) },
    onClose: (() -> Unit)? = null,
    micaParams: MicaParams
) {
    val context = LocalContext.current
    val isDark  = isSystemInDarkTheme()
    val scope   = rememberCoroutineScope()
    val haptic  = LocalHapticFeedback.current

    val notifCounts by viewModel.notificationCounts.collectAsState()

    var folders by remember { mutableStateOf<List<AppFolder>>(emptyList()) }
    var openFolderId by remember { mutableStateOf<String?>(null) }

    // Physics & Drag State
    var draggingPackage      by remember { mutableStateOf<String?>(null) }
    var ghostInitialPosition by remember { mutableStateOf(Offset.Zero) }
    var draggingOffset       by remember { mutableStateOf(Offset.Zero) }
    val itemBounds           = remember { mutableMapOf<String, Rect>() }
    var isHoveringDelete     by remember { mutableStateOf(false) }

    // Hover targeting states (Visual cues before commit)
    var hoveringFolderId     by remember { mutableStateOf<String?>(null) }
    var hoveringAppId        by remember { mutableStateOf<String?>(null) }

    val isGridInDragMode     = draggingPackage != null

    val allApps by remember(viewModel.otherApps.value, searchQuery, hiddenApps) {
        derivedStateOf {
            viewModel.otherApps.value.filter {
                !hiddenApps.contains(it.packageName) &&
                        (searchQuery.isEmpty() || it.label.contains(searchQuery, true))
            }
        }
    }

    val folderAppPackages = remember(folders) {
        folders.flatMap { it.apps.map { a -> a.packageName } }.toSet()
    }
    val freeApps by remember(allApps, folderAppPackages) {
        derivedStateOf { allApps.filter { it.packageName !in folderAppPackages } }
    }

    val openFolder = remember(openFolderId, folders) {
        folders.find { it.id == openFolderId }
    }

    if (isVertical) {
        // Vertical List Mode
        val appsByLetter by remember(allApps) {
            derivedStateOf { AppGrouper.groupByAlphabet(allApps) }
        }
        Box(Modifier.fillMaxSize()) {
            LazyVerticalGrid(
                state = state,
                columns = GridCells.Fixed(columnCount),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 12.dp, top = 60.dp, end = 20.dp, bottom = 120.dp),
                verticalArrangement = Arrangement.spacedBy(verticalSpacing.dp)
            ) {
                appsByLetter.forEach { (letter, appList) ->
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        MicaSectionHeader(letter, isDark, micaParams)
                    }
                    items(appList) { app ->
                        AppIconItem(
                            app = app, iconSize = iconSize, fontSize = fontSize,
                            showLabel = showLabels, accentColor = accentColor,
                            isDragging = false, isGridInDragMode = false,
                            isHoveredForFolder = false,
                            micaParams = micaParams, onPinRequest = onPinRequest,
                            onClose = onClose ?: {}, onDragStart = {},
                            badgeCount = notifCounts[app.packageName] ?: 0,
                            viewModel = viewModel
                        )
                    }
                }
            }
            WindowsStyleScrollbar(
                state, isDark,
                Modifier
                    .align(Alignment.CenterEnd)
                    .padding(top = 60.dp, bottom = 120.dp, end = 4.dp)
                    .width(16.dp)
                    .fillMaxHeight()
            )
        }
    } else {
        // Horizontal Grid Mode (with Folders)
        val isTransparencyEnabled by viewModel.transparencyEnabled.collectAsState()
        val itemsPerPage = columnCount * rowCount
        val pageCount    = ((freeApps.size + folders.size) + itemsPerPage - 1) / itemsPerPage.coerceAtLeast(1)
        val pagerState   = rememberPagerState(pageCount = { pageCount.coerceAtLeast(1) })

        Box(modifier = Modifier.fillMaxSize()) {

            // Top Delete Zone
            AnimatedVisibility(
                visible = isGridInDragMode,
                enter   = slideInVertically { -it } + fadeIn(),
                exit    = slideOutVertically { -it } + fadeOut(),
                modifier = Modifier
                    .zIndex(100f)
                    .align(Alignment.TopCenter)
                    .padding(top = 48.dp)
            ) {
                WindowsDeleteZone(isHoveringDelete, isDark)
            }

            Column(modifier = Modifier.fillMaxSize().padding(top = 50.dp)) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.weight(1f),
                    userScrollEnabled = !isGridInDragMode
                ) { pageIndex ->
                    val mixedItems: List<GridItem> = buildList {
                        folders.forEach { add(GridItem.Folder(it)) }
                        freeApps.forEach { add(GridItem.App(it)) }
                    }
                    val startIndex = pageIndex * itemsPerPage
                    val endIndex   = (startIndex + itemsPerPage).coerceAtMost(mixedItems.size)
                    val pageItems  = if (startIndex < mixedItems.size)
                        mixedItems.subList(startIndex, endIndex) else emptyList()

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(columnCount),
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 24.dp),
                        verticalArrangement   = Arrangement.SpaceEvenly,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        userScrollEnabled     = false,
                        contentPadding        = PaddingValues(vertical = 10.dp)
                    ) {
                        items(items = pageItems, key = { item ->
                            when (item) {
                                is GridItem.App    -> item.app.packageName
                                is GridItem.Folder -> item.folder.id
                            }
                        }) { item ->
                            when (item) {
                                is GridItem.Folder -> {
                                    val folder = item.folder
                                    val isHovering = hoveringFolderId == folder.id

                                    Box(
                                        modifier = Modifier
                                            .animateItem()
                                            .onGloballyPositioned { coords ->
                                                if (coords.isAttached)
                                                    itemBounds["folder_${folder.id}"] = coords.boundsInWindow()
                                            }
                                            .scale(if (isHovering) 1.08f else 1f)
                                    ) {
                                        FolderIconItem(
                                            folder = folder,
                                            iconSize = iconSize,
                                            fontSize = fontSize,
                                            showLabel = showLabels,
                                            accentColor = accentColor,
                                            micaParams = micaParams,
                                            isTransparencyEnabled = isTransparencyEnabled,
                                            onClick = { openFolderId = folder.id },
                                            onLongPress = {
                                                // Dissolve folder
                                                folders = folders.filter { it.id != folder.id }
                                            }
                                        )
                                    }
                                }

                                is GridItem.App -> {
                                    val app = item.app
                                    val isItemDragging = draggingPackage == app.packageName
                                    val isTargeted = hoveringAppId == app.packageName

                                    Box(
                                        modifier = Modifier
                                            .animateItem(
                                                fadeInSpec  = null,
                                                fadeOutSpec = null,
                                                placementSpec = spring(
                                                    stiffness = Spring.StiffnessMediumLow,
                                                    visibilityThreshold = IntOffset.VisibilityThreshold
                                                )
                                            )
                                            .onGloballyPositioned { coords ->
                                                if (coords.isAttached)
                                                    itemBounds[app.packageName] = coords.boundsInWindow()
                                            }
                                            .zIndex(if (isItemDragging) 0f else 1f)
                                    ) {
                                        AppIconItem(
                                            app              = app,
                                            iconSize         = iconSize,
                                            fontSize         = fontSize,
                                            showLabel        = showLabels,
                                            accentColor      = accentColor,
                                            isDragging       = isItemDragging,
                                            isGridInDragMode = isGridInDragMode,
                                            isHoveredForFolder = isTargeted,
                                            micaParams       = micaParams,
                                            onPinRequest     = onPinRequest,
                                            onClose          = onClose ?: {},
                                            badgeCount       = notifCounts[app.packageName] ?: 0,
                                            viewModel        = viewModel,
                                            onDragStart      = { iconAbsolutePos ->
                                                if (searchQuery.isEmpty()) {
                                                    ghostInitialPosition = iconAbsolutePos
                                                    draggingOffset = Offset.Zero
                                                }
                                            },
                                            onDrag = { dragAmount ->
                                                draggingOffset += dragAmount
                                                if (draggingPackage == null) {
                                                    // Threshold before locking into drag mode
                                                    if (draggingOffset.getDistance() > 15f) {
                                                        draggingPackage = app.packageName
                                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                    }
                                                } else {
                                                    val ghostCenter = ghostInitialPosition + draggingOffset +
                                                            Offset((iconSize / 2).dp.value, (iconSize / 2).dp.value)

                                                    isHoveringDelete = ghostCenter.y < 350f

                                                    if (!isHoveringDelete) {
                                                        // 1. Detect Folder Hover (Strict Hitbox)
                                                        val hoveredFolder = folders.find { f ->
                                                            val bnds = itemBounds["folder_${f.id}"]
                                                            bnds != null && shrinkRect(bnds).contains(ghostCenter)
                                                        }

                                                        if (hoveredFolder != null) {
                                                            hoveringFolderId = hoveredFolder.id
                                                            hoveringAppId = null
                                                        } else {
                                                            hoveringFolderId = null

                                                            // 2. Detect App Hover (Strict Hitbox)
                                                            val targetPkg = itemBounds.entries
                                                                .filter { !it.key.startsWith("folder_") && it.key != draggingPackage }
                                                                .find { (_, bounds) -> shrinkRect(bounds).contains(ghostCenter) }?.key

                                                            if (targetPkg != null) {
                                                                if (hoveringAppId != targetPkg) {
                                                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                                    hoveringAppId = targetPkg
                                                                }
                                                            } else {
                                                                hoveringAppId = null
                                                            }
                                                        }
                                                    } else {
                                                        hoveringFolderId = null
                                                        hoveringAppId = null
                                                    }
                                                }
                                            },
                                            onDragEnd = {
                                                val hovFolder = hoveringFolderId?.let { fid -> folders.find { it.id == fid } }
                                                val targetAppPkg = hoveringAppId
                                                val draggedApp = freeApps.find { it.packageName == draggingPackage }

                                                when {
                                                    // Drop on Delete
                                                    isHoveringDelete && draggingPackage != null -> {
                                                        AppActions.uninstallApp(context, draggingPackage!!)
                                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    }
                                                    // Commit Drop on Folder
                                                    hovFolder != null && draggedApp != null -> {
                                                        folders = folders.map { f ->
                                                            if (f.id == hovFolder.id && f.apps.none { it.packageName == draggedApp.packageName })
                                                                f.copy(apps = f.apps + draggedApp)
                                                            else f
                                                        }
                                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    }
                                                    // Commit Drop on App (Create Folder)
                                                    targetAppPkg != null && draggedApp != null -> {
                                                        val targetApp = freeApps.find { it.packageName == targetAppPkg }
                                                        if (targetApp != null) {
                                                            val newFolder = AppFolder(
                                                                id   = UUID.randomUUID().toString(),
                                                                name = "Dossier",
                                                                apps = listOf(targetApp, draggedApp)
                                                            )
                                                            folders = folders + newFolder
                                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                        }
                                                    }
                                                    // Normal Swap / Reorder
                                                    draggingPackage != null -> {
                                                        val fromIdx = freeApps.indexOfFirst { it.packageName == draggingPackage }
                                                        val ghostCenter = ghostInitialPosition + draggingOffset + Offset((iconSize / 2).dp.value, (iconSize / 2).dp.value)
                                                        // Relaxed bounds for standard swapping
                                                        val swapTargetPkg = itemBounds.entries
                                                            .filter { !it.key.startsWith("folder_") && it.key != draggingPackage }
                                                            .find { (_, bounds) -> bounds.contains(ghostCenter) }?.key

                                                        val toIdx = freeApps.indexOfFirst { it.packageName == swapTargetPkg }

                                                        if (fromIdx != -1 && toIdx != -1) {
                                                            viewModel.moveApp(fromIdx, toIdx)
                                                        }
                                                    }
                                                }

                                                // Cleanup State
                                                draggingPackage  = null
                                                draggingOffset   = Offset.Zero
                                                isHoveringDelete = false
                                                hoveringFolderId = null
                                                hoveringAppId    = null
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                if (pageCount > 1) {
                    PagerIndicator(pageCount, pagerState.currentPage, accentColor, scope, pagerState)
                }
                Spacer(modifier = Modifier.height(100.dp))
            }

            if (draggingPackage != null) {
                val draggedApp = freeApps.find { it.packageName == draggingPackage }
                if (draggedApp != null) {
                    val currentPos = ghostInitialPosition + draggingOffset
                    GhostIcon(draggedApp, currentPos, iconSize, isHoveringDelete)
                }
            }

            AnimatedVisibility(
                visible = openFolder != null,
                enter   = fadeIn() + scaleIn(initialScale = 0.9f),
                exit    = fadeOut() + scaleOut(targetScale = 0.9f),
                modifier = Modifier.zIndex(200f)
            ) {
                openFolder?.let { folder ->
                    FolderOverlay(
                        folder = folder,
                        iconSize = iconSize,
                        fontSize = fontSize,
                        accentColor = accentColor,
                        micaParams = micaParams,
                        isTransparencyEnabled = isTransparencyEnabled,
                        onDismiss = { openFolderId = null },
                        onRename = { newName ->
                            folders = folders.map {
                                if (it.id == folder.id) it.copy(name = newName) else it
                            }
                        },
                        onAppLaunch = { app ->
                            val intent = context.packageManager
                                .getLaunchIntentForPackage(app.packageName)
                            if (intent != null) {
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                context.startActivity(intent)
                            }
                        },
                        onRemoveApp = { app ->
                            folders = folders.map { f ->
                                if (f.id == folder.id)
                                    f.copy(apps = f.apps.filter {
                                        it.packageName != app.packageName
                                    })
                                else f
                            }.filter { it.apps.isNotEmpty() }
                            openFolderId = null
                        }
                    )
                }
            }
        }
    }
}

// =============================================================================
// APP ICON ITEM
// =============================================================================

@Composable
private fun AppIconItem(
    app:             AppModel,
    iconSize:        Float,
    fontSize:        Float,
    showLabel:       Boolean,
    accentColor:     Color,
    isDragging:      Boolean,
    isGridInDragMode: Boolean,
    isHoveredForFolder: Boolean,
    micaParams:      MicaParams,
    onPinRequest:    (AppModel) -> Unit,
    onClose:         () -> Unit,
    badgeCount:      Int = 0,
    viewModel:       AppViewModel,
    onDragStart:     (Offset) -> Unit = {},
    onDrag:          (Offset) -> Unit = {},
    onDragEnd:       () -> Unit = {}
) {
    val context  = LocalContext.current
    val haptic   = LocalHapticFeedback.current
    val scope    = rememberCoroutineScope()
    val density  = LocalDensity.current
    val smartTextColor = micaParams.primaryColor

    var showMenu      by remember { mutableStateOf(false) }
    var itemWindowPos by remember { mutableStateOf(Offset.Zero) }
    var isPressed     by remember { mutableStateOf(false) }
    var hasMoved      by remember { mutableStateOf(false) }

    var isPinning by remember { mutableStateOf(false) }
    val pinScale by animateFloatAsState(
        targetValue = if (isPinning) 1.25f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness    = Spring.StiffnessMedium
        ),
        finishedListener = { _ ->
            if (isPinning) { isPinning = false; onPinRequest(app); onClose() }
        },
        label = "pin_scale"
    )

    LaunchedEffect(isDragging) { if (isDragging) showMenu = false }

    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.93f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessHigh),
        label = "press_scale"
    )

    val targetScale by animateFloatAsState(
        targetValue = if (isHoveredForFolder) 1.15f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "hover_scale"
    )

    val dimAlpha by animateFloatAsState(
        targetValue = when {
            isGridInDragMode && isDragging -> 0f
            isGridInDragMode && !isHoveredForFolder -> 0.5f
            isGridInDragMode && isHoveredForFolder -> 1f
            else -> 1f
        },
        label = "dim"
    )
    val isTransparencyEnabled by viewModel.transparencyEnabled.collectAsState()

    Box(
        modifier = Modifier
            .onGloballyPositioned { coords ->
                if (coords.isAttached) itemWindowPos = coords.positionInWindow()
            }
            .graphicsLayer {
                val finalScale = pinScale * pressScale * targetScale
                scaleX = finalScale
                scaleY = finalScale
                alpha  = dimAlpha
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        try { tryAwaitRelease() } finally { isPressed = false }
                    },
                    onTap = {
                        if (!showMenu && !isPinning) {
                            scope.launch {
                                delay(50)
                                val sizePx = with(density) { iconSize.dp.roundToPx() }
                                AppLauncher.launch(
                                    context, null, app.packageName, itemWindowPos, sizePx
                                )
                            }
                        }
                    },
                    onLongPress = {
                        if (!isPinning && !showMenu) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            showMenu = true
                        }
                    }
                )
            }
            .pointerInput(Unit) {
                detectDragGesturesAfterLongPress(
                    onDragStart = {
                        isPressed = true; hasMoved = false; showMenu = false
                        onDragStart(itemWindowPos)
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        if (dragAmount.getDistance() > 2f) hasMoved = true
                        onDrag(dragAmount)
                    },
                    onDragEnd    = { isPressed = false; onDragEnd() },
                    onDragCancel = { isPressed = false; onDragEnd() }
                )
            },
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            FluentBadgedBox(badgeCount = badgeCount, badgeColor = accentColor) {
                InternalAppIcon(
                    packageName = app.packageName,
                    modifier    = Modifier.size(iconSize.dp)
                )
            }
            if (showLabel) {
                Text(
                    text      = app.label,
                    fontSize  = fontSize.sp,
                    maxLines  = 1,
                    overflow  = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    color     = smartTextColor,
                    modifier  = Modifier
                        .padding(top = 4.dp)
                        .width((iconSize + 24).dp)
                )
            }
        }

        if (showMenu && !isDragging) {
            FluentContextMenu(
                app                   = app,
                iconPosition          = itemWindowPos,
                iconHeight            = with(density) { (iconSize + 30).dp.toPx() },
                iconSize              = iconSize,
                onDismiss             = { showMenu = false },
                isDark                = isSystemInDarkTheme(),
                onPin                 = { showMenu = false; isPinning = true },
                micaParams            = micaParams,
                isTransparencyEnabled = isTransparencyEnabled
            )
        }
    }
}

// =============================================================================
// SCROLLBAR
// =============================================================================

@Composable
private fun WindowsStyleScrollbar(state: LazyGridState, isDark: Boolean, modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier = modifier) {
        val maxHeight = constraints.maxHeight.toFloat()
        val scope     = rememberCoroutineScope()
        var isDragging by remember { mutableStateOf(false) }
        var isActive   by remember { mutableStateOf(false) }

        LaunchedEffect(state.isScrollInProgress, isDragging) {
            if (state.isScrollInProgress || isDragging) isActive = true
            else { delay(1500); isActive = false }
        }

        val barAlpha by animateFloatAsState(if (isActive) 1f else 0f, tween(300), label = "alpha")
        val barWidth by animateDpAsState(
            if (isDragging) 6.dp else 2.dp,
            spring(stiffness = Spring.StiffnessMediumLow),
            label = "width"
        )

        val scrollProgress by remember {
            derivedStateOf {
                val total   = state.layoutInfo.totalItemsCount
                val visible = state.layoutInfo.visibleItemsInfo
                if (total == 0 || visible.isEmpty()) 0f
                else {
                    val first = visible.first()
                    val pIdx  = first.index.toFloat() +
                            (first.offset.y.toFloat() / first.size.height.toFloat()) * -1f
                    (pIdx / total.toFloat()).coerceIn(0f, 1f)
                }
            }
        }

        val animProgress by animateFloatAsState(
            scrollProgress,
            if (isDragging) snap() else tween(50),
            label = "p"
        )
        val thumbH = 48.dp
        val thumbY = animProgress * (maxHeight - with(LocalDensity.current) { thumbH.toPx() })
        val color    = if (isDark) Color.White.copy(0.4f) else Color.Black.copy(0.4f)
        val actColor = if (isDark) Color.White.copy(0.8f) else Color.Black.copy(0.7f)

        if (barAlpha > 0.01f) {
            Box(
                Modifier
                    .fillMaxSize()
                    .alpha(barAlpha)
                    .pointerInput(maxHeight) {
                        detectVerticalDragGestures(
                            onDragStart = {
                                isDragging = true; isActive = true
                                scope.launch {
                                    state.scrollToItem(
                                        ((it.y / maxHeight).coerceIn(0f, 1f) *
                                                state.layoutInfo.totalItemsCount).toInt()
                                    )
                                }
                            },
                            onDragEnd    = { isDragging = false },
                            onDragCancel = { isDragging = false },
                            onVerticalDrag = { c, _ ->
                                c.consume(); isActive = true
                                scope.launch {
                                    state.scrollToItem(
                                        ((c.position.y / maxHeight).coerceIn(0f, 1f) *
                                                state.layoutInfo.totalItemsCount).toInt()
                                    )
                                }
                            }
                        )
                    }
            ) {
                Box(
                    Modifier
                        .offset { IntOffset(0, thumbY.toInt()) }
                        .align(Alignment.TopCenter)
                        .width(barWidth)
                        .height(thumbH)
                        .background(if (isDragging) actColor else color, RoundedCornerShape(50))
                )
            }
        }
    }
}

// =============================================================================
// GHOST ICON
// =============================================================================

@Composable
private fun GhostIcon(
    app: AppModel, currentPos: Offset, iconSize: Float, isHoveringDelete: Boolean
) {
    Box(
        modifier = Modifier
            .offset { IntOffset(currentPos.x.roundToInt(), currentPos.y.roundToInt()) }
            .zIndex(1001f)
            .scale(if (isHoveringDelete) 0.8f else 1.05f)
    ) {
        InternalAppIcon(
            app.packageName,
            Modifier.size(iconSize.dp).shadow(16.dp, AppIconShape)
        )
    }
}

// =============================================================================
// DELETE ZONE
// =============================================================================

@Composable
private fun WindowsDeleteZone(isHovering: Boolean, isDark: Boolean) {
    val bgColor      = if (isHovering) Color(0xFFC42B1C)
    else if (isDark) Color(0xFF2C2C2C) else Color(0xFFF3F3F3)
    val contentColor = if (isHovering) Color.White
    else if (isDark) Color.White else Color.Black
    val scale by animateFloatAsState(if (isHovering) 1.1f else 1f, label = "del_scale")
    Row(
        Modifier
            .scale(scale)
            .height(48.dp)
            .width(160.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .border(1.dp, Color.White.copy(0.1f), RoundedCornerShape(8.dp)),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector        = FluentIcons.Regular.Delete,
            contentDescription = stringResource(R.string.content_desc_delete_icon),
            tint               = contentColor,
            modifier           = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text       = stringResource(R.string.delete_zone_label),
            color      = contentColor,
            fontWeight = FontWeight.Medium,
            fontSize   = 14.sp
        )
    }
}

// =============================================================================
// SECTION HEADER
// =============================================================================

@Composable
private fun MicaSectionHeader(letter: Char, isDark: Boolean, micaParams: MicaParams) {
    val smartTextColor = micaParams.primaryColor
    Row(
        Modifier.fillMaxWidth().padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(contentAlignment = Alignment.Center) {
            Box(
                Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        if (isDark) Color(0xFF202020).copy(0.3f)
                        else Color.White.copy(0.4f)
                    )
            )
            Text(
                letter.toString(),
                fontWeight = FontWeight.Bold,
                fontSize   = 18.sp,
                color      = smartTextColor
            )
        }
    }
}

// =============================================================================
// PAGER INDICATOR
// =============================================================================

@Composable
private fun PagerIndicator(
    pageCount:   Int,
    currentPage: Int,
    accentColor: Color,
    scope:       kotlinx.coroutines.CoroutineScope,
    pagerState:  androidx.compose.foundation.pager.PagerState
) {
    Row(
        Modifier.fillMaxWidth().height(40.dp).padding(bottom = 10.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        repeat(pageCount) { i ->
            val color by animateColorAsState(
                if (currentPage == i) accentColor else Color.Gray.copy(0.5f),
                label = "dot_color"
            )
            val size by animateDpAsState(
                if (currentPage == i) 8.dp else 6.dp,
                label = "dot_size"
            )
            Box(
                Modifier
                    .padding(3.dp)
                    .size(20.dp)
                    .clickable { scope.launch { pagerState.animateScrollToPage(i) } },
                contentAlignment = Alignment.Center
            ) {
                Box(Modifier.size(size).clip(CircleShape).background(color))
            }
        }
    }
}

// =============================================================================
// UTILS
// =============================================================================

object AppLauncher {
    fun launch(ctx: Context, view: android.view.View?, pkg: String, pos: Offset, sizePx: Int) {
        val intent = ctx.packageManager.getLaunchIntentForPackage(pkg) ?: return
        try {
            val opts = if (view != null) android.app.ActivityOptions.makeScaleUpAnimation(
                view, pos.x.toInt(), pos.y.toInt(), sizePx, sizePx
            ).toBundle() else null
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            ctx.startActivity(intent, opts)
        } catch (e: Exception) {
            ctx.startActivity(intent)
        }
    }
}

object AppGrouper {
    fun groupByAlphabet(apps: List<AppModel>): Map<Char, List<AppModel>> {
        val collator = Collator.getInstance().apply { strength = Collator.PRIMARY }
        return apps.distinctBy { it.packageName }
            .sortedWith { a, b -> collator.compare(a.label, b.label) }
            .groupBy {
                it.label.firstOrNull()?.uppercaseChar()?.takeIf { c -> c.isLetter() } ?: '#'
            }
    }
}