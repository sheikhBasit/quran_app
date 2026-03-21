package com.quranapp.data.repository

import com.quranapp.db.*
import com.quranapp.domain.model.QuranScript
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class QuranRepositoryTest {
    private val database: QuranDatabase = mockk()
    private val surahsQueries: SurahsQueries = mockk()
    private val ayahsQueries: AyahsQueries = mockk()
    private val tafsirQueries: TafsirQueries = mockk()
    private val settingsQueries: SettingsQueries = mockk()

    private lateinit var repository: QuranRepositoryImpl

    @BeforeTest
    fun setup() {
        every { database.surahsQueries } returns surahsQueries
        every { database.ayahsQueries } returns ayahsQueries
        every { database.tafsirQueries } returns tafsirQueries
        every { database.settingsQueries } returns settingsQueries
        repository = QuranRepositoryImpl(database)
    }

    @Test
    fun `getAllSurahs returns mapped surah list`() = runTest {
        val dbSurahs = listOf(
            Surahs(1, "الفاتحة", "The Opening", "Al-Fatihah", "Meccan", 7)
        )
        every { surahsQueries.selectAll().executeAsList() } returns dbSurahs

        val result = repository.getAllSurahs()

        assertEquals(1, result.size)
        assertEquals("Al-Fatihah", result[0].nameTransliteration)
        verify { surahsQueries.selectAll().executeAsList() }
    }

    @Test
    fun `getAyahsBySurah returns mapped ayah list`() = runTest {
        val dbAyahs = listOf(
            Ayahs(1L, 1L, 1L, 1L, 1L, "بسم الله", "بسم الله", "In the name of Allah")
        )
        every { ayahsQueries.selectBySurah(1L).executeAsList() } returns dbAyahs

        val result = repository.getAyahsBySurah(1)

        assertEquals(1, result.size)
        assertEquals("In the name of Allah", result[0].translationEnglish)
        verify { ayahsQueries.selectBySurah(1L).executeAsList() }
    }

    @Test
    fun `getAyahsByPage returns mapped ayah list`() = runTest {
        val dbAyahs = listOf(
            Ayahs(1L, 1L, 1L, 1L, 1L, "بسم الله", "بسم الله", "In the name of Allah")
        )
        every { ayahsQueries.selectByPage(1L).executeAsList() } returns dbAyahs

        val result = repository.getAyahsByPage(1)

        assertEquals(1, result.size)
        assertEquals(1, result[0].pageNumber)
    }

    @Test
    fun `getTafsir returns mapped tafsir entries`() = runTest {
        val dbTafsir = listOf(
            Tafsir(1L, 1L, 1L, "ibn_kathir", "Content here")
        )
        every { tafsirQueries.selectByAyah(1L, 1L).executeAsList() } returns dbTafsir

        val result = repository.getTafsir(1, 1)

        assertEquals(1, result.size)
        assertEquals("ibn_kathir", result[0].bookName)
    }

    @Test
    fun `getReadingPosition returns triple`() = runTest {
        val pos = Reading_position(1L, 2L, 3L, 1L, "scroll")
        every { settingsQueries.getPosition().executeAsOne() } returns pos

        val result = repository.getReadingPosition()

        assertEquals(2, result.first) // Surah
        assertEquals(3, result.second) // Ayah
        assertEquals(1, result.third) // Page
    }

    @Test
    fun `saveReadingPosition calls upsert`() = runTest {
        every { settingsQueries.upsertPosition(any(), any(), any(), any()) } returns Unit

        repository.saveReadingPosition(2, 3, 1, "scroll")

        verify { settingsQueries.upsertPosition(2L, 3L, 1L, "scroll") }
    }
}
