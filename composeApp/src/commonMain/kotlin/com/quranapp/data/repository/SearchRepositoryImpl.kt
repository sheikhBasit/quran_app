package com.quranapp.data.repository

import com.quranapp.db.QuranDatabase
import com.quranapp.domain.model.*
import com.quranapp.domain.repository.SearchRepository

class SearchRepositoryImpl(
    private val db: QuranDatabase
) : SearchRepository {

    override suspend fun searchAyahs(query: String): List<SearchResult.AyahResult> {
        return db.ayahsQueries.search("%$query%").executeAsList().map {
            SearchResult.AyahResult(it.toDomain(), query)
        }
    }

    override suspend fun searchHadith(query: String): List<SearchResult.HadithResult> {
        return db.hadithQueries.search("%$query%").executeAsList().map {
            SearchResult.HadithResult(it.toDomain(), query)
        }
    }

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

    private fun com.quranapp.db.Hadith.toDomain() = Hadith(
        id = id,
        collection = collection,
        chapterName = chapter_name,
        hadithNumber = hadith_number.toInt(),
        arabicText = arabic_text,
        translation = translation,
        narrator = narrator
    )
}
