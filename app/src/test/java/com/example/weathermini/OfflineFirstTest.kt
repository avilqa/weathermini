package com.example.weathermini

import com.example.weathermini.data.datastore.AppPreferences
import com.example.weathermini.data.datastore.AppSettings
import com.example.weathermini.data.local.dao.FavouriteDao
import com.example.weathermini.data.local.dao.SearchHistoryDao
import com.example.weathermini.data.local.dao.WeatherCacheDao
import com.example.weathermini.data.local.entity.FavouriteEntity
import com.example.weathermini.data.local.entity.WeatherCacheEntity
import com.example.weathermini.data.model.CurrentWeather
import com.example.weathermini.data.model.WeatherResponse
import com.example.weathermini.data.remote.WeatherApi
import com.example.weathermini.data.repository.WeatherRepository
import com.example.weathermini.data.repository.WeatherResult
import com.google.gson.Gson
import io.mockk.*
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.IOException

class OfflineFirstTest {

    private lateinit var api: WeatherApi
    private lateinit var cacheDao: WeatherCacheDao
    private lateinit var historyDao: SearchHistoryDao
    private lateinit var favouriteDao: FavouriteDao
    private lateinit var prefs: AppPreferences
    private lateinit var repo: WeatherRepository
    private val gson = Gson()

    private val mockWeather = WeatherResponse(
        CurrentWeather(temperature = 20.0, windSpeed = 5.0, weatherCode = 1)
    )

    @Before
    fun setup() {
        api          = mockk()
        cacheDao     = mockk(relaxed = true)
        historyDao   = mockk(relaxed = true)
        favouriteDao = mockk(relaxed = true)
        prefs        = mockk()

        every { prefs.settings } returns flowOf(AppSettings(cacheTtlHours = 3))
        every { favouriteDao.observeAll() } returns flowOf(emptyList<FavouriteEntity>())

        coEvery { historyDao.count() } returns 10

        repo = WeatherRepository(api, favouriteDao, cacheDao, historyDao, prefs, gson)
    }

    @Test
    fun `returns Cached when fresh cache exists`() = runTest {
        val key = WeatherCacheEntity.keyOf(55.75, 37.62)
        coEvery { cacheDao.get(key) } returns WeatherCacheEntity(
            cacheKey  = key,
            cityName  = "Moscow",
            dataJson  = gson.toJson(mockWeather),
            cachedAt  = System.currentTimeMillis(),
            ttlMillis = 3 * 3_600_000L
        )

        val result = repo.getWeatherOfflineFirst(55.75, 37.62, "Moscow")

        assertTrue("Ожидали Cached", result is WeatherResult.Cached)
        assertEquals(
            20.0,
            (result as WeatherResult.Cached).weather.currentWeather.temperature,
            0.01
        )
        // Сеть НЕ должна быть вызвана
        coVerify(exactly = 0) { api.getWeather(any(), any()) }
    }

    @Test
    fun `returns Fresh when no cache and network succeeds`() = runTest {
        val key = WeatherCacheEntity.keyOf(55.75, 37.62)
        coEvery { cacheDao.get(key) } returns null
        coEvery { api.getWeather(any(), any()) } returns mockWeather

        val result = repo.getWeatherOfflineFirst(55.75, 37.62, "Moscow")

        assertTrue("Ожидали Fresh", result is WeatherResult.Fresh)
        coVerify { cacheDao.put(any()) }
        coVerify { historyDao.insert(any()) }
    }

    @Test
    fun `returns Error with stale data when network fails and stale cache exists`() = runTest {
        val key = WeatherCacheEntity.keyOf(55.75, 37.62)
        coEvery { cacheDao.get(key) } returns WeatherCacheEntity(
            cacheKey  = key,
            cityName  = "Moscow",
            dataJson  = gson.toJson(mockWeather),
            cachedAt  = System.currentTimeMillis() - 4 * 3_600_000L,
            ttlMillis = 3 * 3_600_000L
        )
        coEvery { api.getWeather(any(), any()) } throws IOException("No network")

        val result = repo.getWeatherOfflineFirst(55.75, 37.62, "Moscow")

        assertTrue("Ожидали Error", result is WeatherResult.Error)
        val error = result as WeatherResult.Error
        assertNotNull("Stale данные должны присутствовать", error.stale)
        assertEquals(20.0, error.stale!!.currentWeather.temperature, 0.01)
    }

    @Test
    fun `returns Error without stale when no cache and network fails`() = runTest {
        val key = WeatherCacheEntity.keyOf(55.75, 37.62)
        coEvery { cacheDao.get(key) } returns null
        coEvery { api.getWeather(any(), any()) } throws IOException("No network")

        val result = repo.getWeatherOfflineFirst(55.75, 37.62, "Moscow")

        assertTrue("Ожидали Error", result is WeatherResult.Error)
        assertNull("Stale не должно быть", (result as WeatherResult.Error).stale)
    }

    @Test
    fun `writes history with fromCache=false on Fresh response`() = runTest {
        val key = WeatherCacheEntity.keyOf(55.75, 37.62)
        coEvery { cacheDao.get(key) } returns null
        coEvery { api.getWeather(any(), any()) } returns mockWeather

        repo.getWeatherOfflineFirst(55.75, 37.62, "Moscow")

        coVerify {
            historyDao.insert(
                match { entity -> !entity.fromCache && entity.cityName == "Moscow" }
            )
        }
    }

    @Test
    fun `writes history with fromCache=true on Cached response`() = runTest {
        val key = WeatherCacheEntity.keyOf(55.75, 37.62)
        coEvery { cacheDao.get(key) } returns WeatherCacheEntity(
            cacheKey  = key,
            cityName  = "Moscow",
            dataJson  = gson.toJson(mockWeather),
            cachedAt  = System.currentTimeMillis(),
            ttlMillis = 3 * 3_600_000L
        )

        repo.getWeatherOfflineFirst(55.75, 37.62, "Moscow")

        coVerify {
            historyDao.insert(match { entity -> entity.fromCache })
        }
    }
}