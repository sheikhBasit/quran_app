package com.quranapp.data.repository

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.quranapp.db.QuranDatabase
import com.quranapp.domain.model.ReviewResult
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Integration tests for [LearningRepositoryImpl] using an in-memory SQLite database.
 * No emulator or device required — runs on JVM via the SQLDelight JDBC driver.
 *
 * Key epoch conventions used throughout:
 *   - `DUE_EPOCH`  = 9_999_999_999L  — far future; any record whose next_review_at
 *     was set by DEFAULT (strftime('%s','now')) will be <= this, so the card is due.
 *   - `FUTURE_EPOCH` = 0L             — nothing is due before Unix time 0.
 */
class LearningRepositoryImplTest {

    private lateinit var db: QuranDatabase
    private lateinit var repo: LearningRepositoryImpl

    companion object {
        /** A large epoch so every default-inserted review record is considered due. */
        private const val DUE_EPOCH = 9_999_999_999L

        /** Epoch before any default record, so nothing is due. */
        private const val NO_DUE_EPOCH = 0L
    }

    // ── Setup ────────────────────────────────────────────────────────────────

    @BeforeTest
    fun setup() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        QuranDatabase.Schema.create(driver)
        db = QuranDatabase(driver)
        repo = LearningRepositoryImpl(db)
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    /** Insert a word_meanings row so flashcard JOIN queries can resolve. */
    private fun insertWordMeaning(
        surah: Long = 1L,
        ayah: Long = 1L,
        position: Long = 1L,
        arabic: String = "بِسْمِ",
        transliteration: String = "bismi",
        meaning: String = "in the name of",
    ) {
        db.wordMeaningsQueries.insert(
            surahNumber = surah,
            ayahNumber = ayah,
            wordPosition = position,
            arabicWord = arabic,
            transliteration = transliteration,
            englishMeaning = meaning,
            rootArabic = null,
            rootEnglish = null,
            occurrenceCount = 0L,
        )
    }

    /**
     * Convenience: add a word to the bank AND create its review record.
     * Returns the word_bank id.
     */
    private suspend fun addWordWithReview(
        surah: Int = 1,
        ayah: Int = 1,
        position: Int = 1,
    ): Long {
        insertWordMeaning(surah.toLong(), ayah.toLong(), position.toLong())
        repo.addToWordBank(surah, ayah, position)
        val id = repo.getWordBankId(surah, ayah, position)!!
        repo.ensureReviewRecord(id)
        return id
    }

    // ── getWordMeanings ──────────────────────────────────────────────────────

    @Test
    fun `getWordMeanings returns empty list when no words inserted`() = runTest {
        val result = repo.getWordMeanings(1, 1)
        assertTrue(result.isEmpty(), "Expected empty list but got $result")
    }

    @Test
    fun `getWordMeanings returns words for the correct surah and ayah`() = runTest {
        insertWordMeaning(surah = 1L, ayah = 1L, position = 1L, arabic = "بِسْمِ")
        insertWordMeaning(surah = 1L, ayah = 1L, position = 2L, arabic = "اللَّهِ")

        val result = repo.getWordMeanings(1, 1)

        assertEquals(2, result.size)
        assertEquals(1, result[0].wordPosition)
        assertEquals(2, result[1].wordPosition)
        assertTrue(result.all { it.surahNumber == 1 && it.ayahNumber == 1 })
    }

    @Test
    fun `getWordMeanings does not return words from a different ayah`() = runTest {
        insertWordMeaning(surah = 1L, ayah = 1L, position = 1L)
        insertWordMeaning(surah = 1L, ayah = 2L, position = 1L, arabic = "الْحَمْدُ")

        val result = repo.getWordMeanings(1, 1)

        assertEquals(1, result.size)
        assertEquals(1, result[0].ayahNumber)
    }

    // ── addToWordBank + isInWordBank ─────────────────────────────────────────

    @Test
    fun `isInWordBank returns false before adding`() = runTest {
        assertFalse(repo.isInWordBank(1, 1, 1))
    }

    @Test
    fun `isInWordBank returns true after adding`() = runTest {
        repo.addToWordBank(1, 1, 1)
        assertTrue(repo.isInWordBank(1, 1, 1))
    }

    @Test
    fun `addToWordBank is idempotent — adding twice does not throw`() = runTest {
        repo.addToWordBank(1, 1, 1)
        repo.addToWordBank(1, 1, 1) // INSERT OR IGNORE — must not fail
        assertTrue(repo.isInWordBank(1, 1, 1))
        assertEquals(1, repo.getWordBankCount())
    }

    // ── getWordBankId ────────────────────────────────────────────────────────

    @Test
    fun `getWordBankId returns null before adding`() = runTest {
        assertNull(repo.getWordBankId(1, 1, 1))
    }

    @Test
    fun `getWordBankId returns a Long after adding`() = runTest {
        repo.addToWordBank(1, 1, 1)
        val id = repo.getWordBankId(1, 1, 1)
        assertNotNull(id)
        assertTrue(id > 0L)
    }

    // ── ensureReviewRecord ───────────────────────────────────────────────────

    @Test
    fun `ensureReviewRecord creates a review record for the word bank entry`() = runTest {
        repo.addToWordBank(1, 1, 1)
        val id = repo.getWordBankId(1, 1, 1)!!

        repo.ensureReviewRecord(id)

        // If the record was created, getDueCount with a large epoch should be >= 1
        val count = repo.getDueCount(DUE_EPOCH)
        assertEquals(1, count)
    }

    @Test
    fun `ensureReviewRecord is idempotent — calling twice does not fail`() = runTest {
        repo.addToWordBank(1, 1, 1)
        val id = repo.getWordBankId(1, 1, 1)!!

        repo.ensureReviewRecord(id)
        repo.ensureReviewRecord(id) // INSERT OR IGNORE — must not fail

        assertEquals(1, repo.getDueCount(DUE_EPOCH))
    }

    // ── getDueFlashcards ─────────────────────────────────────────────────────

    @Test
    fun `getDueFlashcards returns flashcard when review is due`() = runTest {
        addWordWithReview(surah = 1, ayah = 1, position = 1)

        val cards = repo.getDueFlashcards(DUE_EPOCH)

        assertEquals(1, cards.size)
        val card = cards.first()
        assertEquals(1, card.surahNumber)
        assertEquals(1, card.ayahNumber)
        assertEquals("بِسْمِ", card.arabicWord)
        assertEquals("bismi", card.transliteration)
        assertEquals("in the name of", card.englishMeaning)
    }

    @Test
    fun `getDueFlashcards returns empty list when nothing is due`() = runTest {
        addWordWithReview(surah = 1, ayah = 1, position = 1)

        // Epoch 0 is before the default strftime('%s','now') timestamp
        val cards = repo.getDueFlashcards(NO_DUE_EPOCH)

        assertTrue(cards.isEmpty(), "Expected no due cards at epoch 0 but got $cards")
    }

    @Test
    fun `getDueFlashcards returns only cards whose next_review_at is lte nowEpoch`() = runTest {
        addWordWithReview(surah = 1, ayah = 1, position = 1)
        addWordWithReview(surah = 1, ayah = 1, position = 2)

        // Manually push one card's next_review_at far into the future
        val firstId = repo.getWordBankId(1, 1, 1)!!
        val reviewId = db.learningDataQueries.selectDueReviews(DUE_EPOCH).executeAsList()
            .first { it.word_bank_id == firstId }.id

        db.learningDataQueries.updateReview(
            nextReviewAt   = Long.MAX_VALUE,
            intervalDays   = 30L,
            easeFactor     = 2.5,
            repetitions    = 3L,
            lastReviewedAt = DUE_EPOCH,
            id             = reviewId,
        )

        val cards = repo.getDueFlashcards(DUE_EPOCH)

        assertEquals(1, cards.size, "Only one card should be due")
        assertFalse(cards.any { it.wordBankId == firstId }, "Rescheduled card must not appear")
    }

    // ── getDueCount ──────────────────────────────────────────────────────────

    @Test
    fun `getDueCount matches the number of due cards`() = runTest {
        addWordWithReview(surah = 1, ayah = 1, position = 1)
        addWordWithReview(surah = 1, ayah = 1, position = 2)
        addWordWithReview(surah = 1, ayah = 1, position = 3)

        assertEquals(3, repo.getDueCount(DUE_EPOCH))
        assertEquals(0, repo.getDueCount(NO_DUE_EPOCH))
    }

    // ── updateReview ─────────────────────────────────────────────────────────

    @Test
    fun `updateReview persists new interval, ease, repetitions and next_review_at`() = runTest {
        val wordBankId = addWordWithReview(surah = 1, ayah = 1, position = 1)

        val reviewId = repo.getDueFlashcards(DUE_EPOCH).first().reviewId

        val result = ReviewResult(
            reviewId         = reviewId,
            nextIntervalDays = 6,
            nextEaseFactor   = 2.6,
            nextRepetitions  = 2,
            nextReviewEpoch  = 1_000_000L,
        )
        repo.updateReview(result)

        // Card should no longer be due at epoch 0 (next_review_at == 1_000_000)
        val dueAfter = repo.getDueFlashcards(NO_DUE_EPOCH)
        assertTrue(dueAfter.isEmpty(), "Card should not be due after rescheduling")

        // Verify persisted values via raw DB query
        val rows = db.learningDataQueries.selectDueReviews(Long.MAX_VALUE).executeAsList()
        val row = rows.first { it.word_bank_id == wordBankId }
        assertEquals(6L,      row.interval_days)
        assertEquals(2.6,     row.ease_factor,   absoluteTolerance = 0.0001)
        assertEquals(2L,      row.repetitions)
        assertEquals(1_000_000L, row.next_review_at)
    }

    // ── markAyahStudied + getStudiedAyahCount ────────────────────────────────

    @Test
    fun `getStudiedAyahCount returns 0 before any mark`() = runTest {
        assertEquals(0, repo.getStudiedAyahCount())
    }

    @Test
    fun `getStudiedAyahCount returns 1 after marking one ayah`() = runTest {
        repo.markAyahStudied(1, 1)
        assertEquals(1, repo.getStudiedAyahCount())
    }

    @Test
    fun `markAyahStudied is idempotent — marking twice still counts as 1`() = runTest {
        repo.markAyahStudied(1, 1)
        repo.markAyahStudied(1, 1) // INSERT OR IGNORE
        assertEquals(1, repo.getStudiedAyahCount())
    }

    @Test
    fun `marking different ayahs increments count correctly`() = runTest {
        repo.markAyahStudied(1, 1)
        repo.markAyahStudied(1, 2)
        repo.markAyahStudied(2, 1)
        assertEquals(3, repo.getStudiedAyahCount())
    }

    // ── getStudiedBySurah ────────────────────────────────────────────────────

    @Test
    fun `getStudiedBySurah groups correctly across multiple surahs`() = runTest {
        // Surah 1: 3 ayahs, Surah 2: 2 ayahs
        repo.markAyahStudied(1, 1)
        repo.markAyahStudied(1, 2)
        repo.markAyahStudied(1, 3)
        repo.markAyahStudied(2, 1)
        repo.markAyahStudied(2, 2)

        val bySurah = repo.getStudiedBySurah()

        assertEquals(2,  bySurah.size, "Expected entries for 2 surahs")
        assertEquals(3,  bySurah[1], "Surah 1 should have 3 studied ayahs")
        assertEquals(2,  bySurah[2], "Surah 2 should have 2 studied ayahs")
    }

    @Test
    fun `getStudiedBySurah returns empty map when nothing studied`() = runTest {
        val bySurah = repo.getStudiedBySurah()
        assertTrue(bySurah.isEmpty())
    }

    // ── getProgress ──────────────────────────────────────────────────────────

    @Test
    fun `getProgress aggregates wordBankCount, studiedAyahCount and dueReviewCount`() = runTest {
        // Add 2 words to the bank, both with review records
        addWordWithReview(surah = 1, ayah = 1, position = 1)
        addWordWithReview(surah = 1, ayah = 1, position = 2)

        // Study 3 ayahs across 2 surahs
        repo.markAyahStudied(1, 1)
        repo.markAyahStudied(1, 2)
        repo.markAyahStudied(2, 1)

        val progress = repo.getProgress(DUE_EPOCH)

        assertEquals(2, progress.wordBankCount,    "wordBankCount must be 2")
        assertEquals(3, progress.studiedAyahCount, "studiedAyahCount must be 3")
        assertEquals(2, progress.dueReviewCount,   "dueReviewCount must be 2 (all cards due)")
        assertEquals(0, progress.streakDays,        "streakDays is always 0 (not yet implemented)")
    }

    @Test
    fun `getProgress returns zeros when nothing has been recorded`() = runTest {
        val progress = repo.getProgress(DUE_EPOCH)

        assertEquals(0, progress.wordBankCount)
        assertEquals(0, progress.studiedAyahCount)
        assertEquals(0, progress.dueReviewCount)
        assertTrue(progress.studiedBySurah.isEmpty())
    }

    @Test
    fun `getProgress dueReviewCount is 0 when no cards are due`() = runTest {
        addWordWithReview(surah = 1, ayah = 1, position = 1)

        val progress = repo.getProgress(NO_DUE_EPOCH)

        assertEquals(1, progress.wordBankCount)
        assertEquals(0, progress.dueReviewCount)
    }
}
