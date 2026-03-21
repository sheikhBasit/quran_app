package com.quranapp.data.repository

import com.quranapp.db.Hadith
import com.quranapp.db.HadithQueries
import com.quranapp.db.QuranDatabase
import com.quranapp.domain.repository.HadithRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class HadithRepositoryTest {
    private val db: QuranDatabase = mockk()
    private val hadithQueries: HadithQueries = mockk()
    private lateinit var repository: HadithRepository

    @BeforeTest
    fun setup() {
        every { db.hadithQueries } returns hadithQueries
        repository = HadithRepositoryImpl(db)
    }

    @Test
    fun `getCollections returns mapped collections`() = runTest {
        val dbCollections = listOf(
            com.quranapp.db.SelectCollections("bukhari", 10L, 100L)
        )
        every { hadithQueries.selectCollections().executeAsList() } returns dbCollections

        val result = repository.getCollections()

        assertEquals(1, result.size)
        assertEquals("bukhari", result[0].collection)
        assertEquals(10, result[0].bookCount) // Maps to chapter_count
    }

    @Test
    fun `getChapterNames returns list of strings`() = runTest {
        val chapters = listOf(
            com.quranapp.db.SelectChaptersByCollection("Revelation", 7L)
        )
        every { hadithQueries.selectChaptersByCollection("bukhari").executeAsList() } returns chapters

        val result = repository.getChapterNames("bukhari")

        assertEquals(1, result.size)
        assertEquals("Revelation", result[0])
    }

    @Test
    fun `getHadithByChapter returns mapped hadith list`() = runTest {
        val dbHadith = listOf(
            Hadith(1L, "bukhari", "Revelation", 1L, "إِنَّمَا الأَعْمَالُ بِالنِّيَّاتِ", "Actions are by intentions", "Umar")
        )
        every { hadithQueries.selectByChapter("bukhari", "Revelation").executeAsList() } returns dbHadith

        val result = repository.getHadithByChapter("bukhari", "Revelation")

        assertEquals(1, result.size)
        assertEquals("bukhari", result[0].collection)
        assertEquals("Revelation", result[0].chapterName)
        assertEquals(1, result[0].hadithNumber)
    }

    @Test
    fun `searchHadith returns mapped results`() = runTest {
        val dbHadith = listOf(
            Hadith(1L, "bukhari", "Revelation", 1L, "...", "...", "...")
        )
        every { hadithQueries.search("%prayer%").executeAsList() } returns dbHadith

        val result = repository.searchHadith("prayer")

        assertEquals(1, result.size)
        verify { hadithQueries.search("%prayer%").executeAsList() }
    }
}
