package com.example.weathermini.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "weather_notes")
data class WeatherNoteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val cityName: String,
    val lat: Double,
    val lon: Double,
    val content: String,
    val temperature: Double,
    val weatherCode: Int,
    val createdAt: Long = System.currentTimeMillis()
)