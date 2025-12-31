package com.example.fluentui.ui.components

import android.app.Activity
import android.app.Application
import android.graphics.Bitmap
import android.view.MotionEvent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fluentui.data.SettingsManager
import com.example.fluentui.ui.motion.FluentReveal
import com.example.fluentui.ui.search.*
import com.example.fluentui.ui.theme.AppTheme
import com.example.fluentui.ui.theme.FluentMotion
import com.example.fluentui.wallpaper.WallpaperLayer
import com.example.fluentui.wallpaper.rememberSystemWallpaper
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun LauncherScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isDark = isSystemInDarkTheme()

    SystemBarController(isDark)

    val wallpaperData = rememberSystemWallpaper(context)
    val wallpaperBitmap = wallpaperData.first
    val blurredWallpaperBitmap = wallpaperData.second

    val launcherState = rememberLauncherState()
    val settingsManager = remember { SettingsManager(context) }

    val appViewModel: AppViewModel = viewModel()
    val allApps by appViewModel.otherApps
    val searchViewModel: SearchViewModel = viewModel(
        factory = SearchViewModel.Factory(context.applicationContext as Application)
    )
    val searchState by searchViewModel.uiState.collectAsState()

    LaunchedEffect(allApps) { searchViewModel.updateApps(allApps) }

    val columnCount by settingsManager.columnCount.collectAsState(initial = 4)
    val rowCount by settingsManager.rowCount.collectAsState(initial = 6)
    val iconSize by settingsManager.iconSize.collectAsState(initial = 60f)
    val showLabels by settingsManager.showLabels.collectAsState(initial = true)
    val accentColorInt by settingsManager.accentColor.collectAsState(initial = 0xFF0078D4.toInt())
    val isVertical by settingsManager.isVerticalScroll.collectAsState(initial = false)
    val accentColor = Color(accentColorInt)

    val userPreferences = remember(columnCount, rowCount, iconSize, showLabels, accentColor, isVertical) {
        UserPreferences(columnCount, rowCount, iconSize, showLabels, isVertical, accentColor)
    }

    var showSettings by remember { mutableStateOf(false) }
    var isSearchActive by remember { mutableStateOf(false) }
    val isDashboard = launcherState.pagerState.currentPage == 0
    val isSearchVisible = isSearchActive || searchState.query.isNotEmpty()

    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    fun closeSearch() {
        searchViewModel.onQueryChange("")
        isSearchActive = false
        focusManager.clearFocus()
        keyboardController?.hide()
    }

    fun navigateToPage(page: Int) {
        scope.launch {
            launcherState.pagerState.animateScrollToPage(
                page = page,
                animationSpec = FluentMotion.pageScrollSpec()
            )
        }
    }

    BackHandler(enabled = true) {
        when {
            showSettings -> showSettings = false
            isSearchVisible -> closeSearch()
            !isDashboard -> navigateToPage(0)
        }
    }

    val rawProgress by remember {
        derivedStateOf {
            val current = launcherState.pagerState.currentPage.toFloat()
            val offset = launcherState.pagerState.currentPageOffsetFraction
            if (isSearchVisible) 1f else (current + offset).coerceIn(0f, 1f)
        }
    }

    val motionProgress by animateFloatAsState(
        targetValue = rawProgress,
        animationSpec = FluentMotion.pageScrollSpec(),
        label = "MotionProgress"
    )

    val contentAlpha by animateFloatAsState(
        targetValue = if (isSearchVisible) 0f else 1f,
        animationSpec = FluentMotion.enterSpec(FluentMotion.DurationFast),
        label = "ContentAlpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures { if (isSearchVisible) closeSearch() }
            }
            .then(
                if (!isSearchVisible) {
                    Modifier.pointerInteropFilter { event ->
                        if (event.action == MotionEvent.ACTION_SCROLL) {
                            val scrollY = event.getAxisValue(MotionEvent.AXIS_VSCROLL)
                            when {
                                scrollY < 0 && isDashboard -> { navigateToPage(1); true }
                                scrollY > 0 && !isDashboard -> { navigateToPage(0); true }
                                else -> false
                            }
                        } else false
                    }
                } else Modifier
            )
    ) {
        // LAYER 0: Wallpaper
        if (wallpaperBitmap != null) {
            WallpaperLayer(
                sharpBitmap = wallpaperBitmap,
                blurredBitmap = blurredWallpaperBitmap,
                motionProgress = motionProgress,
                isDark = isDark
            )
        } else {
            Box(Modifier.fillMaxSize().background(Color.Black))
        }

        // LAYER 1: Content Pager (Dashboard & Apps)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = contentAlpha }
        ) {
            ContentPager(
                state = launcherState,
                viewModel = appViewModel,
                userPreferences = userPreferences,
                blurredWallpaper = blurredWallpaperBitmap,
                modifier = Modifier.fillMaxSize()
            )
        }

        // LAYER 2: Search Overlay (Background & Results)
        FluentReveal(
            visible = isSearchVisible,
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(if (isDark) Color.Black.copy(0.4f) else Color.White.copy(0.4f))
                    .statusBarsPadding()
                    .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) {
                        closeSearch()
                    }
            ) {
                if (searchState.query.isEmpty()) {
                    SearchDashboard(allApps, { pkg ->
                        context.packageManager.getLaunchIntentForPackage(pkg)?.let {
                            context.startActivity(it); closeSearch()
                        }
                    }, isDark)
                } else {
                    SearchResultsList(searchState.results, isDark, accentColor, Modifier.fillMaxSize().padding(bottom = 100.dp))
                }
            }
        }

        // LAYER 3: Search Pill (Floating UI)
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 32.dp)
                .imePadding()
        ) {
            FluentSearchPill(
                searchQuery = searchState.query,
                onQueryChange = { query ->
                    searchViewModel.onQueryChange(query)
                    if (!isSearchActive && query.isNotEmpty()) isSearchActive = true
                },
                onSettingsClick = { showSettings = true },
                onFocusChange = { if (it) isSearchActive = true },
                isDark = isDark
            )
        }

        // LAYER 4: Settings Modal
        if (showSettings) {
            ModalBottomSheet(
                onDismissRequest = { showSettings = false },
                containerColor = Color.Transparent,
                dragHandle = null
            ) {
                SettingsContent { showSettings = false }
            }
        }
    }
}

@Composable
private fun ContentPager(
    state: LauncherState,
    viewModel: AppViewModel,
    userPreferences: UserPreferences,
    blurredWallpaper: Bitmap?,
    modifier: Modifier = Modifier
) {
    VerticalPager(
        state = state.pagerState,
        modifier = modifier,
        key = { it },
        beyondViewportPageCount = 1,
        userScrollEnabled = true
    ) { page ->
        val pageOffset = remember(state.pagerState.currentPage, state.pagerState.currentPageOffsetFraction) {
            (state.pagerState.currentPage - page) + state.pagerState.currentPageOffsetFraction
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    if (page == 0) {
                        val absOffset = pageOffset.absoluteValue
                        val scale = lerp(1f, 0.95f, absOffset)
                        scaleX = scale
                        scaleY = scale
                        alpha = lerp(1f, 0.0f, absOffset)
                        translationY = pageOffset * 50f
                    }
                }
        ) {
            when (page) {
                0 -> DashboardPage(
                    viewModel = viewModel,
                    blurredWallpaper = blurredWallpaper,
                    theme = if (isSystemInDarkTheme()) AppTheme.WINDOWS_DARK else AppTheme.WINDOWS_LIGHT
                )

                1 -> AppGrid(
                    viewModel = viewModel,
                    state = state.gridState,
                    columnCount = userPreferences.columnCount,
                    rowCount = userPreferences.rowCount,
                    iconSize = userPreferences.iconSize,
                    showLabels = userPreferences.showLabels,
                    searchQuery = "",
                    accentColor = userPreferences.accentColor,
                    isVertical = userPreferences.isVertical
                )
            }
        }
    }
}

@Composable
private fun SystemBarController(isDark: Boolean) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            val insets = WindowCompat.getInsetsController(window, view)
            insets.isAppearanceLightStatusBars = !isDark
            insets.isAppearanceLightNavigationBars = !isDark
        }
    }
}

@Composable
private fun rememberLauncherState(): LauncherState {
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { 2 })
    val gridState = rememberLazyGridState()
    return remember(pagerState, gridState) { LauncherState(pagerState, gridState) }
}

private data class LauncherState(val pagerState: PagerState, val gridState: LazyGridState)
private data class UserPreferences(
    val columnCount: Int, val rowCount: Int, val iconSize: Float,
    val showLabels: Boolean, val isVertical: Boolean, val accentColor: Color
)