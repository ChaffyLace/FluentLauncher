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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    val micaParams by MicaState.micaParams   // ← était Wallpaperrepository.micaParams

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

    val isFocused = remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isFocused.value) 1.02f else 1f,
        label = "scale"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "Shimmer")
    val shimmerTranslate by if (isTransparencyEnabled) {
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1000f,
            animationSpec = infiniteRepeatable(
                animation = tween(2500, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "ShimmerTranslate"
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

    val windowsShape = CircleShape

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .scale(scale)
            .shadow(
                elevation = 4.dp,
                shape = windowsShape,
                spotColor = if (isDark) Color.Black else Color.Black.copy(0.15f)
            )
            .clip(windowsShape)
            .border(1.dp, borderColor, windowsShape)
    ) {
        // LAYER 1 : Base Mica
        Box(Modifier.fillMaxSize().background(micaBrush))

        // LAYER 2 : Grain (Noise)
        val noiseBrush = micaParams?.noiseBrush
        val noiseAlpha = micaParams?.noiseAlpha ?: 0.05f
        if (isTransparencyEnabled && noiseBrush != null) {
            Box(
                Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = noiseAlpha }
                    .background(noiseBrush)
            )
        }

        // LAYER 3 : Contenu
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AnimatedContent(
                targetState = isFocused.value || searchQuery.isNotEmpty(),
                label = "Icon"
            ) { active ->
                if (active) {
                    Icon(
                        imageVector = FluentIcons.Color.SearchSparkle,
                        contentDescription = null,
                        tint = Color.Unspecified,
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Icon(
                        imageVector = FluentIcons.Filled.Search,
                        contentDescription = null,
                        tint = contentColor.copy(0.7f),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Box(modifier = Modifier.weight(1f)) {
                if (searchQuery.isEmpty()) {
                    androidx.compose.material3.Text(
                        text = "Rechercher",
                        style = TextStyle(
                            fontWeight = FontWeight.Medium,
                            fontSize = 16.sp,
                            letterSpacing = 0.2.sp,
                            brush = if (isFocused.value && isTransparencyEnabled) textShimmerBrush
                            else SolidColor(contentColor.copy(0.6f))
                        )
                    )
                }

                BasicTextField(
                    value = searchQuery,
                    onValueChange = onQueryChange,
                    textStyle = TextStyle(
                        color = contentColor,
                        fontSize = 16.sp,
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

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = onSettingsClick,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = FluentIcons.Regular.MoreVertical,
                    contentDescription = "Settings",
                    tint = contentColor.copy(0.7f),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}