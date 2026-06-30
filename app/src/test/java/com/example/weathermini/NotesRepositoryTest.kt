package com.example.weathermini

import com.example.weathermini.data.local.dao.WeatherNoteDao
import com.example.weathermini.data.local.entity.WeatherNoteEntity
import com.example.weathermini.data.repository.NotesRepository
import io.mockk.*
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class NotesRepositoryTest {

    private lateinit var dao: WeatherNoteDao
    private lateinit var repo: NotesRepository

    @Before
    fun setup() {
        dao = mockk(relaxed = true)
        repo = NotesRepository(dao)
    }

    @Test
    fun `addNote trims content and calls dao insert`() = runTest {
        coEvery { dao.insert(any()) } returns 1L

        val id = repo.addNote(
            cityName = "Moscow",
            lat = 55.75, lon = 37.62,
            content = "  Отличная погода!  ",
            temperature = 25.0,
            weatherCode = 0
        )

        assertEquals(1L, id)
        coVerify {
            dao.insert(match {
                it.content == "Отличная погода!" &&
                        it.cityName == "Moscow" &&
                        it.temperature == 25.0
            })
        }
    }

    @Test
    fun `deleteNote calls dao deleteById`() = runTest {
        repo.deleteNote(42L)
        coVerify { dao.deleteById(42L) }
    }

    @Test
    fun `updateNote calls dao update`() = runTest {
        val note = WeatherNoteEntity(
            id = 1L, cityName = "Moscow",
            lat = 55.75, lon = 37.62,
            content = "old", temperature = 20.0, weatherCode = 0
        )
        repo.updateNote(note.copy(content = "new"))
        coVerify { dao.update(match { it.content == "new" }) }
    }

    @Test
    fun `observeByCity delegates to dao`() = runTest {
        val note = WeatherNoteEntity(
            id = 1L, cityName = "Moscow",
            lat = 55.75, lon = 37.62,
            content = "Test", temperature = 20.0, weatherCode = 0
        )
        every { dao.observeByCity("Moscow") } returns flowOf(listOf(note))

        repo.observeByCity("Moscow").collect { notes ->
            assertEquals(1, notes.size)
            assertEquals("Test", notes[0].content)
        }
        verify { dao.observeByCity("Moscow") }
    }
}