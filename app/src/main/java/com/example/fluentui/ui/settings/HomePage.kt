/**
 * @file HomePage.kt
 * @description Main entry point for the settings module.
 * Implements a scrollable, Windows 11-style island layout with launcher layout switcher.
 * Engineering Note: Both islands and buttons share the strict "Mica Wash" layering
 * for maximum UI consistency and text readability.
 */

package com.example.fluentui.ui.settings

import android.app.WallpaperManager
import android.graphics.BitmapFactory
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.fluentui.R
import com.example.fluentui.data.SettingsManager
import com.example.fluentui.wallpaper.fluentMicaComplex

// Fluent System Icons
import fluent.ui.system.icons.FluentIcons
import fluent.ui.system.icons.regular.Apps
import fluent.ui.system.icons.regular.Checkmark
import fluent.ui.system.icons.regular.ChevronRight
import fluent.ui.system.icons.regular.Color as FluentColorIcon
import fluent.ui.system.icons.regular.Grid
import fluent.ui.system.icons.regular.Image
import fluent.ui.system.icons.regular.Settings
import fluent.ui.system.icons.regular.TapDouble

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun HomePage(
    state: SettingsState,
    settingsManager: SettingsManager,
    onNavigate: (SettingsPageType) -> Unit,
    onSetAccent: (Color) -> Unit,
    onSetWallpaper: (String, String) -> Unit = { _, _ -> },
    onSetDarkMode: (Boolean) -> Unit = {}
) {
    val isDark = isSystemInDarkTheme()
    val titleColor = if (isDark) Color.White else Color(0xFF191919)
    val subtitleColor = if (isDark) Color.White.copy(alpha = 0.7f) else Color(0xFF505050)

    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val quickThemes = remember {
        listOf(
            Pair(R.drawable.windows_11_1_mattbirchler, Color(0xFF0078D4)),
            Pair(R.drawable.windows_11_2_mattbirchler, Color(0xFF881798)),
            Pair(R.drawable.windows_11_3_mattbirchler, Color(0xFFE81123)),
            Pair(R.drawable.windows_11_4_mattbirchler, Color(0xFF00B294))
        )
    }

    fun applyTheme(resId: Int, accent: Color) {
        onSetAccent(accent)
        onSetWallpaper("RES", resId.toString())
        scope.launch(Dispatchers.IO) {
            try {
                val bmp = BitmapFactory.decodeResource(context.resources, resId)
                WallpaperManager.getInstance(context).setBitmap(bmp)
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = stringResource(R.string.settings_home_title),
            color = titleColor,
            fontSize = 28.sp,
            fontWeight = FontWeight.SemiBold
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(modifier = Modifier
                .weight(0.45f)
                .fillMaxHeight()) {
                PhonePreview(
                    isVertical = state.isVertical,
                    columns = state.columnCount,
                    rows = state.rowCount,
                    accentColor = state.accentColor,
                    wallpaperModel = state.currentWallpaperBitmap,
                    fallbackColor = Color(0xFF0078D4)
                )
            }

            Column(
                modifier = Modifier
                    .weight(0.55f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.settings_quick_themes),
                    color = subtitleColor,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        quickThemes.take(2).forEach { (resId, accent) ->
                            val isSelected = state.wallpaperType == "RES" && state.wallpaperValue == resId.toString() && state.accentColor == accent
                            WallpaperThemeBox(
                                resId = resId,
                                accentColor = accent,
                                isSelected = isSelected,
                                modifier = Modifier.weight(1f)
                            ) { applyTheme(resId, accent) }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        quickThemes.drop(2).forEach { (resId, accent) ->
                            val isSelected = state.wallpaperType == "RES" && state.wallpaperValue == resId.toString() && state.accentColor == accent
                            WallpaperThemeBox(
                                resId = resId,
                                accentColor = accent,
                                isSelected = isSelected,
                                modifier = Modifier.weight(1f)
                            ) { applyTheme(resId, accent) }
                        }
                    }
                }

                MicaWashButton(
                    text = stringResource(R.string.settings_change_wallpaper_btn),
                    onClick = { onNavigate(SettingsPageType.BACKGROUND) }
                )
            }
        }

        Spacer(Modifier.height(4.dp))

        // --- Layout Switcher Island ---
        LayoutSwitcherIsland(
            settingsManager = settingsManager,
            onLayoutChange = { /* Optional callback */ }
        )

        Spacer(Modifier.height(8.dp))

        // --- Isolated Settings Cards (Îlots adoucis) ---
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SettingIslandCard(
                icon = FluentIcons.Regular.Image,
                title = stringResource(R.string.settings_island_bg_title),
                subtitle = stringResource(R.string.settings_island_bg_desc),
                onClick = { onNavigate(SettingsPageType.BACKGROUND) }
            )
            SettingIslandCard(
                icon = FluentIcons.Regular.FluentColorIcon,
                title = stringResource(R.string.settings_island_colors_title),
                subtitle = stringResource(R.string.settings_island_colors_desc),
                onClick = { onNavigate(SettingsPageType.COLOR) }
            )
            SettingIslandCard(
                icon = FluentIcons.Regular.Grid,
                title = stringResource(R.string.settings_island_layout_title),
                subtitle = stringResource(R.string.settings_island_layout_desc),
                onClick = { onNavigate(SettingsPageType.LAYOUT) }
            )
            SettingIslandCard(
                icon = FluentIcons.Regular.TapDouble,
                title = stringResource(R.string.settings_island_gestures_title),
                subtitle = stringResource(R.string.settings_island_gestures_desc),
                onClick = { onNavigate(SettingsPageType.GESTURES) }
            )
            SettingIslandCard(
                icon = FluentIcons.Regular.Settings,
                title = stringResource(R.string.settings_island_system_title),
                subtitle = stringResource(R.string.settings_island_system_desc),
                onClick = { onNavigate(SettingsPageType.SYSTEM_ADVANCED) }
            )
        }

        Spacer(Modifier.height(60.dp))
    }
}

@Composable
private fun MicaWashButton(
    text: String,
    onClick: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.96f else 1f, label = "scale")

    val textColor = if (isDark) Color.White else Color(0xFF1D1D1D)
    val borderColor = if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.08f)
    val washColor = if (isDark) Color(0xFF1E1E1E).copy(alpha = 0.55f) else Color.White.copy(alpha = 0.65f)

    val micaParams = LocalMicaParams.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(36.dp)
            .scale(scale)
            .shadow(2.dp, RoundedCornerShape(4.dp), spotColor = Color.Black.copy(alpha = 0.05f))
            .fluentMicaComplex(micaParams = micaParams, enabled = true, shape = RoundedCornerShape(4.dp))
            .background(washColor, RoundedCornerShape(4.dp))
            .border(1.dp, borderColor, RoundedCornerShape(4.dp))
            .clickable(interactionSource = interactionSource, indication = null) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, color = textColor, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun SettingIslandCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.96f else 1f, label = "scale")

    val textColor = if (isDark) Color.White else Color(0xFF1D1D1D)
    val subTextColor = if (isDark) Color.White.copy(alpha = 0.6f) else Color(0xFF5E5E5E)
    val borderColor = if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.08f)
    val washColor = if (isDark) Color(0xFF1E1E1E).copy(alpha = 0.55f) else Color.White.copy(alpha = 0.65f)

    val micaParams = LocalMicaParams.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(76.dp)
            .scale(scale)
            .shadow(2.dp, RoundedCornerShape(8.dp), spotColor = Color.Black.copy(alpha = 0.05f))
            .fluentMicaComplex(micaParams = micaParams, enabled = true, shape = RoundedCornerShape(8.dp))
            .background(washColor, RoundedCornerShape(8.dp))
            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
            .clickable(interactionSource = interactionSource, indication = null) { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = textColor, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(16.dp))

            Column(Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
                Text(text = title, color = textColor, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(2.dp))
                Text(text = subtitle, color = subTextColor, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }

            Icon(FluentIcons.Regular.ChevronRight, null, tint = textColor.copy(alpha = 0.4f), modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
fun WallpaperThemeBox(
    resId: Int,
    accentColor: Color,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    Box(
        modifier = modifier
            .aspectRatio(1.4f)
            .clip(RoundedCornerShape(8.dp))
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) accentColor else if (isDark) Color.White.copy(alpha = 0.15f) else Color.Black.copy(alpha = 0.1f),
                shape = RoundedCornerShape(8.dp)
            )
            .clickable { onClick() }
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(resId)
                .crossfade(true)
                .size(300)
                .build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(6.dp)
                .size(width = 36.dp, height = 24.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(if (isDark) Color(0xFF1E1E1E).copy(alpha = 0.85f) else Color.White.copy(alpha = 0.85f))
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 4.dp)
                    .size(width = 16.dp, height = 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(accentColor)
            )
        }
        if (isSelected) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(accentColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(FluentIcons.Regular.Checkmark, contentDescription = null, tint = Color.White, modifier = Modifier.size(10.dp))
            }
        }
    }
}

@Composable
private fun LayoutSwitcherIsland(
    settingsManager: SettingsManager,
    onLayoutChange: (String) -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val scope = rememberCoroutineScope()

    val currentLayout by settingsManager.launcherLayout.collectAsState("FLUENT_TILES")
    val isFluent = currentLayout == "FLUENT_TILES"

    val textColor = if (isDark) Color.White else Color(0xFF1D1D1D)
    val subTextColor = if (isDark) Color.White.copy(alpha = 0.6f) else Color(0xFF5E5E5E)
    val borderColor = if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.08f)
    val washColor = if (isDark) Color(0xFF1E1E1E).copy(alpha = 0.55f) else Color.White.copy(alpha = 0.65f)

    val micaParams = LocalMicaParams.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .shadow(2.dp, RoundedCornerShape(8.dp), spotColor = Color.Black.copy(alpha = 0.05f))
            .fluentMicaComplex(micaParams = micaParams, enabled = true, shape = RoundedCornerShape(8.dp))
            .background(washColor, RoundedCornerShape(8.dp))
            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Launcher Layout",
                color = textColor,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LayoutButton(
                    icon = FluentIcons.Regular.Grid,
                    label = "Fluent Tiles",
                    isSelected = isFluent,
                    isDark = isDark,
                    onClick = {
                        scope.launch {
                            settingsManager.setLauncherLayout("FLUENT_TILES")
                            onLayoutChange("FLUENT_TILES")
                        }
                    },
                    modifier = Modifier.weight(1f)
                )

                LayoutButton(
                    icon = FluentIcons.Regular.Apps,
                    label = "Classic",
                    isSelected = !isFluent,
                    isDark = isDark,
                    onClick = {
                        scope.launch {
                            settingsManager.setLauncherLayout("ONE_UI_CLASSIC")
                            onLayoutChange("ONE_UI_CLASSIC")
                        }
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun LayoutButton(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    isDark: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.94f else 1f, label = "scale")

    val bgColor = if (isSelected) {
        Color(0xFF0078D4).copy(alpha = 0.15f)
    } else {
        if (isDark) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.03f)
    }

    val borderColor = if (isSelected) {
        Color(0xFF0078D4)
    } else {
        if (isDark) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.1f)
    }

    val textColor = if (isDark) Color.White else Color(0xFF1D1D1D)

    Box(
        modifier = modifier
            .height(40.dp)
            .scale(scale)
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor, RoundedCornerShape(6.dp))
            .border(1.dp, borderColor, RoundedCornerShape(6.dp))
            .clickable(interactionSource = interactionSource, indication = null) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(icon, contentDescription = null, tint = textColor, modifier = Modifier.size(16.dp))
            Text(label, color = textColor, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        }
    }
}
