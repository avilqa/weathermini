package com.example.weathermini.data.remote

import com.example.weathermini.data.model.CitySearchResponse
import com.example.weathermini.data.model.WeatherResponse
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Url

interface WeatherApi {

    @GET
    suspend fun searchCity(
        @Url url: String = "https://geocoding-api.open-meteo.com/v1/search",
        @Query("name") name: String,
        @Query("count") count: Int = 10,
        @Query("language") language: String = "ru",
        @Query("format") format: String = "json"
    ): CitySearchResponse

    @GET("v1/forecast")
    suspend fun getWeather(
        @Query("latitude") lat: Double,
        @Query("longitude") lon: Double,
        @Query("current_weather") current: Boolean = true
    ): WeatherResponse
}