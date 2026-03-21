package com.quranapp.data.repository

import com.quranapp.db.Bookmarks
import com.quranapp.db.Notes
import com.quranapp.db.QuranDatabase
import com.quranapp.domain.repository.UserDataRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class UserDataRepositoryTest {
    private val db: QuranDatabase = mockk()
    private val userDataQueries = mockk<com.quranapp.db.UserDataQueries>()
    private lateinit var repository: UserDataRepository

    @BeforeTest
    fun setup() {
        every { db.userDataQueries } returns userDataQueries
        repository = UserDataRepositoryImpl(db)
    }

    @Test
    fun `toggleBookmark inserts if not present`() = runTest {
        every { userDataQueries.isBookmarked("ayah", 1L).executeAsOne() } returns 0L
        every { userDataQueries.insertBookmark("ayah", 1L) } returns Unit

        val result = repository.toggleBookmark("ayah", 1L)

        assertTrue(result)
        verify { userDataQueries.insertBookmark("ayah", 1L) }
    }

    @Test
    fun `toggleBookmark deletes if already present`() = runTest {
        every { userDataQueries.isBookmarked("ayah", 1L).executeAsOne() } returns 1L
        every { userDataQueries.deleteBookmark("ayah", 1L) } returns Unit

        val result = repository.toggleBookmark("ayah", 1L)

        assertFalse(result)
        verify { userDataQueries.deleteBookmark("ayah", 1L) }
    }

    @Test
    fun `isBookmarked returns true if count is 1`() = runTest {
        every { userDataQueries.isBookmarked("ayah", 1L).executeAsOne() } returns 1L
        assertTrue(repository.isBookmarked("ayah", 1L))
    }

    @Test
    fun `getAllBookmarks returns mapped list`() = runTest {
        val dbBookmarks = listOf(
            Bookmarks(1L, "ayah", 1L, "2024-01-01 12:00:00")
        )
        every { userDataQueries.selectAllBookmarks("ayah").executeAsList() } returns dbBookmarks

        val result = repository.getAllBookmarks("ayah")

        assertEquals(1, result.size)
        assertEquals(1L, result[0].referenceId)
    }

    @Test
    fun `getHighlightColor returns color if present`() = runTest {
        every { userDataQueries.selectHighlightColor(1L).executeAsOneOrNull() } returns "#FFD700"
        assertEquals("#FFD700", repository.getHighlightColor(1L))
    }

    @Test
    fun `getNote returns mapped note`() = runTest {
        val dbNote = Notes(1L, "ayah", 1L, "My Note", "2024-01-01 12:00:00")
        every { userDataQueries.selectNote("ayah", 1L).executeAsOneOrNull() } returns dbNote

        val result = repository.getNote("ayah", 1L)

        assertEquals("My Note", result?.content)
    }
}
