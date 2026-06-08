package com.example.fluentui.ui.homescreen.widgets

import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.text.format.DateFormat
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.fluentui.R
import com.example.fluentui.ui.theme.FluentFontFamily
import com.example.fluentui.ui.system.TileConfig
import com.example.fluentui.ui.system.TileSize
import fluent.ui.system.icons.FluentIcons
import fluent.ui.system.icons.color.Alert
import fluent.ui.system.icons.color.Clock
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*

// ============================================================================
// CONSTANTS  — top-level val, alloués une seule fois, jamais recréés
// ============================================================================

private val DATE_FORMATTER_24H  = DateTimeFormatter.ofPattern("HH:mm")
private val DATE_FORMATTER_12H  = DateTimeFormatter.ofPattern("h:mm a")
private val ALARM_DATE_PATTERN  = DateTimeFormatter.ofPattern("EEEE dd", Locale.getDefault())

// LineHeightStyle.Trim.Both supprime le padding vertical parasite des grands
// textes — c'est le vrai fix du rognage en MEDIUM/LARGE.
@Suppress("DEPRECATION") // includeFontPadding déprécié Compose 1.6 mais LineHeightStyle.Trim reste utile
private val clockTextStyle = TextStyle(
    fontFamily     = FluentFontFamily,
    platformStyle  = PlatformTextStyle(includeFontPadding = false),
    lineHeightStyle = LineHeightStyle(
        alignment = LineHeightStyle.Alignment.Center,
        trim      = LineHeightStyle.Trim.Both
    ),
    letterSpacing  = (-1.0).sp
)

// ============================================================================
// DATA MODEL
// ============================================================================

private sealed class ClockState {
    object Time : ClockState()
    data class Alarm(val time: String, val label: String) : ClockState()
}

// ============================================================================
// WIDGET
// ============================================================================

@Composable
fun ClockWidget(
    config:       TileConfig,
    contentColor: Color,
    accentColor:  Color = Color(0xFF0078D4),
    modifier:     Modifier = Modifier
) {
    val context = LocalContext.current

    // TINY — icône seule, aucune logique
    if (config.size == TileSize.TINY) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Icon(FluentIcons.Color.Clock, null, Modifier.size(26.dp), tint = Color.Unspecified)
        }
        return
    }

    var is24h      by remember { mutableStateOf(DateFormat.is24HourFormat(context)) }
    var currentTime by remember { mutableStateOf(getCurrentTime(is24h)) }
    var alarmData   by remember { mutableStateOf<ClockState.Alarm?>(null) }
    var clockState  by remember { mutableStateOf<ClockState>(ClockState.Time) }

    // ── Alarm observer ────────────────────────────────────────────────────────
    // ACTION_TIME_CHANGED + ACTION_TIMEZONE_CHANGED : réagit si l'utilisateur
    // change l'heure manuellement ou change de pays.
    // RECEIVER_NOT_EXPORTED : requis Android 14+ (targetSdk 34).
    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                is24h     = DateFormat.is24HourFormat(context)
                alarmData = fetchRealAlarm(context)
            }
        }
        val filter = IntentFilter().apply {
            addAction(AlarmManager.ACTION_NEXT_ALARM_CLOCK_CHANGED)
            addAction(Intent.ACTION_TIME_CHANGED)
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
        }
        ContextCompat.registerReceiver(
            context, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED
        )
        alarmData = fetchRealAlarm(context)
        onDispose { runCatching { context.unregisterReceiver(receiver) } }
    }

    // ── Ticker haute précision ────────────────────────────────────────────────
    // delay(1000 - millis%1000) cale le tick sur le changement de seconde système.
    // Sans ça, delay(1000) dérive et l'horloge peut sauter visuellement une seconde.
    LaunchedEffect(is24h) {
        while (isActive) {
            currentTime = getCurrentTime(is24h)
            delay(1_000L - (System.currentTimeMillis() % 1_000L))
        }
    }

    // ── Cycle flip ────────────────────────────────────────────────────────────
    LaunchedEffect(alarmData, config.size) {
        if (alarmData == null || config.size == TileSize.SMALL) {
            clockState = ClockState.Time
            return@LaunchedEffect
        }
        while (isActive) {
            clockState = ClockState.Time
            delay(15_000L)
            clockState = alarmData!!
            delay(5_000L)
        }
    }

    // ── Rendu ─────────────────────────────────────────────────────────────────
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = if (config.size == TileSize.LARGE) 32.dp else 12.dp),
        contentAlignment = Alignment.Center
    ) {
        AnimatedContent(
            targetState  = clockState,
            transitionSpec = {
                (fadeIn(tween(500)) + scaleIn(initialScale = 0.95f)) togetherWith
                        (fadeOut(tween(500)) + scaleOut(targetScale = 0.95f))
            },
            label = "ClockFlip"
        ) { state ->
            val isCentered = config.size == TileSize.SMALL || config.size == TileSize.LARGE
            Box(
                modifier         = Modifier.fillMaxSize(),
                contentAlignment = if (isCentered) Alignment.Center else Alignment.CenterStart
            ) {
                when (state) {
                    is ClockState.Time  -> Text(
                        text       = currentTime,
                        color      = contentColor,
                        fontSize   = getAdaptiveFontSize(config.size, is24h),
                        fontWeight = FontWeight.Light,
                        style      = clockTextStyle,
                        textAlign  = if (isCentered) TextAlign.Center else TextAlign.Start,
                        maxLines   = 1,
                        softWrap   = false,
                        overflow   = TextOverflow.Visible
                    )
                    is ClockState.Alarm -> AlarmView(state, contentColor, config.size)
                }
            }
        }
    }
}

// ============================================================================
// UTILS
// ============================================================================

private fun fetchRealAlarm(context: Context): ClockState.Alarm? {
    val am   = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val next = am.nextAlarmClock ?: return null
    val pkg = next.showIntent?.creatorPackage ?: return null
    if (pkg.isNotEmpty() &&
        !pkg.contains("clock", ignoreCase = true) &&
        !pkg.contains("alarm", ignoreCase = true) &&
        !pkg.contains("deskclock", ignoreCase = true)) return null

    val trigger   = Instant.ofEpochMilli(next.triggerTime).atZone(ZoneId.systemDefault())
    val alarmDate = trigger.toLocalDate()
    val today     = LocalDate.now()
    val is24h     = DateFormat.is24HourFormat(context)

    val timeStr = trigger.format(if (is24h) DATE_FORMATTER_24H else DATE_FORMATTER_12H)

    val todayStr = context.getString(R.string.clock_alarm_today)
    val tomorrowStr = context.getString(R.string.clock_alarm_tomorrow)
    val alarmLabelStr = context.getString(R.string.clock_alarm_label)

    val label   = when {
        alarmDate == today             -> todayStr
        alarmDate == today.plusDays(1) -> tomorrowStr
        else -> trigger.format(ALARM_DATE_PATTERN).replaceFirstChar { it.uppercase() }
    }

    return ClockState.Alarm(timeStr, "$alarmLabelStr · $label")
}

private fun getCurrentTime(is24h: Boolean): String =
    LocalTime.now().format(if (is24h) DATE_FORMATTER_24H else DATE_FORMATTER_12H)

private fun getAdaptiveFontSize(size: TileSize, is24h: Boolean) = when (size) {
    TileSize.SMALL  -> if (is24h) 34.sp else 28.sp
    TileSize.MEDIUM -> if (is24h) 54.sp else 46.sp
    TileSize.LARGE  -> if (is24h) 78.sp else 68.sp
    else            -> 32.sp
}

// ============================================================================
// ALARM VIEW
// ============================================================================

@Composable
private fun AlarmView(data: ClockState.Alarm, color: Color, size: TileSize) {
    val isCentered = size == TileSize.SMALL || size == TileSize.LARGE
    Column(
        modifier              = Modifier.fillMaxWidth(),
        verticalArrangement   = Arrangement.Center,
        horizontalAlignment   = if (isCentered) Alignment.CenterHorizontally else Alignment.Start
    ) {
        Row(
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(FluentIcons.Color.Alert, null, Modifier.size(12.dp), tint = Color.Unspecified)
            Text(
                text       = data.label,
                color      = color.copy(alpha = 0.5f),
                fontSize   = 10.sp,
                fontWeight = FontWeight.Medium,
                maxLines   = 1,
                overflow   = TextOverflow.Ellipsis
            )
        }
        Text(
            text       = data.time,
            color      = color,
            fontSize   = if (size == TileSize.SMALL) 24.sp else 40.sp,
            fontWeight = FontWeight.Normal,
            style      = clockTextStyle,
            textAlign  = if (isCentered) TextAlign.Center else TextAlign.Start,
            maxLines   = 1
        )
    }
}