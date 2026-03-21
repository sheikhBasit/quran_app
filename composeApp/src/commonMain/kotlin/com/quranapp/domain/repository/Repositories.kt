package com.quranapp.domain.repository

import com.quranapp.domain.model.*

interface QuranRepository {
    suspend fun getAllSurahs(): List<Surah>
    suspend fun getAyahsBySurah(surahNumber: Int): List<Ayah>
    suspend fun getAyahsByPage(pageNumber: Int): List<Ayah>
    suspend fun getTafsir(surahNumber: Int, ayahNumber: Int): List<TafsirEntry>
    suspend fun searchAyahs(query: String): List<Ayah>
    suspend fun getReadingPosition(): Triple<Int, Int, Int>
    suspend fun saveReadingPosition(surah: Int, ayah: Int, page: Int, mode: String)
}

interface HadithRepository {
    suspend fun getCollections(): List<HadithCollection>
    suspend fun getChapterNames(collection: String): List<String>
    suspend fun getHadithByChapter(collection: String, chapterName: String): List<Hadith>
    suspend fun searchHadith(query: String): List<Hadith>
}

interface UserDataRepository {
    suspend fun toggleBookmark(type: String, referenceId: Long): Boolean
    suspend fun isBookmarked(type: String, referenceId: Long): Boolean
    suspend fun getAllBookmarks(type: String): List<Bookmark>
    suspend fun setHighlight(ayahId: Long, color: String)
    suspend fun removeHighlight(ayahId: Long)
    suspend fun getHighlightColor(ayahId: Long): String?
    suspend fun saveNote(type: String, referenceId: Long, content: String)
    suspend fun getNote(type: String, referenceId: Long): Note?
    suspend fun deleteNote(type: String, referenceId: Long)
}

interface SearchRepository {
    suspend fun searchAyahs(query: String): List<SearchResult.AyahResult>
    suspend fun searchHadith(query: String): List<SearchResult.HadithResult>
}

interface ChatbotRepository {
    suspend fun sendMessage(message: String): ChatResponse
}

interface SettingsRepository {
    fun getString(key: String, default: String): String
    fun setString(key: String, value: String)
}
