package com.example.weathermini.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.weathermini.data.model.CityDto
import com.example.weathermini.data.model.WeatherResponse
import com.example.weathermini.data.repository.WeatherRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

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

@HiltViewModel
class WeatherViewModel @Inject constructor(
    private val repository: WeatherRepository
) : ViewModel() {

    var searchState: SearchUiState by mutableStateOf(SearchUiState.Idle)
        private set

    var detailState: WeatherDetailState by mutableStateOf(WeatherDetailState.Loading)
        private set

    var searchQuery by mutableStateOf("")
        private set

    private var searchJob: Job? = null

    val favoriteCities: StateFlow<List<CityDto>> = repository
        .observeFavourites()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    val favoriteIds: StateFlow<Set<Int>> = repository
        .observeFavourites()
        .map { list -> list.map { it.id }.toSet() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptySet()
        )


    fun onSearchQueryChange(newQuery: String) {
        searchQuery = newQuery
        searchJob?.cancel()

        if (newQuery.isBlank()) {
            searchState = SearchUiState.Idle
            return
        }

        searchJob = viewModelScope.launch {
            delay(500)
            performSearch(newQuery)
        }
    }

    private suspend fun performSearch(query: String) {
        searchState = SearchUiState.Loading
        searchState = try {
            val results = repository.searchCities(query)
            if (results.isEmpty()) SearchUiState.Empty
            else SearchUiState.Success(results)
        } catch (e: Exception) {
            SearchUiState.Error("Ошибка: ${e.localizedMessage}")
        }
    }


    fun loadWeather(lat: Double, lon: Double, name: String) {
        detailState = WeatherDetailState.Loading
        viewModelScope.launch {
            detailState = try {
                val weather = repository.getWeather(lat, lon)
                WeatherDetailState.Success(weather, name)
            } catch (e: Exception) {
                WeatherDetailState.Error("Не удалось загрузить")
            }
        }
    }


    fun toggleFavorite(city: CityDto) {
        viewModelScope.launch {
            if (repository.isFavourite(city.id)) {
                repository.removeFavourite(city)
            } else {
                repository.addFavourite(city)
            }
        }
    }
}