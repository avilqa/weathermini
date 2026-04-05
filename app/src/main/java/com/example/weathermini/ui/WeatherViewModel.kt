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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

//UI-модели
data class CityUiItem(
    val city: CityDto,
    val isFavourite: Boolean
)

sealed interface SearchUiState {
    object Idle : SearchUiState
    object Loading : SearchUiState
    object Empty : SearchUiState
    data class Success(val cities: List<CityUiItem>) : SearchUiState
    data class Error(val message: String) : SearchUiState
}

sealed interface WeatherDetailState {
    object Loading : WeatherDetailState
    data class Success(val weather: WeatherResponse, val cityName: String) : WeatherDetailState
    data class Error(val message: String) : WeatherDetailState
}

// ViewModel

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class WeatherViewModel @Inject constructor(
    private val repository: WeatherRepository
) : ViewModel() {

    // Источник 1: строка поиска (пользовательский ввод)
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Источник 2: режим сортировки (пользовательский выбор)
    private val _sortMode = MutableStateFlow(SortMode.DEFAULT)
    val sortMode: StateFlow<SortMode> = _sortMode.asStateFlow()

    // Источник 3: избранные из Room (data layer)
    private val favouriteIds: Flow<Set<Int>> = repository
        .observeFavourites()
        .map { list -> list.map { it.id }.toSet() }

    // Поток результатов поиска из API
    // query -> debounce -> distinctUntilChanged -> flatMapLatest(network)
    private val searchResultsFlow: Flow<SearchUiState> = _searchQuery
        .debounce(500L)
        .distinctUntilChanged()
        .flatMapLatest { query ->
            if (query.isBlank()) {
                flowOf<SearchUiState>(SearchUiState.Idle)  // ← явный тип
            } else {
                flow<SearchUiState> {                       // ← явный тип
                    emit(SearchUiState.Loading)
                    try {
                        val results = repository.searchCities(query)
                        emit(
                            if (results.isEmpty()) SearchUiState.Empty
                            else SearchUiState.Success(results.map { CityUiItem(it, false) })
                        )
                    } catch (e: Exception) {
                        emit(SearchUiState.Error("Ошибка: ${e.localizedMessage}"))
                    }
                }
            }
        }

    // Финальный StateFlow 3 источников
    // searchResultsFlow  favouriteIds  sortMode -> SearchUiState
    val searchUiState: StateFlow<SearchUiState> = combine(
        searchResultsFlow,   // результаты поиска из API
        favouriteIds,        // избранные из Room
        _sortMode            // выбранная сортировка
    ) { state, favIds, sort ->
        when (state) {
            is SearchUiState.Success -> {
                // применяем сортировку
                val enriched = state.cities
                    .map { item -> item.copy(isFavourite = item.city.id in favIds) }
                    .let { list ->
                        when (sort) {
                            SortMode.DEFAULT  -> list
                            SortMode.NAME_ASC -> list.sortedBy { it.city.name }
                            SortMode.NAME_DESC -> list.sortedByDescending { it.city.name }
                            SortMode.COUNTRY  -> list.sortedBy { it.city.country ?: "" }
                        }
                    }
                SearchUiState.Success(enriched)
            }
            else -> state
        }
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = SearchUiState.Idle
        )

    // Избранные города для FavouritesScreen
    val favoriteCities: StateFlow<List<CityDto>> = repository
        .observeFavourites()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    // Detail
    var detailState: WeatherDetailState by mutableStateOf(WeatherDetailState.Loading)
        private set

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

    // Действия пользователя

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun onSortModeChange(mode: SortMode) {
        _sortMode.value = mode
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