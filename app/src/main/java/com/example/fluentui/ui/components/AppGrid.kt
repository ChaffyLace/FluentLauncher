package com.example.fluentui.ui.components

import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fluentui.ui.utils.IconCache
import com.example.fluentui.wallpaper.MicaParams
import fluent.ui.system.icons.FluentIcons
import fluent.ui.system.icons.regular.Delete
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.Collator
import kotlin.math.roundToInt

// Standard Windows 11 Icon Shape
private val AppIconShape = RoundedCornerShape(12.dp)

@Composable
fun InternalAppIcon(
    packageName: String,
    size: Float,
    modifier: Modifier = Modifier,
    badgeCount: Int = 0,
    badgeColor: Color = Color(0xFF60CDFF)
) {
    Box(modifier = modifier) {
        // 1. Image Icône
        val bitmap = remember(packageName) { IconCache.get(packageName) }
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.matchParentSize(),
                contentScale = ContentScale.Fit
            )
        } else {
            Box(modifier = Modifier.matchParentSize().background(Color.Gray.copy(0.2f), AppIconShape))
        }

        if (badgeCount > 0) {
            val badgeText = if (badgeCount > 99) "99+" else badgeCount.toString()
            val textColor = if (badgeColor.luminance() > 0.5f) Color.Black else Color.White

            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 4.dp, y = (-2).dp)
                    .defaultMinSize(minWidth = 22.dp, minHeight = 22.dp)
                    .clip(CircleShape)
                    .background(badgeColor)
                    .zIndex(10f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = badgeText,
                    color = textColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                )
            }
        }
    }
}

// --- GRID PRINCIPALE ---
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
    val isDark = isSystemInDarkTheme()
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    // Données Notifications
    val notifCounts by viewModel.notificationCounts.collectAsState()

    var draggingPackage by remember { mutableStateOf<String?>(null) }
    var ghostInitialPosition by remember { mutableStateOf(Offset.Zero) }
    var draggingOffset by remember { mutableStateOf(Offset.Zero) }

    val itemBounds = remember { mutableMapOf<String, Rect>() }
    var isHoveringDelete by remember { mutableStateOf(false) }
    var lastSwapTime by remember { mutableStateOf(0L) }

    val isGridInDragMode = draggingPackage != null

    val allApps by remember(viewModel.otherApps.value, searchQuery, hiddenApps) {
        derivedStateOf {
            viewModel.otherApps.value.filter {
                !hiddenApps.contains(it.packageName) &&
                        (searchQuery.isEmpty() || it.label.contains(searchQuery, true))
            }
        }
    }

    if (isVertical) {
        val appsByLetter by remember(allApps) { derivedStateOf { AppGrouper.groupByAlphabet(allApps) } }
        Box(Modifier.fillMaxSize()) {
            LazyVerticalGrid(
                state = state,
                columns = GridCells.Fixed(columnCount),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 12.dp, top = 60.dp, end = 20.dp, bottom = 120.dp),
                verticalArrangement = Arrangement.spacedBy(verticalSpacing.dp)
            ) {
                appsByLetter.forEach { (letter, appList) ->
                    item(span = { GridItemSpan(maxLineSpan) }) { MicaSectionHeader(letter, isDark, micaParams) }
                    items(appList) { app ->
                        AppIconItem(
                            app = app, iconSize = iconSize, fontSize = fontSize, showLabel = showLabels,
                            accentColor = accentColor, isDragging = false, isGridInDragMode = false,
                            micaParams = micaParams, onPinRequest = onPinRequest, onClose = onClose ?: {},
                            onDragStart = {}, badgeCount = notifCounts[app.packageName] ?: 0,
                            viewModel = viewModel
                        )
                    }
                }
            }
            // Scrollbar Windows
            WindowsStyleScrollbar(
                state,
                isDark,
                Modifier
                    .align(Alignment.CenterEnd)
                    .padding(top = 60.dp, bottom = 120.dp, end = 4.dp)
                    .width(16.dp)
                    .fillMaxHeight()
            )
        }
    } else {
        val itemsPerPage = columnCount * rowCount
        val pageCount = (allApps.size + itemsPerPage - 1) / itemsPerPage.coerceAtLeast(1)
        val pagerState = rememberPagerState(pageCount = { pageCount.coerceAtLeast(1) })

        Box(modifier = Modifier.fillMaxSize()) {

            // Delete Zone
            AnimatedVisibility(
                visible = isGridInDragMode,
                enter = slideInVertically { -it } + fadeIn(),
                exit = slideOutVertically { -it } + fadeOut(),
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

                    val startIndex = pageIndex * itemsPerPage
                    val endIndex = (startIndex + itemsPerPage).coerceAtMost(allApps.size)
                    val pageApps = if (startIndex < allApps.size) allApps.subList(startIndex, endIndex) else emptyList()

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(columnCount),
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 24.dp),
                        verticalArrangement = Arrangement.SpaceEvenly,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        userScrollEnabled = false,
                        contentPadding = PaddingValues(vertical = 10.dp)
                    ) {
                        items(items = pageApps, key = { it.packageName }) { app ->

                            val isItemDragging = (draggingPackage == app.packageName)

                            Box(
                                modifier = Modifier
                                    // CORRECTION : animateItem remplace animateItemPlacement
                                    .animateItem(
                                        fadeInSpec = null,
                                        fadeOutSpec = null,
                                        placementSpec = spring(
                                            stiffness = Spring.StiffnessMediumLow,
                                            visibilityThreshold = IntOffset.VisibilityThreshold
                                        )
                                    )
                                    .onGloballyPositioned { coords ->
                                        if (coords.isAttached) itemBounds[app.packageName] = coords.boundsInWindow()
                                    }
                                    .zIndex(if (isItemDragging) 0f else 1f)
                            ) {
                                AppIconItem(
                                    app = app,
                                    iconSize = iconSize,
                                    fontSize = fontSize,
                                    showLabel = showLabels,
                                    accentColor = accentColor,
                                    isDragging = isItemDragging,
                                    isGridInDragMode = isGridInDragMode,
                                    micaParams = micaParams,
                                    onPinRequest = onPinRequest,
                                    onClose = onClose ?: {},
                                    badgeCount = notifCounts[app.packageName] ?: 0,
                                    viewModel = viewModel,

                                    onDragStart = { iconAbsolutePos ->
                                        if (searchQuery.isEmpty()) {
                                            ghostInitialPosition = iconAbsolutePos
                                            draggingOffset = Offset.Zero
                                        }
                                    },

                                    onDrag = { dragAmount ->
                                        draggingOffset += dragAmount

                                        if (draggingPackage == null) {
                                            if (draggingOffset.getDistance() > 30f) {
                                                draggingPackage = app.packageName
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            }
                                        } else {
                                            val ghostCenter = ghostInitialPosition + draggingOffset + Offset(
                                                (iconSize / 2).dp.value,
                                                (iconSize / 2).dp.value
                                            )
                                            isHoveringDelete = ghostCenter.y < 350f

                                            if (!isHoveringDelete) {
                                                val targetPackage = itemBounds.entries.find { (_, bounds) -> bounds.contains(ghostCenter) }?.key

                                                if (targetPackage != null && targetPackage != draggingPackage) {
                                                    val fromIndex = allApps.indexOfFirst { it.packageName == draggingPackage }
                                                    val toIndex = allApps.indexOfFirst { it.packageName == targetPackage }
                                                    val now = System.currentTimeMillis()

                                                    if (fromIndex != -1 && toIndex != -1 && (now - lastSwapTime > 200)) {
                                                        viewModel.moveApp(fromIndex, toIndex)
                                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                        lastSwapTime = now
                                                    }
                                                }
                                            }
                                        }
                                    },

                                    onDragEnd = {
                                        if (isHoveringDelete && draggingPackage != null) {
                                            AppActions.uninstallApp(context, draggingPackage!!)
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        }
                                        draggingPackage = null
                                        draggingOffset = Offset.Zero
                                        isHoveringDelete = false
                                    }
                                )
                            }
                        }
                    }
                }

                // DOTS
                if (pageCount > 1) {
                    PagerIndicator(pageCount, pagerState.currentPage, accentColor, scope, pagerState)
                }
                Spacer(modifier = Modifier.height(100.dp))
            }

            // FANTÔME
            if (draggingPackage != null) {
                val draggedApp = allApps.find { it.packageName == draggingPackage }
                if (draggedApp != null) {
                    val currentPos = ghostInitialPosition + draggingOffset
                    GhostIcon(draggedApp, currentPos, iconSize, isHoveringDelete)
                }
            }
        }
    }
}

@Composable
private fun AppIconItem(
    app: AppModel,
    iconSize: Float,
    fontSize: Float,
    showLabel: Boolean,
    accentColor: Color,
    isDragging: Boolean,
    isGridInDragMode: Boolean,
    micaParams: MicaParams,
    onPinRequest: (AppModel) -> Unit,
    onClose: () -> Unit,
    badgeCount: Int = 0,
    viewModel: AppViewModel,
    onDragStart: (Offset) -> Unit = {},
    onDrag: (Offset) -> Unit = {},
    onDragEnd: () -> Unit = {}
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val smartTextColor = micaParams.primaryColor
    val density = LocalDensity.current

    var showMenu by remember { mutableStateOf(false) }
    var itemWindowPos by remember { mutableStateOf(Offset.Zero) }
    var isPressed by remember { mutableStateOf(false) }
    var hasMoved by remember { mutableStateOf(false) }

    LaunchedEffect(isDragging) { if (isDragging) showMenu = false }

    val scale by animateFloatAsState(if (isPressed) 0.95f else 1f, label = "scale")
    val targetAlpha = if (isGridInDragMode) {
        if (isDragging) 0f else 0.5f
    } else 1f
    val alpha by animateFloatAsState(targetAlpha, label = "dimming")

    // RÉCUPÉRATION DE L'ÉTAT DE TRANSPARENCE
    val isTransparencyEnabled by viewModel.transparencyEnabled.collectAsState()

    Box(
        modifier = Modifier
            .onGloballyPositioned { coordinates ->
                if (coordinates.isAttached) itemWindowPos = coordinates.positionInWindow()
            }
            .scale(scale)
            .graphicsLayer { this.alpha = alpha }
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = { isPressed = true; tryAwaitRelease(); isPressed = false },
                    onTap = {
                        scope.launch {
                            delay(50)
                            val sizePx = with(density) { iconSize.dp.roundToPx() }
                            AppLauncher.launch(context, null, app.packageName, itemWindowPos, sizePx)
                        }
                    }
                )
            }
            .pointerInput(Unit) {
                detectDragGesturesAfterLongPress(
                    onDragStart = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        isPressed = true
                        hasMoved = false
                        onDragStart(itemWindowPos)
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        if (dragAmount.getDistance() > 2f) hasMoved = true
                        onDrag(dragAmount)
                    },
                    onDragEnd = {
                        isPressed = false
                        if (!hasMoved) showMenu = true
                        onDragEnd()
                    },
                    onDragCancel = {
                        isPressed = false
                        onDragEnd()
                    }
                )
            },
        contentAlignment = Alignment.TopCenter
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {

            InternalAppIcon(
                packageName = app.packageName,
                size = iconSize,
                modifier = Modifier.size(iconSize.dp),
                badgeCount = badgeCount,
                badgeColor = accentColor
            )

            if (showLabel) {
                Text(
                    text = app.label,
                    fontSize = fontSize.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    color = smartTextColor,
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .width((iconSize + 24).dp)
                )
            }
        }

        if (showMenu && !isDragging) {
            FluentContextMenu(
                app = app,
                iconPosition = itemWindowPos,
                iconHeight = with(density) { (iconSize + 30).dp.toPx() },
                iconSize = iconSize,
                onDismiss = { showMenu = false },
                isDark = isSystemInDarkTheme(),
                onPin = { onPinRequest(app); showMenu = false; onClose() },
                micaParams = micaParams,
                isTransparencyEnabled = isTransparencyEnabled

            )
        }
    }
}

@Composable
private fun WindowsStyleScrollbar(state: LazyGridState, isDark: Boolean, modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier = modifier) {
        val maxHeight = constraints.maxHeight.toFloat()
        val scope = rememberCoroutineScope()
        var isDragging by remember { mutableStateOf(false) }
        var isActive by remember { mutableStateOf(false) }

        LaunchedEffect(state.isScrollInProgress, isDragging) {
            if (state.isScrollInProgress || isDragging) isActive = true else {
                delay(1500); isActive = false
            }
        }

        val barAlpha by animateFloatAsState(if (isActive) 1f else 0f, tween(300), label = "alpha")
        val barWidth by animateDpAsState(
            if (isDragging) 6.dp else 2.dp,
            spring(stiffness = Spring.StiffnessMediumLow),
            label = "width"
        )

        val scrollProgress by remember {
            derivedStateOf {
                val total = state.layoutInfo.totalItemsCount
                val visible = state.layoutInfo.visibleItemsInfo
                if (total == 0 || visible.isEmpty()) 0f
                else {
                    val first = visible.first()
                    val pIdx = first.index.toFloat() + (first.offset.y.toFloat() / first.size.height.toFloat()) * -1f
                    (pIdx / total.toFloat()).coerceIn(0f, 1f)
                }
            }
        }

        val animProgress by animateFloatAsState(
            if (isDragging) scrollProgress else scrollProgress,
            if (isDragging) snap() else tween(50),
            label = "p"
        )
        val thumbH = 48.dp
        val thumbY = animProgress * (maxHeight - with(LocalDensity.current) { thumbH.toPx() })
        val color = if (isDark) Color.White.copy(0.4f) else Color.Black.copy(0.4f)
        val actColor = if (isDark) Color.White.copy(0.8f) else Color.Black.copy(0.7f)

        if (barAlpha > 0.01f) {
            Box(Modifier
                .fillMaxSize()
                .alpha(barAlpha)
                .pointerInput(maxHeight) {
                    detectVerticalDragGestures(
                        onDragStart = {
                            isDragging = true; isActive = true; scope.launch {
                            state.scrollToItem(((it.y / maxHeight).coerceIn(0f, 1f) * state.layoutInfo.totalItemsCount).toInt())
                        }
                        },
                        onDragEnd = { isDragging = false }, onDragCancel = { isDragging = false },
                        onVerticalDrag = { c, _ ->
                            c.consume(); isActive = true; scope.launch {
                            state.scrollToItem(((c.position.y / maxHeight).coerceIn(0f, 1f) * state.layoutInfo.totalItemsCount).toInt())
                        }
                        }
                    )
                }) {
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

@Composable
private fun GhostIcon(app: AppModel, currentPos: Offset, iconSize: Float, isHoveringDelete: Boolean) {
    Box(
        modifier = Modifier
            .offset { IntOffset(currentPos.x.roundToInt(), currentPos.y.roundToInt()) }
            .zIndex(1001f)
            .scale(if (isHoveringDelete) 0.8f else 1.05f)
    ) {
        InternalAppIcon(
            app.packageName,
            iconSize,
            Modifier
                .size(iconSize.dp)
                .shadow(16.dp, AppIconShape)
        )
    }
}

@Composable
private fun WindowsDeleteZone(isHovering: Boolean, isDark: Boolean) {
    val bgColor = if (isHovering) Color(0xFFC42B1C) else (if (isDark) Color(0xFF2C2C2C) else Color(0xFFF3F3F3))
    val contentColor = if (isHovering) Color.White else (if (isDark) Color.White else Color.Black)
    val scale by animateFloatAsState(if (isHovering) 1.1f else 1f)
    Row(
        Modifier
            .scale(scale)
            .height(48.dp)
            .width(160.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .border(1.dp, Color.White.copy(0.1f), RoundedCornerShape(8.dp)),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(FluentIcons.Regular.Delete, null, tint = contentColor, modifier = Modifier.size(20.dp)); Spacer(
        Modifier.width(10.dp)
    ); Text(
        "Désinstaller",
        color = contentColor,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp
    )
    }
}

@Composable
private fun MicaSectionHeader(letter: Char, isDark: Boolean, micaParams: MicaParams) {
    val smartTextColor = micaParams.primaryColor
    Row(
        Modifier
            .fillMaxWidth()
            .padding(12.dp), verticalAlignment = Alignment.CenterVertically
    ) {
        Box(contentAlignment = Alignment.Center) {
            Box(
                Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(if (isDark) Color(0xFF202020).copy(0.3f) else Color.White.copy(0.4f))
            )
            Text(
                letter.toString(),
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = smartTextColor
            )
        }
    }
}

@Composable
private fun PagerIndicator(
    pageCount: Int,
    currentPage: Int,
    accentColor: Color,
    scope: kotlinx.coroutines.CoroutineScope,
    pagerState: androidx.compose.foundation.pager.PagerState
) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(40.dp)
            .padding(bottom = 10.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        repeat(pageCount) { i ->
            val color = if (currentPage == i) accentColor else Color.Gray.copy(0.5f)
            val size by animateDpAsState(if (currentPage == i) 8.dp else 6.dp, label = "d")
            Box(
                Modifier
                    .padding(3.dp)
                    .size(20.dp)
                    .clickable { scope.launch { pagerState.animateScrollToPage(i) } },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    Modifier
                        .size(size)
                        .clip(CircleShape)
                        .background(color)
                )
            }
        }
    }
}

object AppLauncher {
    fun launch(ctx: Context, view: android.view.View?, pkg: String, pos: Offset, sizePx: Int) {
        val intent = ctx.packageManager.getLaunchIntentForPackage(pkg) ?: return
        try {
            val opts = if (view != null) ActivityOptions.makeScaleUpAnimation(
                view,
                pos.x.toInt(),
                pos.y.toInt(),
                sizePx,
                sizePx
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