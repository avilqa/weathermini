package com.example.weathermini.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.weathermini.data.local.entity.WeatherNoteEntity
import com.example.weathermini.data.repository.NotesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NotesUiState(
    val notes: List<WeatherNoteEntity> = emptyList(),
    val cityName: String = ""
)

@HiltViewModel
class NotesViewModel @Inject constructor(
    private val repository: NotesRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val cityName: String = checkNotNull(savedStateHandle["cityName"])
    private val lat: Double = checkNotNull(savedStateHandle["lat"])
    private val lon: Double = checkNotNull(savedStateHandle["lon"])

    val uiState: StateFlow<NotesUiState> = repository.observeByCity(cityName)
        .map { notes -> NotesUiState(notes = notes, cityName = cityName) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = NotesUiState(cityName = cityName)
        )

    fun addNote(
        content: String,
        temperature: Double,
        weatherCode: Int
    ) {
        if (content.isBlank()) return
        viewModelScope.launch {
            repository.addNote(
                cityName = cityName,
                lat = lat,
                lon = lon,
                content = content.trim(),
                temperature = temperature,
                weatherCode = weatherCode
            )
        }
    }

    fun updateNote(entity: WeatherNoteEntity, newContent: String) {
        viewModelScope.launch {
            repository.updateNote(entity.copy(content = newContent.trim()))
        }
    }

    fun deleteNote(id: Long) {
        viewModelScope.launch { repository.deleteNote(id) }
    }
}