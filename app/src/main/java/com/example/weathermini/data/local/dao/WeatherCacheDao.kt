package com.example.weathermini.data.local.dao

import androidx.room.*
import com.example.weathermini.data.local.entity.WeatherCacheEntity

@Dao
interface WeatherCacheDao {

    @Query("SELECT * FROM weather_cache WHERE cacheKey = :key LIMIT 1")
    suspend fun get(key: String): WeatherCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun put(entity: WeatherCacheEntity)

    @Query("DELETE FROM weather_cache WHERE cacheKey = :key")
    suspend fun delete(key: String)

    @Query("SELECT * FROM weather_cache")
    suspend fun getAll(): List<WeatherCacheEntity>

    @Query("DELETE FROM weather_cache WHERE (cachedAt + ttlMillis) < :now")
    suspend fun deleteExpired(now: Long = System.currentTimeMillis())

    @Query("SELECT COUNT(*) FROM weather_cache")
    suspend fun count(): Int
}