package com.example.weathermini.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.weathermini.data.model.CityDto
import com.example.weathermini.ui.CityUiItem
import com.example.weathermini.ui.SearchUiState
import com.example.weathermini.ui.SortMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    state: SearchUiState,
    query: String,
    sortMode: SortMode,
    onQueryChange: (String) -> Unit,
    onSortModeChange: (SortMode) -> Unit,
    onCityClick: (CityDto) -> Unit,
    onToggleFavourite: (CityDto) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Weather Mini") })
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp)) {
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                label = { Text("Поиск города") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(8.dp))

            SortModeSelector(current = sortMode, onSelect = onSortModeChange)

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
                        text = state.message,
                        color = MaterialTheme.colorScheme.error
                    )

                is SearchUiState.Success ->
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        items(state.cities, key = { it.city.id }) { item ->
                            CityItem(
                                city = item.city,
                                isFavorite = item.isFavourite,
                                onClick = { onCityClick(item.city) },
                                onFavoriteClick = { onToggleFavourite(item.city) }
                            )
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
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        listOf(
            SortMode.DEFAULT   to "По умолчанию",
            SortMode.NAME_ASC  to "А → Я",
            SortMode.NAME_DESC to "Я → А",
            SortMode.COUNTRY   to "По стране"
        ).forEach { (mode, label) ->
            FilterChip(
                selected = current == mode,
                onClick = { onSelect(mode) },
                label = { Text(label, style = MaterialTheme.typography.labelSmall) }
            )
        }
    }
}