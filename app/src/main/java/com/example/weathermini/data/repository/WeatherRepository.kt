package com.example.weathermini.data.repository

import com.example.weathermini.data.datastore.AppPreferences
import com.example.weathermini.data.local.dao.FavouriteDao
import com.example.weathermini.data.local.dao.SearchHistoryDao
import com.example.weathermini.data.local.dao.WeatherCacheDao
import com.example.weathermini.data.local.entity.FavouriteEntity
import com.example.weathermini.data.local.entity.SearchHistoryEntity
import com.example.weathermini.data.local.entity.WeatherCacheEntity
import com.example.weathermini.data.model.CityDto
import com.example.weathermini.data.model.WeatherResponse
import com.example.weathermini.data.remote.WeatherApi
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

sealed interface WeatherResult {
    data class Fresh(val weather: WeatherResponse) : WeatherResult
    data class Cached(val weather: WeatherResponse, val cachedAt: Long) : WeatherResult
    data class Error(val message: String, val stale: WeatherResponse? = null) : WeatherResult
}

@Singleton
class WeatherRepository @Inject constructor(
    private val api: WeatherApi,
    private val favouriteDao: FavouriteDao,
    private val cacheDao: WeatherCacheDao,
    private val historyDao: SearchHistoryDao,
    private val prefs: AppPreferences,
    private val gson: Gson
) {


    suspend fun searchCities(query: String): List<CityDto> =
        api.searchCity(name = query).results ?: emptyList()

    suspend fun getWeather(lat: Double, lon: Double): WeatherResponse =
        api.getWeather(lat = lat, lon = lon)

    fun observeFavourites(): Flow<List<CityDto>> =
        favouriteDao.observeAll().map { list -> list.map { it.toCityDto() } }

    suspend fun isFavourite(id: Int): Boolean =
        favouriteDao.countById(id) > 0

    suspend fun addFavourite(city: CityDto) =
        favouriteDao.insert(city.toEntity())

    suspend fun removeFavourite(city: CityDto) =
        favouriteDao.delete(city.toEntity())


    suspend fun getWeatherOfflineFirst(
        lat: Double,
        lon: Double,
        cityName: String
    ): WeatherResult {
        val ttlMs = prefs.settings.first().cacheTtlHours * 3_600_000L
        val key   = WeatherCacheEntity.keyOf(lat, lon)

        val cached = cacheDao.get(key)
        if (cached != null && !cached.copy(ttlMillis = ttlMs).isExpired()) {
            val weather = gson.fromJson(cached.dataJson, WeatherResponse::class.java)
            writeHistory(cityName, lat, lon, weather, fromCache = true)
            return WeatherResult.Cached(weather, cached.cachedAt)
        }

        return try {
            val weather = api.getWeather(lat, lon)
            cacheDao.put(
                WeatherCacheEntity(
                    cacheKey  = key,
                    cityName  = cityName,
                    dataJson  = gson.toJson(weather),
                    ttlMillis = ttlMs
                )
            )
            writeHistory(cityName, lat, lon, weather, fromCache = false)
            WeatherResult.Fresh(weather)
        } catch (e: Exception) {
            val stale = cached?.let { gson.fromJson(it.dataJson, WeatherResponse::class.java) }
            WeatherResult.Error(e.localizedMessage ?: "Ошибка сети", stale)
        }
    }


    suspend fun prefetchFavourites() {
        val ttlMs = prefs.settings.first().cacheTtlHours * 3_600_000L

        val cities: List<FavouriteEntity> = favouriteDao.observeAll().first()

        cities.forEach { city ->
            try {
                val weather = api.getWeather(city.latitude, city.longitude)
                val key = WeatherCacheEntity.keyOf(city.latitude, city.longitude)
                cacheDao.put(
                    WeatherCacheEntity(
                        cacheKey  = key,
                        cityName  = city.name,
                        dataJson  = gson.toJson(weather),
                        ttlMillis = ttlMs
                    )
                )
            } catch (_: Exception) {
            }
        }
    }


    private suspend fun writeHistory(
        cityName: String,
        lat: Double,
        lon: Double,
        weather: WeatherResponse,
        fromCache: Boolean
    ) {
        historyDao.insert(
            SearchHistoryEntity(
                cityName    = cityName,
                lat         = lat,
                lon         = lon,
                temperature = weather.currentWeather.temperature,
                weatherCode = weather.currentWeather.weatherCode,
                fromCache   = fromCache
            )
        )
        if (historyDao.count() > 200) historyDao.keepLatest(200)
    }

    private fun FavouriteEntity.toCityDto() = CityDto(
        id        = id,
        name      = name,
        latitude  = latitude,
        longitude = longitude,
        country   = country,
        region    = region
    )

    private fun CityDto.toEntity() = FavouriteEntity(
        id        = id,
        name      = name,
        latitude  = latitude,
        longitude = longitude,
        country   = country ?: "",
        region    = region  ?: ""
    )
}