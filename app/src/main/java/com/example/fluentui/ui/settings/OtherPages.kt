package com.example.fluentui.ui.settings

import android.app.WallpaperManager
import android.graphics.BitmapFactory
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.fluentui.R
import com.example.fluentui.ui.components.AppModel
import com.example.fluentui.ui.utils.IconCache
import fluent.ui.system.icons.FluentIcons
import fluent.ui.system.icons.regular.Color as FluentColorIcon
import fluent.ui.system.icons.regular.Grid
import fluent.ui.system.icons.regular.Image
import fluent.ui.system.icons.regular.SlideHide
import fluent.ui.system.icons.regular.Sparkle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

// =================================================================================
// HOME PAGE
// =================================================================================
@Composable
fun HomePage(
    state: SettingsState,
    onNavigate: (SettingsPageType) -> Unit,
    onSetAccent: (Color) -> Unit,
    onSetWallpaper: (String, String) -> Unit = { _, _ -> },
    onSetDarkMode: (Boolean) -> Unit = {}
) {
    val isDark = isSystemInDarkTheme()
    val titleColor = if (isDark) Color.White else Color(0xFF191919)
    val subtitleColor = if (isDark) Color.White.copy(0.7f) else Color(0xFF505050)

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

        Text(
            text = "Personnalisation",
            color = titleColor,
            fontSize = 28.sp,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(Modifier.height(20.dp))

        Row(
            modifier = Modifier.fillMaxWidth().height(240.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(modifier = Modifier.weight(0.45f).fillMaxHeight()) {
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
                modifier = Modifier.weight(0.55f).fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Thèmes rapides",
                    color = subtitleColor,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )

                // 4 thèmes : wallpaper + couleur accent associée
                val quickThemes = listOf(
                    Triple(R.drawable.windows_11_1_mattbirchler, Color(0xFF0078D4), false),
                    Triple(R.drawable.windows_11_2_mattbirchler, Color(0xFF881798), true),
                    Triple(R.drawable.windows_11_3_mattbirchler, Color(0xFFE81123), true),
                    Triple(R.drawable.windows_11_4_mattbirchler, Color(0xFF00B294), false)
                )

                val scope = rememberCoroutineScope()
                val context = LocalContext.current

                fun applyTheme(resId: Int, accent: Color, isDark: Boolean) {
                    onSetAccent(accent)
                    onSetWallpaper("RES", resId.toString())
                    // Change le thème dark/light système
                    onSetDarkMode(isDark)
                    scope.launch(Dispatchers.IO) {
                        try {
                            val bmp = BitmapFactory.decodeResource(context.resources, resId)
                            WallpaperManager.getInstance(context).setBitmap(bmp)
                        } catch (e: Exception) { e.printStackTrace() }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        quickThemes.take(2).forEach { (resId, accent, isDark) ->
                            val isSelected = state.wallpaperType == "RES"
                                    && state.wallpaperValue == resId.toString()
                                    && state.accentColor == accent
                            WallpaperThemeBox(
                                resId = resId,
                                accentColor = accent,
                                isSelected = isSelected,
                                modifier = Modifier.weight(1f)
                            ) { applyTheme(resId, accent, isDark) }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        quickThemes.drop(2).forEach { (resId, accent, isDark) ->
                            val isSelected = state.wallpaperType == "RES"
                                    && state.wallpaperValue == resId.toString()
                                    && state.accentColor == accent
                            WallpaperThemeBox(
                                resId = resId,
                                accentColor = accent,
                                isSelected = isSelected,
                                modifier = Modifier.weight(1f)
                            ) { applyTheme(resId, accent, isDark) }
                        }
                    }
                }

                WindowsButton(text = "Changer le fond", onClick = { onNavigate(SettingsPageType.BACKGROUND) })
            }
        }

        Spacer(Modifier.height(4.dp))

        FluentCard(padding = 4.dp) {
            Column {
                FluentSettingItem(
                    icon = FluentIcons.Regular.Image,
                    title = "Fond d'écran",
                    subtitle = "Image de bureau, système",
                    onClick = { onNavigate(SettingsPageType.BACKGROUND) }
                )
                FluentSettingItem(
                    icon = FluentIcons.Regular.FluentColorIcon,
                    title = "Couleur & Effets",
                    subtitle = "Transparence Mica, accents",
                    onClick = { onNavigate(SettingsPageType.COLOR) }
                )
                FluentSettingItem(
                    icon = FluentIcons.Regular.Grid,
                    title = "Thème & Grille",
                    subtitle = "Icônes, colonnes, disposition",
                    isLast = true,
                    onClick = { onNavigate(SettingsPageType.LAYOUT) }
                )
            }
        }
    }
}

// =================================================================================
// COLOR PAGE
// =================================================================================
@Composable
fun ColorPage(
    state: SettingsState,
    onSetAccent: (Color) -> Unit,
    onSetTransparency: (Boolean) -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val titleColor = if (isDark) Color.White else Color(0xFF191919)
    val textColor = if (isDark) Color.White else Color(0xFF1D1D1D)
    val subTextColor = if (isDark) Color.White.copy(0.7f) else Color(0xFF606060)

    val windowsColors = remember {
        listOf(
            Color(0xFFFFB900), Color(0xFFFF8C00), Color(0xFFF7630C), Color(0xFFCA5010), Color(0xFFDA3B01), Color(0xFFEF6950), Color(0xFFD13438), Color(0xFFFF4343),
            Color(0xFFE74856), Color(0xFFE81123), Color(0xFFEA005E), Color(0xFFC30052), Color(0xFFE3008C), Color(0xFFBF0077), Color(0xFFC239B3), Color(0xFF9A0089),
            Color(0xFF0078D7), Color(0xFF0063B1), Color(0xFF8E8CD8), Color(0xFF6B69D6), Color(0xFF8764B8), Color(0xFF744DA9), Color(0xFFB146C2), Color(0xFF881798),
            Color(0xFF0099BC), Color(0xFF2D7D9A), Color(0xFF00B7C3), Color(0xFF038387), Color(0xFF00B294), Color(0xFF018574), Color(0xFF00CC6A), Color(0xFF10893E),
            Color(0xFF7A7574), Color(0xFF5D5A58), Color(0xFF68768A), Color(0xFF515C6B), Color(0xFF567C73), Color(0xFF486860), Color(0xFF498205), Color(0xFF107C10)
        )
    }

    Column {
        Text(
            "Couleur & Effets",
            color = titleColor,
            fontSize = 24.sp,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(Modifier.height(20.dp))

        FluentCard {
            Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(FluentIcons.Regular.Sparkle, null, tint = textColor, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text("Effets de transparence", color = textColor, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    Text("Effet Mica matériel", color = subTextColor, fontSize = 12.sp)
                }
                WindowsSwitch(
                    checked = state.transparencyEnabled,
                    onCheckedChange = onSetTransparency,
                    enabled = true,
                    accentColor = state.accentColor
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        Text(
            "Couleurs Windows",
            color = titleColor,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
        )

        FluentCard(padding = 16.dp) {
            Column {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 44.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.heightIn(max = 400.dp)
                ) {
                    items(windowsColors) { color ->
                        WindowsColorSwatch(color = color, isSelected = color == state.accentColor, onClick = { onSetAccent(color) })
                    }
                }
            }
        }
    }
}

// =================================================================================
// BACKGROUND PAGE
// =================================================================================
@Composable
fun BackgroundPage(
    state: SettingsState,
    onSetWallpaper: (String, String) -> Unit,
    onOpenGallery: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isDark = isSystemInDarkTheme()
    val textColor = if (isDark) Color.White else Color(0xFF1D1D1D)
    val subTextColor = if (isDark) Color.White.copy(0.7f) else Color(0xFF606060)
    var isDropdownExpanded by remember { mutableStateOf(false) }

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

    fun applyToSystem(resId: Int) {
        scope.launch(Dispatchers.IO) {
            try {
                val bitmap = BitmapFactory.decodeResource(context.resources, resId)
                WallpaperManager.getInstance(context).setBitmap(bitmap)
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    fun applyColorToSystem(color: Int) {
        scope.launch(Dispatchers.IO) {
            try {
                val bitmap = android.graphics.Bitmap.createBitmap(1, 1, android.graphics.Bitmap.Config.ARGB_8888)
                bitmap.eraseColor(color)
                WallpaperManager.getInstance(context).setBitmap(bitmap)
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Text(
            "Arrière-plan",
            color = textColor,
            fontSize = 24.sp,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(Modifier.height(20.dp))

        Box(modifier = Modifier.fillMaxWidth().height(300.dp), contentAlignment = Alignment.Center) {
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

        FluentCard {
            Column(Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(FluentIcons.Regular.Image, null, tint = textColor, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("Personnaliser votre arrière-plan", color = textColor, fontWeight = FontWeight.Medium, fontSize = 15.sp)
                        Text("Images, couleurs unies ou diaporama", color = subTextColor, fontSize = 12.sp)
                    }
                }

                Spacer(Modifier.height(16.dp))

                Box {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = if (isDark) Color.White.copy(0.07f) else Color.Black.copy(0.05f),
                        border = BorderStroke(1.dp, if (isDark) Color.White.copy(0.05f) else Color.Black.copy(0.1f)),
                        modifier = Modifier.fillMaxWidth().height(36.dp).clickable { isDropdownExpanded = true }
                    ) {
                        Row(modifier = Modifier.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                            val label = when (state.wallpaperType) { "SOLID" -> "Couleur unie"; "URI" -> "Image personnalisée"; else -> "Image" }
                            Text(label, color = textColor, fontSize = 13.sp)
                            Icon(Icons.Default.ArrowDropDown, null, tint = subTextColor, modifier = Modifier.size(16.dp))
                        }
                    }

                    DropdownMenu(
                        expanded = isDropdownExpanded,
                        onDismissRequest = { isDropdownExpanded = false },
                        modifier = Modifier.background(if (isDark) Color(0xFF2C2C2C) else Color(0xFFF3F3F3)).width(200.dp)
                    ) {
                        DropdownMenuItem(text = { Text("Image", color = textColor) }, onClick = { isDropdownExpanded = false; if (state.wallpaperType != "RES" && myWallpapers.isNotEmpty()) onSetWallpaper("RES", myWallpapers[0].toString()) })
                        DropdownMenuItem(text = { Text("Couleur unie", color = textColor) }, onClick = { isDropdownExpanded = false; if (state.wallpaperType != "SOLID") onSetWallpaper("SOLID", Color.Black.toArgb().toString()) })
                        DropdownMenuItem(text = { Text("Parcourir les photos", color = textColor) }, onClick = { isDropdownExpanded = false; onOpenGallery() })
                    }
                }

                Spacer(Modifier.height(24.dp))

                if (state.wallpaperType == "RES" || state.wallpaperType == "URI") {
                    Text("Images récentes", color = textColor, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(12.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(myWallpapers) { resId ->
                            val isSelected = (state.wallpaperType == "RES" && state.wallpaperValue == resId.toString())
                            Box(modifier = Modifier.width(72.dp).height(144.dp).clip(RoundedCornerShape(4.dp)).border(if (isSelected) 3.dp else 0.dp, state.accentColor, RoundedCornerShape(4.dp)).clickable {
                                onSetWallpaper("RES", resId.toString())
                                applyToSystem(resId)
                            }) {
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
                    Text("Choisir une photo", color = textColor, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(12.dp))
                    WindowsButton(text = "Parcourir les photos") { onOpenGallery() }

                } else if (state.wallpaperType == "SOLID") {
                    Text("Couleurs récentes", color = textColor, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(12.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(solidColors) { color ->
                            val isSelected = (state.wallpaperValue == color.toArgb().toString())
                            Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(color)
                                .border(if (isSelected) 3.dp else 1.dp, if (isSelected) state.accentColor else Color.Gray.copy(0.3f), CircleShape)
                                .clickable { onSetWallpaper("SOLID", color.toArgb().toString()); applyColorToSystem(color.toArgb()) }
                            ) {
                                if (isSelected) Icon(Icons.Default.Check, null, tint = if (color.luminance() > 0.5) Color.Black else Color.White, modifier = Modifier.align(Alignment.Center))
                            }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(50.dp))
    }
}

// =================================================================================
// LAYOUT PAGE
// =================================================================================
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
    onSetHidden: (String, Boolean) -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val textColor = if (isDark) Color.White else Color(0xFF1D1D1D)
    var showHiddenAppsDialog by remember { mutableStateOf(false) }

    var localIconSize by remember(state.iconSize) { mutableFloatStateOf(state.iconSize) }
    var localFontSize by remember(state.fontSize) { mutableFloatStateOf(state.fontSize) }

    if (showHiddenAppsDialog) {
        HiddenAppsDialog(allApps, state.hiddenApps, state.accentColor, { showHiddenAppsDialog = false }, onSetHidden)
    }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Text(
            "Thème & Grille",
            color = textColor,
            fontSize = 24.sp,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(Modifier.height(4.dp)) // verticalArrangement spacedBy(20.dp) fait déjà le reste

        Box(modifier = Modifier.fillMaxWidth().height(260.dp), contentAlignment = Alignment.Center) {
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

        Text("Mode d'affichage", color = textColor, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 4.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SelectionCard(selected = !state.isVertical, title = "Grille", subtitle = "Pages", icon = FluentIcons.Regular.Grid, accentColor = state.accentColor, modifier = Modifier.weight(1f)) { onSetVertical(false) }
            SelectionCard(selected = state.isVertical, title = "Liste", subtitle = "A-Z", icon = Icons.AutoMirrored.Filled.List, accentColor = state.accentColor, modifier = Modifier.weight(1f)) { onSetVertical(true) }
        }

        FluentCard {
            Column(Modifier.padding(16.dp)) {
                val alpha by animateFloatAsState(if (state.isVertical) 0.4f else 1f, label = "alpha")
                Column(Modifier.alpha(alpha)) {
                    SettingsGridPicker("Colonnes", state.columnCount, listOf(3, 4, 5, 6), state.accentColor, !state.isVertical, onSetCols)
                    Spacer(Modifier.height(16.dp))
                    SettingsGridPicker("Lignes", state.rowCount, listOf(4, 5, 6, 7, 8), state.accentColor, !state.isVertical, onSetRows)
                }
                Spacer(Modifier.height(16.dp))
                HorizontalDivider(color = if (isDark) Color.White.copy(0.05f) else Color.Black.copy(0.05f))
                Spacer(Modifier.height(16.dp))

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Taille icônes: ${localIconSize.toInt()}", color = textColor)
                    IconButton({ localIconSize = 60f; onSetIconSize(60f) }, Modifier.size(24.dp)) {
                        Icon(Icons.Default.Refresh, null, tint = textColor.copy(0.6f))
                    }
                }
                WindowsSlider(value = localIconSize, onValueChange = { localIconSize = it }, onValueChangeFinished = { onSetIconSize(localIconSize) }, range = 40f..80f, accentColor = state.accentColor)

                Spacer(Modifier.height(12.dp))

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Taille texte: ${localFontSize.toInt()}", color = textColor)
                    IconButton({ localFontSize = 13f; onSetFontSize(13f) }, Modifier.size(24.dp)) {
                        Icon(Icons.Default.Refresh, null, tint = textColor.copy(0.6f))
                    }
                }
                WindowsSlider(value = localFontSize, onValueChange = { localFontSize = it }, onValueChangeFinished = { onSetFontSize(localFontSize) }, range = 10f..20f, accentColor = state.accentColor)

                Spacer(Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Afficher les noms", color = textColor, modifier = Modifier.weight(1f))
                    WindowsSwitch(state.showLabels, onSetLabels, true, state.accentColor)
                }
            }
        }

        FluentCard(padding = 4.dp) {
            FluentSettingItem(
                icon = FluentIcons.Regular.SlideHide,
                title = "Masquer les applications",
                subtitle = "${state.hiddenApps.size} masquée(s)",
                isLast = true,
                onClick = { showHiddenAppsDialog = true }
            )
        }
        Spacer(Modifier.height(50.dp))
    }
}

// =================================================================================
// WALLPAPER THEME BOX
// =================================================================================
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
                color = if (isSelected) accentColor
                else (if (isDark) Color.White.copy(0.15f) else Color.Black.copy(0.1f)),
                shape = RoundedCornerShape(8.dp)
            )
            .clickable { onClick() }
    ) {
        // Fond d'écran en miniature
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

        // Mini widget en bas à droite avec la couleur accent
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(6.dp)
                .size(width = 36.dp, height = 24.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(
                    if (isDark) Color(0xFF1E1E1E).copy(alpha = 0.85f)
                    else Color.White.copy(alpha = 0.85f)
                )
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

        // Checkmark si sélectionné
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
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(10.dp)
                )
            }
        }
    }
}

// =================================================================================
// HIDDEN APPS DIALOG
// =================================================================================
@Composable
fun HiddenAppsDialog(
    allApps: List<AppModel>,
    hiddenApps: Set<String>,
    accentColor: Color,
    onDismiss: () -> Unit,
    onToggleApp: (String, Boolean) -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val bgColor = if (isDark) Color(0xFF1F1F1F) else Color(0xFFF9F9F9)
    val textColor = if (isDark) Color.White else Color(0xFF1D1D1D)
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = bgColor,
            modifier = Modifier.fillMaxWidth().heightIn(max = 500.dp)
                .border(1.dp, if (isDark) Color.White.copy(0.1f) else Color.Black.copy(0.1f), RoundedCornerShape(12.dp))
        ) {
            Column(Modifier.padding(16.dp)) {
                Text("Masquer les applications", color = textColor, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(16.dp))
                androidx.compose.foundation.lazy.LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(allApps) { app ->
                        val isHidden = hiddenApps.contains(app.packageName)
                        Row(
                            Modifier.fillMaxWidth().clickable { onToggleApp(app.packageName, !isHidden) }.padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val iconBitmap = remember(app.packageName) { IconCache.get(app.packageName) }
                            if (iconBitmap != null) {
                                Image(bitmap = iconBitmap.asImageBitmap(), contentDescription = null, modifier = Modifier.size(32.dp))
                            } else {
                                Box(Modifier.size(32.dp).background(Color.Gray.copy(0.2f), RoundedCornerShape(8.dp)))
                            }
                            Spacer(Modifier.width(12.dp))
                            Text(app.label, color = textColor, modifier = Modifier.weight(1f), fontSize = 14.sp)
                            Checkbox(
                                checked = isHidden,
                                onCheckedChange = { onToggleApp(app.packageName, it) },
                                colors = CheckboxDefaults.colors(checkedColor = accentColor)
                            )
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    WindowsButton("Fermer", onClick = onDismiss)
                }
            }
        }
    }
}