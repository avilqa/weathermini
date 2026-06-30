package com.example.weathermini.data.local.dao

import androidx.room.*
import com.example.weathermini.data.local.entity.SearchHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SearchHistoryDao {

    @Query("SELECT * FROM search_history ORDER BY searchedAt DESC LIMIT :limit")
    fun observeRecent(limit: Int = 50): Flow<List<SearchHistoryEntity>>

    @Query("""
        SELECT * FROM search_history
        WHERE cityName LIKE '%' || :query || '%'
        ORDER BY searchedAt DESC
        LIMIT 50
    """)
    fun searchByCity(query: String): Flow<List<SearchHistoryEntity>>

    @Insert
    suspend fun insert(entity: SearchHistoryEntity)

    @Query("DELETE FROM search_history")
    suspend fun clearAll()

    @Query("DELETE FROM search_history WHERE searchedAt < :timestamp")
    suspend fun deleteOlderThan(timestamp: Long)

    @Query("SELECT COUNT(*) FROM search_history")
    suspend fun count(): Int

    @Query("""
        DELETE FROM search_history WHERE id NOT IN (
            SELECT id FROM search_history ORDER BY searchedAt DESC LIMIT :keepCount
        )
    """)
    suspend fun keepLatest(keepCount: Int)
}