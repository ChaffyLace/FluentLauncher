package com.example.fluentui.ui.homescreen.widgets

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.util.Log
import android.content.Context
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.example.fluentui.R
import com.example.fluentui.ui.system.TileConfig
import com.example.fluentui.ui.system.TileSize
import com.example.fluentui.wallpaper.MicaParams

private const val TAG = "WeatherWidget"

data class WeatherState(
    val temp: Int,
    val condition: String,
    val humidity: Int,
    val windSpeed: Int,
    val location: String,
    val iconRes: Int
)

// ============================================================================
// DATA LAYER (comme Calendar)
// ============================================================================
object WeatherDataProvider {
    suspend fun getWeatherData(context: Context): WeatherState =
        withContext(Dispatchers.IO) {
            Log.d(TAG, "Fetching weather data...")
            // TODO: appel API réel ici
            WeatherState(
                temp = 22,
                condition = "Partiellement nuageux",
                humidity = 65,
                windSpeed = 12,
                location = "Paris",
                iconRes = R.drawable.ic_cloudy
            )
        }
}

// ============================================================================
// WIDGET PRINCIPAL
// ============================================================================

@Composable
fun WeatherWidget(
    config: TileConfig,
    contentColor: Color,
    micaParams: MicaParams?,
    modifier: Modifier = Modifier
) {
    Log.d(TAG, "WeatherWidget composing - size: ${config.size}")

    val context = LocalContext.current

    // Charge les données comme Calendar avec produceState
    var refreshTick by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(30 * 60 * 1_000L) // 30 min
            refreshTick++
        }
    }

    val weatherState by produceState<WeatherState?>(
        initialValue = null,
        key1 = refreshTick
    ) {
        value = WeatherDataProvider.getWeatherData(context)
    }

    Log.d(TAG, "weatherState: $weatherState")

    weatherState?.let { state ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(12.dp)
        ) {
            when (config.size) {
                TileSize.TINY -> WeatherTiny(state, contentColor)
                TileSize.SMALL -> WeatherSmall(state, contentColor)
                TileSize.MEDIUM -> WeatherMedium(state, contentColor)
                TileSize.LARGE -> WeatherLarge(state, contentColor)
            }
        }
    } ?: run {
        Log.d(TAG, "Weather data not loaded yet")
        Box(contentAlignment = Alignment.Center, modifier = modifier.fillMaxSize()) {
            Text("Chargement...", color = contentColor)
        }
    }
}

@Composable
private fun WeatherTiny(state: WeatherState, contentColor: Color) {
    Log.d(TAG, "Rendering WeatherTiny")
    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
        Icon(
            painter = painterResource(state.iconRes),
            contentDescription = null,
            modifier = Modifier.size(36.dp),
            tint = Color.Unspecified
        )
    }
}

@Composable
private fun WeatherSmall(state: WeatherState, contentColor: Color) {
    Log.d(TAG, "Rendering WeatherSmall")
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            painter = painterResource(state.iconRes),
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = Color.Unspecified
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "${state.temp}°",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = contentColor
        )
        Text(
            text = state.condition,
            fontSize = 11.sp,
            color = contentColor.copy(0.7f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun WeatherMedium(state: WeatherState, contentColor: Color) {
    Log.d(TAG, "Rendering WeatherMedium")
    Box(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopStart),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = state.location,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = contentColor
                )
                Text(
                    text = state.condition,
                    fontSize = 12.sp,
                    color = contentColor.copy(0.7f)
                )
            }
            Icon(
                painter = painterResource(state.iconRes),
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = Color.Unspecified
            )
        }

        Text(
            text = "${state.temp}°C",
            fontSize = 44.sp,
            fontWeight = FontWeight.Bold,
            color = contentColor,
            modifier = Modifier.align(Alignment.Center)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomStart),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            WeatherStat("💧", "${state.humidity}%", contentColor)
            WeatherStat("💨", "${state.windSpeed}km/h", contentColor)
        }
    }
}

@Composable
private fun WeatherLarge(state: WeatherState, contentColor: Color) {
    Log.d(TAG, "Rendering WeatherLarge")
    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.align(Alignment.TopStart)) {
            Text(
                text = state.location,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = contentColor
            )
            Text(
                text = state.condition,
                fontSize = 14.sp,
                color = contentColor.copy(0.7f)
            )
        }

        Icon(
            painter = painterResource(state.iconRes),
            contentDescription = null,
            modifier = Modifier
                .size(80.dp)
                .align(Alignment.Center),
            tint = Color.Unspecified
        )

        Column(modifier = Modifier.align(Alignment.BottomStart)) {
            Text(
                text = "${state.temp}°C",
                fontSize = 56.sp,
                fontWeight = FontWeight.Bold,
                color = contentColor
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                WeatherStat("💧", "${state.humidity}%", contentColor)
                WeatherStat("💨", "${state.windSpeed}km/h", contentColor)
                WeatherStat("🧭", "NE", contentColor)
            }
        }
    }
}

@Composable
private fun WeatherStat(emoji: String, value: String, contentColor: Color) {
    Column {
        Text(text = emoji, fontSize = 16.sp)
        Text(
            text = value,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = contentColor
        )
    }
}
