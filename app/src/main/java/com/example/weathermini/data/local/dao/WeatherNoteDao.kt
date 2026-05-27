package com.example.weathermini.data.local.dao

import androidx.room.*
import com.example.weathermini.data.local.entity.WeatherNoteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WeatherNoteDao {

    @Query("""
        SELECT * FROM weather_notes
        WHERE cityName = :cityName
        ORDER BY createdAt DESC
    """)
    fun observeByCity(cityName: String): Flow<List<WeatherNoteEntity>>

    @Query("SELECT * FROM weather_notes ORDER BY createdAt DESC LIMIT 100")
    fun observeAll(): Flow<List<WeatherNoteEntity>>

    @Insert
    suspend fun insert(entity: WeatherNoteEntity): Long

    @Update
    suspend fun update(entity: WeatherNoteEntity)

    @Query("DELETE FROM weather_notes WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT COUNT(*) FROM weather_notes WHERE cityName = :cityName")
    suspend fun countByCity(cityName: String): Int
}