package com.quranapp.domain.usecase.learning

import com.quranapp.domain.model.*
import com.quranapp.domain.repository.LearningRepository

class GetWordMeaningsUseCase(private val repo: LearningRepository) {
    suspend operator fun invoke(surahNumber: Int, ayahNumber: Int): List<WordMeaning> =
        repo.getWordMeanings(surahNumber, ayahNumber)
}

class AddToWordBankUseCase(private val repo: LearningRepository) {
    suspend operator fun invoke(surahNumber: Int, ayahNumber: Int, wordPosition: Int) {
        repo.addToWordBank(surahNumber, ayahNumber, wordPosition)
        // After insert, get the row id to create flashcard review record
        // The ViewModel will handle ensureReviewRecord separately if needed
    }
}

class GetDueFlashcardsUseCase(private val repo: LearningRepository) {
    suspend operator fun invoke(): List<DueFlashcard> {
        val nowEpoch = System.currentTimeMillis() / 1000L
        return repo.getDueFlashcards(nowEpoch)
    }
}

class RecordReviewUseCase(private val repo: LearningRepository) {
    suspend operator fun invoke(card: DueFlashcard, rating: ReviewRating): ReviewResult {
        val result = calculateNextReview(
            intervalDays = card.intervalDays,
            easeFactor = card.easeFactor,
            repetitions = card.repetitions,
            rating = rating,
            reviewId = card.reviewId,
        )
        repo.updateReview(result)
        return result
    }
}

class MarkAyahStudiedUseCase(private val repo: LearningRepository) {
    suspend operator fun invoke(surahNumber: Int, ayahNumber: Int) =
        repo.markAyahStudied(surahNumber, ayahNumber)
}

class GetProgressUseCase(private val repo: LearningRepository) {
    suspend operator fun invoke(): LearningProgress {
        val nowEpoch = System.currentTimeMillis() / 1000L
        return repo.getProgress(nowEpoch)
    }
}
