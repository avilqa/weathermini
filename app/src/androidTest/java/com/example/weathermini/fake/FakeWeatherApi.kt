package com.example.weathermini.fake

import com.example.weathermini.data.model.CityDto
import com.example.weathermini.data.model.CitySearchResponse
import com.example.weathermini.data.model.CurrentWeather
import com.example.weathermini.data.model.WeatherResponse
import com.example.weathermini.data.remote.WeatherApi
import java.io.IOException

class FakeWeatherApi : WeatherApi {

    var citiesToReturn: List<CityDto> = emptyList()
    var weatherToReturn: WeatherResponse =
        WeatherResponse(CurrentWeather(temperature = 20.0, windSpeed = 5.0, weatherCode = 1))
    var shouldThrow: Boolean = false

    override suspend fun searchCity(
        url: String,
        name: String,
        count: Int,
        language: String,
        format: String
    ): CitySearchResponse {
        if (shouldThrow) throw IOException("Fake network error")
        return CitySearchResponse(citiesToReturn)
    }

    override suspend fun getWeather(
        lat: Double,
        lon: Double,
        current: Boolean
    ): WeatherResponse {
        if (shouldThrow) throw IOException("Fake network error")
        return weatherToReturn
    }
}