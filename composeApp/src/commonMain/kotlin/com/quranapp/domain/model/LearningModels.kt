package com.quranapp.domain.model

data class WordMeaning(
    val id: Long,
    val surahNumber: Int,
    val ayahNumber: Int,
    val wordPosition: Int,
    val arabicWord: String,
    val transliteration: String,
    val englishMeaning: String,
    val rootArabic: String?,
    val rootEnglish: String?,
    val quranOccurrenceCount: Int,
)

data class DueFlashcard(
    val reviewId: Long,
    val wordBankId: Long,
    val arabicWord: String,
    val transliteration: String,
    val englishMeaning: String,
    val surahNumber: Int,
    val ayahNumber: Int,
    val intervalDays: Int,
    val easeFactor: Double,
    val repetitions: Int,
)

enum class ReviewRating { AGAIN, HARD, KNOW_IT }

data class ReviewResult(
    val reviewId: Long,
    val nextIntervalDays: Int,
    val nextEaseFactor: Double,
    val nextRepetitions: Int,
    val nextReviewEpoch: Long,
)

data class LearningProgress(
    val wordBankCount: Int,
    val studiedAyahCount: Int,
    val dueReviewCount: Int,
    val streakDays: Int,
    /** Map of surah_number → studied ayah count in that surah */
    val studiedBySurah: Map<Int, Int>,
)
