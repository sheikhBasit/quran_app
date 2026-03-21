package com.quranapp.data.repository

import com.quranapp.db.QuranDatabase
import com.quranapp.domain.model.Hadith
import com.quranapp.domain.model.HadithCollection
import com.quranapp.domain.repository.HadithRepository

class HadithRepositoryImpl(
    private val db: QuranDatabase
) : HadithRepository {

    override suspend fun getCollections(): List<HadithCollection> {
        return db.hadithQueries.selectCollections().executeAsList().map {
            val displayName = when(it.collection) {
                "bukhari" -> "Sahih Al-Bukhari"
                "muslim" -> "Sahih Muslim"
                "tirmidhi" -> "Jami' At-Tirmidhi"
                "abu_dawud" -> "Sunan Abu Dawud"
                "nasai" -> "Sunan An-Nasa'i"
                "ibn_majah" -> "Sunan Ibn Majah"
                else -> it.collection.replaceFirstChar { char -> char.uppercase() }
            }
            HadithCollection(
                collection = it.collection,
                displayName = displayName,
                bookCount = it.chapter_count.toInt(),
                hadithCount = it.hadith_count.toInt()
            )
        }
    }

    override suspend fun getChapterNames(collection: String): List<String> {
        return db.hadithQueries.selectChaptersByCollection(collection).executeAsList().map { it.chapter_name }
    }

    override suspend fun getHadithByChapter(collection: String, chapterName: String): List<Hadith> {
        return db.hadithQueries.selectByChapter(collection, chapterName).executeAsList().map { it.toDomain() }
    }

    override suspend fun searchHadith(query: String): List<Hadith> {
        return db.hadithQueries.search("%$query%").executeAsList().map { it.toDomain() }
    }

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
