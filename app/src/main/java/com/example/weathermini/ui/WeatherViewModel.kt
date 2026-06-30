package com.example.weathermini.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.WorkManager
import com.example.weathermini.data.datastore.AppPreferences
import com.example.weathermini.data.datastore.AppSettings
import com.example.weathermini.data.datastore.AppTheme
import com.example.weathermini.data.datastore.TemperatureUnit
import com.example.weathermini.data.model.CityDto
import com.example.weathermini.data.model.WeatherResponse
import com.example.weathermini.data.repository.WeatherRepository
import com.example.weathermini.data.repository.WeatherResult
import com.example.weathermini.worker.WeatherSyncWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject



data class CityUiItem(
    val city: CityDto,
    val isFavourite: Boolean
)

sealed interface SearchUiState {
    object Idle    : SearchUiState
    object Loading : SearchUiState
    object Empty   : SearchUiState
    data class Success(val cities: List<CityUiItem>) : SearchUiState
    data class Error(val message: String) : SearchUiState
}

sealed interface WeatherDetailState {
    object Loading : WeatherDetailState

    data class Success(
        val weather: WeatherResponse,
        val cityName: String,
        val fromCache: Boolean = false,
        val cachedAt: Long? = null
    ) : WeatherDetailState

    data class StaleError(
        val weather: WeatherResponse,
        val cityName: String,
        val errorMessage: String
    ) : WeatherDetailState

    data class Error(val message: String) : WeatherDetailState
}


@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class WeatherViewModel @Inject constructor(
    private val repository: WeatherRepository,
    private val prefs: AppPreferences,
    private val workManager: WorkManager
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _sortMode = MutableStateFlow(SortMode.DEFAULT)
    val sortMode: StateFlow<SortMode> = _sortMode.asStateFlow()

    private val favouriteIds: Flow<Set<Int>> = repository
        .observeFavourites()
        .map { list -> list.map { it.id }.toSet() }

    val settings: StateFlow<AppSettings> = prefs.settings
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = AppSettings()
        )

    private val searchResultsFlow: Flow<SearchUiState> = _searchQuery
        .debounce(500L)
        .distinctUntilChanged()
        .flatMapLatest { query ->
            if (query.isBlank()) {
                flowOf<SearchUiState>(SearchUiState.Idle)
            } else {
                flow<SearchUiState> {
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

    val searchUiState: StateFlow<SearchUiState> = combine(
        searchResultsFlow,
        favouriteIds,
        _sortMode
    ) { state, favIds, sort ->
        when (state) {
            is SearchUiState.Success -> {
                val enriched = state.cities
                    .map { item -> item.copy(isFavourite = item.city.id in favIds) }
                    .let { list ->
                        when (sort) {
                            SortMode.DEFAULT   -> list
                            SortMode.NAME_ASC  -> list.sortedBy { it.city.name }
                            SortMode.NAME_DESC -> list.sortedByDescending { it.city.name }
                            SortMode.COUNTRY   -> list.sortedBy { it.city.country ?: "" }
                        }
                    }
                SearchUiState.Success(enriched)
            }
            else -> state
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SearchUiState.Idle
    )
    val favoriteCities: StateFlow<List<CityDto>> = repository
        .observeFavourites()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    var detailState: WeatherDetailState by mutableStateOf(WeatherDetailState.Loading)
        private set

    fun loadWeather(lat: Double, lon: Double, name: String) {
        detailState = WeatherDetailState.Loading
        viewModelScope.launch {
            detailState = when (val result = repository.getWeatherOfflineFirst(lat, lon, name)) {
                is WeatherResult.Fresh -> WeatherDetailState.Success(
                    weather   = result.weather,
                    cityName  = name,
                    fromCache = false
                )
                is WeatherResult.Cached -> WeatherDetailState.Success(
                    weather   = result.weather,
                    cityName  = name,
                    fromCache = true,
                    cachedAt  = result.cachedAt
                )
                is WeatherResult.Error -> {
                    if (result.stale != null) {
                        WeatherDetailState.StaleError(
                            weather      = result.stale,
                            cityName     = name,
                            errorMessage = result.message
                        )
                    } else {
                        WeatherDetailState.Error(result.message)
                    }
                }
            }
        }
    }


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
                repository.prefetchFavourites()
            }
        }
    }


    fun setTemperatureUnit(unit: TemperatureUnit) {
        viewModelScope.launch { prefs.setTemperatureUnit(unit) }
    }

    fun setTheme(theme: AppTheme) {
        viewModelScope.launch { prefs.setTheme(theme) }
    }

    fun setCacheTtlHours(hours: Int) {
        viewModelScope.launch {
            runCatching { prefs.setCacheTtlHours(hours) }
        }
    }

    fun setBackgroundSyncEnabled(enabled: Boolean) {
        viewModelScope.launch {
            prefs.setBackgroundSyncEnabled(enabled)
            if (enabled) {
                workManager.enqueueUniquePeriodicWork(
                    WeatherSyncWorker.WORK_NAME,
                    ExistingPeriodicWorkPolicy.UPDATE,
                    WeatherSyncWorker.buildRequest()
                )
            } else {
                workManager.cancelUniqueWork(WeatherSyncWorker.WORK_NAME)
            }
        }
    }
}