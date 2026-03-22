package com.example.fluentui.ui.settings

import androidx.activity.compose.BackHandler
import android.app.UiModeManager
import android.content.Context
import android.os.Build
import androidx.compose.ui.platform.LocalContext
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
import com.example.fluentui.ui.components.AppModel
import com.example.fluentui.wallpaper.MicaParams

@Composable
fun SettingsOverlay(
    isVisible: Boolean,
    micaParams: MicaParams?,
    allApps: List<AppModel>,
    onDismiss: () -> Unit,
    viewModel: SettingsViewModel = viewModel(),
    onWallpaperChangeClick: () -> Unit
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically(initialOffsetY = { it / 10 }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it / 10 }) + fadeOut(),
        modifier = Modifier.zIndex(100f)
    ) {
        SettingsScreenContent(
            micaParams = micaParams,
            onDismiss = onDismiss,
            viewModel = viewModel,
            allApps = allApps,
            onOpenGallery = onWallpaperChangeClick
        )
    }
}

@Composable
private fun SettingsScreenContent(
    micaParams: MicaParams?,
    onDismiss: () -> Unit,
    viewModel: SettingsViewModel,
    allApps: List<AppModel>,
    onOpenGallery: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    var currentPage by remember { mutableStateOf(SettingsPageType.HOME) }
    val isDark = isSystemInDarkTheme()

    BackHandler(enabled = currentPage != SettingsPageType.HOME) {
        currentPage = SettingsPageType.HOME
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { }
    ) {
        if (state.transparencyEnabled && micaParams != null) {
            MicaSolidBackground(micaParams = micaParams, isDark = isDark)
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
                        val context = LocalContext.current
                        HomePage(
                            state = state,
                            onNavigate = { currentPage = it },
                            onSetAccent = viewModel::setAccentColor,
                            onSetWallpaper = viewModel::setWallpaper,
                            onSetDarkMode = { isDark ->
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                    val uiModeManager = context.getSystemService(Context.UI_MODE_SERVICE) as UiModeManager
                                    uiModeManager.setApplicationNightMode(
                                        if (isDark) UiModeManager.MODE_NIGHT_YES
                                        else UiModeManager.MODE_NIGHT_NO
                                    )
                                }
                            }
                        )
                    }
                    SettingsPageType.BACKGROUND -> BackgroundPage(
                        state = state,
                        onSetWallpaper = viewModel::setWallpaper,
                        onOpenGallery = onOpenGallery
                    )
                    SettingsPageType.COLOR -> ColorPage(
                        state = state,
                        onSetAccent = viewModel::setAccentColor,
                        onSetTransparency = viewModel::setTransparency
                    )
                    SettingsPageType.LAYOUT -> LayoutPage(
                        state = state,
                        allApps = allApps,
                        onSetCols = viewModel::setColumnCount,
                        onSetRows = viewModel::setRowCount,
                        onSetIconSize = viewModel::setIconSize,
                        onSetFontSize = viewModel::setFontSize,
                        onSetSpacing = viewModel::setVerticalSpacing,
                        onSetLabels = viewModel::setShowLabels,
                        onSetVertical = viewModel::setVerticalScroll,
                        onSetHidden = viewModel::setAppHidden
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