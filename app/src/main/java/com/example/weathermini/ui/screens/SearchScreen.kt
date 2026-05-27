package com.example.weathermini.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.weathermini.data.model.CityDto
import com.example.weathermini.ui.SearchUiState
import com.example.weathermini.ui.SortMode
import com.example.weathermini.ui.WeatherViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    viewModel: WeatherViewModel,
    onCityClick: (CityDto) -> Unit,
    onNavigateToFavorites: () -> Unit
) {
    val state    by viewModel.searchUiState.collectAsState()
    val query    by viewModel.searchQuery.collectAsState()
    val sortMode by viewModel.sortMode.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Weather Mini") }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { viewModel.onSearchQueryChange(it) },
                label = { Text("Поиск города") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(8.dp))

            SortModeSelector(
                current = sortMode,
                onSelect = { viewModel.onSortModeChange(it) }
            )

            Spacer(modifier = Modifier.height(8.dp))

            when (state) {
                is SearchUiState.Loading -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator() }

                is SearchUiState.Empty ->
                    Text("Города не найдены")

                is SearchUiState.Error ->
                    Text(
                        text = (state as SearchUiState.Error).message,
                        color = MaterialTheme.colorScheme.error
                    )

                is SearchUiState.Success -> {
                    val items = (state as SearchUiState.Success).cities
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        items(items, key = { it.city.id }) { item ->
                            CityItem(
                                city = item.city,
                                isFavorite = item.isFavourite,
                                onClick = { onCityClick(item.city) },
                                onFavoriteClick = { viewModel.toggleFavorite(item.city) }
                            )
                        }
                    }
                }

                is SearchUiState.Idle ->
                    Text("Введите название города", color = Color.Gray)
            }
        }
    }
}

@Composable
private fun SortModeSelector(current: SortMode, onSelect: (SortMode) -> Unit) {
    val modes = listOf(
        SortMode.DEFAULT   to "По умолчанию",
        SortMode.NAME_ASC  to "А → Я",
        SortMode.NAME_DESC to "Я → А",
        SortMode.COUNTRY   to "По стране"
    )
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        modes.forEach { (mode, label) ->
            FilterChip(
                selected = current == mode,
                onClick = { onSelect(mode) },
                label = { Text(label, style = MaterialTheme.typography.labelSmall) }
            )
        }
    }
}