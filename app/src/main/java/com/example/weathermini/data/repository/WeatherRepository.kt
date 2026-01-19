package com.example.weathermini.data.repository

import com.example.weathermini.data.model.CityDto
import com.example.weathermini.data.model.WeatherResponse
import com.example.weathermini.data.remote.RetrofitClient

class WeatherRepository {
    private val api = RetrofitClient.api

    suspend fun searchCities(query: String): List<CityDto> {
        return api.searchCity(name = query).results ?: emptyList()
    }

    suspend fun getWeather(lat: Double, lon: Double): WeatherResponse {
        return api.getWeather(lat = lat, lon = lon)
    }
}