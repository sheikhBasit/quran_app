package com.quranapp.viewmodel

import com.quranapp.TestFixtures
import com.quranapp.domain.model.*
import com.quranapp.domain.usecase.quran.*
import com.quranapp.domain.usecase.chatbot.SendChatMessageUseCase
import io.mockk.coEvery
import io.mockk.mockk
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
    private lateinit var vm: QuranViewModel

    @BeforeTest fun setup() {
        Dispatchers.setMain(dispatcher)
        coEvery { getSurahList() } returns Result.success(emptyList())
        vm = QuranViewModel(getSurahList, getAyahsBySurah, getAyahsForPage, getTafsir)
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
}

@OptIn(ExperimentalCoroutinesApi::class)
class ChatbotViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val sendMessage: SendChatMessageUseCase = mockk()
    private lateinit var vm: ChatbotViewModel

    @BeforeTest fun setup() {
        Dispatchers.setMain(dispatcher)
        vm = ChatbotViewModel(sendMessage)
    }

    @AfterTest fun teardown() { Dispatchers.resetMain() }

    @Test fun `initial state has empty messages and not loading`() {
        assertTrue(vm.uiState.value.messages.isEmpty())
        assertFalse(vm.uiState.value.isLoading)
    }

    @Test fun `sendMessage adds user message immediately`() = runTest {
        coEvery { sendMessage(any()) } coAnswers {
            delay(500)
            Result.success(TestFixtures.fakeChatResponse)
        }
        vm.sendMessage("What is Zakat?")
        advanceTimeBy(10)
        assertTrue(vm.uiState.value.messages
            .any { it.role == ChatRole.USER && it.content == "What is Zakat?" })
    }

    @Test fun `sendMessage adds assistant response on success`() = runTest {
        coEvery { sendMessage(any()) } returns Result.success(TestFixtures.fakeChatResponse)
        vm.sendMessage("What is Zakat?")
        advanceUntilIdle()
        assertTrue(vm.uiState.value.messages.any { it.role == ChatRole.ASSISTANT })
        assertFalse(vm.uiState.value.isLoading)
    }

    @Test fun `sendMessage shows error on failure`() = runTest {
        coEvery { sendMessage(any()) } returns Result.failure(RuntimeException("No internet"))
        vm.sendMessage("test")
        advanceUntilIdle()
        val lastMsg = vm.uiState.value.messages.last()
        assertEquals(ChatRole.ASSISTANT, lastMsg.role)
        assertTrue(lastMsg.content.contains("error", ignoreCase = true) ||
                   lastMsg.content.contains("unavailable", ignoreCase = true))
    }

    @Test fun `empty message is not sent`() = runTest {
        vm.sendMessage("   ")
        advanceUntilIdle()
        assertTrue(vm.uiState.value.messages.isEmpty())
    }
}
