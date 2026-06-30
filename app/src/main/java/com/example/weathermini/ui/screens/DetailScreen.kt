package com.example.weathermini.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.weathermini.data.datastore.AppSettings
import com.example.weathermini.data.datastore.TemperatureUnit
import com.example.weathermini.data.model.CurrentWeather
import com.example.weathermini.ui.WeatherDetailState
import com.example.weathermini.util.WeatherCodeUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    state: WeatherDetailState,
    settings: AppSettings,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onOpenNotes: (cityName: String) -> Unit
) {
    val title = when (state) {
        is WeatherDetailState.Success    -> state.cityName
        is WeatherDetailState.StaleError -> state.cityName
        else                             -> "Погода"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    if (state is WeatherDetailState.Success || state is WeatherDetailState.StaleError) {
                        IconButton(onClick = { onOpenNotes(title) }) {
                            Icon(Icons.Default.Edit, contentDescription = "Заметки")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            when (state) {
                is WeatherDetailState.Loading ->
                    CircularProgressIndicator()

                is WeatherDetailState.Error ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(state.message, color = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = onRetry) { Text("Повторить") }
                    }

                is WeatherDetailState.StaleError ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Spacer(Modifier.width(8.dp))
                                Column {
                                    Text(
                                        "Нет сети — показаны устаревшие данные",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                    Text(
                                        state.errorMessage,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        WeatherContent(
                            weather  = state.weather.currentWeather,
                            settings = settings,
                            fromCache = true,
                            cachedAt  = null
                        )
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(onClick = onRetry, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Default.Refresh, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Повторить при наличии сети")
                        }
                        OutlinedButton(
                            onClick = { onOpenNotes(state.cityName) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Edit, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Заметки")
                        }
                    }

                is WeatherDetailState.Success ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        WeatherContent(
                            weather   = state.weather.currentWeather,
                            settings  = settings,
                            fromCache = state.fromCache,
                            cachedAt  = state.cachedAt
                        )
                        Spacer(Modifier.height(12.dp))
                        OutlinedButton(
                            onClick = { onOpenNotes(state.cityName) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Edit, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Заметки для ${state.cityName}")
                        }
                    }
            }
        }
    }
}

@Composable
private fun WeatherContent(
    weather: CurrentWeather,
    settings: AppSettings,
    fromCache: Boolean,
    cachedAt: Long?
) {
    val displayTemp = when (settings.temperatureUnit) {
        TemperatureUnit.CELSIUS    -> "${weather.temperature.toInt()}°C"
        TemperatureUnit.FAHRENHEIT -> "${(weather.temperature * 9 / 5 + 32).toInt()}°F"
    }

    Text(displayTemp, fontSize = 56.sp)
    Text(
        WeatherCodeUtils.toDescription(weather.weatherCode),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(Modifier.height(8.dp))
    Text("Ветер: ${weather.windSpeed} км/ч")

    if (fromCache) {
        Spacer(Modifier.height(8.dp))
        val timeStr = cachedAt?.let {
            "Кэш от ${SimpleDateFormat("HH:mm dd.MM", Locale.getDefault()).format(Date(it))}"
        } ?: "Данные из кэша"
        Surface(
            color = MaterialTheme.colorScheme.tertiaryContainer,
            shape = MaterialTheme.shapes.small
        ) {
            Text(
                "📦 $timeStr",
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )
        }
    }
}