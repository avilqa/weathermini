package com.example.weathermini.di

import android.content.Context
import androidx.room.Room
import com.example.weathermini.data.local.AppDatabase
import com.example.weathermini.data.local.MIGRATION_1_2
import com.example.weathermini.data.local.dao.FavouriteDao
import com.example.weathermini.data.local.dao.SearchHistoryDao
import com.example.weathermini.data.local.dao.WeatherCacheDao
import com.example.weathermini.data.local.dao.WeatherNoteDao
import com.example.weathermini.data.repository.WeatherRepository
import com.example.weathermini.data.datastore.AppPreferences
// import com.example.weathermini.data.local.dao.FavoriteCityDao
import com.google.gson.Gson
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideGson(): Gson = Gson()

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "weathermini.db")
            .addMigrations(MIGRATION_1_2)
            .build()

    @Provides
    fun provideFavouriteDao(db: AppDatabase): FavouriteDao = db.favouriteDao()

    @Provides
    fun provideWeatherCacheDao(db: AppDatabase): WeatherCacheDao = db.weatherCacheDao()

    @Provides
    fun provideSearchHistoryDao(db: AppDatabase): SearchHistoryDao = db.searchHistoryDao()

    @Provides
    fun provideWeatherNoteDao(db: AppDatabase): WeatherNoteDao = db.weatherNoteDao()
}
