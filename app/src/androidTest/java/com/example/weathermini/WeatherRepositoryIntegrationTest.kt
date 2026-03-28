package com.example.weathermini

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.example.weathermini.data.local.AppDatabase
import com.example.weathermini.data.model.CityDto
import com.example.weathermini.data.repository.WeatherRepository
import com.example.weathermini.fake.FakeWeatherApi
import kotlinx.coroutines.test.runTest
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WeatherRepositoryIntegrationTest {

    private lateinit var db: AppDatabase
    private lateinit var repository: WeatherRepository
    private val fakeApi = FakeWeatherApi()

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
        repository = WeatherRepository(fakeApi, db.favouriteDao())
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
}