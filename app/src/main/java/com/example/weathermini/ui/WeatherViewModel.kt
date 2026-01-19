package com.example.weathermini.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.weathermini.data.model.CityDto
import com.example.weathermini.data.model.WeatherResponse
import com.example.weathermini.data.remote.RetrofitClient
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


sealed interface SearchUiState {
    object Idle : SearchUiState
    object Loading : SearchUiState
    object Empty : SearchUiState
    data class Success(val cities: List<CityDto>) : SearchUiState
    data class Error(val message: String) : SearchUiState
}

sealed interface WeatherDetailState {
    object Loading : WeatherDetailState
    data class Success(val weather: WeatherResponse, val cityName: String) : WeatherDetailState
    data class Error(val message: String) : WeatherDetailState
}

class WeatherViewModel : ViewModel() {

    var searchState: SearchUiState by mutableStateOf(SearchUiState.Idle)
        private set

    var detailState: WeatherDetailState by mutableStateOf(WeatherDetailState.Loading)
        private set

    var searchQuery by mutableStateOf("")
        private set
    private var searchJob: Job? = null

    var favoriteCities by mutableStateOf<List<CityDto>>(emptyList())
        private set

    val favoriteIds: Set<Int>
        get() = favoriteCities.map { it.id }.toSet()



    fun onSearchQueryChange(newQuery: String) {
        searchQuery = newQuery
        searchJob?.cancel()

        if (newQuery.isBlank()) {
            searchState = SearchUiState.Idle
            return
        }

        searchJob = viewModelScope.launch {
            delay(500) // Debounce
            performSearch(newQuery)
        }
    }

    private suspend fun performSearch(query: String) {
        searchState = SearchUiState.Loading
        try {
            val response = RetrofitClient.api.searchCity(name = query)
            searchState = if (response.results.isNullOrEmpty()) {
                SearchUiState.Empty
            } else {
                SearchUiState.Success(response.results)
            }
        } catch (e: Exception) {
            searchState = SearchUiState.Error("Ошибка: ${e.localizedMessage}")
        }
    }

    fun loadWeather(lat: Double, lon: Double, name: String) {
        detailState = WeatherDetailState.Loading
        viewModelScope.launch {
            try {
                val weather = RetrofitClient.api.getWeather(lat, lon)
                detailState = WeatherDetailState.Success(weather, name)
            } catch (e: Exception) {
                detailState = WeatherDetailState.Error("Не удалось загрузить")
            }
        }
    }

    fun toggleFavorite(city: CityDto) {
        val currentList = favoriteCities.toMutableList()
        val existing = currentList.find { it.id == city.id }

        if (existing != null) {
            currentList.remove(existing)
        } else {
            currentList.add(city)
        }
        favoriteCities = currentList
    }
}