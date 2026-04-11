package com.quranapp.domain.usecase

import com.quranapp.domain.model.ReviewRating
import com.quranapp.domain.usecase.learning.calculateNextReview
import kotlin.test.Test
import kotlin.test.assertEquals

class SpacedRepetitionTest {

    @Test
    fun `AGAIN resets interval to 1 and repetitions to 0`() {
        val result = calculateNextReview(
            intervalDays = 7, easeFactor = 2.5, repetitions = 3, rating = ReviewRating.AGAIN,
        )
        assertEquals(1, result.nextIntervalDays)
        assertEquals(0, result.nextRepetitions)
    }

    @Test
    fun `AGAIN decreases ease factor by 0_20`() {
        val result = calculateNextReview(
            intervalDays = 7, easeFactor = 2.5, repetitions = 3, rating = ReviewRating.AGAIN,
        )
        assertEquals(2.3, result.nextEaseFactor, absoluteTolerance = 0.01)
    }

    @Test
    fun `ease factor never drops below 1_3`() {
        val result = calculateNextReview(
            intervalDays = 1, easeFactor = 1.3, repetitions = 0, rating = ReviewRating.AGAIN,
        )
        assertEquals(1.3, result.nextEaseFactor, absoluteTolerance = 0.01)
    }

    @Test
    fun `HARD keeps interval and decreases ease by 0_15`() {
        val result = calculateNextReview(
            intervalDays = 6, easeFactor = 2.5, repetitions = 2, rating = ReviewRating.HARD,
        )
        assertEquals(6, result.nextIntervalDays)
        assertEquals(2.35, result.nextEaseFactor, absoluteTolerance = 0.01)
        assertEquals(2, result.nextRepetitions)
    }

    @Test
    fun `KNOW_IT first repetition sets interval to 1`() {
        val result = calculateNextReview(
            intervalDays = 1, easeFactor = 2.5, repetitions = 0, rating = ReviewRating.KNOW_IT,
        )
        assertEquals(1, result.nextIntervalDays)
        assertEquals(1, result.nextRepetitions)
    }

    @Test
    fun `KNOW_IT second repetition sets interval to 6`() {
        val result = calculateNextReview(
            intervalDays = 1, easeFactor = 2.5, repetitions = 1, rating = ReviewRating.KNOW_IT,
        )
        assertEquals(6, result.nextIntervalDays)
        assertEquals(2, result.nextRepetitions)
    }

    @Test
    fun `KNOW_IT third repetition multiplies interval by ease factor`() {
        val result = calculateNextReview(
            intervalDays = 6, easeFactor = 2.5, repetitions = 2, rating = ReviewRating.KNOW_IT,
        )
        assertEquals(15, result.nextIntervalDays) // floor(6 * 2.5) = 15
        assertEquals(3, result.nextRepetitions)
    }

    @Test
    fun `nextReviewEpoch is nowEpoch plus intervalDays times 86400`() {
        val nowEpoch = 1_700_000_000L
        val result = calculateNextReview(
            intervalDays = 3, easeFactor = 2.5, repetitions = 2,
            rating = ReviewRating.KNOW_IT, nowEpoch = nowEpoch,
        )
        val expectedEpoch = nowEpoch + (result.nextIntervalDays * 86400L)
        assertEquals(expectedEpoch, result.nextReviewEpoch)
    }
}
