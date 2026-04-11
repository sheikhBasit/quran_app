package com.quranapp.domain.repository

import com.quranapp.domain.model.*

interface LearningRepository {
    // Word meanings (Layer 1)
    suspend fun getWordMeanings(surahNumber: Int, ayahNumber: Int): List<WordMeaning>

    // Word bank (Layer 3)
    suspend fun addToWordBank(surahNumber: Int, ayahNumber: Int, wordPosition: Int)
    suspend fun getWordBankId(surahNumber: Int, ayahNumber: Int, wordPosition: Int): Long?
    suspend fun isInWordBank(surahNumber: Int, ayahNumber: Int, wordPosition: Int): Boolean
    suspend fun getWordBankCount(): Int

    // Flashcard reviews (Layer 3)
    suspend fun ensureReviewRecord(wordBankId: Long)
    suspend fun getDueFlashcards(nowEpoch: Long): List<DueFlashcard>
    suspend fun getDueCount(nowEpoch: Long): Int
    suspend fun updateReview(result: ReviewResult)

    // Studied ayahs (Layer 2)
    suspend fun markAyahStudied(surahNumber: Int, ayahNumber: Int)
    suspend fun getStudiedAyahCount(): Int
    suspend fun getStudiedBySurah(): Map<Int, Int>

    // Progress aggregation
    suspend fun getProgress(nowEpoch: Long): LearningProgress
}
