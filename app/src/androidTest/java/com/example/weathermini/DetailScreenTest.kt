package com.example.weathermini

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.weathermini.data.model.CurrentWeather
import com.example.weathermini.data.model.WeatherResponse
import com.example.weathermini.data.repository.WeatherRepository
import com.example.weathermini.ui.WeatherViewModel
import com.example.weathermini.ui.screens.DetailScreen
import io.mockk.*
import kotlinx.coroutines.flow.flowOf
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DetailScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()


    private fun makeViewModel(
        weatherResult: suspend () -> WeatherResponse
    ): WeatherViewModel {
        val repo = mockk<WeatherRepository>(relaxed = true)
        every { repo.observeFavourites() } returns flowOf(emptyList())
        coEvery { repo.getWeather(any(), any()) } coAnswers { weatherResult() }
        return WeatherViewModel(repo)
    }

    @Test
    fun detailScreen_shows_Retry_on_error() {
        val viewModel = makeViewModel { throw Exception("Network error") }

        composeTestRule.setContent {
            DetailScreen(
                viewModel = viewModel,
                lat = 55.75, lon = 37.62,
                cityName = "Moscow",
                onBack = {}
            )
        }

        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithText("Retry").fetchSemanticsNodes().isNotEmpty()
        }

        composeTestRule.onNodeWithText("Retry").assertIsDisplayed()
        composeTestRule.onNodeWithText("Не удалось загрузить").assertIsDisplayed()
    }

    @Test
    fun clicking_Retry_triggers_new_request_and_shows_Success() {
        var callCount = 0
        val successWeather = WeatherResponse(
            CurrentWeather(temperature = 15.0, windSpeed = 3.0, weatherCode = 0)
        )

        val repo = mockk<WeatherRepository>(relaxed = true)
        every { repo.observeFavourites() } returns flowOf(emptyList())
        coEvery { repo.getWeather(any(), any()) } coAnswers {
            callCount++
            if (callCount == 1) throw Exception("First attempt fails")
            else successWeather
        }

        val viewModel = WeatherViewModel(repo)

        composeTestRule.setContent {
            DetailScreen(
                viewModel = viewModel,
                lat = 55.75, lon = 37.62,
                cityName = "Moscow",
                onBack = {}
            )
        }

        // Ждём появления Retry
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithText("Retry").fetchSemanticsNodes().isNotEmpty()
        }

        composeTestRule.onNodeWithText("Retry").assertIsDisplayed()
        val countAfterFirst = callCount

        // Нажимаем Retry
        composeTestRule.onNodeWithText("Retry").performClick()

        // Ждём появления температуры
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithText("15.0°C").fetchSemanticsNodes().isNotEmpty()
        }

        assertTrue("Retry должен вызвать новый запрос", callCount > countAfterFirst)
        composeTestRule.onNodeWithText("15.0°C").assertIsDisplayed()
        composeTestRule.onNodeWithText("Retry").assertDoesNotExist()
    }
}