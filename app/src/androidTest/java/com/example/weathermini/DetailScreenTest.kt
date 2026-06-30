package com.example.weathermini

import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasProgressBarRangeInfo
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNode
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.weathermini.data.datastore.AppSettings
import com.example.weathermini.data.model.CurrentWeather
import com.example.weathermini.data.model.WeatherResponse
import com.example.weathermini.ui.WeatherDetailState
import com.example.weathermini.ui.screens.DetailScreen
import com.example.weathermini.ui.theme.WeatherMiniTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DetailScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val weatherResult = WeatherResponse(
        CurrentWeather(temperature = 22.0, windSpeed = 4.0, weatherCode = 1)
    )

    @Test
    fun detailScreen_shows_temperature_on_success() {
        val state = WeatherDetailState.Success(
            weather = weatherResult,
            cityName = "Moscow",
            fromCache = false
        )

        composeRule.setContent {
            WeatherMiniTheme {
                DetailScreen(
                    state = state,
                    settings = AppSettings(),
                    onBack = {},
                    onRetry = {},
                    onOpenNotes = {}
                )
            }
        }

        composeRule.onNodeWithText("22°C", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("Moscow").assertIsDisplayed()
    }

    @Test
    fun detailScreen_shows_cache_badge_when_from_cache() {
        val state = WeatherDetailState.Success(
            weather = weatherResult,
            cityName = "Moscow",
            fromCache = true,
            cachedAt = System.currentTimeMillis() - 3600_000L
        )

        composeRule.setContent {
            WeatherMiniTheme {
                DetailScreen(
                    state = state,
                    settings = AppSettings(),
                    onBack = {},
                    onRetry = {},
                    onOpenNotes = {}
                )
            }
        }

        composeRule.onNodeWithText("📦", substring = true).assertIsDisplayed()
    }

    @Test
    fun detailScreen_shows_error_banner_and_stale_data_on_StaleError() {
        val state = WeatherDetailState.StaleError(
            weather = weatherResult,
            cityName = "Moscow",
            errorMessage = "Нет сети"
        )

        composeRule.setContent {
            WeatherMiniTheme {
                DetailScreen(
                    state = state,
                    settings = AppSettings(),
                    onBack = {},
                    onRetry = {},
                    onOpenNotes = {}
                )
            }
        }

        composeRule.onNodeWithText("Нет сети — показаны устаревшие данные", substring = true)
            .assertIsDisplayed()
        composeRule.onNodeWithText("22°C", substring = true).assertIsDisplayed()
    }

    @Test
    fun detailScreen_shows_retry_button_on_error() {
        var retryClicked = false
        val state = WeatherDetailState.Error("Нет подключения")

        composeRule.setContent {
            WeatherMiniTheme {
                DetailScreen(
                    state = state,
                    settings = AppSettings(),
                    onBack = {},
                    onRetry = { retryClicked = true },
                    onOpenNotes = {}
                )
            }
        }

        composeRule.onNodeWithText("Повторить").assertIsDisplayed()
        composeRule.onNodeWithText("Повторить").performClick()
        assert(retryClicked)
    }

    @Test
    fun detailScreen_shows_progress_indicator_on_loading() {
        composeRule.setContent {
            WeatherMiniTheme {
                DetailScreen(
                    state = WeatherDetailState.Loading,
                    settings = AppSettings(),
                    onBack = {},
                    onRetry = {},
                    onOpenNotes = {}
                )
            }
        }

        composeRule.onNode(hasProgressBarRangeInfo(ProgressBarRangeInfo.Indeterminate))
            .assertIsDisplayed()
    }

    @Test
    fun detailScreen_onOpenNotes_called_when_notes_button_clicked() {
        var notesCityName = ""
        val state = WeatherDetailState.Success(
            weather = weatherResult,
            cityName = "Moscow"
        )

        composeRule.setContent {
            WeatherMiniTheme {
                DetailScreen(
                    state = state,
                    settings = AppSettings(),
                    onBack = {},
                    onRetry = {},
                    onOpenNotes = { city -> notesCityName = city }
                )
            }
        }

        composeRule.onNodeWithText("Заметки для Moscow", substring = true).performClick()
        assert(notesCityName == "Moscow")
    }
}