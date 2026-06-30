package com.example.weathermini.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.weathermini.data.local.dao.FavouriteDao
import com.example.weathermini.data.local.dao.SearchHistoryDao
import com.example.weathermini.data.local.dao.WeatherCacheDao
import com.example.weathermini.data.local.dao.WeatherNoteDao
import com.example.weathermini.data.local.entity.FavouriteEntity
import com.example.weathermini.data.local.entity.SearchHistoryEntity
import com.example.weathermini.data.local.entity.WeatherCacheEntity
import com.example.weathermini.data.local.entity.WeatherNoteEntity

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `weather_cache` (
                `cacheKey`   TEXT    NOT NULL PRIMARY KEY,
                `cityName`   TEXT    NOT NULL,
                `dataJson`   TEXT    NOT NULL,
                `cachedAt`   INTEGER NOT NULL,
                `ttlMillis`  INTEGER NOT NULL DEFAULT 10800000
            )
        """.trimIndent())

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `search_history` (
                `id`          INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                `cityName`    TEXT    NOT NULL,
                `lat`         REAL    NOT NULL,
                `lon`         REAL    NOT NULL,
                `temperature` REAL    NOT NULL,
                `weatherCode` INTEGER NOT NULL,
                `searchedAt`  INTEGER NOT NULL,
                `fromCache`   INTEGER NOT NULL DEFAULT 0
            )
        """.trimIndent())

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `weather_notes` (
                `id`          INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                `cityName`    TEXT    NOT NULL,
                `lat`         REAL    NOT NULL,
                `lon`         REAL    NOT NULL,
                `content`     TEXT    NOT NULL,
                `temperature` REAL    NOT NULL,
                `weatherCode` INTEGER NOT NULL,
                `createdAt`   INTEGER NOT NULL
            )
        """.trimIndent())
    }
}

@Database(
    entities = [
        FavouriteEntity::class,
        WeatherCacheEntity::class,
        SearchHistoryEntity::class,
        WeatherNoteEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun favouriteDao(): FavouriteDao
    abstract fun weatherCacheDao(): WeatherCacheDao
    abstract fun searchHistoryDao(): SearchHistoryDao
    abstract fun weatherNoteDao(): WeatherNoteDao
}