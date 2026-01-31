package com.example.fluentui.ui.components

import android.app.Application
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fluentui.data.SettingsManager
import com.example.fluentui.ui.homescreen.DashboardPage
import com.example.fluentui.ui.motion.FluentReveal
import com.example.fluentui.ui.search.*
import com.example.fluentui.ui.settings.SettingsOverlay
import com.example.fluentui.ui.settings.SettingsState
import com.example.fluentui.ui.settings.SettingsViewModel
import com.example.fluentui.ui.theme.FluentMotion
import com.example.fluentui.wallpaper.MicaParams
import com.example.fluentui.wallpaper.WallpaperLayer
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

@Composable
fun AppScreen(
    viewModel: AppViewModel = viewModel(),
    settingsViewModel: SettingsViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isDark = isSystemInDarkTheme()
    val settingsManager = remember { SettingsManager(context) }

    // --- LE LANCEUR DE GALERIE ---
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.setCustomWallpaper(uri)
        }
    }

    // --- Settings Collection ---
    val wpType by settingsManager.wallpaperType.collectAsState("SOLID")
    val wpValue by settingsManager.wallpaperValue.collectAsState("-16777216")
    val accentColorInt by settingsManager.accentColor.collectAsState(0xFF60CDFF.toInt())
    val transparencyEnabled by settingsManager.transparencyEnabled.collectAsState(true)
    val hiddenApps by settingsManager.hiddenApps.collectAsState(emptySet())
    val colCount by settingsManager.columnCount.collectAsState(4)
    val rowCount by settingsManager.rowCount.collectAsState(6)
    val iconSize by settingsManager.iconSize.collectAsState(60f)
    val showLabels by settingsManager.showLabels.collectAsState(true)
    val isVertical by settingsManager.isVerticalScroll.collectAsState(false)
    val fontSize by settingsManager.fontSize.collectAsState(13f)
    val vSpacing by settingsManager.verticalSpacing.collectAsState(16f)

    val settingsState = SettingsState(
        wallpaperType = wpType,
        wallpaperValue = wpValue,
        accentColor = Color(accentColorInt),
        transparencyEnabled = transparencyEnabled,
        columnCount = colCount,
        rowCount = rowCount,
        iconSize = iconSize,
        fontSize = fontSize,
        showLabels = showLabels,
        isVertical = isVertical,
        verticalSpacing = vSpacing,
        hiddenApps = hiddenApps
    )

    SystemBarController(isDark)

    val pagerState = rememberPagerState(initialPage = 0, pageCount = { 2 })

    LaunchedEffect(Unit) {
        viewModel.navigationEvent.collect { event ->
            when (event) {
                is NavigationEvent.ScrollToHome -> {
                    if (pagerState.currentPage != 0) {
                        pagerState.animateScrollToPage(0, 0f, spring(stiffness = Spring.StiffnessMedium))
                    }
                }
            }
        }
    }

    val allApps by viewModel.otherApps
    val searchViewModel: SearchViewModel = viewModel(factory = SearchViewModel.Factory(context.applicationContext as Application))
    val searchState by searchViewModel.uiState.collectAsState()

    LaunchedEffect(allApps) { searchViewModel.updateApps(allApps) }

    val micaParams by viewModel.micaParams.collectAsState()

    var showSettings by remember { mutableStateOf(false) }
    var isSearchActive by remember { mutableStateOf(false) }
    val isDashboard = pagerState.currentPage == 0
    val isSearchVisible = isSearchActive || searchState.query.isNotEmpty()
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    fun closeSearch() {
        searchViewModel.onQueryChange("")
        isSearchActive = false
        focusManager.clearFocus()
        keyboardController?.hide()
    }

    BackHandler(enabled = true) {
        when {
            showSettings -> showSettings = false
            isSearchVisible -> closeSearch()
            !isDashboard -> scope.launch { pagerState.animateScrollToPage(0) }
        }
    }

    val rawProgress by remember { derivedStateOf { if (isSearchVisible) 1f else (pagerState.currentPage.toFloat() + pagerState.currentPageOffsetFraction).coerceIn(0f, 1f) } }
    val motionProgress by animateFloatAsState(targetValue = rawProgress, animationSpec = FluentMotion.pageScrollSpec(), label = "MotionProgress")
    val contentAlpha by animateFloatAsState(if (isSearchVisible) 0f else 1f, FluentMotion.enterSpec(FluentMotion.DurationFast), label = "alpha")

    Box(modifier = Modifier.fillMaxSize().pointerInput(Unit) { detectTapGestures { if (isSearchVisible) closeSearch() } }) {

        WallpaperLayer(settings = settingsState, isDark = isDark, scrollProgress = motionProgress,)

        Box(modifier = Modifier.fillMaxSize().graphicsLayer { alpha = contentAlpha }) {
            VerticalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                pageSpacing = 0.dp,
                flingBehavior = PagerDefaults.flingBehavior(
                    state = pagerState,
                    snapPositionalThreshold = 0.05f,
                    snapAnimationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessHigh)
                )
            ) { page ->
                Box(modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        val pageOffset = (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
                        if (page == 0) {
                            translationY = pageOffset * size.height * 0.3f
                            val scale = 1f - (pageOffset.absoluteValue * 0.05f)
                            scaleX = scale
                            scaleY = scale
                            alpha = (1f - pageOffset.absoluteValue * 3f).coerceIn(0f, 1f)
                        }
                    }
                ) {
                    when (page) {
                        0 -> DashboardPage(viewModel = viewModel)
                        1 -> AppGridWrapper(viewModel, settingsManager, micaParams)
                    }
                }
            }
        }

        FluentReveal(visible = isSearchVisible, modifier = Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxSize().background(if (isDark) Color.Black.copy(0.6f) else Color.White.copy(0.6f)).statusBarsPadding().clickable(null, null) { closeSearch() }) {
                if (searchState.query.isEmpty()) SearchDashboard(allApps, { context.packageManager.getLaunchIntentForPackage(it)?.let { i -> context.startActivity(i); closeSearch() } }, isDark)
                else SearchResultsList(searchState.results, isDark, Color(accentColorInt), Modifier.fillMaxSize().padding(bottom = 100.dp))
            }
        }

        Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(horizontal = 24.dp, vertical = 32.dp).imePadding()) {
            FluentSearchPill(searchState.query, { searchViewModel.onQueryChange(it); if (!isSearchActive && it.isNotEmpty()) isSearchActive = true }, { showSettings = true }, { if (it) isSearchActive = true }, isDark)
        }

        // --- CORRECTION DES PARAMÈTRES ICI ---
        if (showSettings) {
            SettingsOverlay(
                isVisible = showSettings,              // Correction : 'show' -> 'isVisible'
                micaParams = micaParams,
                allApps = allApps,
                onDismiss = { showSettings = false },  // Correction : 'onClose' -> 'onDismiss'
                viewModel = settingsViewModel,
                onWallpaperChangeClick = {             // Correction : 'onOpenGallery' -> 'onWallpaperChangeClick'
                    galleryLauncher.launch("image/*")
                }
            )
        }
    }
}

@Composable
fun AppGridWrapper(viewModel: AppViewModel, settingsManager: SettingsManager, micaParams: MicaParams) {
    val colCount by settingsManager.columnCount.collectAsState(4)
    val rowCount by settingsManager.rowCount.collectAsState(6)
    val iconSize by settingsManager.iconSize.collectAsState(60f)
    val showLabels by settingsManager.showLabels.collectAsState(true)
    val isVertical by settingsManager.isVerticalScroll.collectAsState(false)
    val accentColorInt by settingsManager.accentColor.collectAsState(0xFF60CDFF.toInt())
    val hiddenApps by settingsManager.hiddenApps.collectAsState(emptySet())
    val fontSize by settingsManager.fontSize.collectAsState(13f)
    val vSpacing by settingsManager.verticalSpacing.collectAsState(16f)

    AppGrid(
        viewModel = viewModel, columnCount = colCount, rowCount = rowCount, iconSize = iconSize, showLabels = showLabels, accentColor = Color(accentColorInt), isVertical = isVertical, fontSize = fontSize, verticalSpacing = vSpacing, hiddenApps = hiddenApps, onPinRequest = { viewModel.pinApp(it) },
        micaParams = micaParams
    )
}

@Composable
private fun SystemBarController(isDark: Boolean) {
    val view = LocalView.current
    if (!view.isInEditMode) { SideEffect { (view.context as android.app.Activity).window.let { w -> androidx.core.view.WindowCompat.getInsetsController(w, view).apply { isAppearanceLightStatusBars = !isDark; isAppearanceLightNavigationBars = !isDark } } } }
}