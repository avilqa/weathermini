package com.example.weathermini.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.weathermini.data.datastore.TemperatureUnit
import com.example.weathermini.data.model.CurrentWeather
import com.example.weathermini.ui.WeatherDetailState
import com.example.weathermini.ui.WeatherViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    viewModel: WeatherViewModel,
    lat: Double,
    lon: Double,
    cityName: String,
    onBack: () -> Unit,
    onOpenNotes: (cityName: String, lat: Double, lon: Double) -> Unit
) {
    LaunchedEffect(lat, lon) {
        viewModel.loadWeather(lat, lon, cityName)
    }

    val state = viewModel.detailState
    val settings by viewModel.settings.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(cityName) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    IconButton(onClick = { onOpenNotes(cityName, lat, lon) }) {
                        Icon(Icons.Default.Edit, contentDescription = "Заметки")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            when (state) {
                is WeatherDetailState.Loading -> CircularProgressIndicator()

                is WeatherDetailState.Error -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = state.message,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(Modifier.height(8.dp))
                        if (state.stale != null) {
                            Text(
                                "⚠ Нет интернета — показаны старые данные",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = { viewModel.loadWeather(lat, lon, cityName) }) {
                            Text("Повторить")
                        }
                    }
                }

                is WeatherDetailState.Success -> {
                    val weather = state.weather.currentWeather
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(16.dp)
                    ) {
                        val displayTemp = formatTemperature(
                            celsius = weather.temperature,
                            unit = settings.temperatureUnit
                        )
                        Text(text = displayTemp, fontSize = 56.sp)

                        Text(
                            text = weatherCodeToDescription(weather.weatherCode),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        HorizontalDivider()

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            WeatherDetailItem(label = "Ветер", value = "${weather.windSpeed} км/ч")
                            WeatherDetailItem(label = "Код погоды", value = "${weather.weatherCode}")
                        }

                        if (state.fromCache) {
                            val timeStr = state.cachedAt?.let { ts ->
                                val fmt = SimpleDateFormat("HH:mm dd.MM", Locale.getDefault())
                                "Кэш от ${fmt.format(Date(ts))}"
                            } ?: "Кэш (нет сети)"

                            Surface(
                                color = MaterialTheme.colorScheme.tertiaryContainer,
                                shape = MaterialTheme.shapes.small
                            ) {
                                Text(
                                    text = "📦 $timeStr",
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            }
                        }

                        Spacer(Modifier.height(8.dp))

                        OutlinedButton(
                            onClick = { onOpenNotes(cityName, lat, lon) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text("Заметки для $cityName")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WeatherDetailItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun formatTemperature(celsius: Double, unit: TemperatureUnit): String =
    when (unit) {
        TemperatureUnit.CELSIUS    -> "${celsius.toInt()}°C"
        TemperatureUnit.FAHRENHEIT -> "${(celsius * 9 / 5 + 32).toInt()}°F"
    }

private fun weatherCodeToDescription(code: Int): String = when (code) {
    0            -> "Ясно"
    1            -> "Преимущественно ясно"
    2            -> "Переменная облачность"
    3            -> "Пасмурно"
    45, 48       -> "Туман"
    51, 53, 55   -> "Морось"
    61, 63, 65   -> "Дождь"
    71, 73, 75   -> "Снег"
    80, 81, 82   -> "Ливни"
    95           -> "Гроза"
    96, 99       -> "Гроза с градом"
    else         -> "Код $code"
}