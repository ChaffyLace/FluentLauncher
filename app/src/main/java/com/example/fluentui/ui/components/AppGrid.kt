package com.example.fluentui.ui.components

import android.annotation.SuppressLint
import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.provider.Settings
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import fluent.ui.system.icons.FluentIcons
import fluent.ui.system.icons.regular.AppsAddIn
import fluent.ui.system.icons.regular.Delete
import fluent.ui.system.icons.regular.Pin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.Collator
import kotlin.math.roundToInt

@Composable
fun AppGrid(
    viewModel: AppViewModel = viewModel(),
    state: LazyGridState = rememberLazyGridState(),
    columnCount: Int = 4,
    rowCount: Int = 6,
    iconSize: Float = 60f,
    showLabels: Boolean = true,
    searchQuery: String = "",
    accentColor: Color = Color(0xFF0078D4),
    isVertical: Boolean = false,
    onPinRequest: (AppModel) -> Unit = { viewModel.pinApp(it) },
    onClose: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val isDark = isSystemInDarkTheme()
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    var draggingGlobalIndex by remember { mutableStateOf<Int?>(null) }
    var draggingOffset by remember { mutableStateOf(Offset.Zero) }
    var draggingItemInitialPos by remember { mutableStateOf(Offset.Zero) }
    var isHoveringDelete by remember { mutableStateOf(false) }

    val allApps by remember(viewModel.otherApps.value, searchQuery) {
        derivedStateOf {
            if (searchQuery.isEmpty()) viewModel.otherApps.value
            else viewModel.otherApps.value.filter { it.label.contains(searchQuery, ignoreCase = true) }
        }
    }

    if (isVertical) {
        val appsByLetter by remember(allApps) {
            derivedStateOf { AppGrouper.groupByAlphabet(allApps) }
        }

        LazyVerticalGrid(
            state = state,
            columns = GridCells.Fixed(columnCount),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 12.dp, top = 60.dp, end = 12.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            appsByLetter.forEach { (letter, appList) ->
                item(span = { GridItemSpan(maxLineSpan) }) { MicaSectionHeader(letter, isDark) }
                items(appList) { app ->
                    AppIconItem(
                        app = app,
                        size = iconSize,
                        showLabel = showLabels,
                        accentColor = accentColor,
                        isDragging = false,
                        onPinRequest = onPinRequest,
                        onClose = onClose ?: {}
                    )
                }
            }
        }
    } else {
        val itemsPerPage = columnCount * rowCount
        val pages = remember(allApps, itemsPerPage) { allApps.chunked(itemsPerPage) }
        val pagerState = rememberPagerState(pageCount = { pages.size.coerceAtLeast(1) })

        Box(modifier = Modifier.fillMaxSize()) {

            AnimatedVisibility(
                visible = draggingGlobalIndex != null,
                enter = slideInVertically { -it } + fadeIn(),
                exit = slideOutVertically { -it } + fadeOut(),
                modifier = Modifier
                    .zIndex(1000f)
                    .align(Alignment.TopCenter)
                    .padding(top = 48.dp)
            ) {
                FluentDeletePill(isHovering = isHoveringDelete, isDark = isDark)
            }

            Column(modifier = Modifier.fillMaxSize().padding(top = 50.dp)) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.weight(1f),
                    userScrollEnabled = draggingGlobalIndex == null,
                    beyondViewportPageCount = 1
                ) { pageIndex ->
                    val pageApps = pages.getOrNull(pageIndex) ?: emptyList()
                    val startIndex = pageIndex * itemsPerPage

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(columnCount),
                        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
                        verticalArrangement = Arrangement.SpaceEvenly,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        userScrollEnabled = false,
                        contentPadding = PaddingValues(vertical = 10.dp)
                    ) {
                        items(pageApps.size) { localIndex ->
                            val globalIndex = startIndex + localIndex
                            val app = pageApps[localIndex]
                            val isBeingDragged = (draggingGlobalIndex == globalIndex)

                            Box(
                                modifier = Modifier.onGloballyPositioned { coordinates ->
                                    if (isBeingDragged && draggingItemInitialPos == Offset.Zero) {
                                        draggingItemInitialPos = coordinates.positionInWindow()
                                    }
                                }
                            ) {
                                AppIconItem(
                                    app = app,
                                    size = iconSize,
                                    showLabel = showLabels,
                                    accentColor = accentColor,
                                    isDragging = isBeingDragged,
                                    onPinRequest = onPinRequest,
                                    onClose = onClose ?: {},
                                    onDragStart = {
                                        if (searchQuery.isEmpty()) {
                                            draggingGlobalIndex = globalIndex
                                            draggingOffset = Offset.Zero
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        }
                                    },
                                    onDrag = { _, dragAmount ->
                                        draggingOffset += dragAmount
                                        val currentFingerPos = draggingItemInitialPos + draggingOffset
                                        isHoveringDelete = currentFingerPos.y < 350f

                                        if (!isHoveringDelete) {
                                            val threshold = iconSize * 1.5f
                                            val colShift = (draggingOffset.x / threshold).roundToInt()
                                            val rowShift = (draggingOffset.y / threshold).roundToInt()

                                            if (colShift != 0 || rowShift != 0) {
                                                val targetLocal = localIndex + colShift + (rowShift * columnCount)
                                                val targetGlobal = startIndex + targetLocal

                                            }
                                        }
                                    },
                                    onDragEnd = {
                                        if (isHoveringDelete && draggingGlobalIndex != null) {
                                            val pkg = allApps[draggingGlobalIndex!!].packageName
                                            AppActions.uninstallApp(context, pkg)
                                        }
                                        draggingGlobalIndex = null
                                        draggingOffset = Offset.Zero
                                        draggingItemInitialPos = Offset.Zero
                                        isHoveringDelete = false
                                    }
                                )
                            }
                        }
                    }
                }

                if (pages.size > 1) {
                    Row(
                        modifier = Modifier.fillMaxWidth().height(40.dp).padding(bottom = 10.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        repeat(pagerState.pageCount) { i ->
                            val color = if (pagerState.currentPage == i) accentColor else Color.Gray.copy(0.5f)
                            val size by animateDpAsState(if (pagerState.currentPage == i) 8.dp else 6.dp, label = "dot")
                            Box(
                                modifier = Modifier.padding(3.dp).size(20.dp).clickable { scope.launch { pagerState.animateScrollToPage(i) } },
                                contentAlignment = Alignment.Center
                            ) {
                                Box(modifier = Modifier.size(size).clip(CircleShape).background(color))
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(100.dp))
            }

            if (draggingGlobalIndex != null) {
                val draggedApp = allApps.getOrNull(draggingGlobalIndex!!)
                if (draggedApp != null) {
                    Box(
                        modifier = Modifier
                            .offset { IntOffset(draggingOffset.x.roundToInt(), draggingOffset.y.roundToInt()) }
                            .offset { IntOffset(draggingItemInitialPos.x.toInt(), draggingItemInitialPos.y.toInt()) }
                            .zIndex(999f)
                            .scale(if (isHoveringDelete) 0.8f else 1.15f)
                            .alpha(if (isHoveringDelete) 0.6f else 1.0f)
                    ) {
                        InternalAppIcon(bitmap = draggedApp.icon, modifier = Modifier.size(iconSize.dp).shadow(12.dp, CircleShape))
                    }
                }
            }
        }
    }
}

@Composable
private fun FluentDeletePill(isHovering: Boolean, isDark: Boolean) {
    val baseMicaColor = if (isDark) Color(0xFF303030) else Color(0xFFF9F9F9)
    val startAlpha = if (isDark) 0.90f else 0.85f
    val endAlpha = if (isDark) 0.98f else 0.95f
    val bgColor = if (isHovering) Color.Red.copy(0.9f) else baseMicaColor.copy(alpha = startAlpha)
    val contentColor = if (isHovering) Color.White else if (isDark) Color.White.copy(0.9f) else Color.Black.copy(0.9f)
    val borderColor = if (isHovering) Color.Red else if (isDark) Color.White.copy(0.15f) else Color.Black.copy(0.08f)
    val scale by animateFloatAsState(if (isHovering) 1.05f else 1f, label = "pillScale")

    Row(
        modifier = Modifier
            .scale(scale)
            .height(52.dp)
            .width(180.dp)
            .shadow(16.dp, CircleShape, spotColor = if(isDark) Color.Black.copy(0.5f) else Color.Black.copy(0.15f))
            .clip(CircleShape)
            .background(bgColor)
            .border(1.5.dp, borderColor, CircleShape),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = FluentIcons.Regular.Delete,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.width(12.dp))
        Text("Désinstaller", color = contentColor, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun AppIconItem(
    app: AppModel,
    size: Float,
    showLabel: Boolean,
    accentColor: Color,
    isDragging: Boolean,
    onPinRequest: (AppModel) -> Unit,
    onClose: () -> Unit,
    onDragStart: (Offset) -> Unit = {},
    onDrag: (change: androidx.compose.ui.input.pointer.PointerInputChange, dragAmount: Offset) -> Unit = { _, _ -> },
    onDragEnd: () -> Unit = {}
) {
    val context = LocalContext.current
    val view = LocalView.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val isDark = isSystemInDarkTheme()

    var showMenu by remember { mutableStateOf(false) }
    var iconPos by remember { mutableStateOf(Offset.Zero) }
    var isPressed by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.9f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessLow), label = "scale"
    )

    val alpha = if (isDragging) 0f else 1f

    Box(
        modifier = Modifier
            .onGloballyPositioned { iconPos = it.positionInWindow() }
            .scale(scale)
            .alpha(alpha)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        tryAwaitRelease()
                        isPressed = false
                    },
                    onLongPress = {
                        showMenu = true
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        isPressed = false
                    },
                    onTap = {
                        scope.launch { delay(50); AppLauncher.launch(context, view, app.packageName, iconPos, size) }
                    }
                )
            }
            .pointerInput(Unit) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { offset ->
                        isPressed = true
                        showMenu = false
                        onDragStart(offset)
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        onDrag(change, dragAmount)
                    },
                    onDragEnd = {
                        isPressed = false
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
            InternalAppIcon(bitmap = app.icon, modifier = Modifier.size(size.dp))
            if (showLabel) {
                Text(
                    text = app.label,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    color = if (isDark) Color.White else Color.Black,
                    modifier = Modifier.padding(top = 4.dp).width((size + 24).dp)
                )
            }
        }

        if (showMenu && !isDragging) {
            FluentContextMenu(
                app = app,
                iconPosition = iconPos,
                onDismiss = { showMenu = false },
                isDark = isDark,
                onPin = {
                    onPinRequest(app)
                    showMenu = false
                    onClose()
                }
            )
        }
    }
}

@Composable
private fun MicaSectionHeader(letter: Char, isDark: Boolean) {
    val txtColor = if (isDark) Color.White else Color(0xFF1A1A1A)
    val micaColor = if (isDark) Color(0xFF202020).copy(0.3f) else Color(0xFFFFFFFF).copy(0.4f)
    val borderColor = if (isDark) Color.White.copy(0.1f) else Color.Black.copy(0.05f)
    Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(contentAlignment = Alignment.Center) {
            Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(micaColor).border(1.dp, borderColor, CircleShape))
            Text(text = letter.toString(), fontWeight = FontWeight.Bold, fontSize = 18.sp, color = txtColor)
        }
    }
}

@Composable
fun InternalAppIcon(bitmap: Bitmap?, modifier: Modifier = Modifier) {
    if (bitmap != null) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = null,
            modifier = modifier,
            contentScale = ContentScale.Fit
        )
    } else {
        Box(modifier = modifier.background(Color.Gray.copy(0.2f), CircleShape))
    }
}

@SuppressLint("ConfigurationScreenWidthHeight")
@Composable
private fun FluentContextMenu(
    app: AppModel,
    iconPosition: Offset,
    onDismiss: () -> Unit,
    isDark: Boolean,
    onPin: () -> Unit
) {
    val context = LocalContext.current
    val canFitAbove = iconPosition.y > 600f
    val bgColor = if (isDark) Color(0xFF1F1F1F).copy(0.98f) else Color(0xFFFEFEFE).copy(0.98f)
    val txtColor = if (isDark) Color.White.copy(0.9f) else Color.Black.copy(0.9f)
    val borderColor = if (isDark) Color.White.copy(0.12f) else Color.Black.copy(0.08f)

    Popup(
        alignment = if (canFitAbove) Alignment.BottomCenter else Alignment.TopCenter,
        offset = IntOffset(0, if (canFitAbove) -180 else 180),
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true, dismissOnClickOutside = true, clippingEnabled = false)
    ) {
        Column(
            Modifier.width(220.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(bgColor)
                .border(1.dp, borderColor, RoundedCornerShape(12.dp))
                .padding(vertical = 4.dp)
        ) {
            MenuItem(FluentIcons.Regular.Pin, "Épingler à l'accueil", txtColor) {
                onPin()
            }

            HorizontalDivider(Modifier.padding(vertical = 4.dp), color = borderColor, thickness = 0.5.dp)

            MenuItem(Icons.Default.Info, "Infos App", txtColor) {
                AppActions.openAppInfo(context, app.packageName)
                onDismiss()
            }

            MenuItem(Icons.Default.Delete, "Désinstaller", txtColor) {
                AppActions.uninstallApp(context, app.packageName)
                onDismiss()
            }
        }
    }
}

@Composable
private fun MenuItem(icon: ImageVector, text: String, textColor: Color, onClick: () -> Unit) {
    Surface(onClick = onClick, color = Color.Transparent, modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = textColor, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(12.dp))
            Text(text = text, color = textColor, fontSize = 14.sp)
        }
    }
}

private object AppGrouper {
    fun groupByAlphabet(apps: List<AppModel>): Map<Char, List<AppModel>> {
        val collator = Collator.getInstance().apply { strength = Collator.PRIMARY }
        return apps.distinctBy { it.packageName }.sortedWith { a, b -> collator.compare(a.label, b.label) }.groupBy { it.label.firstOrNull()?.uppercaseChar()?.takeIf { c -> c.isLetter() } ?: '#' }
    }
}

private object AppLauncher {
    fun launch(context: Context, view: android.view.View, packageName: String, position: Offset, size: Float) {
        val intent = context.packageManager.getLaunchIntentForPackage(packageName) ?: return
        val options = ActivityOptions.makeScaleUpAnimation(view, position.x.toInt(), position.y.toInt(), size.toInt(), size.toInt())
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent, options.toBundle())
    }
}

private object AppActions {
    fun openAppInfo(context: Context, packageName: String) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply { data = Uri.fromParts("package", packageName, null) }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }
    fun uninstallApp(context: Context, packageName: String) {
        val intent = Intent(Intent.ACTION_DELETE).apply { data = Uri.fromParts("package", packageName, null) }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }
}