/**
 * @file FluentSearchPill.kt
 * @description Windows 11 style search bar with Mica effects and
 * localized strings for production readiness.
 */

package com.example.fluentui.ui.search

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource // Import indispensable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fluentui.R // Assure-toi d'importer ton package R
import com.example.fluentui.wallpaper.MicaState
import fluent.ui.system.icons.FluentIcons
import fluent.ui.system.icons.color.SearchSparkle
import fluent.ui.system.icons.filled.Search
import fluent.ui.system.icons.regular.MoreVertical

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun FluentSearchPill(
    searchQuery: String,
    onQueryChange: (String) -> Unit,
    onSettingsClick: () -> Unit,
    onFocusChange: (Boolean) -> Unit,
    isDark: Boolean,
    isTransparencyEnabled: Boolean = true
) {
    val micaParams by MicaState.micaParams

    // UI Colors & Brushes
    val baseMicaColor = if (isDark) Color(0xFF202020) else Color(0xFFFAFAFA)
    val micaAlpha = if (isTransparencyEnabled) 0.85f else 1f

    val micaBrush = Brush.linearGradient(
        colors = listOf(
            baseMicaColor.copy(alpha = micaAlpha),
            if (isDark) Color(0xFF181818).copy(alpha = micaAlpha) else Color(0xFFF0F0F0).copy(alpha = micaAlpha)
        ),
        start = Offset(0f, 0f),
        end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
    )

    val contentColor = if (isDark) Color.White.copy(0.90f) else Color.Black.copy(0.80f)
    val borderColor  = if (isDark) Color.White.copy(0.10f) else Color.Black.copy(0.06f)

    // Animation States
    val isFocused = remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isFocused.value) 1.02f else 1f,
        label = "pill_scale"
    )

    // Shimmer effect on text
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val shimmerTranslate by if (isTransparencyEnabled) {
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1000f,
            animationSpec = infiniteRepeatable(
                animation = tween(2500, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "shimmer_pos"
        )
    } else {
        remember { mutableStateOf(0f) }
    }

    val textShimmerBrush = Brush.linearGradient(
        colors = listOf(
            contentColor.copy(0.4f),
            contentColor.copy(0.9f),
            contentColor.copy(0.4f)
        ),
        start = Offset(shimmerTranslate, 0f),
        end = Offset(shimmerTranslate + 200f, 200f)
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .scale(scale)
            .shadow(4.dp, CircleShape)
            .clip(CircleShape)
            .border(1.dp, borderColor, CircleShape)
    ) {
        // Mica background & Noise
        Box(Modifier.fillMaxSize().background(micaBrush))
        micaParams?.noiseBrush?.let { safeNoiseBrush ->
            if (isTransparencyEnabled) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .graphicsLayer { alpha = micaParams?.noiseAlpha ?: 0.05f }
                        .background(safeNoiseBrush)
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon switching logic
            AnimatedContent(targetState = isFocused.value || searchQuery.isNotEmpty()) { active ->
                Icon(
                    imageVector = if (active) FluentIcons.Color.SearchSparkle else FluentIcons.Filled.Search,
                    contentDescription = null,
                    tint = if (active) Color.Unspecified else contentColor.copy(0.7f),
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Box(modifier = Modifier.weight(1f)) {
                if (searchQuery.isEmpty()) {
                    Text(
                        // TRADUCTION ICI
                        text = stringResource(id = R.string.search_placeholder),
                        style = TextStyle(
                            fontWeight = FontWeight.Medium,
                            fontSize = 16.sp,
                            brush = if (isFocused.value && isTransparencyEnabled) textShimmerBrush
                            else SolidColor(contentColor.copy(0.6f))
                        )
                    )
                }

                BasicTextField(
                    value = searchQuery,
                    onValueChange = onQueryChange,
                    textStyle = TextStyle(color = contentColor, fontSize = 16.sp, fontWeight = FontWeight.Medium),
                    singleLine = true,
                    cursorBrush = SolidColor(contentColor),
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged {
                            isFocused.value = it.isFocused
                            onFocusChange(it.isFocused)
                        }
                )
            }

            // Settings Button with localized content description for accessibility
            IconButton(onClick = onSettingsClick) {
                Icon(
                    imageVector = FluentIcons.Regular.MoreVertical,
                    contentDescription = stringResource(id = R.string.search_settings_description),
                    tint = contentColor.copy(0.7f),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}