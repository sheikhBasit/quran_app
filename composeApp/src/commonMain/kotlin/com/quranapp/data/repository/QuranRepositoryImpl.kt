package com.quranapp.data.repository

import com.quranapp.db.QuranDatabase
import com.quranapp.domain.model.Ayah
import com.quranapp.domain.model.Surah
import com.quranapp.domain.model.TafsirEntry
import com.quranapp.domain.repository.QuranRepository

class QuranRepositoryImpl(
    private val db: QuranDatabase
) : QuranRepository {

    override suspend fun getAllSurahs(): List<Surah> {
        return db.surahsQueries.selectAll().executeAsList().map { it.toDomain() }
    }

    override suspend fun getAyahsBySurah(surahNumber: Int): List<Ayah> {
        return db.ayahsQueries.selectBySurah(surahNumber.toLong()).executeAsList().map { it.toDomain() }
    }

    override suspend fun getAyahsByPage(pageNumber: Int): List<Ayah> {
        return db.ayahsQueries.selectByPage(pageNumber.toLong()).executeAsList().map { it.toDomain() }
    }

    override suspend fun getTafsir(surahNumber: Int, ayahNumber: Int): List<TafsirEntry> {
        return db.tafsirQueries.selectByAyah(surahNumber.toLong(), ayahNumber.toLong()).executeAsList().map {
            TafsirEntry(bookName = it.book_name, content = it.content)
        }
    }

    override suspend fun searchAyahs(query: String): List<Ayah> {
        return db.ayahsQueries.search("%$query%").executeAsList().map { it.toDomain() }
    }

    override suspend fun getReadingPosition(): Triple<Int, Int, Int> {
        val pos = db.settingsQueries.getPosition().executeAsOne()
        return Triple(pos.surah_number.toInt(), pos.ayah_number.toInt(), pos.page_number.toInt())
    }

    override suspend fun saveReadingPosition(surah: Int, ayah: Int, page: Int, mode: String) {
        db.settingsQueries.upsertPosition(
            surahNumber = surah.toLong(),
            ayahNumber = ayah.toLong(),
            pageNumber = page.toLong(),
            mode = mode
        )
    }

    // --- Mappers ---

    private fun com.quranapp.db.Surahs.toDomain() = Surah(
        number = number.toInt(),
        nameArabic = name_arabic,
        nameEnglish = name_english,
        nameTransliteration = name_transliteration,
        revelationType = revelation_type,
        ayahCount = ayah_count.toInt()
    )

    private fun com.quranapp.db.Ayahs.toDomain() = Ayah(
        id = id,
        surahNumber = surah_number.toInt(),
        ayahNumber = ayah_number.toInt(),
        pageNumber = page_number.toInt(),
        juzNumber = juz_number.toInt(),
        arabicTextHafs = arabic_text_hafs,
        arabicTextWarsh = arabic_text_warsh,
        translationEnglish = translation_english
    )
}
