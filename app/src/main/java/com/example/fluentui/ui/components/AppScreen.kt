package com.example.fluentui.ui.components

import android.app.Application
import android.net.Uri
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
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

// ==============================================================================
// AGSL SHADER — WINDOWS 11 ACRYLIC NOISE
// ==============================================================================
private const val ACRYLIC_NOISE_SHADER = """
    uniform float2 resolution;
    float random(float2 st) {
        return fract(sin(dot(st.xy, float2(12.9898,78.233))) * 43758.5453123);
    }
    half4 main(float2 fragCoord) {
        float noise = random(fragCoord);
        return half4(noise, noise, noise, 0.05);
    }
"""

fun Modifier.windowsAcrylicNoise(isTransparencyEnabled: Boolean): Modifier = composed {
    if (isTransparencyEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val shader = remember { android.graphics.RuntimeShader(ACRYLIC_NOISE_SHADER) }
        this.drawWithCache {
            shader.setFloatUniform("resolution", size.width, size.height)
            val brush = ShaderBrush(shader)
            onDrawBehind { drawRect(brush = brush) }
        }
    } else {
        this
    }
}

// ==============================================================================
// MAIN APP SCREEN (ROOT COMPOSABLE)
// ==============================================================================
@Composable
fun AppScreen(
    viewModel: AppViewModel = viewModel(),
    settingsViewModel: SettingsViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isDark = isSystemInDarkTheme()
    val settingsManager = remember { SettingsManager(context) }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.setCustomWallpaper(it) }
    }

    // --- STATE COLLECTION ---
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
                        pagerState.animateScrollToPage(
                            page = 0,
                            pageOffsetFraction = 0f,
                            animationSpec = spring(stiffness = Spring.StiffnessMedium)
                        )
                    }
                }
            }
        }
    }

    // --- SEARCH ---
    val allApps by viewModel.otherApps
    val searchViewModel: SearchViewModel = viewModel(
        factory = SearchViewModel.Factory(context.applicationContext as Application)
    )
    val searchState by searchViewModel.uiState.collectAsState()

    LaunchedEffect(allApps) { searchViewModel.updateApps(allApps) }

    val micaParams by viewModel.micaParams.collectAsState()

    // --- UI STATES ---
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

    // --- ANIMATIONS ---
    val rawProgress by remember {
        derivedStateOf {
            if (isSearchVisible) 1f
            else (pagerState.currentPage.toFloat() + pagerState.currentPageOffsetFraction).coerceIn(0f, 1f)
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
        label = "LayerAlpha"
    )

    val backgroundBlur by animateDpAsState(
        targetValue = if (isSearchVisible && settingsState.transparencyEnabled) 45.dp else 0.dp,
        animationSpec = tween(400),
        label = "AcrylicBlur"
    )

    // ==============================================================================
    // ROOT UI STACK
    // ==============================================================================
    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures { if (isSearchVisible) closeSearch() }
            }
    ) {

        // --- LAYERS 1 & 2: WALLPAPER + PAGER ---
        Box(
            modifier = Modifier
                .fillMaxSize()
                .blur(backgroundBlur)
        ) {
            WallpaperLayer(
                settings = settingsState,
                isDark = isDark,
                scrollProgress = motionProgress,
                repo = viewModel.wallpaperRepo
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = contentAlpha }
            ) {
                VerticalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                    pageSpacing = 0.dp,
                    flingBehavior = PagerDefaults.flingBehavior(
                        state = pagerState,
                        snapPositionalThreshold = 0.05f,
                        snapAnimationSpec = spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessHigh
                        )
                    )
                ) { page ->
                    Box(
                        modifier = Modifier
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
        }

        // --- LAYER 3: SEARCH OVERLAY (TRUE ACRYLIC) ---
        FluentReveal(
            visible = isSearchVisible,
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        if (isDark) Color(0xFF1E1E1E).copy(alpha = if (settingsState.transparencyEnabled) 0.88f else 1f)
                        else Color(0xFFF3F3F3).copy(alpha = if (settingsState.transparencyEnabled) 0.88f else 1f)
                    )
                    .windowsAcrylicNoise(settingsState.transparencyEnabled)
                    .statusBarsPadding()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { closeSearch() }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .imePadding()
                        .padding(bottom = 90.dp) // Laisse la place pour la pilule de recherche
                ) {
                    if (searchState.query.isEmpty()) {
                        SearchDashboard(
                            topApps = searchState.topApps.ifEmpty { allApps.take(8) },
                            recentSearches = searchState.recentSearches,
                            onAppClick = { pkg ->
                                searchViewModel.onAppLaunched(pkg, searchState.query)
                                context.packageManager.getLaunchIntentForPackage(pkg)?.let {
                                    context.startActivity(it)
                                    closeSearch()
                                }
                            },
                            onRecentSearchClick = { query ->
                                searchViewModel.onQueryChange(query)
                                isSearchActive = true
                            },
                            isDark = isDark,
                            accentColor = Color(accentColorInt)
                        )
                    } else {
                        SearchResultsList(
                            results = searchState.results,
                            isDark = isDark,
                            accentColor = Color(accentColorInt),
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }

        // --- LAYER 4: FLOATING SEARCH PILL ---
        // DE RETOUR ICI : Permanente, visible tout le temps, au-dessus du reste.
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .imePadding()
                .padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 16.dp)
        ) {
            FluentSearchPill(
                searchQuery = searchState.query,
                onQueryChange = { query ->
                    searchViewModel.onQueryChange(query)
                    if (!isSearchActive && query.isNotEmpty()) isSearchActive = true
                },
                onSettingsClick = { showSettings = true },
                onFocusChange = { focused -> if (focused) isSearchActive = true },
                isDark = isDark,
                isTransparencyEnabled = settingsState.transparencyEnabled
            )
        }

        // --- LAYER 5: SETTINGS OVERLAY ---
        if (showSettings) {
            SettingsOverlay(
                isVisible = showSettings,
                micaParams = micaParams,
                allApps = allApps,
                onDismiss = { showSettings = false },
                viewModel = settingsViewModel,
                onWallpaperChangeClick = { galleryLauncher.launch("image/*") }
            )
        }
    }
}

// ==============================================================================
// APP GRID WRAPPER
// ==============================================================================
@Composable
fun AppGridWrapper(
    viewModel: AppViewModel,
    settingsManager: SettingsManager,
    micaParams: MicaParams
) {
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
        viewModel = viewModel,
        columnCount = colCount,
        rowCount = rowCount,
        iconSize = iconSize,
        showLabels = showLabels,
        accentColor = Color(accentColorInt),
        isVertical = isVertical,
        fontSize = fontSize,
        verticalSpacing = vSpacing,
        hiddenApps = hiddenApps,
        onPinRequest = { viewModel.pinApp(it) },
        micaParams = micaParams
    )
}

// ==============================================================================
// SYSTEM BAR CONTROLLER
// ==============================================================================
@Composable
private fun SystemBarController(isDark: Boolean) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val activity = view.context as? android.app.Activity ?: return@SideEffect
            androidx.core.view.WindowCompat.getInsetsController(activity.window, view).apply {
                isAppearanceLightStatusBars = !isDark
                isAppearanceLightNavigationBars = !isDark
            }
        }
    }
}