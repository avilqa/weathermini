package com.example.weathermini.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences>
        by preferencesDataStore(name = "app_settings")

enum class TemperatureUnit { CELSIUS, FAHRENHEIT }

enum class AppTheme { SYSTEM, LIGHT, DARK }

data class AppSettings(
    val temperatureUnit: TemperatureUnit = TemperatureUnit.CELSIUS,
    val theme: AppTheme = AppTheme.SYSTEM,
    val cacheTtlHours: Int = 3,
    val backgroundSyncEnabled: Boolean = true
)

@Singleton
class AppPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val TEMPERATURE_UNIT  = stringPreferencesKey("temperature_unit")
        val THEME             = stringPreferencesKey("theme")
        val CACHE_TTL_HOURS   = intPreferencesKey("cache_ttl_hours")
        val BG_SYNC_ENABLED   = booleanPreferencesKey("bg_sync_enabled")
    }

    val settings: Flow<AppSettings> = context.dataStore.data
        .catch { e ->
            if (e is IOException) emit(emptyPreferences())
            else throw e
        }
        .map { prefs ->
            AppSettings(
                temperatureUnit = prefs[Keys.TEMPERATURE_UNIT]
                    ?.let { runCatching { TemperatureUnit.valueOf(it) }.getOrNull() }
                    ?: TemperatureUnit.CELSIUS,
                theme = prefs[Keys.THEME]
                    ?.let { runCatching { AppTheme.valueOf(it) }.getOrNull() }
                    ?: AppTheme.SYSTEM,
                cacheTtlHours = prefs[Keys.CACHE_TTL_HOURS] ?: 3,
                backgroundSyncEnabled = prefs[Keys.BG_SYNC_ENABLED] ?: true
            )
        }

    suspend fun setTemperatureUnit(unit: TemperatureUnit) {
        context.dataStore.edit { it[Keys.TEMPERATURE_UNIT] = unit.name }
    }

    suspend fun setTheme(theme: AppTheme) {
        context.dataStore.edit { it[Keys.THEME] = theme.name }
    }

    suspend fun setCacheTtlHours(hours: Int) {
        require(hours in 1..24)
        context.dataStore.edit { it[Keys.CACHE_TTL_HOURS] = hours }
    }

    suspend fun setBackgroundSyncEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.BG_SYNC_ENABLED] = enabled }
    }
}