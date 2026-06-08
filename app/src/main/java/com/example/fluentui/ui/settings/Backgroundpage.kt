/**
 * @file BackgroundPage.kt
 * @description Windows 11 Personalization Settings - Background Selection.
 * Implements real-time preview and wallpaper application (System-wide).
 * Engineering Note: Optimized for Dark/Light mode and localized.
 */

package com.example.fluentui.ui.settings

import android.app.WallpaperManager
import android.graphics.BitmapFactory
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.fluentui.R
import fluent.ui.system.icons.FluentIcons
import fluent.ui.system.icons.regular.Image
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun BackgroundPage(
    state: SettingsState,
    onSetWallpaper: (String, String) -> Unit,
    onOpenGallery: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isDark = isSystemInDarkTheme()

    // UI Constants based on Fluent Design
    val textColor = if (isDark) Color.White else Color(0xFF1D1D1D)
    val subTextColor = if (isDark) Color.White.copy(alpha = 0.7f) else Color(0xFF606060)
    var isDropdownExpanded by remember { mutableStateOf(false) }

    // Built-in high-quality wallpapers
    val myWallpapers = remember {
        listOf(
            R.drawable.windows_11_1_mattbirchler, R.drawable.windows_11_2_mattbirchler,
            R.drawable.windows_11_3_mattbirchler, R.drawable.windows_11_4_mattbirchler,
            R.drawable.windows_11_5_mattbirchler, R.drawable.windows_11_6_mattbirchler,
            R.drawable.windows_11_7_mattbirchler, R.drawable.windows_11_8_mattbirchler,
            R.drawable.windows_11_9_mattbirchler, R.drawable.windows_11_10_mattbirchler,
            R.drawable.windows_11_11_mattbirchler, R.drawable.windows_11_12_mattbirchler,
            R.drawable.windows_11_15_mattbirchler, R.drawable.windows_11_16_mattbirchler,
            R.drawable.windows_11_17_mattbirchler, R.drawable.windows_11_18_mattbirchler,
        )
    }

    val solidColors = remember {
        listOf(
            Color(0xFF000000), Color(0xFF1F1F1F), Color(0xFF0078D4),
            Color(0xFF00B294), Color(0xFFE81123), Color(0xFFFFB900),
            Color(0xFF5D5A58), Color(0xFF881798), Color(0xFF4C4A48)
        )
    }

    // Wallpaper Application Logic (IO Thread)
    val wallpaperManager = remember { WallpaperManager.getInstance(context) }

    fun applyToSystem(resId: Int) {
        scope.launch(Dispatchers.IO) {
            try {
                val bitmap = BitmapFactory.decodeResource(context.resources, resId)
                wallpaperManager.setBitmap(bitmap)
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    fun applyColorToSystem(color: Int) {
        scope.launch(Dispatchers.IO) {
            try {
                val bitmap = android.graphics.Bitmap.createBitmap(1, 1, android.graphics.Bitmap.Config.ARGB_8888)
                bitmap.eraseColor(color)
                wallpaperManager.setBitmap(bitmap)
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {

        Text(
            text = stringResource(R.string.settings_background_title),
            color = textColor,
            fontSize = 24.sp,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(Modifier.height(20.dp))

        // --- PREVIEW SECTION ---
        Box(
            modifier = Modifier.fillMaxWidth().height(300.dp),
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
                iconScale = state.iconSize / 60f,
                fontSizeScale = state.fontSize / 13f,
                verticalSpacingScale = state.verticalSpacing / 16f
            )
        }

        Spacer(Modifier.height(20.dp))

        // --- CONTROLS SECTION ---
        FluentCard {
            Column(Modifier.padding(20.dp)) {

                // Header
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = FluentIcons.Regular.Image,
                        contentDescription = null,
                        tint = textColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            text = stringResource(R.string.settings_background_customize),
                            color = textColor,
                            fontWeight = FontWeight.Medium,
                            fontSize = 15.sp
                        )
                        Text(
                            text = stringResource(R.string.settings_background_desc),
                            color = subTextColor,
                            fontSize = 12.sp
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Selector Dropdown
                Box {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = if (isDark) Color.White.copy(0.07f) else Color.Black.copy(0.05f),
                        border = BorderStroke(1.dp, if (isDark) Color.White.copy(0.05f) else Color.Black.copy(0.1f)),
                        modifier = Modifier.fillMaxWidth().height(36.dp).clickable { isDropdownExpanded = true }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            val label = when (state.wallpaperType) {
                                "SOLID" -> stringResource(R.string.bg_type_solid)
                                "URI"   -> stringResource(R.string.bg_type_custom)
                                else    -> stringResource(R.string.bg_type_image)
                            }
                            Text(label, color = textColor, fontSize = 13.sp)
                            Icon(Icons.Default.ArrowDropDown, null, tint = subTextColor, modifier = Modifier.size(16.dp))
                        }
                    }

                    DropdownMenu(
                        expanded = isDropdownExpanded,
                        onDismissRequest = { isDropdownExpanded = false },
                        modifier = Modifier.background(if (isDark) Color(0xFF2C2C2C) else Color(0xFFF3F3F3)).width(200.dp)
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.bg_type_image), color = textColor) },
                            onClick = {
                                isDropdownExpanded = false
                                if (state.wallpaperType != "RES" && myWallpapers.isNotEmpty())
                                    onSetWallpaper("RES", myWallpapers[0].toString())
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.bg_type_solid), color = textColor) },
                            onClick = {
                                isDropdownExpanded = false
                                if (state.wallpaperType != "SOLID")
                                    onSetWallpaper("SOLID", Color.Black.toArgb().toString())
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.bg_action_browse), color = textColor) },
                            onClick = { isDropdownExpanded = false; onOpenGallery() }
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))

                // --- CONTENT BASED ON TYPE ---
                when (state.wallpaperType) {
                    "RES", "URI" -> {
                        Text(stringResource(R.string.bg_recent_images), color = textColor, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        Spacer(Modifier.height(12.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(myWallpapers) { resId ->
                                val isSelected = state.wallpaperType == "RES" && state.wallpaperValue == resId.toString()
                                Box(
                                    modifier = Modifier
                                        .width(72.dp)
                                        .height(144.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .border(if (isSelected) 3.dp else 0.dp, state.accentColor, RoundedCornerShape(4.dp))
                                        .clickable {
                                            onSetWallpaper("RES", resId.toString())
                                            applyToSystem(resId)
                                        }
                                ) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(LocalContext.current).data(resId).crossfade(true).build(),
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    if (isSelected) {
                                        Box(Modifier.fillMaxSize().background(Color.Black.copy(0.3f)))
                                        Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.align(Alignment.Center))
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(24.dp))
                        Text(stringResource(R.string.bg_choose_photo), color = textColor, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        Spacer(Modifier.height(12.dp))
                        WindowsButton(text = stringResource(R.string.bg_action_browse)) { onOpenGallery() }
                    }

                    "SOLID" -> {
                        Text(stringResource(R.string.bg_recent_colors), color = textColor, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        Spacer(Modifier.height(12.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(solidColors) { color ->
                                val isSelected = state.wallpaperValue == color.toArgb().toString()
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                        .border(if (isSelected) 3.dp else 1.dp, if (isSelected) state.accentColor else Color.Gray.copy(0.3f), CircleShape)
                                        .clickable {
                                            onSetWallpaper("SOLID", color.toArgb().toString())
                                            applyColorToSystem(color.toArgb())
                                        }
                                ) {
                                    if (isSelected) Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = if (color.luminance() > 0.5) Color.Black else Color.White,
                                        modifier = Modifier.align(Alignment.Center)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(50.dp))
    }
}