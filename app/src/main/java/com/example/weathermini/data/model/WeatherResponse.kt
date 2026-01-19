package com.example.weathermini.data.model

import com.google.gson.annotations.SerializedName

data class WeatherResponse(
    @SerializedName("current_weather") val currentWeather: CurrentWeather
)

data class CurrentWeather(
    val temperature: Double,
    @SerializedName("windspeed") val windSpeed: Double,
    @SerializedName("weathercode") val weatherCode: Int
)