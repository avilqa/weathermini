package com.example.weathermini

import com.example.weathermini.data.local.entity.WeatherCacheEntity
import org.junit.Assert.*
import org.junit.Test

class WeatherCacheEntityTest {

    @Test
    fun `isExpired returns false when cache is fresh`() {
        val cache = WeatherCacheEntity(
            cacheKey = "55.7500_37.6200",
            cityName = "Moscow",
            dataJson = "{}",
            cachedAt = System.currentTimeMillis(),
            ttlMillis = 3 * 3_600_000L
        )
        assertFalse("Свежий кэш не должен быть устаревшим", cache.isExpired())
    }

    @Test
    fun `isExpired returns true when cache is older than TTL`() {
        val cache = WeatherCacheEntity(
            cacheKey = "55.7500_37.6200",
            cityName = "Moscow",
            dataJson = "{}",
            cachedAt = System.currentTimeMillis() - 4 * 3_600_000L,
            ttlMillis = 3 * 3_600_000L
        )
        assertTrue("Кэш старше TTL должен считаться устаревшим", cache.isExpired())
    }

    @Test
    fun `isExpired false when cache is exactly before TTL boundary`() {
        val ttl = 3 * 3_600_000L
        val cache = WeatherCacheEntity(
            cacheKey = "0_0",
            cityName = "Test",
            dataJson = "{}",
            cachedAt = System.currentTimeMillis() - ttl + 1_000L,
            ttlMillis = ttl
        )
        assertFalse(cache.isExpired())
    }

    @Test
    fun `isExpired with zero TTL always true`() {
        val cache = WeatherCacheEntity(
            cacheKey = "0_0",
            cityName = "Test",
            dataJson = "{}",
            cachedAt = System.currentTimeMillis() - 1,
            ttlMillis = 0L
        )
        assertTrue(cache.isExpired())
    }

    @Test
    fun `keyOf formats correctly`() {
        val key = WeatherCacheEntity.keyOf(55.75, 37.62)
        assertEquals("55.7500_37.6200", key)
    }

    @Test
    fun `keyOf handles negative coordinates`() {
        val key = WeatherCacheEntity.keyOf(-33.8688, 151.2093)
        assertEquals("-33.8688_151.2093", key)
    }
}