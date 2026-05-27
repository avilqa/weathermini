package com.example.weathermini

import app.cash.turbine.test
import com.example.weathermini.data.datastore.AppPreferences
import com.example.weathermini.data.datastore.AppSettings
import com.example.weathermini.data.model.CityDto
import com.example.weathermini.data.model.CurrentWeather
import com.example.weathermini.data.model.WeatherResponse
import com.example.weathermini.data.repository.WeatherRepository
import com.example.weathermini.data.repository.WeatherResult
import com.example.weathermini.ui.SearchUiState
import com.example.weathermini.ui.WeatherDetailState
import com.example.weathermini.ui.WeatherViewModel
import com.example.weathermini.util.MainDispatcherRule
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class WeatherViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var repository: WeatherRepository
    private lateinit var prefs: AppPreferences
    private lateinit var viewModel: WeatherViewModel

    private val testCity = CityDto(
        id = 1, name = "Moscow",
        latitude = 55.75, longitude = 37.62,
        country = "Russia", region = "Moscow Oblast"
    )
    private val testWeather = WeatherResponse(
        currentWeather = CurrentWeather(temperature = 20.0, windSpeed = 5.0, weatherCode = 1)
    )

    @Before
    fun setUp() {
        repository = mockk(relaxed = true)
        prefs = mockk()
        every { repository.observeFavourites() } returns flowOf(emptyList())
        every { prefs.settings } returns flowOf(AppSettings())
        viewModel = WeatherViewModel(repository, prefs)
    }

    @Test
    fun `initial search state is Idle`() = runTest {
        viewModel.searchUiState.test {
            assertEquals(SearchUiState.Idle, awaitItem())
            cancel()
        }
    }

    @Test
    fun `search success - transitions through Loading then reaches Success`() = runTest {
        coEvery { repository.searchCities("Moscow") } returns listOf(testCity)

        viewModel.searchUiState.test {
            awaitItem()

            viewModel.onSearchQueryChange("Moscow")
            awaitItem()

            advanceTimeBy(501)
            val result = awaitItem()
            assertTrue("Ожидали Success, получили: $result", result is SearchUiState.Success)
            val cities = (result as SearchUiState.Success).cities
            assertEquals(1, cities.size)
            assertEquals(testCity, cities[0].city)

            cancel()
        }
    }

    @Test
    fun `search error - state transitions to Error`() = runTest {
        coEvery { repository.searchCities(any()) } throws Exception("Network error")

        viewModel.searchUiState.test {
            awaitItem() // Idle
            viewModel.onSearchQueryChange("Moscow")
            awaitItem() // Loading
            advanceTimeBy(501)
            val result = awaitItem()
            assertTrue("Ожидали Error, получили: $result", result is SearchUiState.Error)
            cancel()
        }
    }

    @Test
    fun `empty search result sets Empty state not Success with empty list`() = runTest {
        coEvery { repository.searchCities(any()) } returns emptyList()

        viewModel.searchUiState.test {
            awaitItem() // Idle
            viewModel.onSearchQueryChange("xyz123")
            awaitItem() // Loading
            advanceTimeBy(501)
            val result = awaitItem()
            assertEquals("Ожидали Empty", SearchUiState.Empty, result)
            assertNotEquals(SearchUiState.Success(emptyList()), result)
            cancel()
        }
    }

    @Test
    fun `loadWeather success sets WeatherDetailState Success`() = runTest {
        coEvery {
            repository.getWeatherOfflineFirst(55.75, 37.62, "Moscow")
        } returns WeatherResult.Fresh(testWeather)

        viewModel.loadWeather(55.75, 37.62, "Moscow")
        runCurrent()

        val state = viewModel.detailState
        assertTrue("Ожидали Success", state is WeatherDetailState.Success)
        val success = state as WeatherDetailState.Success
        assertEquals(testWeather, success.weather)
        assertEquals("Moscow", success.cityName)
        assertFalse(success.fromCache)
    }

    @Test
    fun `loadWeather error sets WeatherDetailState Error`() = runTest {
        coEvery {
            repository.getWeatherOfflineFirst(any(), any(), any())
        } returns WeatherResult.Error("Ошибка сети")

        viewModel.loadWeather(55.75, 37.62, "Moscow")
        runCurrent()

        assertTrue(viewModel.detailState is WeatherDetailState.Error)
    }

    @Test
    fun `loadWeather cached shows fromCache flag`() = runTest {
        val cachedAt = System.currentTimeMillis()
        coEvery {
            repository.getWeatherOfflineFirst(55.75, 37.62, "Moscow")
        } returns WeatherResult.Cached(testWeather, cachedAt)

        viewModel.loadWeather(55.75, 37.62, "Moscow")
        runCurrent()

        val state = viewModel.detailState as WeatherDetailState.Success
        assertTrue(state.fromCache)
        assertEquals(cachedAt, state.cachedAt)
    }

    @Test
    fun `toggleFavorite calls addFavourite when city is not favourite`() = runTest {
        coEvery { repository.isFavourite(testCity.id) } returns false
        coEvery { repository.prefetchFavourites() } just Runs

        viewModel.toggleFavorite(testCity)
        runCurrent()

        coVerify(exactly = 1) { repository.addFavourite(testCity) }
        coVerify(exactly = 0) { repository.removeFavourite(any()) }
        coVerify(exactly = 1) { repository.prefetchFavourites() }
    }

    @Test
    fun `toggleFavorite calls removeFavourite when city is already favourite`() = runTest {
        coEvery { repository.isFavourite(testCity.id) } returns true

        viewModel.toggleFavorite(testCity)
        runCurrent()

        coVerify(exactly = 1) { repository.removeFavourite(testCity) }
        coVerify(exactly = 0) { repository.addFavourite(any()) }
        coVerify(exactly = 0) { repository.prefetchFavourites() }
    }

    @Test
    fun `rapid query change cancels stale request - only last result reaches UI`() = runTest {
        val milanResult = listOf(testCity.copy(id = 2, name = "Milan"))

        coEvery { repository.searchCities("Mo") } returns listOf(testCity)
        coEvery { repository.searchCities("Milan") } returns milanResult

        viewModel.searchUiState.test {
            awaitItem()

            viewModel.onSearchQueryChange("Mo")

            advanceTimeBy(400)

            viewModel.onSearchQueryChange("Milan")

            advanceTimeBy(500)
            val loading = awaitItem()
            assertEquals(
                "После debounce должен быть Loading",
                SearchUiState.Loading,
                loading
            )

            val result = awaitItem()
            assertTrue("Ожидали Success для Milan, получили: $result",
                result is SearchUiState.Success)
            val cities = (result as SearchUiState.Success).cities
            assertEquals("Milan", cities[0].city.name)

            coVerify(exactly = 0) { repository.searchCities("Mo") }
            coVerify(exactly = 1) { repository.searchCities("Milan") }

            cancel()
        }
    }

    @Test
    fun `favoriteCities emits correct sequence when repository flow updates`() = runTest {
        val fakeFlow = MutableStateFlow<List<CityDto>>(emptyList())
        every { repository.observeFavourites() } returns fakeFlow

        val vm = WeatherViewModel(repository, prefs)

        vm.favoriteCities.test {
            assertEquals(emptyList<CityDto>(), awaitItem())

            fakeFlow.value = listOf(testCity)
            assertEquals(listOf(testCity), awaitItem())

            fakeFlow.value = emptyList()
            assertEquals(emptyList<CityDto>(), awaitItem())

            cancel()
        }
    }
}