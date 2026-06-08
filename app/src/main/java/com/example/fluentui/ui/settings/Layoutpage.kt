/**
 * @file LayoutPage.kt
 * @description Settings page for visual layout properties.
 * Configures Home Grid dimensions, Icon sizes, and App Drawer behavior.
 * Fully integrates the Fluent Design System with localized resources.
 */

package com.example.fluentui.ui.settings

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.fluentui.R
import com.example.fluentui.ui.components.AppModel
import com.example.fluentui.ui.utils.IconCache
import fluent.ui.system.icons.FluentIcons
import fluent.ui.system.icons.regular.ChevronRight

@Composable
fun LayoutPage(
    state: SettingsState,
    allApps: List<AppModel>,
    onSetCols: (Int) -> Unit,
    onSetRows: (Int) -> Unit,
    onSetIconSize: (Float) -> Unit,
    onSetFontSize: (Float) -> Unit,
    onSetSpacing: (Float) -> Unit,
    onSetLabels: (Boolean) -> Unit,
    onSetVertical: (Boolean) -> Unit,
    onSetHidden: (String, Boolean) -> Unit,
    onSetShowDrawerSearch: (Boolean) -> Unit,
    onSetAutoKeyboard: (Boolean) -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val textColor = if (isDark) Color.White else Color(0xFF1D1D1D)
    val subColor = if (isDark) Color.White.copy(alpha = 0.6f) else Color(0xFF5E5E5E)
    val sectionLabelColor = if (isDark) Color.White.copy(alpha = 0.5f) else Color(0xFF707070)

    var showHiddenAppsDialog by remember { mutableStateOf(false) }
    var localIconSize by remember(state.iconSize) { mutableFloatStateOf(state.iconSize) }
    var localFontSize by remember(state.fontSize) { mutableFloatStateOf(state.fontSize) }

    if (showHiddenAppsDialog) {
        HiddenAppsDialog(
            allApps = allApps,
            hiddenApps = state.hiddenApps,
            accentColor = state.accentColor,
            onDismiss = { showHiddenAppsDialog = false },
            onToggleApp = onSetHidden
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {

        Text(
            text = stringResource(R.string.settings_layout_title),
            color = textColor,
            fontSize = 28.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = (-0.3).sp
        )

        // Preview Section
        Box(
            modifier = Modifier.fillMaxWidth().height(240.dp),
            contentAlignment = Alignment.Center
        ) {
            val fallback = if (state.wallpaperType == "SOLID")
                Color(state.wallpaperValue.toIntOrNull() ?: Color.Black.toArgb())
            else Color.Black

            PhonePreview(
                isVertical = state.isVertical,
                columns = state.columnCount,
                rows = state.rowCount,
                accentColor = state.accentColor,
                wallpaperModel = state.currentWallpaperBitmap,
                fallbackColor = fallback,
                showLabels = state.showLabels,
                iconScale = localIconSize / 60f,
                fontSizeScale = localFontSize / 13f,
                verticalSpacingScale = state.verticalSpacing / 16f
            )
        }

        // --- SECTION: DISPLAY MODE ---
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SectionLabel(stringResource(R.string.settings_section_display_mode), sectionLabelColor)
            FluentCard {
                Row(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                    DisplayModeItem(
                        label = stringResource(R.string.settings_mode_grid),
                        isSelected = !state.isVertical,
                        accentColor = state.accentColor,
                        icon = { GridIcon(active = !state.isVertical) },
                        onClick = { onSetVertical(false) },
                        modifier = Modifier.weight(1f)
                    )
                    DisplayModeItem(
                        label = stringResource(R.string.settings_mode_list),
                        isSelected = state.isVertical,
                        accentColor = state.accentColor,
                        icon = { ListIcon(active = state.isVertical) },
                        onClick = { onSetVertical(true) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // --- SECTION: DISPOSITION ---
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SectionLabel(stringResource(R.string.settings_section_disposition), sectionLabelColor)
            FluentCard {
                PickerRow(stringResource(R.string.settings_label_columns), listOf(3, 4, 5, 6), state.columnCount, state.accentColor, textColor, subColor, onSetCols)
                TransparentDivider()
                PickerRow(stringResource(R.string.settings_label_rows), listOf(4, 5, 6), state.rowCount, state.accentColor, textColor, subColor, onSetRows)
            }
        }

        // --- SECTION: SIZES ---
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SectionLabel(stringResource(R.string.settings_section_sizes), sectionLabelColor)
            FluentCard {
                SliderRow(
                    label = stringResource(R.string.settings_label_icons), value = localIconSize, range = 40f..80f, unit = "px", accentColor = state.accentColor,
                    textColor = textColor, subColor = subColor, onReset = { localIconSize = 60f; onSetIconSize(60f) },
                    onValueChange = { localIconSize = it }, onValueChangeFinished = { onSetIconSize(localIconSize) }
                )
                TransparentDivider()
                SliderRow(
                    label = stringResource(R.string.settings_label_text), value = localFontSize, range = 10f..20f, unit = "sp", accentColor = state.accentColor,
                    textColor = textColor, subColor = subColor, onReset = { localFontSize = 13f; onSetFontSize(13f) },
                    onValueChange = { localFontSize = it }, onValueChangeFinished = { onSetFontSize(localFontSize) }
                )
            }
        }

        // --- SECTION: VISIBILITY ---
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SectionLabel(stringResource(R.string.settings_section_visibility), sectionLabelColor)
            FluentCard {
                TransparentSwitchItem(
                    title = stringResource(R.string.settings_show_labels_title),
                    subtitle = stringResource(R.string.settings_show_labels_desc),
                    checked = state.showLabels,
                    onCheckedChange = onSetLabels,
                    accentColor = state.accentColor,
                    textColor = textColor,
                    subColor = subColor
                )
                TransparentDivider()
                // Hidden Apps Trigger
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { showHiddenAppsDialog = true }.padding(horizontal = 16.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.settings_hide_apps_title), color = textColor, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                        val hiddenCountText = if (state.hiddenApps.isEmpty()) stringResource(R.string.settings_hide_apps_none)
                        else stringResource(R.string.settings_hide_apps_count, state.hiddenApps.size)
                        Text(hiddenCountText, color = subColor, fontSize = 13.sp)
                    }
                    if (state.hiddenApps.isNotEmpty()) {
                        Box(modifier = Modifier.clip(CircleShape).background(state.accentColor.copy(alpha = 0.15f)).padding(horizontal = 8.dp, vertical = 3.dp)) {
                            Text("${state.hiddenApps.size}", color = state.accentColor, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                        Spacer(Modifier.width(8.dp))
                    }
                    Icon(FluentIcons.Regular.ChevronRight, null, tint = subColor.copy(alpha = 0.5f), modifier = Modifier.size(16.dp))
                }
            }
        }

        // --- SECTION: DRAWER ---
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SectionLabel(stringResource(R.string.settings_section_drawer), sectionLabelColor)
            FluentCard {
                TransparentSwitchItem(
                    title = stringResource(R.string.settings_drawer_search_title),
                    subtitle = stringResource(R.string.settings_drawer_search_desc),
                    checked = state.showDrawerSearch,
                    onCheckedChange = onSetShowDrawerSearch,
                    accentColor = state.accentColor,
                    textColor = textColor,
                    subColor = subColor
                )
                TransparentDivider()
                TransparentSwitchItem(
                    title = stringResource(R.string.settings_drawer_keyboard_title),
                    subtitle = stringResource(R.string.settings_drawer_keyboard_desc),
                    checked = state.autoOpenKeyboard,
                    onCheckedChange = onSetAutoKeyboard,
                    accentColor = state.accentColor,
                    textColor = if (state.showDrawerSearch) textColor else subColor,
                    subColor = subColor,
                    enabled = state.showDrawerSearch
                )
            }
        }

        Spacer(Modifier.height(40.dp))
    }
}

// --- SUB-COMPONENTS (Private API) ---

@Composable
private fun DisplayModeItem(
    label: String,
    isSelected: Boolean,
    accentColor: Color,
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(40.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(if (isSelected) accentColor else Color.Transparent)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            icon()
            Text(label, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = if (isSelected) Color.White else Color.Gray)
        }
    }
}

@Composable
private fun SectionLabel(text: String, color: Color) {
    Text(text.uppercase(), color = color, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp, modifier = Modifier.padding(start = 4.dp))
}

@Composable
private fun TransparentDivider() {
    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = if (isSystemInDarkTheme()) Color.White.copy(0.06f) else Color.Black.copy(0.06f))
}

@Composable
private fun TransparentSwitchItem(
    title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit,
    accentColor: Color, textColor: Color, subColor: Color, enabled: Boolean = true
) {
    Row(modifier = Modifier.fillMaxWidth().clickable(enabled = enabled) { onCheckedChange(!checked) }.padding(horizontal = 16.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, color = if (enabled) textColor else textColor.copy(alpha = 0.4f), fontSize = 16.sp, fontWeight = FontWeight.Medium)
            Text(subtitle, color = if (enabled) subColor else subColor.copy(alpha = 0.4f), fontSize = 13.sp)
        }
        WindowsSwitch(checked, onCheckedChange, enabled, accentColor)
    }
}

@Composable
private fun PickerRow(label: String, options: List<Int>, current: Int, accentColor: Color, textColor: Color, subColor: Color, onSelect: (Int) -> Unit) {
    val isDark = isSystemInDarkTheme()
    val idleBg = if (isDark) Color.White.copy(0.06f) else Color.Black.copy(0.04f)
    val borderColor = if (isDark) Color.White.copy(0.08f) else Color.Black.copy(0.08f)

    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = textColor, fontSize = 16.sp, fontWeight = FontWeight.Medium, modifier = Modifier.width(80.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            options.forEach { option ->
                val selected = current == option
                Box(
                    modifier = Modifier.weight(1f).height(36.dp).clip(RoundedCornerShape(6.dp))
                        .background(if (selected) accentColor else idleBg)
                        .then(if (!selected) Modifier.border(1.dp, borderColor, RoundedCornerShape(6.dp)) else Modifier)
                        .clickable { onSelect(option) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(option.toString(), fontSize = 14.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium, color = if (selected) Color.White else textColor)
                }
            }
        }
    }
}

@Composable
private fun SliderRow(label: String, value: Float, range: ClosedFloatingPointRange<Float>, unit: String, accentColor: Color, textColor: Color, subColor: Color, onReset: () -> Unit, onValueChange: (Float) -> Unit, onValueChangeFinished: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = textColor, fontSize = 16.sp, fontWeight = FontWeight.Medium)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(accentColor.copy(alpha = 0.15f)).padding(horizontal = 8.dp, vertical = 4.dp)) {
                    Text("${value.toInt()} $unit", color = accentColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                IconButton(onClick = onReset, modifier = Modifier.size(24.dp)) { Icon(Icons.Default.Refresh, null, tint = subColor, modifier = Modifier.size(16.dp)) }
            }
        }
        Spacer(Modifier.height(8.dp))
        WindowsSlider(value, onValueChange, onValueChangeFinished, range, accentColor)
    }
}

@Composable private fun GridIcon(active: Boolean) { val color = if (active) Color.White else Color.Gray; Box(modifier = Modifier.size(14.dp)) { Column(verticalArrangement = Arrangement.spacedBy(2.dp)) { repeat(2) { Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) { repeat(2) { Box(modifier = Modifier.size(6.dp).clip(RoundedCornerShape(1.dp)).background(color)) } } } } } }
@Composable private fun ListIcon(active: Boolean) { val color = if (active) Color.White else Color.Gray; Column(modifier = Modifier.size(width = 14.dp, height = 14.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) { repeat(3) { Box(modifier = Modifier.fillMaxWidth().height(2.dp).clip(RoundedCornerShape(1.dp)).background(color)) } } }

@Composable
fun HiddenAppsDialog(allApps: List<AppModel>, hiddenApps: Set<String>, accentColor: Color, onDismiss: () -> Unit, onToggleApp: (String, Boolean) -> Unit) {
    val isDark = isSystemInDarkTheme()
    val bgColor = if (isDark) Color(0xFF202020) else Color.White
    val textColor = if (isDark) Color.White else Color(0xFF1D1D1D)
    val borderColor = if (isDark) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.1f)

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(12.dp), color = bgColor, modifier = Modifier.fillMaxWidth().heightIn(max = 500.dp).border(1.dp, borderColor, RoundedCornerShape(12.dp))) {
            Column(Modifier.padding(20.dp)) {
                Text(stringResource(R.string.settings_hide_apps_title), color = textColor, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 16.dp))
                LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.weight(1f)) {
                    items(allApps) { app ->
                        val isHidden = hiddenApps.contains(app.packageName)
                        Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable { onToggleApp(app.packageName, !isHidden) }.padding(vertical = 8.dp, horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            val iconBitmap = remember(app.packageName) { IconCache.get(app.packageName) }
                            if (iconBitmap != null) { Image(bitmap = iconBitmap.asImageBitmap(), contentDescription = null, modifier = Modifier.size(36.dp).clip(RoundedCornerShape(8.dp))) } else { Box(Modifier.size(36.dp).clip(RoundedCornerShape(8.dp)).background(Color.Gray.copy(alpha = 0.2f))) }
                            Spacer(Modifier.width(16.dp))
                            Text(app.label, color = textColor, modifier = Modifier.weight(1f), fontSize = 15.sp, fontWeight = FontWeight.Medium)
                            Checkbox(checked = isHidden, onCheckedChange = { onToggleApp(app.packageName, it) }, colors = CheckboxDefaults.colors(checkedColor = accentColor))
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    WindowsButton(stringResource(R.string.btn_close), onClick = onDismiss)
                }
            }
        }
    }
}