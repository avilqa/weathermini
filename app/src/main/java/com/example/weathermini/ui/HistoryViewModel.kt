package com.example.weathermini.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.weathermini.data.local.entity.SearchHistoryEntity
import com.example.weathermini.data.repository.HistoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HistoryUiState(
    val items: List<SearchHistoryEntity> = emptyList(),
    val query: String = "",
    val isEmpty: Boolean = false
)

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val repository: HistoryRepository
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<HistoryUiState> = _query
        .debounce(300L)
        .flatMapLatest { q ->
            if (q.isBlank()) repository.observeRecent()
            else repository.searchByCity(q)
        }
        .map { items ->
            HistoryUiState(
                items = items,
                query = _query.value,
                isEmpty = items.isEmpty()
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = HistoryUiState()
        )

    fun onQueryChange(q: String) { _query.value = q }

    fun clearHistory() {
        viewModelScope.launch { repository.clearAll() }
    }
}