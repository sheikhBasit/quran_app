package com.quranapp.viewmodel

import com.quranapp.TestFixtures
import com.quranapp.domain.model.*
import com.quranapp.domain.usecase.quran.*
import com.quranapp.domain.usecase.chatbot.StreamChatMessageUseCase
import com.quranapp.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.flow
import com.quranapp.domain.repository.ChatHistoryRepository
import io.mockk.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.*
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class QuranViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val getSurahList: GetSurahListUseCase = mockk()
    private val getAyahsBySurah: GetAyahsBySurahUseCase = mockk()
    private val getAyahsForPage: GetAyahsForPageUseCase = mockk()
    private val getTafsir: GetTafsirUseCase = mockk()
    private val settingsRepository: SettingsRepository = mockk()
    private lateinit var vm: QuranViewModel

    @BeforeTest fun setup() {
        Dispatchers.setMain(dispatcher)
        coEvery { getSurahList() } returns Result.success(emptyList())
        every { settingsRepository.showTranslation } returns flowOf(true)
        every { settingsRepository.arabicFontSize } returns flowOf(28f)
        vm = QuranViewModel(getSurahList, getAyahsBySurah, getAyahsForPage, getTafsir, settingsRepository)
    }

    @AfterTest fun teardown() { Dispatchers.resetMain() }

    @Test fun `initial state is empty and not loading`() {
        with(vm.uiState.value) {
            assertTrue(ayahs.isEmpty())
            assertFalse(isLoading)
            assertNull(error)
            assertTrue(showTranslation)
            assertEquals(QuranScript.HAFS, script)
            assertEquals(ReadingMode.SCROLL, readingMode)
        }
    }

    @Test fun `loadSurah populates ayahs on success`() = runTest {
        coEvery { getAyahsBySurah(1) } returns
            Result.success(listOf(TestFixtures.fakeAyah, TestFixtures.fakeAyah2))
        vm.loadSurah(1)
        advanceUntilIdle()
        with(vm.uiState.value) {
            assertEquals(2, ayahs.size)
            assertFalse(isLoading)
            assertNull(error)
        }
    }

    @Test fun `loadSurah sets error on failure`() = runTest {
        coEvery { getAyahsBySurah(any()) } returns Result.failure(RuntimeException("DB error"))
        vm.loadSurah(1)
        advanceUntilIdle()
        with(vm.uiState.value) {
            assertNotNull(error)
            assertTrue(ayahs.isEmpty())
            assertFalse(isLoading)
        }
    }

    @Test fun `toggleTranslation flips showTranslation`() {
        assertTrue(vm.uiState.value.showTranslation)
        vm.toggleTranslation()
        assertFalse(vm.uiState.value.showTranslation)
        vm.toggleTranslation()
        assertTrue(vm.uiState.value.showTranslation)
    }

    @Test fun `toggleScript switches between Hafs and Warsh`() {
        assertEquals(QuranScript.HAFS, vm.uiState.value.script)
        vm.toggleScript()
        assertEquals(QuranScript.WARSH, vm.uiState.value.script)
        vm.toggleScript()
        assertEquals(QuranScript.HAFS, vm.uiState.value.script)
    }

    @Test fun `toggleReadingMode switches between SCROLL and PAGE`() {
        assertEquals(ReadingMode.SCROLL, vm.uiState.value.readingMode)
        vm.toggleReadingMode()
        assertEquals(ReadingMode.PAGE, vm.uiState.value.readingMode)
    }

    @Test fun `loadTafsir populates tafsiers on success`() = runTest {
        val fakeTafsir = TafsirEntry("book", "content")
        coEvery { getTafsir(any(), any()) } returns Result.success(listOf(fakeTafsir))
        
        vm.loadTafsir(TestFixtures.fakeAyah)
        advanceUntilIdle()
        
        assertEquals(1, vm.uiState.value.tafsiers.size)
        assertEquals("content", vm.uiState.value.tafsiers.first().content)
    }

    @Test fun `loadPage populates ayahs on success`() = runTest {
        coEvery { getAyahsForPage(1) } returns Result.success(listOf(TestFixtures.fakeAyah))
        
        vm.loadPage(1)
        advanceUntilIdle()
        
        assertEquals(1, vm.uiState.value.ayahs.size)
        assertEquals(1, vm.uiState.value.currentPage)
    }

    @Test fun `settings repository updates reflect in uiState`() = runTest {
        val showFlow = MutableStateFlow(true)
        val sizeFlow = MutableStateFlow(28f)
        every { settingsRepository.showTranslation } returns showFlow
        every { settingsRepository.arabicFontSize } returns sizeFlow
        
        // Re-init VM to use these flows
        val testVm = QuranViewModel(getSurahList, getAyahsBySurah, getAyahsForPage, getTafsir, settingsRepository)
        
        assertTrue(testVm.uiState.value.showTranslation)
        assertEquals(28f, testVm.uiState.value.arabicFontSize)
        
        showFlow.value = false
        sizeFlow.value = 35f
        
        advanceUntilIdle()
        
        assertFalse(testVm.uiState.value.showTranslation)
        assertEquals(35f, testVm.uiState.value.arabicFontSize)
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class ChatbotViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val streamChatMessage: StreamChatMessageUseCase = mockk()
    private val historyRepository: ChatHistoryRepository = mockk()
    private lateinit var vm: ChatbotViewModel

    @BeforeTest fun setup() {
        Dispatchers.setMain(dispatcher)
        coEvery { historyRepository.getAllSessions() } returns flow { emit(emptyList()) }
        vm = ChatbotViewModel(streamChatMessage, historyRepository)
    }

    @AfterTest fun teardown() { Dispatchers.resetMain() }

    @Test fun `initial state has empty messages and not loading`() {
        assertTrue(vm.uiState.value.messages.isEmpty())
        assertFalse(vm.uiState.value.isLoading)
    }

    @Test fun `sendMessage adds user message immediately`() = runTest {
        coEvery { historyRepository.saveSession(any<ChatSession>()) } returns Unit
        coEvery { historyRepository.saveMessage(any<String>(), any<ChatMessage>()) } returns Unit
        coEvery { streamChatMessage(any<String>(), any<List<ChatMessage>>()) } returns flow {
            emit("Test")
        }
        vm.sendMessage("What is patience?")
        advanceTimeBy(10)
        assertTrue(vm.uiState.value.messages
            .any { it.role == ChatRole.USER && it.content == "What is patience?" })
    }

    @Test fun `sendMessage adds assistant response from flow tokens`() = runTest {
        coEvery { historyRepository.saveSession(any<ChatSession>()) } returns Unit
        coEvery { historyRepository.saveMessage(any<String>(), any<ChatMessage>()) } returns Unit
        coEvery { streamChatMessage(any<String>(), any<List<ChatMessage>>()) } returns flow {
            emit("Patience ")
            emit("is ")
            emit("key.")
        }
        vm.sendMessage("What is patience?")
        advanceUntilIdle()
        val assistantMsg = vm.uiState.value.messages.find { it.role == ChatRole.ASSISTANT }
        assertNotNull(assistantMsg)
        assertEquals("Patience is key.", assistantMsg.content)
        assertFalse(vm.uiState.value.isLoading)
        assertFalse(assistantMsg.isStreaming)
    }

    @Test fun `sendMessage shows error connection message on failure`() = runTest {
        coEvery { historyRepository.saveSession(any<ChatSession>()) } returns Unit
        coEvery { historyRepository.saveMessage(any<String>(), any<ChatMessage>()) } returns Unit
        coEvery { streamChatMessage(any<String>(), any<List<ChatMessage>>()) } returns flow {
            throw RuntimeException("No internet")
        }
        vm.sendMessage("test")
        advanceUntilIdle()
        val lastMsg = vm.uiState.value.messages.last()
        assertEquals(ChatRole.ASSISTANT, lastMsg.role)
        assertTrue(lastMsg.content.contains("connect", ignoreCase = true))
        assertFalse(lastMsg.isStreaming)
    }

    @Test fun `empty message is not sent`() = runTest {
        vm.sendMessage("   ")
        advanceUntilIdle()
        assertTrue(vm.uiState.value.messages.isEmpty())
    }
}
