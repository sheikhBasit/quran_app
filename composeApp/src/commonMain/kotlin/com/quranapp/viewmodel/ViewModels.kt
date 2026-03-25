package com.quranapp.viewmodel

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.quranapp.domain.model.*
import com.quranapp.domain.repository.ChatHistoryRepository
import com.quranapp.domain.usecase.chatbot.StreamChatMessageUseCase
import com.quranapp.domain.usecase.hadith.*
import com.quranapp.domain.usecase.prayer.*
import com.quranapp.domain.usecase.qibla.GetQiblaDirectionUseCase
import com.quranapp.domain.usecase.quran.*
import com.quranapp.domain.repository.SettingsRepository
import com.quranapp.util.randomUUID
import com.quranapp.util.randomUUID
import com.quranapp.util.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

// ─── Quran ────────────────────────────────────────────────────────────────────

data class QuranUiState(
    val surahs: List<Surah> = emptyList(),
    val ayahs: List<Ayah> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val showTranslation: Boolean = true,
    val script: QuranScript = QuranScript.HAFS,
    val readingMode: ReadingMode = ReadingMode.SCROLL,
    val currentPage: Int = 1,
    val currentSurah: Int = 1,
    val tafsiers: List<TafsirEntry> = emptyList(),
    val arabicFontSize: Float = 28f,
)

class QuranViewModel(
    private val getSurahList: GetSurahListUseCase,
    private val getAyahsBySurah: GetAyahsBySurahUseCase,
    private val getAyahsForPage: GetAyahsForPageUseCase,
    private val getTafsir: GetTafsirUseCase,
    private val settingsRepository: SettingsRepository,
) : ScreenModel {

    private val _uiState = MutableStateFlow(QuranUiState())
    val uiState: StateFlow<QuranUiState> = _uiState.asStateFlow()

    init {
        loadSurahList()
        // Sync Settings
        screenModelScope.launch {
            settingsRepository.showTranslation.collect { show ->
                _uiState.update { it.copy(showTranslation = show) }
            }
        }
        screenModelScope.launch {
            settingsRepository.arabicFontSize.collect { size ->
                _uiState.update { it.copy(arabicFontSize = size) }
            }
        }
    }

    fun loadSurahList() {
        screenModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val surahs = getSurahList().getOrThrow()
                println("QuranViewModel: Loaded ${surahs.size} surahs")
                _uiState.update { it.copy(surahs = surahs, isLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message, isLoading = false) }
            }
        }
    }

    fun loadSurah(surahNumber: Int) {
        screenModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val ayahs = getAyahsBySurah(surahNumber).getOrThrow()
                println("QuranViewModel: Loaded ${ayahs.size} ayahs for surah $surahNumber")
                _uiState.update { it.copy(ayahs = ayahs, isLoading = false, currentSurah = surahNumber) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message, isLoading = false) }
            }
        }
    }

    fun loadPage(pageNumber: Int) {
        screenModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, currentPage = pageNumber) }
            try {
                val ayahs = getAyahsForPage(pageNumber).getOrThrow()
                _uiState.update { it.copy(ayahs = ayahs, isLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message, isLoading = false) }
            }
        }
    }

    fun loadTafsir(ayah: Ayah) {
        screenModelScope.launch {
            // We don't set isLoading=true here to avoid flickering the main reader
            try {
                val entries = getTafsir(ayah.surahNumber, ayah.ayahNumber).getOrThrow()
                _uiState.update { it.copy(tafsiers = entries) }
            } catch (e: Exception) {
                // Handle error silently or log it
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
    val currentSessionId: String? = null
)

class ChatbotViewModel(
    private val streamChatMessageUseCase: StreamChatMessageUseCase,
    private val historyRepository: ChatHistoryRepository
) : ScreenModel {

    private val _uiState = MutableStateFlow(ChatbotUiState())
    val uiState: StateFlow<ChatbotUiState> = _uiState.asStateFlow()

    val sessions: Flow<List<ChatSession>> = historyRepository.getAllSessions()

    init {
        // Load the most recent session or create a new one
        screenModelScope.launch {
            historyRepository.getAllSessions().firstOrNull()?.firstOrNull()?.let { lastSession ->
                loadSession(lastSession.id)
            } ?: startNewSession()
        }
    }

    fun startNewSession() {
        val newId = randomUUID()
        _uiState.update { it.copy(messages = emptyList(), currentSessionId = newId, isLoading = false) }
    }

    fun loadSession(sessionId: String) {
        screenModelScope.launch {
            val history = historyRepository.getMessagesBySession(sessionId)
            _uiState.update { it.copy(messages = history, currentSessionId = sessionId, isLoading = false) }
        }
    }

    fun sendMessage(text: String) {
        if (text.isBlank()) return

        val sessionId = _uiState.value.currentSessionId ?: randomUUID()
        val isFirstMessage = _uiState.value.messages.isEmpty()

        // Capture history BEFORE adding the current user message
        val history = _uiState.value.messages
            .filter { !it.isLoading && !it.isStreaming && it.content.isNotBlank() }
            .takeLast(6)

        val timestamp = currentTimeMillis()
        val userMsg = ChatMessage(id = randomUUID(), role = ChatRole.USER, content = text, timestamp = timestamp)
        
        val assistantMsgId = randomUUID()
        val loadingMsg = ChatMessage(id = assistantMsgId, role = ChatRole.ASSISTANT, content = "", isLoading = true, isStreaming = true, timestamp = timestamp + 1)

        _uiState.update { it.copy(messages = it.messages + userMsg + loadingMsg, isLoading = true, currentSessionId = sessionId) }

        screenModelScope.launch {
            // Save session if first message
            if (isFirstMessage) {
                val title = if (text.length > 50) text.take(47) + "..." else text
                historyRepository.saveSession(ChatSession(sessionId, title, timestamp, text, timestamp))
            }

            // Save user message
            historyRepository.saveMessage(sessionId, userMsg)

            try {
                var fullContent = ""
                streamChatMessageUseCase(text, history).collect { token ->
                    fullContent += token
                    _uiState.update { state ->
                        val updated = state.messages.map { msg ->
                            if (msg.id == assistantMsgId) {
                                msg.copy(content = fullContent, isLoading = false)
                            } else msg
                        }
                        state.copy(messages = updated, isLoading = false)
                    }
                }
                
                // Finalize assistant message
                val finalAssistantMsg = _uiState.value.messages.find { it.id == assistantMsgId }?.copy(
                    isStreaming = false,
                    timestamp = currentTimeMillis()
                )

                if (finalAssistantMsg != null) {
                    historyRepository.saveMessage(sessionId, finalAssistantMsg)
                    
                    _uiState.update { state ->
                        val updated = state.messages.map { if (it.id == assistantMsgId) finalAssistantMsg else it }
                        state.copy(messages = updated)
                    }
                }
            } catch (e: Exception) {
                println("CHATBOT_ERROR: ${e.message}")
                val errorMsg = ChatMessage(
                    id = assistantMsgId,
                    role = ChatRole.ASSISTANT,
                    content = "Sorry, I could not connect to the server. Please check your connection.",
                    isLoading = false,
                    isStreaming = false,
                    timestamp = currentTimeMillis()
                )
                _uiState.update { state ->
                    val updated = state.messages.map { if (it.id == assistantMsgId) errorMsg else it }
                    state.copy(messages = updated, isLoading = false)
                }
                historyRepository.saveMessage(sessionId, errorMsg)
            }
        }
    }

    fun deleteSession(sessionId: String) {
        screenModelScope.launch {
            historyRepository.deleteSession(sessionId)
            if (_uiState.value.currentSessionId == sessionId) {
                startNewSession()
            }
        }
    }
}

// Prayer UI state and VM moved to PrayerViewModel.kt
// Qibla VM moved to QiblaViewModel.kt
