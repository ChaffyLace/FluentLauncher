/**
 * @file SettingsOverlay.kt
 * @description Master router and container for the settings experience.
 * Broadcasts Mica globally and enforces system-driven Light/Dark mode.
 */

package com.example.fluentui.ui.settings

import androidx.activity.compose.BackHandler
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fluentui.data.SettingsManager
import com.example.fluentui.ui.components.AppModel
import com.example.fluentui.wallpaper.MicaParams

val LocalMicaParams = compositionLocalOf<MicaParams?> { null }

@Composable
fun SettingsOverlay(
    isVisible: Boolean,
    micaParams: MicaParams?,
    allApps: List<AppModel>,
    onDismiss: () -> Unit,
    settingsManager: SettingsManager,
    viewModel: SettingsViewModel = viewModel(),
    onWallpaperChangeClick: () -> Unit
) {
    CompositionLocalProvider(LocalMicaParams provides micaParams) {
        AnimatedVisibility(
            visible = isVisible,
            enter = slideInVertically(initialOffsetY = { it / 10 }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it / 10 }) + fadeOut(),
            modifier = Modifier.zIndex(100f)
        ) {
            SettingsScreenContent(
                onDismiss = onDismiss,
                viewModel = viewModel,
                settingsManager = settingsManager,
                allApps = allApps,
                onOpenGallery = onWallpaperChangeClick
            )
        }
    }
}

@Composable
private fun SettingsScreenContent(
    onDismiss: () -> Unit,
    viewModel: SettingsViewModel,
    settingsManager: SettingsManager,
    allApps: List<AppModel>,
    onOpenGallery: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    var currentPage by remember { mutableStateOf(SettingsPageType.HOME) }
    val isDark = isSystemInDarkTheme()
    val currentMicaParams = LocalMicaParams.current

    LaunchedEffect(Unit) {
        try {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        } catch (e: Exception) {
            // Silently ignore — theme will fall back to system default
        }
    }

    BackHandler(enabled = true) {
        if (currentPage != SettingsPageType.HOME) {
            currentPage = SettingsPageType.HOME
        } else {
            onDismiss()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { }
    ) {
        if (state.transparencyEnabled && currentMicaParams != null) {
            MicaSolidBackground(micaParams = currentMicaParams, isDark = isDark)
        } else {
            val fallbackColor = if (isDark) Color(0xFF202020) else Color(0xFFF3F3F3)
            Box(Modifier.fillMaxSize().background(fallbackColor))
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
                .padding(top = 56.dp, bottom = 16.dp)
        ) {
            AnimatedContent(
                targetState = currentPage,
                label = "SettingsNav",
                transitionSpec = {
                    if (targetState != SettingsPageType.HOME) {
                        slideInHorizontally { it / 4 } + fadeIn() togetherWith
                                slideOutHorizontally { -it / 4 } + fadeOut()
                    } else {
                        slideInHorizontally { -it / 4 } + fadeIn() togetherWith
                                slideOutHorizontally { it / 4 } + fadeOut()
                    }
                }
            ) { page ->
                when (page) {
                    SettingsPageType.HOME -> {
                        HomePage(
                            state = state,
                            settingsManager = settingsManager,
                            onNavigate = { currentPage = it },
                            onSetAccent = { color -> viewModel.setAccentColor(color) },
                            onSetWallpaper = { type, value -> viewModel.setWallpaper(type, value) }
                        )
                    }
                    SettingsPageType.BACKGROUND -> BackgroundPage(
                        state = state,
                        onSetWallpaper = { type, value -> viewModel.setWallpaper(type, value) },
                        onOpenGallery = onOpenGallery
                    )
                    SettingsPageType.COLOR -> ColorPage(
                        state = state,
                        onSetAccent = { color -> viewModel.setAccentColor(color) },
                        onSetTransparency = { enabled -> viewModel.setTransparency(enabled) }
                    )
                    SettingsPageType.LAYOUT -> LayoutPage(
                        state = state,
                        allApps = allApps,
                        onSetCols = { cols -> viewModel.setColumnCount(cols) },
                        onSetRows = { rows -> viewModel.setRowCount(rows) },
                        onSetIconSize = { size -> viewModel.setIconSize(size) },
                        onSetFontSize = { size -> viewModel.setFontSize(size) },
                        onSetSpacing = { spacing -> viewModel.setVerticalSpacing(spacing) },
                        onSetLabels = { show -> viewModel.setShowLabels(show) },
                        onSetVertical = { isVert -> viewModel.setVerticalScroll(isVert) },
                        onSetHidden = { pkg, hidden -> viewModel.setAppHidden(pkg, hidden) },
                        onSetShowDrawerSearch = { show -> viewModel.setShowDrawerSearch(show) },
                        onSetAutoKeyboard = { auto -> viewModel.setAutoOpenKeyboard(auto) }
                    )
                    SettingsPageType.GESTURES -> GesturesPage(
                        state = state,
                        onSetShowBadges = { enabled -> viewModel.setShowBadges(enabled) },
                        onSetSwipeDown = { enabled -> viewModel.setSwipeDownEnabled(enabled) },
                        onSetDoubleTap = { enabled -> viewModel.setDoubleTapToSleep(enabled) },
                        onSetGlobalActionCenter = { viewModel.setGlobalActionCenter(it) }
                    )
                    SettingsPageType.SYSTEM_ADVANCED -> SystemAdvancedPage(
                        state = state,
                        onSetSecureHiddenApps = { secure -> viewModel.setSecureHiddenApps(secure) }
                    )
                }
            }
        }
    }
}

@Composable
private fun MicaSolidBackground(micaParams: MicaParams, isDark: Boolean) {
    Box(Modifier.fillMaxSize()) {
        val baseNeutral = if (isDark) Color(0xFF202020) else Color(0xFFEEEEEE)
        Box(Modifier.fillMaxSize().background(baseNeutral))
        val tintAlpha = if (isDark) 0.35f else 0.60f
        Box(Modifier.fillMaxSize().background(micaParams.tintBrush, alpha = tintAlpha))
        micaParams.noiseBrush?.let {
            Box(Modifier.fillMaxSize().background(it, alpha = 0.02f))
        }
    }
}
