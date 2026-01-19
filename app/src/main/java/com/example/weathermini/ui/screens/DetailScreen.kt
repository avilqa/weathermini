package com.example.weathermini.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.weathermini.ui.WeatherDetailState
import com.example.weathermini.ui.WeatherViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    viewModel: WeatherViewModel,
    lat: Double,
    lon: Double,
    cityName: String,
    onBack: () -> Unit
) {
    LaunchedEffect(Unit) {
        viewModel.loadWeather(lat, lon, cityName)
    }

    val state = viewModel.detailState

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(cityName) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
                        Text(state.message)
                        Button(onClick = { viewModel.loadWeather(lat, lon, cityName) }) {
                            Text("Retry")
                        }
                    }
                }
                is WeatherDetailState.Success -> {
                    val weather = state.weather.currentWeather
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(text = "${weather.temperature}°C", fontSize = 48.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(text = "Ветер: ${weather.windSpeed} km/h")
                        Text(text = "Код погоды: ${weather.weatherCode}")
                    }
                }
            }
        }
    }
}