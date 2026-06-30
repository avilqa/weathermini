package com.example.weathermini.data.repository

import com.example.weathermini.data.local.dao.WeatherNoteDao
import com.example.weathermini.data.local.entity.WeatherNoteEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotesRepository @Inject constructor(
    private val dao: WeatherNoteDao
) {
    fun observeByLocation(cityName: String, lat: Double, lon: Double): Flow<List<WeatherNoteEntity>> =
        dao.observeByLocation(cityName, lat, lon)

    fun observeAll(): Flow<List<WeatherNoteEntity>> =
        dao.observeAll()

    suspend fun addNote(
        cityName: String,
        lat: Double,
        lon: Double,
        content: String,
        temperature: Double,
        weatherCode: Int
    ): Long = dao.insert(
        WeatherNoteEntity(
            cityName = cityName,
            lat = lat,
            lon = lon,
            content = content.trim(),
            temperature = temperature,
            weatherCode = weatherCode
        )
    )

    suspend fun updateNote(entity: WeatherNoteEntity) = dao.update(entity)

    suspend fun deleteNote(id: Long) = dao.deleteById(id)
}