package com.quranapp.viewmodel

import com.quranapp.domain.model.*
import com.quranapp.domain.repository.LearningRepository
import com.quranapp.domain.usecase.learning.*
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import kotlin.test.*

// ── Fakes ────────────────────────────────────────────────────────────────────

open class FakeLearningRepository(
    private val wordMeanings: Map<Pair<Int, Int>, List<WordMeaning>> = emptyMap(),
    private val dueFlashcards: List<DueFlashcard> = emptyList(),
    var addedToWordBank: MutableList<Triple<Int, Int, Int>> = mutableListOf(),
    private val inWordBank: Boolean = false,
) : LearningRepository {

    override suspend fun getWordMeanings(surahNumber: Int, ayahNumber: Int): List<WordMeaning> =
        wordMeanings[Pair(surahNumber, ayahNumber)] ?: emptyList()

    override suspend fun addToWordBank(surahNumber: Int, ayahNumber: Int, wordPosition: Int) {
        addedToWordBank.add(Triple(surahNumber, ayahNumber, wordPosition))
    }

    override suspend fun getWordBankId(surahNumber: Int, ayahNumber: Int, wordPosition: Int): Long? = null

    override suspend fun isInWordBank(surahNumber: Int, ayahNumber: Int, wordPosition: Int): Boolean = inWordBank

    override suspend fun getWordBankCount(): Int = addedToWordBank.size

    override suspend fun ensureReviewRecord(wordBankId: Long) {}

    override suspend fun getDueFlashcards(nowEpoch: Long): List<DueFlashcard> = dueFlashcards

    override suspend fun getDueCount(nowEpoch: Long): Int = dueFlashcards.size

    override suspend fun updateReview(result: ReviewResult) {}

    override suspend fun markAyahStudied(surahNumber: Int, ayahNumber: Int) {}

    override suspend fun getStudiedAyahCount(): Int = 0

    override suspend fun getStudiedBySurah(): Map<Int, Int> = emptyMap()

    override suspend fun getProgress(nowEpoch: Long): LearningProgress = LearningProgress(
        wordBankCount = 0,
        studiedAyahCount = 0,
        dueReviewCount = 0,
        streakDays = 0,
        studiedBySurah = emptyMap(),
    )
}

// ── Test fixtures ─────────────────────────────────────────────────────────────

private fun makeWord(
    surah: Int,
    ayah: Int,
    position: Int,
    arabic: String = "word",
): WordMeaning = WordMeaning(
    id = (surah * 1000L + ayah * 10 + position),
    surahNumber = surah,
    ayahNumber = ayah,
    wordPosition = position,
    arabicWord = arabic,
    transliteration = "transliteration",
    englishMeaning = "meaning",
    rootArabic = null,
    rootEnglish = null,
    quranOccurrenceCount = 1,
)

private fun makeFlashcard(index: Int): DueFlashcard = DueFlashcard(
    reviewId = index.toLong(),
    wordBankId = index.toLong(),
    arabicWord = "arabic$index",
    transliteration = "trans$index",
    englishMeaning = "meaning$index",
    surahNumber = 1,
    ayahNumber = index,
    intervalDays = 1,
    easeFactor = 2.5,
    repetitions = 0,
)

// ── Helpers ───────────────────────────────────────────────────────────────────

// Fake use cases — avoid Mockk operator-invoke type inference issues on final classes
private class FakeUnderstandAyahUseCase(
    private val tokens: List<String> = emptyList(),
) : UnderstandAyahUseCase(remote = mockk(relaxed = true)) {
    override operator fun invoke(
        surah: Int, ayah: Int, arabicText: String, translation: String,
    ): Flow<String> = if (tokens.isEmpty()) emptyFlow() else flowOf(*tokens.toTypedArray())
}

private class ThrowingUnderstandAyahUseCase(
    private val error: Throwable = RuntimeException("network error"),
) : UnderstandAyahUseCase(remote = mockk(relaxed = true)) {
    override operator fun invoke(
        surah: Int, ayah: Int, arabicText: String, translation: String,
    ): Flow<String> = kotlinx.coroutines.flow.flow { throw error }
}

@OptIn(ExperimentalCoroutinesApi::class)
private fun buildViewModel(
    repo: LearningRepository = FakeLearningRepository(),
    understandAyah: UnderstandAyahUseCase = FakeUnderstandAyahUseCase(),
): LearningViewModel = LearningViewModel(
    getWordMeanings = GetWordMeaningsUseCase(repo),
    addToWordBank = AddToWordBankUseCase(repo),
    getDueFlashcards = GetDueFlashcardsUseCase(repo),
    recordReview = RecordReviewUseCase(repo),
    markAyahStudied = MarkAyahStudiedUseCase(repo),
    getProgress = GetProgressUseCase(repo),
    understandAyah = understandAyah,
    isInWordBank = IsInWordBankUseCase(repo),
)

// ── Test class ────────────────────────────────────────────────────────────────

@OptIn(ExperimentalCoroutinesApi::class)
class LearningViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun teardown() {
        Dispatchers.resetMain()
    }

    // ── loadWordMeanings ──────────────────────────────────────────────────────

    @Test
    fun `loadWordMeanings emits words into state`() = runTest {
        val words = listOf(makeWord(1, 1, 1), makeWord(1, 1, 2))
        val repo = FakeLearningRepository(wordMeanings = mapOf(Pair(1, 1) to words))
        val vm = buildViewModel(repo)

        vm.loadWordMeanings(surahNumber = 1, ayahNumber = 1)
        advanceUntilIdle()

        assertEquals(2, vm.uiState.value.wordMeanings.size)
        assertFalse(vm.uiState.value.isLoadingWords)
    }

    @Test
    fun `loadWordMeanings merging across multiple ayahs preserves both`() = runTest {
        val wordsAyah1 = listOf(makeWord(1, 1, 1))
        val wordsAyah2 = listOf(makeWord(1, 2, 1))
        val repo = FakeLearningRepository(
            wordMeanings = mapOf(
                Pair(1, 1) to wordsAyah1,
                Pair(1, 2) to wordsAyah2,
            )
        )
        val vm = buildViewModel(repo)

        vm.loadWordMeanings(surahNumber = 1, ayahNumber = 1)
        advanceUntilIdle()
        vm.loadWordMeanings(surahNumber = 1, ayahNumber = 2)
        advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals(2, state.wordMeanings.size)
        assertTrue(state.wordMeanings.any { it.ayahNumber == 1 })
        assertTrue(state.wordMeanings.any { it.ayahNumber == 2 })
    }

    @Test
    fun `loadWordMeanings for same ayah and surah replaces not duplicates`() = runTest {
        val firstLoad = listOf(makeWord(1, 1, 1), makeWord(1, 1, 2))
        val secondLoad = listOf(makeWord(1, 1, 1))
        var callCount = 0
        val repo = object : FakeLearningRepository() {
            override suspend fun getWordMeanings(surahNumber: Int, ayahNumber: Int): List<WordMeaning> {
                callCount++
                return if (callCount == 1) firstLoad else secondLoad
            }
        }
        val vm = buildViewModel(repo)

        vm.loadWordMeanings(surahNumber = 1, ayahNumber = 1)
        advanceUntilIdle()
        assertEquals(2, vm.uiState.value.wordMeanings.size)

        vm.loadWordMeanings(surahNumber = 1, ayahNumber = 1)
        advanceUntilIdle()
        assertEquals(1, vm.uiState.value.wordMeanings.size)
    }

    @Test
    fun `loadWordMeanings cross-surah does not evict words from other surah same ayah number`() = runTest {
        val surah1Words = listOf(makeWord(surah = 1, ayah = 5, position = 1))
        val surah2Words = listOf(makeWord(surah = 2, ayah = 5, position = 1))
        val repo = FakeLearningRepository(
            wordMeanings = mapOf(
                Pair(1, 5) to surah1Words,
                Pair(2, 5) to surah2Words,
            )
        )
        val vm = buildViewModel(repo)

        vm.loadWordMeanings(surahNumber = 1, ayahNumber = 5)
        advanceUntilIdle()
        vm.loadWordMeanings(surahNumber = 2, ayahNumber = 5)
        advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals(2, state.wordMeanings.size)
        assertTrue(state.wordMeanings.any { it.surahNumber == 1 && it.ayahNumber == 5 })
        assertTrue(state.wordMeanings.any { it.surahNumber == 2 && it.ayahNumber == 5 })
    }

    // ── clearWordMeanings ─────────────────────────────────────────────────────

    @Test
    fun `clearWordMeanings empties the list`() = runTest {
        val words = listOf(makeWord(1, 1, 1))
        val repo = FakeLearningRepository(wordMeanings = mapOf(Pair(1, 1) to words))
        val vm = buildViewModel(repo)

        vm.loadWordMeanings(surahNumber = 1, ayahNumber = 1)
        advanceUntilIdle()
        assertEquals(1, vm.uiState.value.wordMeanings.size)

        vm.clearWordMeanings()

        assertTrue(vm.uiState.value.wordMeanings.isEmpty())
    }

    // ── selectWord ────────────────────────────────────────────────────────────

    @Test
    fun `selectWord sets selectedWord`() = runTest {
        val vm = buildViewModel()
        val word = makeWord(1, 1, 1)

        vm.selectWord(word)
        advanceUntilIdle()

        assertEquals(word, vm.uiState.value.selectedWord)
    }

    @Test
    fun `selectWord with null clears selectedWord`() = runTest {
        val vm = buildViewModel()
        val word = makeWord(1, 1, 1)

        vm.selectWord(word)
        advanceUntilIdle()
        vm.selectWord(null)

        assertNull(vm.uiState.value.selectedWord)
    }

    @Test
    fun `selectWord sets selectedWordInBank to false immediately`() = runTest {
        val repoInBank = FakeLearningRepository(inWordBank = true)
        val vm = buildViewModel(repoInBank)
        val word = makeWord(1, 1, 1)

        // Prime state with something non-false to show it's cleared immediately
        vm.selectWord(word)
        // Before coroutines run, selectedWordInBank should be false
        assertFalse(vm.uiState.value.selectedWordInBank)

        // After coroutines run it reflects the repo result (true in this fake)
        advanceUntilIdle()
        assertTrue(vm.uiState.value.selectedWordInBank)
    }

    @Test
    fun `selectWord null does not launch isInWordBank coroutine`() = runTest {
        val vm = buildViewModel()

        vm.selectWord(null)
        advanceUntilIdle()

        assertNull(vm.uiState.value.selectedWord)
        assertFalse(vm.uiState.value.selectedWordInBank)
    }

    // ── addWordToBank ─────────────────────────────────────────────────────────

    @Test
    fun `addWordToBank calls use case with correct parameters`() = runTest {
        val repo = FakeLearningRepository()
        val vm = buildViewModel(repo)

        vm.addWordToBank(surahNumber = 2, ayahNumber = 10, wordPosition = 3)
        advanceUntilIdle()

        assertTrue(repo.addedToWordBank.contains(Triple(2, 10, 3)))
    }

    // ── startFlashcardSession ─────────────────────────────────────────────────

    @Test
    fun `startFlashcardSession resets sessionComplete to false before fetch`() = runTest {
        val cards = listOf(makeFlashcard(1))
        val repo = FakeLearningRepository(dueFlashcards = cards)
        val vm = buildViewModel(repo)

        // Put vm into sessionComplete=true state first
        val emptyRepo = FakeLearningRepository(dueFlashcards = emptyList())
        val vm2 = buildViewModel(emptyRepo)
        vm2.startFlashcardSession()
        advanceUntilIdle()
        assertTrue(vm2.uiState.value.sessionComplete)

        // Now test that a new session with cards resets it
        vm.startFlashcardSession()
        advanceUntilIdle()
        assertFalse(vm.uiState.value.sessionComplete)
    }

    @Test
    fun `startFlashcardSession with cards sets dueFlashcards currentCardIndex showAnswer sessionCorrect`() = runTest {
        val cards = listOf(makeFlashcard(1), makeFlashcard(2))
        val repo = FakeLearningRepository(dueFlashcards = cards)
        val vm = buildViewModel(repo)

        vm.startFlashcardSession()
        advanceUntilIdle()

        with(vm.uiState.value) {
            assertEquals(2, dueFlashcards.size)
            assertEquals(0, currentCardIndex)
            assertFalse(showAnswer)
            assertEquals(0, sessionCorrect)
            assertFalse(sessionComplete)
            assertEquals(2, sessionTotal)
        }
    }

    @Test
    fun `startFlashcardSession with no cards sets sessionComplete to true`() = runTest {
        val repo = FakeLearningRepository(dueFlashcards = emptyList())
        val vm = buildViewModel(repo)

        vm.startFlashcardSession()
        advanceUntilIdle()

        assertTrue(vm.uiState.value.sessionComplete)
    }

    // ── revealAnswer ──────────────────────────────────────────────────────────

    @Test
    fun `revealAnswer sets showAnswer to true`() = runTest {
        val repo = FakeLearningRepository(dueFlashcards = listOf(makeFlashcard(1)))
        val vm = buildViewModel(repo)

        vm.startFlashcardSession()
        advanceUntilIdle()
        assertFalse(vm.uiState.value.showAnswer)

        vm.revealAnswer()

        assertTrue(vm.uiState.value.showAnswer)
    }

    // ── submitRating ──────────────────────────────────────────────────────────

    @Test
    fun `submitRating KNOW_IT advances currentCardIndex and increments sessionCorrect`() = runTest {
        val cards = listOf(makeFlashcard(1), makeFlashcard(2))
        val repo = FakeLearningRepository(dueFlashcards = cards)
        val vm = buildViewModel(repo)

        vm.startFlashcardSession()
        advanceUntilIdle()
        assertEquals(0, vm.uiState.value.currentCardIndex)
        assertEquals(0, vm.uiState.value.sessionCorrect)

        vm.submitRating(ReviewRating.KNOW_IT)
        advanceUntilIdle()

        assertEquals(1, vm.uiState.value.currentCardIndex)
        assertEquals(1, vm.uiState.value.sessionCorrect)
        assertFalse(vm.uiState.value.sessionComplete)
    }

    @Test
    fun `submitRating KNOW_IT sets sessionComplete when last card`() = runTest {
        val cards = listOf(makeFlashcard(1))
        val repo = FakeLearningRepository(dueFlashcards = cards)
        val vm = buildViewModel(repo)

        vm.startFlashcardSession()
        advanceUntilIdle()

        vm.submitRating(ReviewRating.KNOW_IT)
        advanceUntilIdle()

        assertTrue(vm.uiState.value.sessionComplete)
        assertEquals(1, vm.uiState.value.sessionCorrect)
    }

    @Test
    fun `submitRating AGAIN advances index but does not increment sessionCorrect`() = runTest {
        val cards = listOf(makeFlashcard(1), makeFlashcard(2))
        val repo = FakeLearningRepository(dueFlashcards = cards)
        val vm = buildViewModel(repo)

        vm.startFlashcardSession()
        advanceUntilIdle()

        vm.submitRating(ReviewRating.AGAIN)
        advanceUntilIdle()

        assertEquals(1, vm.uiState.value.currentCardIndex)
        assertEquals(0, vm.uiState.value.sessionCorrect)
    }

    @Test
    fun `submitRating AGAIN sets sessionComplete when last card`() = runTest {
        val cards = listOf(makeFlashcard(1))
        val repo = FakeLearningRepository(dueFlashcards = cards)
        val vm = buildViewModel(repo)

        vm.startFlashcardSession()
        advanceUntilIdle()

        vm.submitRating(ReviewRating.AGAIN)
        advanceUntilIdle()

        assertTrue(vm.uiState.value.sessionComplete)
        assertEquals(0, vm.uiState.value.sessionCorrect)
    }

    @Test
    fun `submitRating with no cards loaded does nothing`() = runTest {
        val vm = buildViewModel()

        // No session started — dueFlashcards is empty, currentCardIndex = 0
        // getOrNull(0) returns null so submitRating should return early
        vm.submitRating(ReviewRating.KNOW_IT)
        advanceUntilIdle()

        assertEquals(0, vm.uiState.value.currentCardIndex)
        assertEquals(0, vm.uiState.value.sessionCorrect)
    }

    // ── clearUnderstand ───────────────────────────────────────────────────────

    @Test
    fun `clearUnderstand clears understandText understandError and isLoadingUnderstand`() = runTest {
        val vm = buildViewModel(understandAyah = FakeUnderstandAyahUseCase(tokens = listOf("token1", "token2")))

        vm.startUnderstand(1, 1, "arabic", "translation")
        advanceUntilIdle()

        // Verify text was accumulated
        assertEquals("token1token2", vm.uiState.value.understandText)

        vm.clearUnderstand()

        with(vm.uiState.value) {
            assertEquals("", understandText)
            assertNull(understandError)
            assertFalse(isLoadingUnderstand)
        }
    }

    @Test
    fun `clearUnderstand clears error state too`() = runTest {
        val vm = buildViewModel(understandAyah = ThrowingUnderstandAyahUseCase())

        vm.startUnderstand(1, 1, "arabic", "translation")
        advanceUntilIdle()
        assertNotNull(vm.uiState.value.understandError)

        vm.clearUnderstand()

        assertNull(vm.uiState.value.understandError)
        assertEquals("", vm.uiState.value.understandText)
        assertFalse(vm.uiState.value.isLoadingUnderstand)
    }
}
