package com.example.weathermini.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Locale

@Entity(tableName = "weather_cache")
data class WeatherCacheEntity(
    @PrimaryKey
    val cacheKey: String,
    val cityName: String,
    val dataJson: String,
    val cachedAt: Long = System.currentTimeMillis(),
    val ttlMillis: Long = DEFAULT_TTL
) {
    fun isExpired(): Boolean =
        System.currentTimeMillis() > cachedAt + ttlMillis

    companion object {
        const val DEFAULT_TTL = 3L * 60 * 60 * 1000

        fun keyOf(lat: Double, lon: Double): String =
            "%.4f_%.4f".format(Locale.US, lat, lon)
    }
}