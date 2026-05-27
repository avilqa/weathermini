package com.example.weathermini.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.weathermini.data.datastore.AppTheme
import com.example.weathermini.data.datastore.TemperatureUnit
import com.example.weathermini.ui.WeatherViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: WeatherViewModel
) {
    val settings by viewModel.settings.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Настройки") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            SettingsSection("Единица температуры") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = settings.temperatureUnit == TemperatureUnit.CELSIUS,
                        onClick = { viewModel.setTemperatureUnit(TemperatureUnit.CELSIUS) }
                    )
                    Text("Цельсий (°C)")
                    Spacer(Modifier.width(24.dp))
                    RadioButton(
                        selected = settings.temperatureUnit == TemperatureUnit.FAHRENHEIT,
                        onClick = { viewModel.setTemperatureUnit(TemperatureUnit.FAHRENHEIT) }
                    )
                    Text("Фаренгейт (°F)")
                }
            }

            HorizontalDivider()

            SettingsSection("Тема приложения") {
                listOf(
                    AppTheme.SYSTEM to "Системная",
                    AppTheme.LIGHT  to "Светлая",
                    AppTheme.DARK   to "Тёмная"
                ).forEach { (theme, label) ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 2.dp)
                    ) {
                        RadioButton(
                            selected = settings.theme == theme,
                            onClick = { viewModel.setTheme(theme) }
                        )
                        Text(label)
                    }
                }
            }

            HorizontalDivider()

            SettingsSection("Время жизни кэша: ${settings.cacheTtlHours} ч") {
                Slider(
                    value = settings.cacheTtlHours.toFloat(),
                    onValueChange = { viewModel.setCacheTtlHours(it.toInt()) },
                    valueRange = 1f..24f,
                    steps = 22,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("1 ч", style = MaterialTheme.typography.labelSmall)
                    Text("24 ч", style = MaterialTheme.typography.labelSmall)
                }
            }

            HorizontalDivider()

            SettingsSection("Фоновая синхронизация") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Автообновление избранных")
                        Text(
                            "Каждые 6 часов при наличии сети",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = settings.backgroundSyncEnabled,
                        onCheckedChange = { viewModel.setBackgroundSyncEnabled(it) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary
    )
    Spacer(Modifier.height(6.dp))
    Column(content = content)
}