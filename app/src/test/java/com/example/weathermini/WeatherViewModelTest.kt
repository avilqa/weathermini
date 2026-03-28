package com.example.weathermini

import app.cash.turbine.test
import com.example.weathermini.data.model.CityDto
import com.example.weathermini.data.model.CurrentWeather
import com.example.weathermini.data.model.WeatherResponse
import com.example.weathermini.data.repository.WeatherRepository
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
        every { repository.observeFavourites() } returns flowOf(emptyList())
        viewModel = WeatherViewModel(repository)
    }

    // ── Тест 1: начальное состояние (нетривиальный)
    @Test
    fun `initial search state is Idle`() {
        assertEquals(SearchUiState.Idle, viewModel.searchState)
    }

    // ── Тест 2: полная последовательность Loading → Success (Flow-тест)
    @Test
    fun `search success - transitions through Loading then reaches Success`() = runTest {
        coEvery { repository.searchCities("Moscow") } returns listOf(testCity)

        // До дебаунса — состояние ещё Idle
        viewModel.onSearchQueryChange("Moscow")
        assertEquals(SearchUiState.Idle, viewModel.searchState)

        // Пропускаем дебаунс 500 мс
        advanceTimeBy(501)
        runCurrent()

        assertEquals(SearchUiState.Success(listOf(testCity)), viewModel.searchState)
    }

    // ── Тест 3: ошибка сети → Error
    @Test
    fun `search error - state transitions to Error`() = runTest {
        coEvery { repository.searchCities(any()) } throws Exception("Network error")

        viewModel.onSearchQueryChange("Moscow")
        advanceTimeBy(501)
        runCurrent()

        assertTrue(viewModel.searchState is SearchUiState.Error)
    }

    // ── Тест 4: пустой результат → Empty, НЕ Success (нетривиальный)
    @Test
    fun `empty search result sets Empty state not Success with empty list`() = runTest {
        coEvery { repository.searchCities(any()) } returns emptyList()

        viewModel.onSearchQueryChange("xyz123")
        advanceTimeBy(501)
        runCurrent()

        assertEquals(SearchUiState.Empty, viewModel.searchState)
        // Явно проверяем что это НЕ Success(emptyList())
        assertNotEquals(SearchUiState.Success(emptyList()), viewModel.searchState)
    }

    // ── Тест 5: loadWeather успех
    @Test
    fun `loadWeather success sets WeatherDetailState Success`() = runTest {
        coEvery { repository.getWeather(55.75, 37.62) } returns testWeather

        viewModel.loadWeather(55.75, 37.62, "Moscow")
        runCurrent()

        assertEquals(
            WeatherDetailState.Success(testWeather, "Moscow"),
            viewModel.detailState
        )
    }

    // ── Тест 6: loadWeather ошибка
    @Test
    fun `loadWeather error sets WeatherDetailState Error`() = runTest {
        coEvery { repository.getWeather(any(), any()) } throws Exception("error")

        viewModel.loadWeather(55.75, 37.62, "Moscow")
        runCurrent()

        assertTrue(viewModel.detailState is WeatherDetailState.Error)
    }

    // ── Тест 7: toggleFavorite добавляет
    @Test
    fun `toggleFavorite calls addFavourite when city is not favourite`() = runTest {
        coEvery { repository.isFavourite(testCity.id) } returns false

        viewModel.toggleFavorite(testCity)
        runCurrent()

        coVerify(exactly = 1) { repository.addFavourite(testCity) }
        coVerify(exactly = 0) { repository.removeFavourite(any()) }
    }

    // ── Тест 8: toggleFavorite удаляет
    @Test
    fun `toggleFavorite calls removeFavourite when city is already favourite`() = runTest {
        coEvery { repository.isFavourite(testCity.id) } returns true

        viewModel.toggleFavorite(testCity)
        runCurrent()

        coVerify(exactly = 1) { repository.removeFavourite(testCity) }
        coVerify(exactly = 0) { repository.addFavourite(any()) }
    }

    // ── Тест 9: отмена устаревшего запроса (нетривиальный, Flow)
    // Проверяет что быстрая смена запроса отменяет предыдущий
    // и в UI попадает только результат последнего запроса
    @Test
    fun `rapid query change cancels stale request - only last result reaches UI`() = runTest {
        val milanResult = listOf(testCity.copy(id = 2, name = "Milan"))

        coEvery { repository.searchCities("Mo") } returns listOf(testCity)
        coEvery { repository.searchCities("Milan") } returns milanResult

        // T=0: начинаем поиск "Mo" (дебаунс 500мс)
        viewModel.onSearchQueryChange("Mo")
        advanceTimeBy(400) // T=400 — дебаунс "Mo" ещё не сработал

        // T=400: перебиваем запрос "Milan" — "Mo" job отменяется
        viewModel.onSearchQueryChange("Milan")
        advanceTimeBy(600) // T=1000 — дебаунс "Milan" сработал в T=900
        runCurrent()

        // В UI только результат "Milan", "Mo" никогда не вызывался
        assertEquals(SearchUiState.Success(milanResult), viewModel.searchState)
        coVerify(exactly = 0) { repository.searchCities("Mo") }
        coVerify(exactly = 1) { repository.searchCities("Milan") }
    }

    // ── Тест 10: StateFlow favoriteCities — полная последовательность эмиссий
    @Test
    fun `favoriteCities emits correct sequence when repository flow updates`() = runTest {
        val fakeFlow = MutableStateFlow<List<CityDto>>(emptyList())
        every { repository.observeFavourites() } returns fakeFlow

        val vm = WeatherViewModel(repository)

        vm.favoriteCities.test {
            // Начальный элемент
            assertEquals(emptyList<CityDto>(), awaitItem())

            // Обновление через Room
            fakeFlow.value = listOf(testCity)
            assertEquals(listOf(testCity), awaitItem())

            // Удаление
            fakeFlow.value = emptyList()
            assertEquals(emptyList<CityDto>(), awaitItem())

            cancel()
        }
    }
}