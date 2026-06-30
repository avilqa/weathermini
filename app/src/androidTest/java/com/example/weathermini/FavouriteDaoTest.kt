package com.example.weathermini

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.example.weathermini.data.local.AppDatabase
import com.example.weathermini.data.local.dao.FavouriteDao
import com.example.weathermini.data.local.entity.FavouriteEntity
import kotlinx.coroutines.test.runTest
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FavouriteDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: FavouriteDao

    private val moscow = FavouriteEntity(
        id = 1, name = "Moscow",
        latitude = 55.75, longitude = 37.62,
        country = "Russia", region = "Oblast"
    )
    private val berlin = FavouriteEntity(
        id = 2, name = "Berlin",
        latitude = 52.52, longitude = 13.40,
        country = "Germany", region = "Berlin"
    )

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = db.favouriteDao()
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun insert_city_observeAll_emits_full_sequence() = runTest {
        dao.observeAll().test {
            assertEquals(emptyList<FavouriteEntity>(), awaitItem())

            dao.insert(moscow)
            val updated = awaitItem()
            assertEquals(1, updated.size)
            assertEquals(moscow, updated[0])

            cancel()
        }
    }

    @Test
    fun delete_city_removed_from_observeAll() = runTest {
        dao.insert(moscow)

        dao.observeAll().test {
            val initial = awaitItem()
            assertEquals(1, initial.size)

            dao.delete(moscow)
            val afterDelete = awaitItem()
            assertTrue(afterDelete.isEmpty())

            cancel()
        }
    }

    @Test
    fun inserting_same_city_twice_no_duplicate() = runTest {
        dao.insert(moscow)
        dao.insert(moscow)

        dao.observeAll().test {
            val list = awaitItem()
            assertEquals("Должен быть 1 город, не 2", 1, list.size)
            cancel()
        }
    }

    @Test
    fun countById_correct_values() = runTest {
        assertEquals(0, dao.countById(moscow.id))
        dao.insert(moscow)
        assertEquals(1, dao.countById(moscow.id))
        assertEquals(0, dao.countById(berlin.id))
    }
}