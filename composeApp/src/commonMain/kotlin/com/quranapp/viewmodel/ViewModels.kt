package com.quranapp.viewmodel

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.quranapp.domain.model.*
import com.quranapp.domain.usecase.chatbot.SendChatMessageUseCase
import com.quranapp.domain.usecase.quran.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

// ─── Quran ────────────────────────────────────────────────────────────────────

data class QuranUiState(
    val ayahs: List<Ayah> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val showTranslation: Boolean = true,
    val script: QuranScript = QuranScript.HAFS,
    val readingMode: ReadingMode = ReadingMode.SCROLL,
    val currentPage: Int = 1,
    val currentSurah: Int = 1,
)

class QuranViewModel(
    private val getAyahsBySurah: GetAyahsBySurahUseCase,
    private val getAyahsForPage: GetAyahsForPageUseCase,
    private val getTafsir: GetTafsirUseCase,
) : ScreenModel {

    private val _uiState = MutableStateFlow(QuranUiState())
    val uiState: StateFlow<QuranUiState> = _uiState.asStateFlow()

    fun loadSurah(surahNumber: Int) {
        screenModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val ayahs = getAyahsBySurah(surahNumber).getOrThrow()
                _uiState.update { it.copy(ayahs = ayahs, isLoading = false, currentSurah = surahNumber) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message, isLoading = false) }
            }
        }
    }

    fun loadPage(pageNumber: Int) {
        screenModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val ayahs = getAyahsForPage(pageNumber).getOrThrow()
                _uiState.update { it.copy(ayahs = ayahs, isLoading = false, currentPage = pageNumber) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message, isLoading = false) }
            }
        }
    }

    fun toggleTranslation() = _uiState.update { it.copy(showTranslation = !it.showTranslation) }

    fun toggleScript() = _uiState.update {
        it.copy(script = if (it.script == QuranScript.HAFS) QuranScript.WARSH else QuranScript.HAFS)
    }

    fun toggleReadingMode() = _uiState.update {
        it.copy(readingMode = if (it.readingMode == ReadingMode.SCROLL) ReadingMode.PAGE else ReadingMode.SCROLL)
    }
}

// ─── Chatbot ──────────────────────────────────────────────────────────────────

data class ChatbotUiState(
    val messages: List<ChatMessage> = emptyList(),
    val isLoading: Boolean = false,
    val isOnline: Boolean = true,
)

class ChatbotViewModel(
    private val sendChatMessageUseCase: SendChatMessageUseCase,
) : ScreenModel {

    private val _uiState = MutableStateFlow(ChatbotUiState())
    val uiState: StateFlow<ChatbotUiState> = _uiState.asStateFlow()

    fun sendMessage(text: String) {
        if (text.isBlank()) return

        val userMsg = ChatMessage(id = UUID.randomUUID().toString(), role = ChatRole.USER, content = text)
        val loadingMsg = ChatMessage(id = UUID.randomUUID().toString(), role = ChatRole.ASSISTANT, content = "", isLoading = true)

        _uiState.update { it.copy(messages = it.messages + userMsg + loadingMsg, isLoading = true) }

        screenModelScope.launch {
            try {
                val response = sendChatMessageUseCase(text).getOrThrow()
                _uiState.update { state ->
                    val updated = state.messages.dropLast(1) +
                        ChatMessage(
                            id = UUID.randomUUID().toString(),
                            role = ChatRole.ASSISTANT,
                            content = response.answer,
                            sources = response.sources,
                        )
                    state.copy(messages = updated, isLoading = false)
                }
            } catch (e: Exception) {
                _uiState.update { state ->
                    val errorMsg = ChatMessage(
                        id = UUID.randomUUID().toString(),
                        role = ChatRole.ASSISTANT,
                        content = "Sorry, an error occurred: ${e.message ?: "Service unavailable"}",
                    )
                    state.copy(messages = state.messages.dropLast(1) + errorMsg, isLoading = false)
                }
            }
        }
    }
}
