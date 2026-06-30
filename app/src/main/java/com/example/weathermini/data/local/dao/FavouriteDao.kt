package com.example.weathermini.data.local.dao

import androidx.room.*
import com.example.weathermini.data.local.entity.FavouriteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FavouriteDao {

    @Query("SELECT * FROM favourites ORDER BY name ASC")
    fun observeAll(): Flow<List<FavouriteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: FavouriteEntity)

    @Delete
    suspend fun delete(entity: FavouriteEntity)

    @Query("SELECT COUNT(*) FROM favourites WHERE id = :id")
    suspend fun countById(id: Int): Int
}