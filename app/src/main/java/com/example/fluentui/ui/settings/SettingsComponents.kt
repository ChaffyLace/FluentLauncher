/**
 * @file SettingsComponents.kt
 * @description Reusable Fluent UI components for the settings module.
 * Integrates global Mica rendering via CompositionLocal for native glass materials.
 * Strictly implements Windows 11 Acrylic/Mica layering to avoid "double-glass" muddiness.
 */

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
import com.example.fluentui.wallpaper.fluentMicaComplex
import fluent.ui.system.icons.FluentIcons
import fluent.ui.system.icons.regular.ChevronRight

@Composable
fun FluentCard(
    modifier: Modifier = Modifier,
    padding: Dp = 0.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val micaParams = LocalMicaParams.current

    val borderColor = if (isDark) Color.White.copy(0.08f) else Color.Black.copy(0.08f)
    // The "Wash" layer ensures text readability while letting Mica blur shine through
    val washColor = if (isDark) Color(0xFF1E1E1E).copy(alpha = 0.55f) else Color.White.copy(alpha = 0.65f)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(8.dp), spotColor = Color.Black.copy(0.05f))
            .fluentMicaComplex(micaParams = micaParams, enabled = true, shape = RoundedCornerShape(8.dp))
            .background(washColor, RoundedCornerShape(8.dp))
            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
            .padding(padding),
        content = content
    )
}

// =============================================================================
// FLUENT SETTING ITEM (Transparent inner item)
// =============================================================================
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
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "scale"
    )

    val isDark = isSystemInDarkTheme()
    val textColor = if (isDark) Color.White else Color(0xFF1D1D1D)
    val subTextColor = if (isDark) Color.White.copy(0.6f) else Color(0xFF5E5E5E)

    // Note: No background modifier here! It relies entirely on the parent FluentCard
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clickable(interactionSource = interactionSource, indication = LocalIndication.current) { onClick() }
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = textColor.copy(alpha = 0.9f), modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(16.dp))

        Column(Modifier.weight(1f)) {
            Text(title, color = textColor, fontSize = 16.sp, fontWeight = FontWeight.Medium)
            if (subtitle.isNotEmpty()) {
                Text(subtitle, color = subTextColor, fontSize = 13.sp)
            }
        }

        Icon(FluentIcons.Regular.ChevronRight, null, tint = textColor.copy(alpha = 0.4f), modifier = Modifier.size(20.dp))
    }

    if (!isLast) {
        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 48.dp, vertical = 0.dp), // Indented divider like Windows
            thickness = 0.5.dp,
            color = if (isDark) Color.White.copy(0.06f) else Color.Black.copy(0.06f)
        )
    }
}

// =============================================================================
// WINDOWS BUTTON
// =============================================================================
@Composable
fun WindowsButton(text: String, onClick: () -> Unit) {
    val isDark = isSystemInDarkTheme()
    val micaParams = LocalMicaParams.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.96f else 1f, label = "scale")

    val washColor = if (isDark) Color(0xFF1E1E1E).copy(alpha = 0.55f) else Color.White.copy(alpha = 0.65f)
    val borderColor = if (isDark) Color.White.copy(0.1f) else Color(0xFFD0D0D0)
    val textColor = if (isDark) Color.White else Color(0xFF1D1D1D)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(36.dp)
            .scale(scale)
            .shadow(2.dp, RoundedCornerShape(4.dp), spotColor = Color.Black.copy(0.05f))
            .fluentMicaComplex(micaParams = micaParams, enabled = true, shape = RoundedCornerShape(4.dp))
            .background(washColor, RoundedCornerShape(4.dp))
            .border(1.dp, borderColor, RoundedCornerShape(4.dp))
            .clickable(interactionSource = interactionSource, indication = null) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(text, fontSize = 13.sp, color = textColor, fontWeight = FontWeight.Medium)
    }
}

// =============================================================================
// CONTROLS (Slider, Switch, Colors)
// =============================================================================
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
                modifier = Modifier.size(24.dp).scale(thumbScale),
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
                Box(modifier = Modifier.fillMaxWidth(fraction).fillMaxHeight().clip(RoundedCornerShape(2.dp)).background(accentColor))
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
        modifier = Modifier.size(48.dp).clip(RoundedCornerShape(4.dp)).background(color).clickable { onClick() },
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

// =============================================================================
// PHONE PREVIEW COMPONENT
// =============================================================================
@Composable
fun PhonePreview(
    isVertical: Boolean, columns: Int, rows: Int, accentColor: Color,
    wallpaperModel: Any?, fallbackColor: Color, showLabels: Boolean = true,
    iconScale: Float = 1f, fontSizeScale: Float = 1f, verticalSpacingScale: Float = 1f,
    showIcons: Boolean = true
) {
    val previewHeight = 240.dp
    val previewWidth = previewHeight * 0.48f

    Box(modifier = Modifier.width(previewWidth).height(previewHeight).shadow(10.dp, RoundedCornerShape(24.dp)).clip(RoundedCornerShape(24.dp)).background(Color.Black).padding(4.dp)) {
        Box(modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(20.dp)).background(fallbackColor)) {
            if (wallpaperModel is ImageBitmap) {
                Image(bitmap = wallpaperModel, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop, alpha = 0.9f)
            } else if (wallpaperModel != null) {
                AsyncImage(model = ImageRequest.Builder(LocalContext.current).data(wallpaperModel).crossfade(true).size(600).build(), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop, alpha = 0.9f)
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