package com.example.fluentui.ui.homescreen.widgets

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fluentui.ui.system.TileConfig
import com.example.fluentui.ui.system.TileSize
import fluent.ui.system.icons.FluentIcons
import fluent.ui.system.icons.filled.Pause
import fluent.ui.system.icons.filled.Play
import fluent.ui.system.icons.filled.Square
import fluent.ui.system.icons.regular.Timer
import kotlinx.coroutines.delay

/**
 * Premium Timer Widget.
 * Features a dynamic responsive layout with pixel-perfect centering for large sizes.
 */
@Composable
fun TimerWidget(config: TileConfig, contentColor: Color, accentColor: Color) {
    // State management for the timer
    var totalSeconds by remember { mutableIntStateOf(300) } // Default 5 minutes
    var remainingSeconds by remember { mutableIntStateOf(300) }
    var isRunning by remember { mutableStateOf(false) }

    // Coroutine ticker for precise countdown
    LaunchedEffect(isRunning) {
        while (isRunning && remainingSeconds > 0) {
            delay(1000)
            remainingSeconds--
            if (remainingSeconds <= 0) {
                isRunning = false
            }
        }
    }

    val padding = if (config.size == TileSize.SMALL) 12.dp else 16.dp
    val progress = if (totalSeconds > 0) 1f - (remainingSeconds.toFloat() / totalSeconds) else 0f

    Box(modifier = Modifier.fillMaxSize().padding(padding)) {

        // --- HEADER LOGIC ---
        if (config.size != TileSize.TINY) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.align(Alignment.TopStart)
            ) {
                Icon(
                    imageVector = FluentIcons.Regular.Timer,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = contentColor
                )
                if (config.size != TileSize.SMALL) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Minuteur",
                        color = contentColor,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        } else {
            Icon(
                imageVector = FluentIcons.Regular.Timer,
                contentDescription = null,
                modifier = Modifier.size(28.dp).align(Alignment.Center),
                tint = contentColor
            )
        }

        // --- RESPONSIVE LAYOUTS ---
        when (config.size) {
            TileSize.TINY -> { /* Handled in header */ }

            TileSize.SMALL -> {
                Text(
                    text = formatTime(remainingSeconds),
                    color = contentColor,
                    fontSize = 24.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.align(Alignment.BottomStart)
                )
            }

            TileSize.MEDIUM -> {
                // MEDIUM: Compact Circular UI
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    TimerRing(progress, remainingSeconds, 90.dp, accentColor, contentColor)

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = formatTime(remainingSeconds),
                            color = contentColor,
                            fontSize = 22.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        TimerControls(
                            isRunning = isRunning,
                            remainingSeconds = remainingSeconds,
                            totalSeconds = totalSeconds,
                            accentColor = accentColor,
                            contentColor = contentColor,
                            buttonSize = 36.dp,
                            iconSize = 16.dp,
                            onToggle = { isRunning = !isRunning },
                            onReset = { remainingSeconds = totalSeconds; isRunning = false }
                        )
                    }
                }
            }

            TileSize.LARGE -> {
                // LARGE: Expanded Circular UI with perfect centering and larger proportions
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    TimerRing(progress, remainingSeconds, 160.dp, accentColor, contentColor)

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = formatTimeLarge(remainingSeconds),
                            color = contentColor,
                            fontSize = 42.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Light
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        TimerControls(
                            isRunning = isRunning,
                            remainingSeconds = remainingSeconds,
                            totalSeconds = totalSeconds,
                            accentColor = accentColor,
                            contentColor = contentColor,
                            buttonSize = 56.dp,
                            iconSize = 24.dp,
                            onToggle = { isRunning = !isRunning },
                            onReset = { remainingSeconds = totalSeconds; isRunning = false }
                        )
                    }
                }
            }
        }
    }
}

// --- REUSABLE UI COMPONENTS ---

@Composable
private fun TimerRing(progress: Float, remainingSeconds: Int, size: androidx.compose.ui.unit.Dp, accentColor: Color, contentColor: Color) {
    Canvas(modifier = Modifier.size(size)) {
        val stroke = this.size.width * 0.06f // Sleek stroke width
        // Background track
        drawArc(
            color = contentColor.copy(alpha = 0.1f),
            startAngle = 0f, sweepAngle = 360f, useCenter = false,
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = stroke)
        )
        // Active progress
        drawArc(
            color = if (remainingSeconds == 0) Color(0xFFFF4343) else accentColor,
            startAngle = -90f, sweepAngle = progress * 360f, useCenter = false,
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = stroke, cap = StrokeCap.Round)
        )
    }
}

@Composable
private fun TimerControls(
    isRunning: Boolean,
    remainingSeconds: Int,
    totalSeconds: Int,
    accentColor: Color,
    contentColor: Color,
    buttonSize: androidx.compose.ui.unit.Dp,
    iconSize: androidx.compose.ui.unit.Dp,
    onToggle: () -> Unit,
    onReset: () -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Play/Pause Button
        Box(
            modifier = Modifier
                .size(buttonSize)
                .clip(CircleShape)
                .background(if (isRunning) accentColor.copy(alpha = 0.2f) else accentColor)
                .clickable { onToggle() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isRunning) FluentIcons.Filled.Pause else FluentIcons.Filled.Play,
                contentDescription = null,
                modifier = Modifier.size(iconSize),
                tint = if (isRunning) accentColor else Color.White
            )
        }

        // Reset Button
        if (!isRunning && remainingSeconds != totalSeconds) {
            Spacer(modifier = Modifier.width(16.dp))
            Box(
                modifier = Modifier
                    .size(buttonSize)
                    .clip(CircleShape)
                    .background(contentColor.copy(alpha = 0.12f))
                    .clickable { onReset() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = FluentIcons.Filled.Square,
                    contentDescription = null,
                    modifier = Modifier.size(iconSize),
                    tint = contentColor
                )
            }
        }
    }
}

// --- UTILS ---

private fun formatTime(seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return String.format("%02d:%02d", m, s)
}

private fun formatTimeLarge(seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return String.format("%02d:%02d", m, s)
}