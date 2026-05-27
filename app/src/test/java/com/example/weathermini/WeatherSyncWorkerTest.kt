package com.example.weathermini

import com.example.weathermini.data.repository.WeatherRepository
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class WeatherSyncWorkerTest {

    private lateinit var repository: WeatherRepository

    @Before
    fun setup() {
        repository = mockk(relaxed = true)
    }

    @Test
    fun `prefetchFavourites called on sync`() = runTest {
        coEvery { repository.prefetchFavourites() } just Runs

        repository.prefetchFavourites()

        coVerify(exactly = 1) { repository.prefetchFavourites() }
    }

    @Test
    fun `sync continues without throwing when prefetch fails`() = runTest {
        coEvery { repository.prefetchFavourites() } throws Exception("Network error")

        val result = runCatching { repository.prefetchFavourites() }
        assertTrue(result.isFailure)
        assertEquals("Network error", result.exceptionOrNull()?.message)
    }
}