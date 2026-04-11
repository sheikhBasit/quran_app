package com.quranapp.viewmodel

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.quranapp.domain.model.*
import com.quranapp.domain.usecase.learning.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class LearningUiState(
    // Layer 1: word breakdown
    val wordMeanings: List<WordMeaning> = emptyList(),
    val isLoadingWords: Boolean = false,
    val selectedWord: WordMeaning? = null,
    val selectedWordInBank: Boolean = false,

    // Layer 2: understand
    val understandText: String = "",
    val isLoadingUnderstand: Boolean = false,
    val understandError: String? = null,

    // Layer 3: flashcards
    val dueFlashcards: List<DueFlashcard> = emptyList(),
    val currentCardIndex: Int = 0,
    val showAnswer: Boolean = false,
    val sessionComplete: Boolean = false,
    val sessionCorrect: Int = 0,
    val sessionTotal: Int = 0,

    // Progress
    val progress: LearningProgress? = null,
)

class LearningViewModel(
    private val getWordMeanings: GetWordMeaningsUseCase,
    private val addToWordBank: AddToWordBankUseCase,
    private val getDueFlashcards: GetDueFlashcardsUseCase,
    private val recordReview: RecordReviewUseCase,
    private val markAyahStudied: MarkAyahStudiedUseCase,
    private val getProgress: GetProgressUseCase,
    private val understandAyah: UnderstandAyahUseCase,
    private val isInWordBank: IsInWordBankUseCase,
) : ScreenModel {

    private val _uiState = MutableStateFlow(LearningUiState())
    val uiState: StateFlow<LearningUiState> = _uiState.asStateFlow()

    // ── Layer 1: Word Breakdown ──────────────────────────────────────────────

    fun loadWordMeanings(surahNumber: Int, ayahNumber: Int) {
        screenModelScope.launch {
            _uiState.update { it.copy(isLoadingWords = true) }
            val words = getWordMeanings(surahNumber, ayahNumber)
            _uiState.update { state ->
                // Merge: keep existing words for other ayahs, replace for this ayah
                val others = state.wordMeanings.filter { it.surahNumber != surahNumber || it.ayahNumber != ayahNumber }
                state.copy(wordMeanings = others + words, isLoadingWords = false)
            }
        }
    }

    fun clearWordMeanings() {
        _uiState.update { it.copy(wordMeanings = emptyList()) }
    }

    fun selectWord(word: WordMeaning?) {
        _uiState.update { it.copy(selectedWord = word, selectedWordInBank = false) }
        if (word != null) {
            screenModelScope.launch {
                val inBank = isInWordBank(word.surahNumber, word.ayahNumber, word.wordPosition)
                _uiState.update { it.copy(selectedWordInBank = inBank) }
            }
        }
    }

    fun addWordToBank(surahNumber: Int, ayahNumber: Int, wordPosition: Int) {
        screenModelScope.launch {
            addToWordBank(surahNumber, ayahNumber, wordPosition)
        }
    }

    // ── Layer 2: Understand ──────────────────────────────────────────────────

    fun startUnderstand(surahNumber: Int, ayahNumber: Int, arabicText: String, translation: String) {
        screenModelScope.launch {
            _uiState.update { it.copy(understandText = "", isLoadingUnderstand = true, understandError = null) }
            try {
                understandAyah(surahNumber, ayahNumber, arabicText, translation)
                    .collect { token ->
                        _uiState.update { it.copy(understandText = it.understandText + token) }
                    }
                markAyahStudied(surahNumber, ayahNumber)
            } catch (e: Exception) {
                _uiState.update { it.copy(understandError = "Failed to load explanation. Check your connection.") }
            } finally {
                _uiState.update { it.copy(isLoadingUnderstand = false) }
            }
        }
    }

    fun clearUnderstand() {
        _uiState.update { it.copy(understandText = "", understandError = null, isLoadingUnderstand = false) }
    }

    // ── Layer 3: Flashcards ──────────────────────────────────────────────────

    fun startFlashcardSession() {
        screenModelScope.launch {
            _uiState.update { it.copy(sessionComplete = false) }
            val cards = getDueFlashcards()
            _uiState.update {
                it.copy(
                    dueFlashcards = cards,
                    currentCardIndex = 0,
                    showAnswer = false,
                    sessionComplete = cards.isEmpty(),
                    sessionCorrect = 0,
                    sessionTotal = cards.size,
                )
            }
        }
    }

    fun revealAnswer() {
        _uiState.update { it.copy(showAnswer = true) }
    }

    fun submitRating(rating: ReviewRating) {
        val state = _uiState.value
        val card = state.dueFlashcards.getOrNull(state.currentCardIndex) ?: return
        screenModelScope.launch {
            recordReview(card, rating)
            val newCorrect = if (rating == ReviewRating.KNOW_IT) state.sessionCorrect + 1 else state.sessionCorrect
            val nextIndex = state.currentCardIndex + 1
            val isDone = nextIndex >= state.dueFlashcards.size
            _uiState.update {
                it.copy(
                    currentCardIndex = nextIndex,
                    showAnswer = false,
                    sessionComplete = isDone,
                    sessionCorrect = newCorrect,
                )
            }
            if (isDone) refreshProgress()
        }
    }

    // ── Progress ─────────────────────────────────────────────────────────────

    fun refreshProgress() {
        screenModelScope.launch {
            val progress = getProgress()
            _uiState.update { it.copy(progress = progress) }
        }
    }
}
