package com.example.fluentui.ui.widget

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fluentui.R
import com.example.fluentui.ui.motion.FluentAnimLiveTile
import com.example.fluentui.ui.system.TileConfig
import com.example.fluentui.ui.system.TileSize

// --- LOGIQUE DE L'IMAGE DE FOND ---
@Composable
fun getWeatherBackgroundRes(isDay: Boolean, condition: String): Int {
    val isRaining = condition.contains("Pluie") || condition.contains("Rain") ||
            condition.contains("Drizzle") || condition.contains("Shower")

    return when {
        !isDay && isRaining -> R.drawable.pluie_nuit
        !isDay -> R.drawable.nuit_etoile
        isRaining -> R.drawable.pluie
        condition.contains("Nuage") || condition.contains("Cloud") || condition.contains("Couvert") -> R.drawable.ciel_nuageux
        else -> R.drawable.ciel_degage
    }
}

// --- WIDGET PRINCIPAL ---
@Composable
fun WeatherWidget(config: TileConfig, viewModel: WeatherViewModel = viewModel()) {
    val state = viewModel.state
    var showDialog by remember { mutableStateOf(false) }

    // Dialog pour changer la ville
    if (showDialog) {
        ChangeCityDialog(
            currentCity = state?.city ?: "Paris",
            onDismiss = { showDialog = false },
            onConfirm = {
                viewModel.loadWeather(it)
                showDialog = false
            }
        )
    }

    // État de chargement ou erreur (State null)
    if (state == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF2D2D30))
                .clickable { showDialog = true },
            contentAlignment = Alignment.Center
        ) {
            if (viewModel.isLoading) {
                CircularProgressIndicator(
                    color = Color.White,
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Image(
                    painter = painterResource(id = R.drawable.ic_weather),
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    colorFilter = ColorFilter.tint(Color.White.copy(alpha = 0.5f))
                )
            }
        }
        return
    }

    // État Affichage Météo (Avec Image de fond et Animation)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable { showDialog = true }
    ) {
        // 1. Fond d'écran Météo (Image pleine taille)
        Image(
            painter = painterResource(getWeatherBackgroundRes(state.isDay, state.condition)),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // 2. Filtre sombre pour lisibilité
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.25f))
        )

        // 3. Contenu (Animé ou Statique selon la taille)
        Box(modifier = Modifier.fillMaxSize().padding(10.dp)) {
            if (config.size == TileSize.TINY) {
                TinyWeatherView(state)
            } else {
                FluentAnimLiveTile(
                    animationInterval = 10000,
                    frontContent = {
                        when (config.size) {
                            TileSize.SMALL -> SmallWeatherFront(state)
                            TileSize.MEDIUM -> MediumWeatherFront(state)
                            TileSize.LARGE -> LargeWeatherFront(state)
                            else -> {}
                        }
                    },
                    backContent = {
                        when (config.size) {
                            TileSize.SMALL -> SmallWeatherBack(state)
                            TileSize.MEDIUM -> MediumWeatherBack(state)
                            TileSize.LARGE -> LargeWeatherBack(state)
                            else -> {}
                        }
                    }
                )
            }
        }
    }
}

// --- VUES DÉTAILLÉES ---

@Composable
fun TinyWeatherView(state: WeatherUiState) {
    Column(Modifier.fillMaxSize(), Arrangement.SpaceBetween) {
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            Image(
                painter = painterResource(state.icon),
                contentDescription = null,
                modifier = Modifier.size(22.dp)
            )
            Text(state.temp, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
        Text(
            text = state.city,
            color = Color.White.copy(0.9f),
            fontSize = 10.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun SmallWeatherFront(state: WeatherUiState) {
    Column(Modifier.fillMaxSize()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = painterResource(state.icon),
                contentDescription = null,
                modifier = Modifier.size(32.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = state.temp,
                color = Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.Light,
                letterSpacing = (-1).sp
            )
        }
        Text(
            text = state.condition,
            color = Color.White.copy(0.9f),
            fontSize = 13.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 2.dp)
        )
        Spacer(Modifier.weight(1f))
        Text(
            text = state.city,
            color = Color.White.copy(0.8f),
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun SmallWeatherBack(state: WeatherUiState) {
    Row(Modifier.fillMaxSize(), Arrangement.SpaceBetween) {
        state.dailyForecasts.take(4).forEach { DailyForecastItemVertical(it, 0.8f) }
    }
}

@Composable
fun MediumWeatherFront(state: WeatherUiState) {
    Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f), Arrangement.Center) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(state.icon),
                    contentDescription = null,
                    modifier = Modifier.size(40.dp)
                )
                Spacer(Modifier.width(12.dp))
                Text(state.temp, color = Color.White, fontSize = 36.sp, fontWeight = FontWeight.Light)
            }
            Text(state.condition, color = Color.White, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                text = state.city,
                color = Color.White.copy(0.7f),
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
        Spacer(Modifier.width(16.dp))
    }
}

@Composable
fun MediumWeatherBack(state: WeatherUiState) {
    Row(Modifier.fillMaxSize(), Arrangement.SpaceAround, Alignment.CenterVertically) {
        state.dailyForecasts.take(3).forEach { DailyForecastItemHorizontalBig(it) }
    }
}

@Composable
fun LargeWeatherFront(state: WeatherUiState) {
    Column(Modifier.fillMaxSize()) {
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = painterResource(state.icon),
                contentDescription = null,
                modifier = Modifier.size(60.dp)
            )
            Spacer(Modifier.width(16.dp))
            Column {
                Text(state.temp, color = Color.White, fontSize = 58.sp, fontWeight = FontWeight.Thin)
                Text(state.condition, color = Color.White, fontSize = 20.sp)
            }
        }
        Spacer(Modifier.weight(1f))
        Text(state.city, color = Color.White, fontSize = 14.sp, modifier = Modifier.padding(bottom = 4.dp))
    }
}

@Composable
fun LargeWeatherBack(state: WeatherUiState) {
    Column(Modifier.fillMaxSize(), Arrangement.SpaceEvenly) {
        Text("Prévisions", color = Color.White.copy(0.7f), fontSize = 12.sp)
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
            state.dailyForecasts.take(5).forEach { DailyForecastItemVertical(it, 1.1f) }
        }
    }
}

// --- ITEMS DE PRÉVISIONS ---

@Composable
fun DailyForecastItemVertical(day: DailyForecast, scale: Float = 1f) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(day.day.lowercase().take(3), color = Color.White.copy(0.9f), fontSize = (11 * scale).sp)
        Spacer(Modifier.height(4.dp))
        Image(
            painter = painterResource(day.icon),
            contentDescription = null,
            modifier = Modifier.size((24 * scale).dp)
        )
        Spacer(Modifier.height(4.dp))
        Text(day.high.replace("°","°"), color = Color.White, fontWeight = FontWeight.Bold, fontSize = (12 * scale).sp)
        Text(day.low.replace("°","°"), color = Color.White.copy(0.6f), fontSize = (11 * scale).sp)
    }
}

@Composable
fun DailyForecastItemHorizontalBig(day: DailyForecast) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(day.day.lowercase(), color = Color.White, fontSize = 14.sp)
        Spacer(Modifier.height(4.dp))
        Image(
            painter = painterResource(day.icon),
            contentDescription = null,
            modifier = Modifier.size(32.dp)
        )
        Spacer(Modifier.height(4.dp))
        Row {
            Text(day.high, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(4.dp))
            Text(day.low, color = Color.White.copy(0.7f), fontSize = 14.sp)
        }
    }
}

// --- DIALOGUE ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangeCityDialog(currentCity: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ville") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                placeholder = { Text(currentCity) },
                singleLine = true
            )
        },
        confirmButton = {
            Button(
                onClick = { if (text.isNotBlank()) onConfirm(text) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0078D4))
            ) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuler") }
        },
        containerColor = Color(0xFFF3F3F3),
        titleContentColor = Color.Black,
        textContentColor = Color.Black
    )
}