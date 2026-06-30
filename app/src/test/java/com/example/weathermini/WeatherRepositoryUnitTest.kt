package com.example.weathermini

import com.example.weathermini.data.datastore.AppPreferences
import com.example.weathermini.data.datastore.AppSettings
import com.example.weathermini.data.local.dao.FavouriteDao
import com.example.weathermini.data.local.dao.SearchHistoryDao
import com.example.weathermini.data.local.dao.WeatherCacheDao
import com.example.weathermini.data.local.entity.FavouriteEntity
import com.example.weathermini.data.model.CityDto
import com.example.weathermini.data.model.CitySearchResponse
import com.example.weathermini.data.model.CurrentWeather
import com.example.weathermini.data.model.WeatherResponse
import com.example.weathermini.data.remote.WeatherApi
import com.example.weathermini.data.repository.WeatherRepository
import com.google.gson.Gson
import io.mockk.*
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.*
import org.junit.Assert.*

class WeatherRepositoryUnitTest {

    private val api: WeatherApi = mockk()
    private val dao: FavouriteDao = mockk(relaxed = true)

    private val cacheDao: WeatherCacheDao = mockk(relaxed = true)
    private val historyDao: SearchHistoryDao = mockk(relaxed = true)
    private val prefs: AppPreferences = mockk()
    private val gson = Gson()

    private val repository: WeatherRepository

    init {
        every { dao.observeAll() } returns flowOf(emptyList<FavouriteEntity>())
        every { prefs.settings } returns flowOf(AppSettings(cacheTtlHours = 3))

        repository = WeatherRepository(api, dao, cacheDao, historyDao, prefs, gson)
    }

    private val cityDto = CityDto(
        id = 1, name = "Moscow",
        latitude = 55.75, longitude = 37.62,
        country = "Russia", region = "Oblast"
    )

    @Test
    fun `searchCities returns mapped list from api response`() = runTest {
        coEvery {
            api.searchCity(any(), any(), any(), any(), any())
        } returns CitySearchResponse(listOf(cityDto))

        val result = repository.searchCities("Moscow")

        assertEquals(1, result.size)
        assertEquals(cityDto, result[0])
        coVerify { api.searchCity(name = "Moscow") }
    }

    @Test
    fun `searchCities returns empty list when api results is null`() = runTest {
        coEvery {
            api.searchCity(any(), any(), any(), any(), any())
        } returns CitySearchResponse(results = null)

        val result = repository.searchCities("xyz")

        assertTrue(result.isEmpty())
    }

    @Test
    fun `isFavourite returns false when count is 0 and true when count is 1`() = runTest {
        coEvery { dao.countById(1) } returns 0
        coEvery { dao.countById(2) } returns 1

        assertFalse(repository.isFavourite(1))
        assertTrue(repository.isFavourite(2))
    }
}