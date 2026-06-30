package com.example.weathermini

import androidx.work.WorkManager
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
    private lateinit var workManager: WorkManager
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
        repository  = mockk(relaxed = true)
        prefs       = mockk()
        workManager = mockk(relaxed = true)

        every { repository.observeFavourites() } returns flowOf(emptyList())
        every { prefs.settings } returns flowOf(AppSettings())

        viewModel = WeatherViewModel(repository, prefs, workManager)
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
            awaitItem() // Idle
            viewModel.onSearchQueryChange("Moscow")
            awaitItem() // Loading
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
            assertTrue(awaitItem() is SearchUiState.Error)
            cancel()
        }
    }

    @Test
    fun `empty search result sets Empty state`() = runTest {
        coEvery { repository.searchCities(any()) } returns emptyList()

        viewModel.searchUiState.test {
            awaitItem() // Idle
            viewModel.onSearchQueryChange("xyz")
            awaitItem() // Loading
            advanceTimeBy(501)
            assertEquals(SearchUiState.Empty, awaitItem())
            cancel()
        }
    }

    @Test
    fun `loadWeather success sets Success state`() = runTest {
        coEvery {
            repository.getWeatherOfflineFirst(55.75, 37.62, "Moscow")
        } returns WeatherResult.Fresh(testWeather)

        viewModel.loadWeather(55.75, 37.62, "Moscow")
        runCurrent()

        val state = viewModel.detailState as WeatherDetailState.Success
        assertEquals(testWeather, state.weather)
        assertFalse(state.fromCache)
    }

    @Test
    fun `loadWeather error with stale sets StaleError state`() = runTest {
        coEvery {
            repository.getWeatherOfflineFirst(any(), any(), any())
        } returns WeatherResult.Error("Нет сети", stale = testWeather)

        viewModel.loadWeather(55.75, 37.62, "Moscow")
        runCurrent()

        val state = viewModel.detailState
        assertTrue("Ожидали StaleError, получили: $state",
            state is WeatherDetailState.StaleError)
        assertEquals(testWeather, (state as WeatherDetailState.StaleError).weather)
    }

    @Test
    fun `loadWeather error without stale sets Error state`() = runTest {
        coEvery {
            repository.getWeatherOfflineFirst(any(), any(), any())
        } returns WeatherResult.Error("Нет сети", stale = null)

        viewModel.loadWeather(55.75, 37.62, "Moscow")
        runCurrent()

        assertTrue(viewModel.detailState is WeatherDetailState.Error)
    }

    @Test
    fun `toggleFavorite calls addFavourite and prefetch when not favourite`() = runTest {
        coEvery { repository.isFavourite(testCity.id) } returns false
        coEvery { repository.prefetchFavourites() } just Runs

        viewModel.toggleFavorite(testCity)
        runCurrent()

        coVerify(exactly = 1) { repository.addFavourite(testCity) }
        coVerify(exactly = 0) { repository.removeFavourite(any()) }
        coVerify(exactly = 1) { repository.prefetchFavourites() }
    }

    @Test
    fun `toggleFavorite calls removeFavourite when already favourite`() = runTest {
        coEvery { repository.isFavourite(testCity.id) } returns true

        viewModel.toggleFavorite(testCity)
        runCurrent()

        coVerify(exactly = 1) { repository.removeFavourite(testCity) }
        coVerify(exactly = 0) { repository.addFavourite(any()) }
        coVerify(exactly = 0) { repository.prefetchFavourites() }
    }

    @Test
    fun `rapid query change - only last result reaches UI`() = runTest {
        val milanResult = listOf(testCity.copy(id = 2, name = "Milan"))
        coEvery { repository.searchCities("Milan") } returns milanResult

        viewModel.searchUiState.test {
            awaitItem() // Idle
            viewModel.onSearchQueryChange("Mo")
            advanceTimeBy(400)
            viewModel.onSearchQueryChange("Milan")
            advanceTimeBy(500)
            val loading = awaitItem()
            assertEquals(SearchUiState.Loading, loading)
            val result = awaitItem()
            assertTrue(result is SearchUiState.Success)
            assertEquals("Milan", (result as SearchUiState.Success).cities[0].city.name)
            coVerify(exactly = 0) { repository.searchCities("Mo") }
            coVerify(exactly = 1) { repository.searchCities("Milan") }
            cancel()
        }
    }

    @Test
    fun `favoriteCities emits correct sequence`() = runTest {
        val fakeFlow = MutableStateFlow<List<CityDto>>(emptyList())
        every { repository.observeFavourites() } returns fakeFlow
        val vm = WeatherViewModel(repository, prefs, workManager)

        vm.favoriteCities.test {
            assertEquals(emptyList<CityDto>(), awaitItem())
            fakeFlow.value = listOf(testCity)
            assertEquals(listOf(testCity), awaitItem())
            fakeFlow.value = emptyList()
            assertEquals(emptyList<CityDto>(), awaitItem())
            cancel()
        }
    }


    @Test
    fun `setBackgroundSyncEnabled true enqueues periodic work`() = runTest {
        coEvery { prefs.setBackgroundSyncEnabled(true) } just Runs

        viewModel.setBackgroundSyncEnabled(true)
        runCurrent()

        coVerify { prefs.setBackgroundSyncEnabled(true) }
        verify { workManager.enqueueUniquePeriodicWork(any(), any(), any()) }
    }

    @Test
    fun `setBackgroundSyncEnabled false cancels periodic work`() = runTest {
        coEvery { prefs.setBackgroundSyncEnabled(false) } just Runs

        viewModel.setBackgroundSyncEnabled(false)
        runCurrent()

        coVerify { prefs.setBackgroundSyncEnabled(false) }
        verify { workManager.cancelUniqueWork(any()) }
    }
}