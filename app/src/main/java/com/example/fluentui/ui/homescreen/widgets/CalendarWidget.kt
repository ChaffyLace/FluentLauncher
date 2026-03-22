package com.example.fluentui.ui.homescreen.widgets

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import android.util.Log
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
import androidx.core.content.ContextCompat
import com.example.fluentui.ui.system.TileConfig
import com.example.fluentui.ui.system.TileSize
import com.example.fluentui.ui.motion.FluentLiveTile
import fluent.ui.system.icons.FluentIcons
import fluent.ui.system.icons.color.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle as JavaTextStyle
import java.util.Locale

private const val TAG = "CalendarWidget"

// ============================================================================
// DATA LAYER
// ============================================================================

data class CalendarEvent(
    val title:      String,
    val timeString: String,
    val color:      Color
)

object CalendarDataProvider {

    private val PROJECTION = arrayOf(
        CalendarContract.Instances.TITLE,
        CalendarContract.Instances.BEGIN,
        CalendarContract.Instances.END,
        CalendarContract.Instances.DISPLAY_COLOR,
        CalendarContract.Instances.EVENT_COLOR
    )

    private val TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm")

    // 7 jours suffisent pour tous les cas d'affichage.
    // Une fenêtre d'un an retournerait potentiellement des milliers de lignes
    // sur un agenda chargé avant que le LIMIT Kotlin ne coupe — risque mémoire réel.
    private const val WINDOW_DAYS    = 7L
    private const val MAX_EVENTS     = 4
    private const val MS_PER_DAY     = 24L * 60 * 60 * 1_000

    suspend fun getUpcomingEvents(context: Context, defaultAccent: Color): List<CalendarEvent> =
        withContext(Dispatchers.IO) {
            if (ContextCompat.checkSelfPermission(
                    context, Manifest.permission.READ_CALENDAR
                ) != PackageManager.PERMISSION_GRANTED
            ) return@withContext emptyList()

            val now         = System.currentTimeMillis()
            val endOfWindow = now + WINDOW_DAYS * MS_PER_DAY

            val uri = CalendarContract.Instances.CONTENT_URI.buildUpon().also {
                ContentUris.appendId(it, now)
                ContentUris.appendId(it, endOfWindow)
            }.build()

            val events = mutableListOf<CalendarEvent>()
            val today  = LocalDate.now()

            try {
                context.contentResolver.query(
                    uri, PROJECTION, null, null,
                    CalendarContract.Instances.BEGIN + " ASC"
                )?.use { c ->
                    val titleIdx        = c.getColumnIndex(CalendarContract.Instances.TITLE)
                    val startIdx        = c.getColumnIndex(CalendarContract.Instances.BEGIN)
                    val endIdx          = c.getColumnIndex(CalendarContract.Instances.END)
                    val displayColorIdx = c.getColumnIndex(CalendarContract.Instances.DISPLAY_COLOR)
                    val eventColorIdx   = c.getColumnIndex(CalendarContract.Instances.EVENT_COLOR)

                    while (c.moveToNext() && events.size < MAX_EVENTS) {
                        // Si les colonnes essentielles sont absentes on skippe la ligne —
                        // un startMillis = 0L produirait un événement corrompu (epoch = 1970).
                        if (startIdx == -1 || endIdx == -1) continue

                        val startMillis = c.getLong(startIdx)
                        val endMillis   = c.getLong(endIdx)
                        if (startMillis == 0L) continue   // ligne corrompue

                        val title = if (titleIdx != -1)
                            c.getString(titleIdx).orEmpty().ifBlank { "Sans titre" }
                        else "Sans titre"

                        // Priorité DISPLAY_COLOR → EVENT_COLOR → accent système
                        var colorInt = if (displayColorIdx != -1) c.getInt(displayColorIdx) else 0
                        if ((colorInt == 0 || colorInt == -1) && eventColorIdx != -1)
                            colorInt = c.getInt(eventColorIdx)
                        val eventColor = if (colorInt != 0 && colorInt != -1)
                            Color(colorInt) else defaultAccent

                        val zone          = ZoneId.systemDefault()
                        val eventDate     = Instant.ofEpochMilli(startMillis).atZone(zone).toLocalDate()
                        val startTimeStr  = Instant.ofEpochMilli(startMillis).atZone(zone).format(TIME_FORMATTER)
                        val endTimeStr    = Instant.ofEpochMilli(endMillis).atZone(zone).format(TIME_FORMATTER)

                        val displayTime = when (eventDate) {
                            today            -> "$startTimeStr - $endTimeStr"
                            today.plusDays(1) -> "Demain · $startTimeStr"
                            else             -> {
                                val shortDay = eventDate.dayOfWeek
                                    .getDisplayName(JavaTextStyle.SHORT, Locale.getDefault())
                                    .replaceFirstChar { it.uppercase() }
                                "$shortDay ${eventDate.dayOfMonth} · $startTimeStr"
                            }
                        }

                        events += CalendarEvent(title, displayTime, eventColor)
                    }
                }
            } catch (e: Exception) {
                // Log correctement taguée — visible dans Logcat, pas stderr
                Log.e(TAG, "Failed to fetch calendar events", e)
            }

            events
        }
}

// ============================================================================
// WIDGET PRINCIPAL
// ============================================================================

@Composable
fun CalendarWidget(
    config:       TileConfig,
    contentColor: Color,
    accentColor:  Color = Color(0xFF0078D4),
    modifier:     Modifier = Modifier
) {
    // TINY : icône seule, aucune logique supplémentaire
    if (config.size == TileSize.TINY) {
        Box(contentAlignment = Alignment.Center, modifier = modifier.fillMaxSize()) {
            Icon(FluentIcons.Color.Calendar, null, Modifier.size(32.dp), tint = Color.Unspecified)
        }
        return
    }

    val context = LocalContext.current
    val today   = LocalDate.now()

    val dayOfMonth     = today.dayOfMonth.toString()
    // Full name pour MEDIUM/LARGE (assez de place), short pour SMALL (évite le clip)
    val dayOfWeekFull  = today.dayOfWeek
        .getDisplayName(JavaTextStyle.FULL,  Locale.getDefault())
        .replaceFirstChar { it.uppercase() }
    val dayOfWeekShort = today.dayOfWeek
        .getDisplayName(JavaTextStyle.SHORT, Locale.getDefault())
        .replaceFirstChar { it.uppercase() }

    // Refresh toutes les 5 min — produceState(key = today) seul
    // ne se relance qu'à minuit, ratant les nouveaux événements de la journée
    var refreshTick by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) { delay(5 * 60 * 1_000L); refreshTick++ }
    }

    val eventsState by produceState<List<CalendarEvent>>(
        initialValue = emptyList(),
        key1 = today,
        key2 = refreshTick
    ) {
        value = CalendarDataProvider.getUpcomingEvents(context, accentColor)
    }

    if (eventsState.isEmpty()) {
        // Aucun événement — vue date statique
        Box(modifier = modifier.fillMaxSize()) {
            when (config.size) {
                TileSize.SMALL  -> DateView2x2(dayOfWeekShort, dayOfMonth, contentColor)
                TileSize.MEDIUM -> DateView4x2(dayOfWeekFull,  dayOfMonth, contentColor)
                else            -> DateView4x4(dayOfWeekFull,  dayOfMonth, contentColor)
            }
        }
    } else {
        // Live tile — flip toutes les 8s avec délai initial de 5s
        // (5s = temps minimum pour que l'utilisateur lise la vue date)
        FluentLiveTile(
            modifier          = modifier.fillMaxSize(),
            animationInterval = 8_000L,
            initialDelay      = 5_000L,
            frontContent = {
                when (config.size) {
                    TileSize.SMALL  -> DateView2x2(dayOfWeekShort, dayOfMonth, contentColor)
                    TileSize.MEDIUM -> DateView4x2(dayOfWeekFull,  dayOfMonth, contentColor)
                    else            -> DateView4x4(dayOfWeekFull,  dayOfMonth, contentColor)
                }
            },
            backContent = {
                when (config.size) {
                    TileSize.SMALL  -> EventsView2x2(eventsState.first(), contentColor)
                    TileSize.MEDIUM -> EventsView4x2(eventsState,         contentColor)
                    else            -> EventsView4x4(eventsState,         contentColor)
                }
            }
        )
    }
}

// ============================================================================
// VUES DATE
// ============================================================================

@Composable
private fun DateView2x2(dayOfWeekShort: String, dayOfMonth: String, contentColor: Color) {
    Column(
        modifier            = Modifier.fillMaxSize().padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // SHORT ici — "Mercredi" en 16sp clippait en allemand / russe
        Text(
            text       = dayOfWeekShort,
            color      = contentColor.copy(alpha = 0.85f),
            fontSize   = 14.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines   = 1,
            overflow   = TextOverflow.Ellipsis
        )
        Text(
            text       = dayOfMonth,
            color      = contentColor,
            fontSize   = 64.sp,
            fontWeight = FontWeight.Normal,
            lineHeight = 64.sp
        )
    }
}

@Composable
private fun DateView4x2(dayOfWeekFull: String, dayOfMonth: String, contentColor: Color) {
    Box(modifier = Modifier.fillMaxSize().padding(14.dp)) {
        Column(modifier = Modifier.align(Alignment.TopStart)) {
            Text(
                text       = dayOfWeekFull,
                color      = contentColor.copy(alpha = 0.85f),
                fontSize   = 16.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines   = 1,
                overflow   = TextOverflow.Ellipsis
            )
            Text(
                text       = dayOfMonth,
                color      = contentColor,
                fontSize   = 52.sp,
                fontWeight = FontWeight.Normal,
                lineHeight = 52.sp
            )
        }
        Icon(
            FluentIcons.Color.Calendar, null,
            Modifier.align(Alignment.BottomEnd).size(24.dp),
            tint = Color.Unspecified
        )
    }
}

@Composable
private fun DateView4x4(dayOfWeekFull: String, dayOfMonth: String, contentColor: Color) {
    Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Column(modifier = Modifier.align(Alignment.TopStart)) {
            Text(
                text       = dayOfWeekFull,
                color      = contentColor.copy(alpha = 0.85f),
                fontSize   = 18.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines   = 1,
                overflow   = TextOverflow.Ellipsis
            )
            Text(
                text       = dayOfMonth,
                color      = contentColor,
                fontSize   = 76.sp,
                fontWeight = FontWeight.Normal,
                lineHeight = 76.sp
            )
        }
        Icon(
            FluentIcons.Color.Calendar, null,
            Modifier.align(Alignment.BottomEnd).size(28.dp),
            tint = Color.Unspecified
        )
    }
}

// ============================================================================
// VUES ÉVÉNEMENTS
// ============================================================================

@Composable
private fun EventsView2x2(event: CalendarEvent, contentColor: Color) {
    Column(
        modifier            = Modifier.fillMaxSize().padding(12.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text       = "À venir",
            color      = contentColor.copy(alpha = 0.6f),
            fontSize   = 12.sp,
            fontWeight = FontWeight.Medium,
            maxLines   = 1
        )
        Spacer(Modifier.height(8.dp))
        EventItem(event.title, event.timeString, event.color, contentColor)
    }
}

@Composable
private fun EventsView4x2(events: List<CalendarEvent>, contentColor: Color) {
    Box(modifier = Modifier.fillMaxSize().padding(14.dp)) {
        Column(
            modifier            = Modifier
                .align(Alignment.TopStart)
                .fillMaxWidth()
                .padding(end = 32.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 1 seul événement en 4x2 — pas assez de hauteur pour en afficher plus proprement
            events.take(1).forEach { EventItem(it.title, it.timeString, it.color, contentColor) }
        }
        Icon(
            FluentIcons.Color.Calendar, null,
            Modifier.align(Alignment.BottomEnd).size(24.dp),
            tint = Color.Unspecified
        )
    }
}

@Composable
private fun EventsView4x4(events: List<CalendarEvent>, contentColor: Color) {
    Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Column(
            modifier            = Modifier
                .align(Alignment.TopStart)
                .fillMaxWidth()
                .padding(end = 36.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 3 max en 4x4 — le 4ème serait trop serré visuellement
            events.take(3).forEach { EventItem(it.title, it.timeString, it.color, contentColor) }
        }
        Icon(
            FluentIcons.Color.Calendar, null,
            Modifier.align(Alignment.BottomEnd).size(28.dp),
            tint = Color.Unspecified
        )
    }
}

// ============================================================================
// COMPOSANT ATOMIQUE
// ============================================================================

@Composable
private fun EventItem(title: String, time: String, accentColor: Color, contentColor: Color) {
    Row(
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            Modifier
                .width(3.dp)
                .height(28.dp)
                .background(accentColor, RoundedCornerShape(1.5.dp))
        )
        Column {
            Text(
                text       = title,
                color      = contentColor,
                fontSize   = 15.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines   = 1,
                overflow   = TextOverflow.Ellipsis
            )
            Text(
                text       = time,
                color      = contentColor.copy(alpha = 0.7f),
                fontSize   = 13.sp,
                fontWeight = FontWeight.Normal,
                maxLines   = 1,
                overflow   = TextOverflow.Ellipsis
            )
        }
    }
}