package com.example.fluentui.ui.search

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fluent.ui.system.icons.FluentIcons
import fluent.ui.system.icons.color.SearchSparkle
import fluent.ui.system.icons.filled.Search
import fluent.ui.system.icons.regular.MoreVertical

// ==============================================================================
// FLUENT SEARCH PILL - WINDOWS 11 MICA EFFECT
// ==============================================================================

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun FluentSearchPill(
    searchQuery: String,
    onQueryChange: (String) -> Unit,
    onSettingsClick: () -> Unit,
    onFocusChange: (Boolean) -> Unit,
    isDark: Boolean
) {
    // Mica colors
    val baseMicaColor = if (isDark) Color(0xFF303030) else Color(0xFFF9F9F9)
    val startAlpha = if (isDark) 0.90f else 0.85f
    val endAlpha = if (isDark) 0.98f else 0.95f

    val micaBrush = Brush.linearGradient(
        colors = listOf(
            baseMicaColor.copy(alpha = startAlpha),
            baseMicaColor.copy(alpha = endAlpha)
        ),
        start = Offset(0f, 0f),
        end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
    )

    val contentColor = if (isDark) Color.White.copy(0.95f) else Color.Black.copy(0.85f)
    val borderColor = if (isDark) Color.White.copy(0.15f) else Color.Black.copy(0.08f)

    val isFocused = remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isFocused.value) 1.02f else 1f,
        label = "scale"
    )

    // Shimmer effect
    val infiniteTransition = rememberInfiniteTransition(label = "Shimmer")
    val shimmerTranslate by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Shimmer"
    )

    val textShimmerBrush = Brush.linearGradient(
        colors = listOf(
            contentColor.copy(0.5f),
            contentColor.copy(1f),
            contentColor.copy(0.5f)
        ),
        start = Offset(shimmerTranslate, 0f),
        end = Offset(shimmerTranslate + 200f, 200f)
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .scale(scale)
            .shadow(
                elevation = 16.dp,
                shape = CircleShape,
                spotColor = if (isDark) Color.Black.copy(0.5f) else Color.Black.copy(0.15f)
            )
            .clip(CircleShape)
            .background(micaBrush)
            .border(1.5.dp, borderColor, CircleShape)
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon animée
        AnimatedContent(
            targetState = isFocused.value || searchQuery.isNotEmpty(),
            label = "Icon"
        ) { active ->
            if (active) {
                Icon(
                    imageVector = FluentIcons.Color.SearchSparkle,
                    contentDescription = null,
                    tint = Color.Unspecified,
                    modifier = Modifier.size(26.dp)
                )
            } else {
                Icon(
                    imageVector = FluentIcons.Filled.Search,
                    contentDescription = null,
                    tint = contentColor.copy(0.8f),
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Text field
        Box(modifier = Modifier.weight(1f)) {
            if (searchQuery.isEmpty()) {
                androidx.compose.material3.Text(
                    text = "Rechercher...",
                    style = TextStyle(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 17.sp,
                        letterSpacing = 0.5.sp,
                        shadow = Shadow(
                            color = contentColor.copy(0.15f),
                            offset = Offset.Zero,
                            blurRadius = 8f
                        ),
                        brush = if (isFocused.value) textShimmerBrush
                        else SolidColor(contentColor.copy(0.6f))
                    )
                )
            }

            BasicTextField(
                value = searchQuery,
                onValueChange = onQueryChange,
                textStyle = TextStyle(
                    color = contentColor,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.2.sp
                ),
                singleLine = true,
                cursorBrush = SolidColor(contentColor),
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { focusState ->
                        isFocused.value = focusState.isFocused
                        onFocusChange(focusState.isFocused)
                    }
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Settings button
        IconButton(
            onClick = onSettingsClick,
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                imageVector = FluentIcons.Regular.MoreVertical,
                contentDescription = "Settings",
                tint = contentColor.copy(0.9f),
                modifier = Modifier.size(26.dp)
            )
        }
    }
}