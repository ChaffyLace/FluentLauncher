package com.example.fluentui.ui.settings

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
    onWallpaperChangeClick: () -> Unit // Correction : minuscule (camelCase)
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
            onOpenGallery = onWallpaperChangeClick // On passe l'action ici pour qu'elle soit utilisée
        )
    }
}

@Composable
private fun SettingsScreenContent(
    micaParams: MicaParams?,
    onDismiss: () -> Unit,
    viewModel: SettingsViewModel,
    allApps: List<AppModel>,
    onOpenGallery: () -> Unit // On reçoit l'action ici
) {
    val state by viewModel.uiState.collectAsState()
    var currentPage by remember { mutableStateOf(SettingsPageType.HOME) }
    val isDark = isSystemInDarkTheme()

    BackHandler(enabled = currentPage != SettingsPageType.HOME) {
        currentPage = SettingsPageType.HOME
    }

    val closeBtnBg = if (isDark) Color.White.copy(0.08f) else Color.Black.copy(0.05f)
    val iconColor = if (isDark) Color.White else Color(0xFF1D1D1D)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { }
    ) {
        // --- FOND ---
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
                .padding(horizontal = 24.dp, vertical = 20.dp)
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                IconButton(
                    onClick = {
                        if (currentPage == SettingsPageType.HOME) onDismiss()
                        else currentPage = SettingsPageType.HOME
                    },
                    modifier = Modifier.background(closeBtnBg, RoundedCornerShape(8.dp)).size(32.dp)
                ) {
                    Icon(
                        if (currentPage == SettingsPageType.HOME) Icons.Default.Close else Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        null, tint = iconColor, modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            AnimatedContent(
                targetState = currentPage,
                label = "SettingsNav",
                transitionSpec = {
                    if (targetState != SettingsPageType.HOME) {
                        slideInHorizontally { it / 4 } + fadeIn() togetherWith slideOutHorizontally { -it / 4 } + fadeOut()
                    } else {
                        slideInHorizontally { -it / 4 } + fadeIn() togetherWith slideOutHorizontally { it / 4 } + fadeOut()
                    }
                }
            ) { page ->
                when (page) {
                    SettingsPageType.HOME -> HomePage(
                        state = state,
                        onNavigate = { currentPage = it },
                        onSetAccent = viewModel::setAccentColor
                    )
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

// --- FOND MICA "SOLIDE" ET COLORÉ ---
@Composable
private fun MicaSolidBackground(micaParams: MicaParams, isDark: Boolean) {
    Box(Modifier.fillMaxSize()) {
        // 1. BASE NEUTRE
        val baseNeutral = if (isDark) Color(0xFF202020) else Color(0xFFEEEEEE)
        Box(Modifier.fillMaxSize().background(baseNeutral))

        // 2. COUCHE DE TEINTE (Tint)
        val tintAlpha = if (isDark) 0.35f else 0.60f
        Box(Modifier.fillMaxSize().background(micaParams.tintBrush, alpha = tintAlpha))

        // 3. BRUIT (Grain)
        micaParams.noiseBrush?.let {
            Box(Modifier.fillMaxSize().background(it, alpha = 0.02f))
        }
    }
}