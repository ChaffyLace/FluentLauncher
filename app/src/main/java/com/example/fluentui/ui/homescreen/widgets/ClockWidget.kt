package com.example.fluentui.ui.homescreen.widgets

import android.app.AlarmManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fluentui.ui.system.TileConfig
import com.example.fluentui.ui.system.TileSize
import fluent.ui.system.icons.FluentIcons
import fluent.ui.system.icons.color.Clock
import kotlinx.coroutines.delay
import java.util.Calendar
import java.util.Locale

@Composable
fun ClockWidget(config: TileConfig, contentColor: Color, accentColor: Color) {
    val context = LocalContext.current

    // --- System Time & Date ---
    val currentTime by produceState(initialValue = getCurrentTime()) {
        while (true) { delay(1000); value = getCurrentTime() }
    }
    val currentDate by produceState(initialValue = getCurrentDate()) {
        while (true) { delay(60000); value = getCurrentDate() }
    }

    // --- System Alarm ---
    val nextAlarm by produceState<String?>(initialValue = null) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        while (true) {
            val info = alarmManager.nextAlarmClock
            value = if (info != null) {
                val cal = Calendar.getInstance().apply { timeInMillis = info.triggerTime }
                val day = cal.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.SHORT, Locale.FRENCH)
                String.format("%s. %02d:%02d", day, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE))
            } else null
            delay(60000)
        }
    }

    val padding = if (config.size == TileSize.SMALL) 12.dp else 16.dp

    // Safe Box layout preventing any text clipping
    Box(modifier = Modifier.fillMaxSize().padding(padding)) {

        // 1. Icon (Top Left) - Fills TINY size
        Icon(
            imageVector = FluentIcons.Color.Clock,
            contentDescription = null,
            modifier = Modifier.size(if (config.size == TileSize.TINY) 28.dp else 24.dp).align(if (config.size == TileSize.TINY) Alignment.Center else Alignment.TopStart),
            tint = Color.Unspecified
        )

        // 2. Alarm Badge (Top Right - M & L only)
        if (nextAlarm != null && (config.size == TileSize.MEDIUM || config.size == TileSize.LARGE)) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .background(accentColor.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(text = "⏰ $nextAlarm", color = accentColor, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        // 3. Time & Date (Bottom Left - Non-TINY only)
        if (config.size != TileSize.TINY) {
            Column(
                modifier = Modifier.align(Alignment.BottomStart),
                verticalArrangement = Arrangement.Bottom
            ) {
                Text(
                    text = currentTime,
                    color = contentColor,
                    fontSize = when (config.size) {
                        TileSize.SMALL -> 28.sp
                        TileSize.MEDIUM -> 36.sp
                        TileSize.LARGE -> 52.sp
                        else -> 28.sp
                    },
                    fontWeight = FontWeight.Light,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (config.size != TileSize.SMALL) {
                    Text(
                        text = currentDate,
                        color = contentColor.copy(alpha = 0.7f),
                        fontSize = if (config.size == TileSize.MEDIUM) 13.sp else 15.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

private fun getCurrentTime(): String {
    val c = java.util.Calendar.getInstance()
    return String.format("%02d:%02d", c.get(java.util.Calendar.HOUR_OF_DAY), c.get(java.util.Calendar.MINUTE))
}

private fun getCurrentDate(): String {
    val c = java.util.Calendar.getInstance()
    val day = c.get(java.util.Calendar.DAY_OF_MONTH)
    val month = c.getDisplayName(java.util.Calendar.MONTH, java.util.Calendar.SHORT, java.util.Locale.FRENCH)
    return "$day $month" // Streamlined format to prevent overflow
}