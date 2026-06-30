package com.example.weathermini

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import com.example.weathermini.data.repository.WeatherRepository
import com.example.weathermini.worker.WeatherSyncWorker
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WeatherSyncWorkerTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private lateinit var repository: WeatherRepository

    private inner class TestWorkerFactory : WorkerFactory() {
        override fun createWorker(
            appContext: Context,
            workerClassName: String,
            workerParameters: WorkerParameters
        ): ListenableWorker = WeatherSyncWorker(appContext, workerParameters, repository)
    }

    @Before
    fun setUp() {
        repository = mockk(relaxed = true)
    }

    @Test
    fun doWork_calls_prefetchFavourites_and_returns_success() = runTest {
        coEvery { repository.prefetchFavourites() } just Runs

        val worker = TestListenableWorkerBuilder<WeatherSyncWorker>(context)
            .setWorkerFactory(TestWorkerFactory())
            .build()

        val result = worker.doWork()

        assertEquals(ListenableWorker.Result.success(), result)
        coVerify(exactly = 1) { repository.prefetchFavourites() }
    }

    @Test
    fun doWork_returns_retry_on_first_attempt_when_prefetch_throws() = runTest {
        coEvery { repository.prefetchFavourites() } throws Exception("Network error")

        val worker = TestListenableWorkerBuilder<WeatherSyncWorker>(context)
            .setWorkerFactory(TestWorkerFactory())
            .setRunAttemptCount(0)
            .build()

        val result = worker.doWork()

        assertEquals(ListenableWorker.Result.retry(), result)
    }

    @Test
    fun doWork_returns_failure_after_max_attempts() = runTest {
        coEvery { repository.prefetchFavourites() } throws Exception("Network error")

        val worker = TestListenableWorkerBuilder<WeatherSyncWorker>(context)
            .setWorkerFactory(TestWorkerFactory())
            .setRunAttemptCount(3)
            .build()

        val result = worker.doWork()

        assertEquals(ListenableWorker.Result.failure(), result)
    }
}