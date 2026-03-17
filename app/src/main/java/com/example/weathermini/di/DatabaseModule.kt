package com.example.weathermini.di

import android.content.Context
import androidx.room.Room
import com.example.weathermini.data.local.AppDatabase
import com.example.weathermini.data.local.dao.FavouriteDao
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
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "weathermini.db"
        ).build()

    @Provides
    fun provideFavouriteDao(db: AppDatabase): FavouriteDao = db.favouriteDao()
}