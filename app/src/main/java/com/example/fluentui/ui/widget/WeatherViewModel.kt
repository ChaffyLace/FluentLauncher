package com.example.fluentui.ui.widget

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.fluentui.R
import com.example.fluentui.data.SettingsManager
import com.example.fluentui.data.WeatherClient
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale
data class WeatherUiState(
    val temp: String,
    val condition: String,
    val icon: Int,
    val city: String,
    val isDay: Boolean,
    val dailyForecasts: List<DailyForecast>
)

data class DailyForecast(val day: String, val icon: Int, val high: String, val low: String)

// --- ViewModel ---
class WeatherViewModel(application: Application) : AndroidViewModel(application) {
    var state by mutableStateOf<WeatherUiState?>(null)
    var isLoading by mutableStateOf(false)

    // On utilise ton SettingsManager existant
    private val settingsManager = SettingsManager(application)

    init {
        viewModelScope.launch {
            // Lecture de la ville sauvegardée au démarrage
            val savedCity = settingsManager.savedCity.first()

            if (!savedCity.isNullOrBlank()) {
                loadWeather(savedCity, saveToMemory = false)
            } else {
                loadWeather("Paris", saveToMemory = false)
            }
        }
    }

    fun loadWeather(cityName: String, saveToMemory: Boolean = true) {
        viewModelScope.launch {
            isLoading = true
            try {
                // 1. Recherche de la ville (Géocodage)
                val searchResponse = WeatherClient.api.searchCity(name = cityName)
                val foundCity = searchResponse.results?.firstOrNull()

                if (foundCity != null) {
                    // 2. Récupération Météo
                    val response = WeatherClient.api.getForecast(foundCity.latitude, foundCity.longitude)

                    state = WeatherUiState(
                        temp = "${response.current.temperature_2m.toInt()}°",
                        condition = WeatherCodeMapper.getCondition(response.current.weather_code),
                        icon = WeatherCodeMapper.getIcon(response.current.weather_code, response.current.is_day == 1),
                        city = foundCity.name,
                        isDay = response.current.is_day == 1,
                        dailyForecasts = (0..4).map { i ->
                            DailyForecast(
                                day = getDayName(i),
                                icon = WeatherCodeMapper.getIcon(response.daily.weather_code[i], true),
                                high = "${response.daily.temperature_2m_max[i].toInt()}°",
                                low = "${response.daily.temperature_2m_min[i].toInt()}°"
                            )
                        }
                    )

                    // 3. Sauvegarde (si demandé)
                    if (saveToMemory) {
                        settingsManager.saveCity(foundCity.name)
                    }
                } else {
                    println("Ville introuvable : $cityName")
                }

            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isLoading = false
            }
        }
    }

    private fun getDayName(offset: Int): String {
        val days = listOf("Dim", "Lun", "Mar", "Mer", "Jeu", "Ven", "Sam")
        val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, offset) }
        return days[cal.get(Calendar.DAY_OF_WEEK) - 1]
    }
}
object WeatherCodeMapper {
    fun getCondition(code: Int): String = when (code) {
        0 -> "Dégagé"
        1, 2, 3 -> "Nuageux"
        45, 48 -> "Brouillard"
        51, 61, 80 -> "Pluie"
        71, 73, 75 -> "Neige"
        95, 96, 99 -> "Orage"
        else -> "Nuageux"
    }

    fun getIcon(code: Int, isDay: Boolean): Int = when (code) {
        0 -> if (isDay) R.drawable.ic_sunny else R.drawable.ic_night
        1, 2, 3 -> R.drawable.ic_cloudy
        45, 48 -> R.drawable.ic_fog
        51, 61, 80 -> if (isDay) R.drawable.ic_rain else R.drawable.ic_rainy_night
        71, 73, 75, 85, 86 -> R.drawable.ic_snow
        95, 96, 99 -> R.drawable.ic_storm
        else -> R.drawable.ic_cloudy
    }


}