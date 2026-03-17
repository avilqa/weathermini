package com.example.weathermini.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.weathermini.data.model.CityDto
import com.example.weathermini.ui.SearchUiState
import com.example.weathermini.ui.WeatherViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    viewModel: WeatherViewModel,
    favoriteIds: Set<Int>,
    onCityClick: (CityDto) -> Unit,
    onNavigateToFavorites: () -> Unit
) {
    val state = viewModel.searchState
    val query = viewModel.searchQuery

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Weather Mini") },
                actions = {
                    IconButton(onClick = onNavigateToFavorites) {
                        Icon(Icons.Default.Favorite, contentDescription = null, tint = Color.Red)
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp)) {
            OutlinedTextField(
                value = query,
                onValueChange = { viewModel.onSearchQueryChange(it) },
                label = { Text("Поиск города") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            when (state) {
                is SearchUiState.Loading -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator() }

                is SearchUiState.Empty ->
                    Text("Города не найдены")

                is SearchUiState.Error ->
                    Text(text = state.message, color = MaterialTheme.colorScheme.error)

                is SearchUiState.Success -> LazyColumn {
                    items(state.cities) { city ->
                        CityItem(
                            city = city,
                            isFavorite = favoriteIds.contains(city.id),
                            onClick = { onCityClick(city) },
                            onFavoriteClick = { viewModel.toggleFavorite(city) }
                        )
                    }
                }

                is SearchUiState.Idle ->
                    Text("Введите название города", color = Color.Gray)
            }
        }
    }
}