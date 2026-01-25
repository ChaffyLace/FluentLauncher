package com.example.fluentui.ui.settings

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest

// --- 1. FLUENT CARD (Simplifié) ---
@Composable
fun FluentCard(
    modifier: Modifier = Modifier,
    padding: Dp = 0.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    val isDark = isSystemInDarkTheme()
    // Fond subtil en dégradé
    val backgroundBrush = if (isDark) {
        Brush.verticalGradient(listOf(Color(0xFF2D2D2D), Color(0xFF252525)))
    } else {
        Brush.verticalGradient(listOf(Color(0xFFFFFFFF), Color(0xFFF3F3F3)))
    }
    val borderColor = if (isDark) Color.White.copy(0.08f) else Color.Black.copy(0.08f)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(8.dp), spotColor = Color.Black.copy(0.05f))
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundBrush)
            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
            .padding(padding),
        content = content
    )
}

// --- 2. SETTING ITEM (Correction des carrés imbriqués) ---
@Composable
fun FluentSettingItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    isLast: Boolean = false,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // Animation simple de réduction (Scale)
    val scale by animateFloatAsState(if (isPressed) 0.96f else 1f, label = "scale")

    val isDark = isSystemInDarkTheme()
    val textColor = if (isDark) Color.White else Color(0xFF1D1D1D)
    val subTextColor = if (isDark) Color.White.copy(0.6f) else Color(0xFF5E5E5E)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale) // L'animation est sur tout le conteneur
            .padding(bottom = 1.dp) // Petit espacement
            .clip(RoundedCornerShape(4.dp))
            .clickable(interactionSource = interactionSource, indication = null) { onClick() }
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = textColor, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = textColor, fontSize = 16.sp, fontWeight = FontWeight.Medium)
            Text(subtitle, color = subTextColor, fontSize = 13.sp)
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = textColor.copy(alpha = 0.4f), modifier = Modifier.size(20.dp))
    }

    if (!isLast) {
        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = if(isDark) Color.White.copy(0.1f) else Color.Black.copy(0.1f))
    }
}

// --- 3. SELECTION CARD ---
@Composable
fun SelectionCard(
    selected: Boolean,
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.95f else 1f, label = "scale")

    val backgroundColor = if (selected) accentColor else (if (isDark) Color(0xFF353535) else Color(0xFFF7F7F7))
    val textColor = if (selected) Color.White else (if (isDark) Color.White else Color(0xFF1D1D1D))
    val subTextColor = if (selected) Color.White.copy(0.8f) else (if (isDark) Color.White.copy(0.6f) else Color(0xFF606060))
    val borderColor = if (!selected) (if (isDark) Color.White.copy(0.1f) else Color.Black.copy(0.1f)) else Color.Transparent

    // Correction : Une seule Surface propre
    Surface(
        modifier = modifier
            .height(72.dp)
            .scale(scale)
            .clickable(interactionSource = interactionSource, indication = null) { onClick() },
        shape = RoundedCornerShape(8.dp),
        color = backgroundColor,
        border = BorderStroke(1.dp, borderColor),
        shadowElevation = if(selected) 4.dp else 0.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = textColor, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(14.dp))
            Column(verticalArrangement = Arrangement.Center) {
                Text(title, color = textColor, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Text(subtitle, color = subTextColor, fontSize = 12.sp)
            }
            Spacer(Modifier.weight(1f))
            if (selected) {
                Box(Modifier.size(6.dp).background(Color.White, CircleShape))
            }
        }
    }
}

// --- 4. THEME PREVIEW ---
@Composable
fun ThemePreviewBox(color: Color, isSelected: Boolean, onClick: () -> Unit) {
    val isDark = isSystemInDarkTheme()
    val borderColor = if (isDark) Color.White.copy(0.2f) else Color.Black.copy(0.1f)

    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(color)
            .border(if (isSelected) 2.dp else 1.dp, if (isSelected) (if(isDark) Color.White else Color.Black) else borderColor, RoundedCornerShape(8.dp))
            .clickable { onClick() }
    ) {
        if (isSelected) Box(Modifier.align(Alignment.Center).size(8.dp).background(Color.White, CircleShape))
    }
}

// --- 5. WINDOWS BUTTON (Correction majeure ici) ---
@Composable
fun WindowsButton(text: String, onClick: () -> Unit) {
    val isDark = isSystemInDarkTheme()
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.95f else 1f, label = "scale")

    val bgColor = if (isDark) Color(0xFF353535) else Color(0xFFFFFFFF)
    val borderColor = if (isDark) Color.White.copy(0.1f) else Color(0xFFD0D0D0)
    val textColor = if (isDark) Color.White else Color(0xFF1D1D1D)

    // On utilise Surface qui gère tout (fond, bordure, forme) en une seule fois
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(36.dp)
            .scale(scale) // Animation
            .clickable(interactionSource = interactionSource, indication = null) { onClick() },
        shape = RoundedCornerShape(4.dp), // Coins arrondis standard Windows
        color = bgColor,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(text, fontSize = 13.sp, color = textColor, fontWeight = FontWeight.Medium)
        }
    }
}

// --- 6. SETTINGS GRID PICKER (Correction des carrés) ---
@Composable
fun SettingsGridPicker(label: String, current: Int, options: List<Int>, accentColor: Color, enabled: Boolean, onSelect: (Int) -> Unit) {
    val isDark = isSystemInDarkTheme()
    val textColor = if (enabled) (if (isDark) Color.White else Color(0xFF1D1D1D)) else (if (isDark) Color.White.copy(0.3f) else Color.Black.copy(0.3f))

    Column {
        Text(label, color = textColor, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            options.forEach { option ->
                val selected = current == option
                val bgColor = if (!enabled) Color.Transparent else if (selected) accentColor else if (isDark) Color(0xFF353535) else Color(0xFFF7F7F7)
                val txtColor = if (!enabled) textColor else if (selected) Color.White else textColor

                // Un simple Box cliquable avec coins arrondis, pas de superposition
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(32.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(bgColor)
                        .clickable(enabled = enabled) { onSelect(option) }
                        .then(if(!selected && enabled) Modifier.border(1.dp, if(isDark) Color.White.copy(0.05f) else Color.Black.copy(0.05f), RoundedCornerShape(4.dp)) else Modifier),
                    contentAlignment = Alignment.Center
                ) {
                    Text(option.toString(), color = txtColor, fontSize = 13.sp)
                }
            }
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WindowsSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: (() -> Unit)? = null,
    range: ClosedFloatingPointRange<Float>,
    accentColor: Color
) {
    val isDark = isSystemInDarkTheme()
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val thumbScale by animateFloatAsState(
        targetValue = if (isPressed) 1.3f else 1.0f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "Scale"
    )

    Slider(
        value = value,
        onValueChange = onValueChange,
        onValueChangeFinished = onValueChangeFinished,
        valueRange = range,
        interactionSource = interactionSource,
        thumb = {
            Surface(
                modifier = Modifier
                    .size(24.dp)
                    .scale(thumbScale),
                shape = CircleShape,
                shadowElevation = 4.dp,
                color = if (isDark) Color(0xFF454545) else Color.White,
                border = BorderStroke(1.dp, if (isDark) Color.White.copy(0.1f) else Color.Black.copy(0.1f))
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(accentColor))
                }
            }
        },
        track = { sliderState ->
            val fraction = (sliderState.value - range.start) / (range.endInclusive - range.start)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(if (isDark) Color.White.copy(0.2f) else Color.Black.copy(0.1f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(2.dp))
                        .background(accentColor)
                )
            }
        },
        modifier = Modifier.height(24.dp)
    )
}
@Composable
fun WindowsSwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit, enabled: Boolean = true, accentColor: Color) {
    val isDark = isSystemInDarkTheme()
    Row(verticalAlignment = Alignment.CenterVertically) {
        Switch(
            checked = checked, onCheckedChange = onCheckedChange, enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White, checkedTrackColor = accentColor, checkedBorderColor = Color.Transparent,
                uncheckedThumbColor = if (isDark) Color(0xFFCECECE) else Color(0xFF5A5A5A),
                uncheckedTrackColor = Color.Transparent,
                uncheckedBorderColor = if (isDark) Color.White.copy(0.4f) else Color.Black.copy(0.4f)
            ),
            modifier = Modifier.scale(0.9f)
        )
        Spacer(Modifier.width(8.dp))
        Text(text = if (checked) "Activé" else "Désactivé", fontSize = 12.sp, color = if (isDark) Color.White.copy(0.7f) else Color.Black.copy(0.6f))
    }
}

@Composable
fun WindowsColorSwatch(color: Color, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(color)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (isSelected) {
            val isLight = (color.red * 0.299 + color.green * 0.587 + color.blue * 0.114) > 0.5f
            val checkColor = if (isLight) Color.Black else Color.White
            Icon(Icons.Default.Check, null, tint = checkColor, modifier = Modifier.size(24.dp))
            Box(Modifier.fillMaxSize().border(2.dp, checkColor.copy(alpha = 0.3f), RoundedCornerShape(4.dp)))
        }
    }
}

// --- PREVIEW AVEC FIX MEMOIRE ---
@Composable
fun PhonePreview(
    isVertical: Boolean,
    columns: Int,
    rows: Int,
    accentColor: Color,
    wallpaperModel: Any?,
    fallbackColor: Color,
    showLabels: Boolean = true,
    iconScale: Float = 1f,
    fontSizeScale: Float = 1f,
    verticalSpacingScale: Float = 1f,
    showIcons: Boolean = true
) {
    val previewHeight = 240.dp
    val previewWidth = previewHeight * 0.48f

    Box(modifier = Modifier.width(previewWidth).height(previewHeight).shadow(10.dp, RoundedCornerShape(24.dp)).clip(RoundedCornerShape(24.dp)).background(Color.Black).padding(4.dp)) {
        Box(modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(20.dp)).background(fallbackColor)) {

            if (wallpaperModel is ImageBitmap) {
                Image(bitmap = wallpaperModel, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop, alpha = 0.9f)
            } else if (wallpaperModel != null) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current).data(wallpaperModel).crossfade(true).size(600).build(),
                    contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop, alpha = 0.9f
                )
            } else {
                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(fallbackColor.copy(0.8f), fallbackColor.copy(0.4f)))))
            }

            Box(Modifier.align(Alignment.TopCenter).padding(top = 8.dp).size(8.dp).clip(CircleShape).background(Color.Black))

            if (showIcons) {
                Column(modifier = Modifier.fillMaxSize().padding(top = 28.dp, bottom = 12.dp, start = 8.dp, end = 8.dp), verticalArrangement = if (isVertical) Arrangement.Top else Arrangement.SpaceEvenly) {
                    if (isVertical) {
                        repeat(5) { Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = (4.dp * verticalSpacingScale).coerceAtLeast(2.dp)).fillMaxWidth()) { Box(Modifier.size(18.dp * iconScale).clip(RoundedCornerShape(6.dp)).background(accentColor)); Spacer(Modifier.width(6.dp)); Box(Modifier.height(3.dp * fontSizeScale).width(40.dp).clip(CircleShape).background(Color.White.copy(0.6f))) } }
                    } else {
                        repeat(rows) { Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { repeat(columns) { Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) { Box(Modifier.size(16.dp * iconScale).clip(RoundedCornerShape(5.dp)).background(accentColor.copy(alpha = 0.9f))); if (showLabels) { Spacer(Modifier.height(2.dp * verticalSpacingScale)); Box(Modifier.height(2.dp * fontSizeScale).width(12.dp * iconScale).background(Color.White.copy(0.5f), CircleShape)) } } } }; if (it < rows - 1) Spacer(Modifier.height(2.dp * verticalSpacingScale)) }
                    }
                }
            }
        }
    }
}