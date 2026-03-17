package com.example.weathermini.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.weathermini.data.local.dao.FavouriteDao
import com.example.weathermini.data.local.entity.FavouriteEntity

@Database(
    entities = [FavouriteEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun favouriteDao(): FavouriteDao
}