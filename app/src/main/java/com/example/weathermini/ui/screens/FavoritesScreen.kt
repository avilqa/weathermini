package com.example.weathermini.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.weathermini.data.model.CityDto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(
    favorites: List<CityDto>,
    onCityClick: (CityDto) -> Unit,
    onFavoriteClick: (CityDto) -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Избранное") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { padding ->
        if (favorites.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) { Text("Список пуст") }
        } else {
            LazyColumn(modifier = Modifier.padding(padding).padding(16.dp)) {
                items(favorites, key = { it.id }) { city ->
                    CityItem(
                        city = city,
                        isFavorite = true,
                        onClick = { onCityClick(city) },
                        onFavoriteClick = { onFavoriteClick(city) }
                    )
                }
            }
        }
    }
}