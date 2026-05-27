package com.example.weathermini.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "search_history")
data class SearchHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val cityName: String,
    val lat: Double,
    val lon: Double,
    val temperature: Double,
    val weatherCode: Int,
    val searchedAt: Long = System.currentTimeMillis(),
    val fromCache: Boolean = false
)