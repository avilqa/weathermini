package com.example.weathermini.data.repository

import com.example.weathermini.data.local.dao.SearchHistoryDao
import com.example.weathermini.data.local.entity.SearchHistoryEntity
import kotlinx.coroutines.flow.Flow
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HistoryRepository @Inject constructor(
    private val dao: SearchHistoryDao
) {
    fun observeRecent(limit: Int = 50): Flow<List<SearchHistoryEntity>> =
        dao.observeRecent(limit)

    fun searchByCity(query: String): Flow<List<SearchHistoryEntity>> =
        dao.searchByCity(query)

    suspend fun clearAll() = dao.clearAll()

    suspend fun deleteOlderThanDays(days: Int) {
        val cutoff = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(days.toLong())
        dao.deleteOlderThan(cutoff)
    }
}