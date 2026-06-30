package com.example.weathermini

import com.example.weathermini.data.local.dao.SearchHistoryDao
import com.example.weathermini.data.local.entity.SearchHistoryEntity
import com.example.weathermini.data.repository.HistoryRepository
import io.mockk.*
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.util.concurrent.TimeUnit

class HistoryRepositoryTest {

    private lateinit var dao: SearchHistoryDao
    private lateinit var repo: HistoryRepository

    private val fakeEntry = SearchHistoryEntity(
        id = 1L, cityName = "Moscow",
        lat = 55.75, lon = 37.62,
        temperature = 20.0, weatherCode = 0,
        searchedAt = System.currentTimeMillis(),
        fromCache = false
    )

    @Before
    fun setup() {
        dao = mockk(relaxed = true)
        repo = HistoryRepository(dao)
    }


    @Test
    fun `observeRecent delegates to dao with default limit`() = runTest {
        every { dao.observeRecent(50) } returns flowOf(listOf(fakeEntry))

        repo.observeRecent().collect { items ->
            assertEquals(1, items.size)
            assertEquals("Moscow", items[0].cityName)
        }
        verify { dao.observeRecent(50) }
    }

    @Test
    fun `observeRecent passes custom limit`() = runTest {
        every { dao.observeRecent(10) } returns flowOf(emptyList())
        repo.observeRecent(10).collect {}
        verify { dao.observeRecent(10) }
    }


    @Test
    fun `searchByCity delegates to dao`() = runTest {
        every { dao.searchByCity("Mos") } returns flowOf(listOf(fakeEntry))
        repo.searchByCity("Mos").collect { items ->
            assertEquals(1, items.size)
        }
        verify { dao.searchByCity("Mos") }
    }


    @Test
    fun `clearAll calls dao clearAll`() = runTest {
        repo.clearAll()
        coVerify { dao.clearAll() }
    }

    @Test
    fun `deleteOlderThanDays calculates correct timestamp`() = runTest {
        val before = System.currentTimeMillis()
        repo.deleteOlderThanDays(30)
        val after = System.currentTimeMillis()

        coVerify {
            dao.deleteOlderThan(
                timestamp = withArg { ts ->
                    val expectedMin = before - TimeUnit.DAYS.toMillis(30)
                    val expectedMax = after  - TimeUnit.DAYS.toMillis(30)
                    assertTrue(ts in expectedMin..expectedMax)
                }
            )
        }
    }
}