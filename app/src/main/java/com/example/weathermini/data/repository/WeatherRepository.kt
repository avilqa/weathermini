package com.example.weathermini.data.repository

import com.example.weathermini.data.local.dao.FavouriteDao
import com.example.weathermini.data.local.entity.FavouriteEntity
import com.example.weathermini.data.model.CityDto
import com.example.weathermini.data.model.WeatherResponse
import com.example.weathermini.data.remote.WeatherApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WeatherRepository @Inject constructor(
    private val api: WeatherApi,
    private val favouriteDao: FavouriteDao
) {


    suspend fun searchCities(query: String): List<CityDto> =
        api.searchCity(name = query).results ?: emptyList()

    suspend fun getWeather(lat: Double, lon: Double): WeatherResponse =
        api.getWeather(lat = lat, lon = lon)


    fun observeFavourites(): Flow<List<CityDto>> =
        favouriteDao.observeAll().map { entities ->
            entities.map { it.toCityDto() }
        }

    suspend fun isFavourite(id: Int): Boolean =
        favouriteDao.countById(id) > 0

    suspend fun addFavourite(city: CityDto) =
        favouriteDao.insert(city.toEntity())

    suspend fun removeFavourite(city: CityDto) =
        favouriteDao.delete(city.toEntity())


    private fun FavouriteEntity.toCityDto() = CityDto(
        id = id,
        name = name,
        latitude = latitude,
        longitude = longitude,
        country = country,
        region = region
    )

    private fun CityDto.toEntity() = FavouriteEntity(
        id = id,
        name = name,
        latitude = latitude,
        longitude = longitude,
        country = country ?: "",
        region = region ?: ""
    )
}