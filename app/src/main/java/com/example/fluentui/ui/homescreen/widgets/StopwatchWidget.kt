package com.example.fluentui.ui.homescreen.widgets

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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
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
 * Premium Stopwatch Widget.
 * Engineered with strict visual hierarchy (Asymmetric button sizing) for LARGE and MEDIUM tiles.
 */
@Composable
fun StopwatchWidget(config: TileConfig, contentColor: Color, accentColor: Color) {
    // 1. STATE MANAGEMENT
    var elapsedTenths by remember { mutableIntStateOf(0) }
    var isRunning by remember { mutableStateOf(false) }

    // 2. PRECISION TICKER
    LaunchedEffect(isRunning) {
        while (isRunning) {
            delay(100)
            elapsedTenths++
        }
    }

    val padding = if (config.size == TileSize.SMALL) 12.dp else 16.dp

    Box(modifier = Modifier.fillMaxSize().padding(padding)) {

        // --- HEADER ---
        when (config.size) {
            TileSize.TINY -> {
                Icon(
                    imageVector = FluentIcons.Regular.Timer,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp).align(Alignment.Center),
                    tint = contentColor
                )
            }
            else -> {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.align(Alignment.TopStart)
                ) {
                    Icon(
                        imageVector = FluentIcons.Regular.Timer,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = contentColor
                    )
                    if (config.size != TileSize.SMALL) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Chrono",
                            color = contentColor,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        // --- RESPONSIVE LAYOUT DISPATCHER ---
        when (config.size) {
            TileSize.TINY -> { /* Handled in header */ }

            TileSize.SMALL -> {
                Text(
                    text = formatStopwatchCompact(elapsedTenths),
                    color = contentColor,
                    fontSize = 20.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.align(Alignment.BottomStart)
                )
            }

            TileSize.MEDIUM -> {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    StopwatchDisplay(
                        elapsedTenths = elapsedTenths,
                        contentColor = contentColor,
                        mainFontSize = 42.dp, // Slightly larger for better readability
                        msFontSize = 24.dp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    StopwatchControls(
                        isRunning = isRunning,
                        elapsedTenths = elapsedTenths,
                        accentColor = accentColor,
                        contentColor = contentColor,
                        primarySize = 52.dp,       // Hero button size
                        secondarySize = 40.dp,     // Subdued button size
                        primaryIconSize = 22.dp,
                        secondaryIconSize = 16.dp,
                        spacing = 20.dp,
                        onToggle = { isRunning = !isRunning },
                        onReset = { elapsedTenths = 0; isRunning = false }
                    )
                }
            }

            TileSize.LARGE -> {
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .fillMaxHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    StopwatchDisplay(
                        elapsedTenths = elapsedTenths,
                        contentColor = contentColor,
                        mainFontSize = 72.dp, // Massive hero text for LARGE tile
                        msFontSize = 38.dp
                    )
                    Spacer(modifier = Modifier.height(36.dp))
                    StopwatchControls(
                        isRunning = isRunning,
                        elapsedTenths = elapsedTenths,
                        accentColor = accentColor,
                        contentColor = contentColor,
                        primarySize = 72.dp,       // Massive Hero button
                        secondarySize = 52.dp,     // Clearly secondary button
                        primaryIconSize = 32.dp,
                        secondaryIconSize = 22.dp,
                        spacing = 32.dp,
                        onToggle = { isRunning = !isRunning },
                        onReset = { elapsedTenths = 0; isRunning = false }
                    )
                }
            }
        }
    }
}

// --- SUB-COMPONENTS ---

/**
 * Pixel-perfect typography layout for the stopwatch readout.
 */
@Composable
private fun StopwatchDisplay(
    elapsedTenths: Int,
    contentColor: Color,
    mainFontSize: Dp,
    msFontSize: Dp
) {
    val minutes = formatMinutes(elapsedTenths)
    val seconds = formatSeconds(elapsedTenths)
    val tenths  = formatTenths(elapsedTenths)

    val mainFs = mainFontSize.value.sp
    val msFs   = msFontSize.value.sp
    val subFs  = (mainFontSize.value * 0.28f).sp

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.Bottom) {
            // "MM" Block
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(minutes, color = contentColor, fontSize = mainFs,
                    fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Light)
                Text("min", color = contentColor.copy(alpha = 0.45f),
                    fontSize = subFs, fontWeight = FontWeight.Medium)
            }

            // Separator
            Text(
                text = ":",
                color = contentColor.copy(alpha = 0.6f),
                fontSize = mainFs,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Light,
                modifier = Modifier.padding(start = 2.dp, end = 2.dp, bottom = (mainFontSize.value * 0.13f).dp)
            )

            // "SS" Block
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(seconds, color = contentColor, fontSize = mainFs,
                    fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Light)
                Text("s", color = contentColor.copy(alpha = 0.45f),
                    fontSize = subFs, fontWeight = FontWeight.Medium)
            }

            // Tenths ",t" Block
            Text(
                text = ",$tenths",
                color = contentColor.copy(alpha = 0.55f),
                fontSize = msFs,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Light,
                modifier = Modifier.padding(
                    start = 4.dp,
                    bottom = (mainFontSize.value * 0.06f).dp
                )
            )
        }
    }
}

/**
 * Asymmetric control cluster. Play is primary (larger), Reset is secondary (smaller).
 */
@Composable
private fun StopwatchControls(
    isRunning: Boolean,
    elapsedTenths: Int,
    accentColor: Color,
    contentColor: Color,
    primarySize: Dp,
    secondarySize: Dp,
    primaryIconSize: Dp,
    secondaryIconSize: Dp,
    spacing: Dp,
    onToggle: () -> Unit,
    onReset: () -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Secondary Action: Reset (Only visible if stopped and time > 0)
        if (!isRunning && elapsedTenths > 0) {
            Box(
                modifier = Modifier
                    .size(secondarySize)
                    .clip(CircleShape)
                    .background(contentColor.copy(alpha = 0.10f))
                    .clickable { onReset() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = FluentIcons.Filled.Square,
                    contentDescription = null,
                    modifier = Modifier.size(secondaryIconSize),
                    tint = contentColor
                )
            }
            Spacer(modifier = Modifier.width(spacing))
        }

        // Primary Action: Play / Pause (Hero Button)
        Box(
            modifier = Modifier
                .size(primarySize)
                .clip(CircleShape)
                .background(if (isRunning) accentColor.copy(alpha = 0.18f) else accentColor)
                .clickable { onToggle() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isRunning) FluentIcons.Filled.Pause else FluentIcons.Filled.Play,
                contentDescription = null,
                modifier = Modifier.size(primaryIconSize),
                tint = if (isRunning) accentColor else Color.White
            )
        }
    }
}

// --- UTILS ---

private fun formatMinutes(tenths: Int) = String.format("%02d", tenths / 600)
private fun formatSeconds(tenths: Int) = String.format("%02d", (tenths % 600) / 10)
private fun formatTenths(tenths: Int)  = (tenths % 10).toString()
private fun formatStopwatchCompact(tenths: Int): String {
    val m = tenths / 600
    val s = (tenths % 600) / 10
    return String.format("%02d:%02d", m, s)
}