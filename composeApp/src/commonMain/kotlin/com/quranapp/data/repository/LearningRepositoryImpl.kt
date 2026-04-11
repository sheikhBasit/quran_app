package com.quranapp.data.repository

import com.quranapp.db.QuranDatabase
import com.quranapp.domain.model.*
import com.quranapp.domain.repository.LearningRepository

class LearningRepositoryImpl(
    private val db: QuranDatabase,
) : LearningRepository {

    override suspend fun getWordMeanings(surahNumber: Int, ayahNumber: Int): List<WordMeaning> =
        db.wordMeaningsQueries
            .selectByAyah(surahNumber.toLong(), ayahNumber.toLong())
            .executeAsList()
            .map { it.toDomain() }

    override suspend fun addToWordBank(surahNumber: Int, ayahNumber: Int, wordPosition: Int) {
        db.learningDataQueries.insertWordBank(
            surahNumber.toLong(), ayahNumber.toLong(), wordPosition.toLong()
        )
    }

    override suspend fun isInWordBank(surahNumber: Int, ayahNumber: Int, wordPosition: Int): Boolean =
        db.learningDataQueries
            .isInWordBank(surahNumber.toLong(), ayahNumber.toLong(), wordPosition.toLong())
            .executeAsOne() > 0L

    override suspend fun getWordBankCount(): Int =
        db.learningDataQueries.countWordBank().executeAsOne().toInt()

    override suspend fun ensureReviewRecord(wordBankId: Long) {
        db.learningDataQueries.insertReview(wordBankId)
    }

    override suspend fun getDueFlashcards(nowEpoch: Long): List<DueFlashcard> =
        db.learningDataQueries.selectDueReviews(nowEpoch).executeAsList().map { row ->
            DueFlashcard(
                reviewId = row.id,
                wordBankId = row.word_bank_id,
                arabicWord = row.arabic_word,
                transliteration = row.transliteration,
                englishMeaning = row.english_meaning,
                surahNumber = row.wm_surah.toInt(),
                ayahNumber = row.wm_ayah.toInt(),
                intervalDays = row.interval_days.toInt(),
                easeFactor = row.ease_factor,
                repetitions = row.repetitions.toInt(),
            )
        }

    override suspend fun getDueCount(nowEpoch: Long): Int =
        db.learningDataQueries.countDueReviews(nowEpoch).executeAsOne().toInt()

    override suspend fun updateReview(result: ReviewResult) {
        val nowEpoch = System.currentTimeMillis() / 1000L
        db.learningDataQueries.updateReview(
            nextReviewAt = result.nextReviewEpoch,
            intervalDays = result.nextIntervalDays.toLong(),
            easeFactor = result.nextEaseFactor,
            repetitions = result.nextRepetitions.toLong(),
            lastReviewedAt = nowEpoch,
            id = result.reviewId,
        )
    }

    override suspend fun markAyahStudied(surahNumber: Int, ayahNumber: Int) {
        db.learningDataQueries.markStudied(surahNumber.toLong(), ayahNumber.toLong())
    }

    override suspend fun getStudiedAyahCount(): Int =
        db.learningDataQueries.countStudied().executeAsOne().toInt()

    override suspend fun getStudiedBySurah(): Map<Int, Int> =
        db.learningDataQueries.countStudiedBySurah().executeAsList()
            .associate { it.surah_number.toInt() to it.count_.toInt() }

    override suspend fun getProgress(nowEpoch: Long): LearningProgress = LearningProgress(
        wordBankCount = getWordBankCount(),
        studiedAyahCount = getStudiedAyahCount(),
        dueReviewCount = getDueCount(nowEpoch),
        streakDays = 0,
        studiedBySurah = getStudiedBySurah(),
    )

    // ── Mapper ──────────────────────────────────────────────────────────────

    private fun com.quranapp.db.Word_meanings.toDomain() = WordMeaning(
        id = id,
        surahNumber = surah_number.toInt(),
        ayahNumber = ayah_number.toInt(),
        wordPosition = word_position.toInt(),
        arabicWord = arabic_word,
        transliteration = transliteration,
        englishMeaning = english_meaning,
        rootArabic = root_arabic,
        rootEnglish = root_english,
        quranOccurrenceCount = quran_occurrence_count?.toInt() ?: 0,
    )
}
