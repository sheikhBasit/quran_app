package com.quranapp.domain.usecase

import com.quranapp.TestFixtures
import com.quranapp.domain.model.*
import com.quranapp.domain.repository.*
import com.quranapp.domain.usecase.quran.*
import com.quranapp.domain.usecase.hadith.*
import com.quranapp.domain.usecase.search.SearchUseCase
import com.quranapp.domain.usecase.userdata.*
import com.quranapp.domain.usecase.qibla.GetQiblaDirectionUseCase
import com.quranapp.domain.usecase.chatbot.SendChatMessageUseCase
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlin.test.*

// ─── Quran ────────────────────────────────────────────────────────────────────

class GetAyahsBySurahUseCaseTest {
    private val repo: QuranRepository = mockk()
    private val useCase = GetAyahsBySurahUseCase(repo)

    @Test fun `returns success for valid surah`() = runTest {
        coEvery { repo.getAyahsBySurah(1) } returns listOf(TestFixtures.fakeAyah)
        assertTrue(useCase(1).isSuccess)
    }

    @Test fun `returns failure for surah 0`() = runTest {
        val result = useCase(0)
        assertTrue(result.isFailure)
        assertIs<IllegalArgumentException>(result.exceptionOrNull())
    }

    @Test fun `returns failure for surah 115`() = runTest {
        assertTrue(useCase(115).isFailure)
    }

    @Test fun `propagates repository exception`() = runTest {
        coEvery { repo.getAyahsBySurah(any()) } throws RuntimeException("DB error")
        assertTrue(useCase(1).isFailure)
    }

    @Test fun `returns empty list when surah is empty`() = runTest {
        coEvery { repo.getAyahsBySurah(50) } returns emptyList()
        val result = useCase(50)
        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull()!!.isEmpty())
    }
}

class GetAyahsForPageUseCaseTest {
    private val repo: QuranRepository = mockk()
    private val useCase = GetAyahsForPageUseCase(repo)

    @Test fun `valid page returns ayahs`() = runTest {
        coEvery { repo.getAyahsByPage(1) } returns listOf(TestFixtures.fakeAyah)
        assertTrue(useCase(1).isSuccess)
    }

    @Test fun `page 0 returns failure`() = runTest {
        assertTrue(useCase(0).isFailure)
    }

    @Test fun `page 605 returns failure`() = runTest {
        assertTrue(useCase(605).isFailure)
    }

    @Test fun `page 604 is valid`() = runTest {
        coEvery { repo.getAyahsByPage(604) } returns emptyList()
        assertTrue(useCase(604).isSuccess)
    }
}

// ─── Search ───────────────────────────────────────────────────────────────────

class SearchUseCaseTest {
    private val repo: SearchRepository = mockk()
    private val useCase = SearchUseCase(repo)

    @Test fun `empty query returns failure`() = runTest {
        assertTrue(useCase("").isFailure)
    }

    @Test fun `query under 3 chars returns failure`() = runTest {
        assertTrue(useCase("al").isFailure)
        assertIs<IllegalArgumentException>(useCase("al").exceptionOrNull())
    }

    @Test fun `valid query returns combined results`() = runTest {
        coEvery { repo.searchAyahs("prayer") } returns
            listOf(SearchResult.AyahResult(TestFixtures.fakeAyah, "prayer"))
        coEvery { repo.searchHadith("prayer") } returns
            listOf(SearchResult.HadithResult(TestFixtures.fakeHadith, "prayer"))
        val result = useCase("prayer")
        assertTrue(result.isSuccess)
        assertEquals(2, result.getOrNull()?.size)
    }

    @Test fun `3 char query is valid`() = runTest {
        coEvery { repo.searchAyahs("god") } returns emptyList()
        coEvery { repo.searchHadith("god") } returns emptyList()
        assertTrue(useCase("god").isSuccess)
    }
}

// ─── Qibla ────────────────────────────────────────────────────────────────────

class GetQiblaDirectionUseCaseTest {
    private val useCase = GetQiblaDirectionUseCase()

    @Test fun `bearing is between 0 and 360`() {
        val b = useCase(31.52, 74.36)
        assertTrue(b in 0.0..360.0)
    }

    @Test fun `London to Mecca is approximately 119 degrees`() {
        val b = useCase(51.5, -0.12)
        assertEquals(119.0, b, absoluteTolerance = 1.5)
    }

    @Test fun `New York to Mecca is approximately 59 degrees`() {
        val b = useCase(40.71, -74.01)
        assertEquals(59.0, b, absoluteTolerance = 1.5)
    }

    @Test fun `Jakarta to Mecca is approximately 295 degrees`() {
        val b = useCase(-6.21, 106.85)
        assertEquals(295.0, b, absoluteTolerance = 2.0)
    }

    @Test fun `same input always returns same result`() {
        assertEquals(useCase(31.52, 74.36), useCase(31.52, 74.36))
    }
}

// ─── Chatbot ──────────────────────────────────────────────────────────────────

class SendChatMessageUseCaseTest {
    private val repo: ChatbotRepository = mockk()
    private val useCase = SendChatMessageUseCase(repo)

    @Test fun `valid message returns success`() = runTest {
        coEvery { repo.sendMessage("What is Zakat?") } returns TestFixtures.fakeChatResponse
        assertTrue(useCase("What is Zakat?").isSuccess)
    }

    @Test fun `empty message returns failure`() = runTest {
        assertTrue(useCase("").isFailure)
        assertIs<IllegalArgumentException>(useCase("").exceptionOrNull())
    }

    @Test fun `whitespace message returns failure`() = runTest {
        assertTrue(useCase("   ").isFailure)
    }

    @Test fun `message over 1000 chars returns failure`() = runTest {
        assertTrue(useCase("x".repeat(1001)).isFailure)
    }

    @Test fun `1000 char message is valid`() = runTest {
        coEvery { repo.sendMessage(any()) } returns TestFixtures.fakeChatResponse
        assertTrue(useCase("x".repeat(1000)).isSuccess)
    }
}

// ─── User Data ────────────────────────────────────────────────────────────────

class SaveNoteUseCaseTest {
    private val repo: UserDataRepository = mockk()
    private val useCase = SaveNoteUseCase(repo)

    @Test fun `valid note saves successfully`() = runTest {
        coEvery { repo.saveNote("ayah", 1L, "My note") } returns Unit
        assertTrue(useCase("ayah", 1L, "My note").isSuccess)
    }

    @Test fun `blank note returns failure`() = runTest {
        assertTrue(useCase("ayah", 1L, "  ").isFailure)
        assertIs<IllegalArgumentException>(useCase("ayah", 1L, "  ").exceptionOrNull())
    }
}
