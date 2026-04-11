package com.quranapp.domain.usecase.learning

import com.quranapp.domain.model.ReviewRating
import com.quranapp.domain.model.ReviewResult
import com.quranapp.util.currentTimeMillis

/**
 * SM-2 spaced repetition algorithm.
 * Pure function — no DB access. Compute the next review schedule,
 * then persist via RecordReviewUseCase.
 */
fun calculateNextReview(
    intervalDays: Int,
    easeFactor: Double,
    repetitions: Int,
    rating: ReviewRating,
    nowEpoch: Long = currentTimeMillis() / 1000L,
    reviewId: Long = 0L,
): ReviewResult {
    val (newInterval, newEase, newReps) = when (rating) {
        ReviewRating.AGAIN -> Triple(1, maxOf(1.3, easeFactor - 0.20), 0)
        ReviewRating.HARD  -> Triple(intervalDays, maxOf(1.3, easeFactor - 0.15), repetitions)
        ReviewRating.KNOW_IT -> {
            val newInterval = when (repetitions) {
                0 -> 1
                1 -> 6
                else -> (intervalDays * easeFactor).toInt()
            }
            Triple(newInterval, easeFactor, repetitions + 1)
        }
    }
    return ReviewResult(
        reviewId = reviewId,
        nextIntervalDays = newInterval,
        nextEaseFactor = newEase,
        nextRepetitions = newReps,
        nextReviewEpoch = nowEpoch + (newInterval * 86400L),
    )
}
