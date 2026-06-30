package com.example.weathermini.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.example.weathermini.data.local.dao.WeatherCacheDao
import com.example.weathermini.data.repository.HistoryRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

@HiltWorker
class CacheCleanupWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val cacheDao: WeatherCacheDao,
    private val historyRepository: HistoryRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            cacheDao.deleteExpired()
            historyRepository.deleteOlderThanDays(30)
            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }

    companion object {
        const val WORK_NAME = "cache_cleanup_daily"

        fun buildRequest(): PeriodicWorkRequest =
            PeriodicWorkRequestBuilder<CacheCleanupWorker>(1, TimeUnit.DAYS)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiresBatteryNotLow(true)
                        .build()
                )
                .build()
    }
}