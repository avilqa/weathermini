package com.example.weathermini

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.example.weathermini.data.datastore.AppPreferences
import com.example.weathermini.data.datastore.AppSettings
import com.example.weathermini.data.local.AppDatabase
import com.example.weathermini.data.local.dao.SearchHistoryDao
import com.example.weathermini.data.local.dao.WeatherCacheDao
import com.example.weathermini.data.local.entity.WeatherCacheEntity
import com.example.weathermini.data.model.CityDto
import com.example.weathermini.data.repository.WeatherRepository
import com.example.weathermini.fake.FakeWeatherApi
import com.google.gson.Gson
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WeatherRepositoryIntegrationTest {

    private lateinit var db: AppDatabase
    private lateinit var repository: WeatherRepository
    private val fakeApi = FakeWeatherApi()
    private val gson = Gson()

    private val prefs: AppPreferences = mockk {
        every { settings } returns flowOf(AppSettings(cacheTtlHours = 3))
    }

    private val testCity = CityDto(
        id = 1, name = "Moscow",
        latitude = 55.75, longitude = 37.62,
        country = "Russia", region = "Oblast"
    )

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()

        repository = WeatherRepository(
            api          = fakeApi,
            favouriteDao = db.favouriteDao(),
            cacheDao     = db.weatherCacheDao(),
            historyDao   = db.searchHistoryDao(),
            prefs        = prefs,
            gson         = gson
        )
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun addFavourite_persists_to_Room_emits_sequence() = runTest {
        repository.observeFavourites().test {
            assertEquals(emptyList<CityDto>(), awaitItem())

            repository.addFavourite(testCity)
            val updated = awaitItem()
            assertEquals(1, updated.size)
            assertEquals(testCity.id, updated[0].id)
            assertEquals(testCity.name, updated[0].name)

            cancel()
        }
    }

    @Test
    fun removeFavourite_removes_city_from_Room() = runTest {
        repository.addFavourite(testCity)

        repository.observeFavourites().test {
            assertEquals(1, awaitItem().size)

            repository.removeFavourite(testCity)
            assertEquals(0, awaitItem().size)

            cancel()
        }
    }

    @Test
    fun addFavourite_twice_no_duplicate() = runTest {
        repository.addFavourite(testCity)
        repository.addFavourite(testCity)

        repository.observeFavourites().test {
            val list = awaitItem()
            assertEquals("Дубль не должен создаваться", 1, list.size)
            cancel()
        }
    }

    @Test
    fun getWeatherOfflineFirst_saves_to_cache_on_fresh_response() = runTest {
        fakeApi.shouldThrow = false

        val result = repository.getWeatherOfflineFirst(55.75, 37.62, "Moscow")

        assertTrue("Ожидали Fresh", result is com.example.weathermini.data.repository.WeatherResult.Fresh)

        val key = WeatherCacheEntity.keyOf(55.75, 37.62)
        val cached = db.weatherCacheDao().get(key)
        assertNotNull("Кэш должен быть сохранён в БД", cached)
        assertEquals("Moscow", cached!!.cityName)
    }

    @Test
    fun getWeatherOfflineFirst_returns_cached_on_second_call() = runTest {
        repository.getWeatherOfflineFirst(55.75, 37.62, "Moscow")

        val result = repository.getWeatherOfflineFirst(55.75, 37.62, "Moscow")
        assertTrue("Второй вызов должен вернуть Cached",
            result is com.example.weathermini.data.repository.WeatherResult.Cached)
    }

    @Test
    fun getWeatherOfflineFirst_records_history_entry() = runTest {
        fakeApi.shouldThrow = false
        repository.getWeatherOfflineFirst(55.75, 37.62, "Moscow")

        db.searchHistoryDao().observeRecent(10).test {
            val items = awaitItem()
            assertEquals("История должна содержать 1 запись", 1, items.size)
            assertEquals("Moscow", items[0].cityName)
            cancel()
        }
    }
}