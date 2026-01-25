package com.example.fluentui.ui.widget

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fluentui.ui.system.TileConfig
import com.example.fluentui.ui.system.TileSize
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ClockWidget(config: TileConfig) {
    // PERFORMANCE OPTIMIZATION: Use primitive Long instead of heavy Calendar object
    var currentTime by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(Unit) {
        while (true) {
            currentTime = System.currentTimeMillis()
            // Sync exactly with the next second tick to avoid drift
            delay(1000 - (currentTime % 1000))
        }
    }

    // Use default Locale for internationalization (en, fr, es, etc.)
    val locale = Locale.getDefault()

    // Memoize formatters to avoid recreating them every second
    val timeStr = remember(currentTime) { SimpleDateFormat("HH:mm", locale).format(Date(currentTime)) }
    val dateStr = remember(currentTime) { SimpleDateFormat("EEEE d MMMM", locale).format(Date(currentTime)) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0078D4)) // Windows Blue
            .padding(12.dp)
    ) {
        when (config.size) {
            TileSize.TINY, TileSize.SMALL -> {
                Text(
                    text = timeStr,
                    color = Color.White,
                    fontSize = if(config.size == TileSize.TINY) 20.sp else 28.sp,
                    modifier = Modifier.align(Alignment.Center),
                    fontWeight = FontWeight.Light
                )
            }
            TileSize.MEDIUM, TileSize.LARGE -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Bottom
                ) {
                    Text(
                        text = timeStr,
                        color = Color.White,
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Thin,
                        // Fix for font padding on large text in Compose
                        style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
                    )
                    Text(
                        text = dateStr.replaceFirstChar { it.uppercase() },
                        color = Color.White.copy(0.8f),
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
fun CalendarWidget(config: TileConfig) {
    // Calendar doesn't need a loop, date changes rarely.
    // We compute it once on composition.
    val date = Date()
    val locale = Locale.getDefault()

    val dayNum = SimpleDateFormat("d", locale).format(date)
    val dayName = SimpleDateFormat("EEEE", locale).format(date)
    val monthYear = SimpleDateFormat("MMMM yyyy", locale).format(date)

    // Visual specs for Mica integration
    val bgColor = Color(0xFF202020)
    val borderColor = Color.White.copy(0.08f)
    val cornerRadius = if (config.size == TileSize.TINY) 3.dp else 4.dp

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(cornerRadius))
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        when (config.size) {
            TileSize.TINY -> {
                Text(dayNum, color = Color.White, fontSize = 20.sp)
            }
            TileSize.SMALL -> {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(dayName.lowercase(), color = Color.White.copy(0.8f), fontSize = 12.sp)
                    Text(dayNum, color = Color.White, fontSize = 42.sp, fontWeight = FontWeight.Thin,
                        style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false)))
                }
            }
            else -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(dayNum, color = Color.White, fontSize = 56.sp, fontWeight = FontWeight.Thin)
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text(dayName.replaceFirstChar { it.uppercase() }, color = Color.White, fontSize = 18.sp)
                        Text(monthYear.replaceFirstChar { it.uppercase() }, color = Color.White.copy(0.6f), fontSize = 13.sp)
                    }
                }
            }
        }
    }
}