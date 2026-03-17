package com.quranapp.data.local

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.quranapp.db.QuranDatabase
import kotlin.test.*

/**
 * Tests for all SQLDelight queries using an in-memory SQLite driver.
 * No emulator or Android device needed — runs in commonTest on JVM.
 */
class SQLDelightTest {

    private lateinit var db: QuranDatabase

    @BeforeTest
    fun setup() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        QuranDatabase.Schema.create(driver)
        db = QuranDatabase(driver)
        seedTestData()
    }

    private fun seedTestData() {
        // Surah
        db.surahsQueries.insert(1L, "الْفَاتِحَة", "The Opening", "Al-Fatihah", "Meccan", 7L)
        db.surahsQueries.insert(2L, "الْبَقَرَة", "The Cow", "Al-Baqarah", "Medinan", 286L)

        // Ayahs — Surah 1
        db.ayahsQueries.insert(1L, 1L, 1L, 1L, 1L,
            "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ",
            "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ",
            "In the name of Allah, the Entirely Merciful.")
        db.ayahsQueries.insert(2L, 1L, 2L, 1L, 1L,
            "الْحَمْدُ لِلَّهِ رَبِّ الْعَالَمِينَ",
            "الْحَمْدُ لِلَّهِ رَبِّ الْعَالَمِينَ",
            "All praise is due to Allah, Lord of the worlds.")

        // Ayahs — Surah 2 (page 2 for page test)
        db.ayahsQueries.insert(3L, 2L, 1L, 2L, 1L,
            "الم", "الم", "Alif Lam Meem.")

        // Tafsir
        db.tafsirQueries.insert(1L, 1L, "ibn_kathir",
            "This verse is the opening of the Quran.")
        db.tafsirQueries.insert(1L, 1L, "maarif",
            "Bismillah is recited before every Surah.")
        db.tafsirQueries.insert(1L, 1L, "ibn_abbas",
            "In the name of Allah means seeking His blessings.")

        // Hadith
        db.hadithQueries.insert("bukhari", "Revelation", 1L, "",
            "Actions are judged by intentions.", "Umar ibn al-Khattab")
        db.hadithQueries.insert("bukhari", "Revelation", 2L, "",
            "Islam is built on five pillars.", "Ibn Umar")
        db.hadithQueries.insert("muslim", "Faith", 1L, "",
            "Whoever believes in Allah and the Last Day.", "Abu Hurairah")
    }

    // ── Surahs ─────────────────────────────────────────────────────────────

    @Test fun `selectAll returns all surahs`() {
        val surahs = db.surahsQueries.selectAll().executeAsList()
        assertEquals(2, surahs.size)
    }

    @Test fun `selectAll returns surahs in order`() {
        val surahs = db.surahsQueries.selectAll().executeAsList()
        assertEquals(1L, surahs[0].number)
        assertEquals(2L, surahs[1].number)
    }

    @Test fun `selectByNumber returns correct surah`() {
        val surah = db.surahsQueries.selectByNumber(1L).executeAsOneOrNull()
        assertNotNull(surah)
        assertEquals("The Opening", surah.name_english)
    }

    // ── Ayahs ──────────────────────────────────────────────────────────────

    @Test fun `selectBySurah returns correct ayahs`() {
        val ayahs = db.ayahsQueries.selectBySurah(1L).executeAsList()
        assertEquals(2, ayahs.size)
    }

    @Test fun `selectBySurah returns ayahs in order`() {
        val ayahs = db.ayahsQueries.selectBySurah(1L).executeAsList()
        assertTrue(ayahs[0].ayah_number < ayahs[1].ayah_number)
    }

    @Test fun `selectByPage returns ayahs on correct page`() {
        val page1 = db.ayahsQueries.selectByPage(1L).executeAsList()
        val page2 = db.ayahsQueries.selectByPage(2L).executeAsList()
        assertEquals(2, page1.size)
        assertEquals(1, page2.size)
    }

    @Test fun `selectByReference returns exact ayah`() {
        val ayah = db.ayahsQueries.selectByReference(1L, 2L).executeAsOneOrNull()
        assertNotNull(ayah)
        assertEquals("All praise is due to Allah, Lord of the worlds.", ayah.translation_english)
    }

    @Test fun `search returns matching ayahs`() {
        val results = db.ayahsQueries.search("%Allah%").executeAsList()
        assertTrue(results.isNotEmpty())
    }

    @Test fun `search returns empty for no match`() {
        val results = db.ayahsQueries.search("%xyznonexistent%").executeAsList()
        assertTrue(results.isEmpty())
    }

    @Test fun `ayah has both Hafs and Warsh text`() {
        val ayah = db.ayahsQueries.selectByReference(1L, 1L).executeAsOneOrNull()
        assertNotNull(ayah)
        assertTrue(ayah.arabic_text_hafs.isNotBlank())
        assertTrue(ayah.arabic_text_warsh.isNotBlank())
    }

    // ── Tafsir ─────────────────────────────────────────────────────────────

    @Test fun `selectByAyah returns all 3 books`() {
        val entries = db.tafsirQueries.selectByAyah(1L, 1L).executeAsList()
        assertEquals(3, entries.size)
    }

    @Test fun `selectByAyahAndBook returns correct book`() {
        val entry = db.tafsirQueries.selectByAyahAndBook(1L, 1L, "ibn_kathir")
            .executeAsOneOrNull()
        assertNotNull(entry)
        assertTrue(entry.content.contains("opening"))
    }

    @Test fun `selectByAyah returns empty for non-existent ayah`() {
        val entries = db.tafsirQueries.selectByAyah(999L, 999L).executeAsList()
        assertTrue(entries.isEmpty())
    }

    // ── Hadith ─────────────────────────────────────────────────────────────

    @Test fun `selectCollections returns correct counts`() {
        val collections = db.hadithQueries.selectCollections().executeAsList()
        assertEquals(2, collections.size)
        val bukhari = collections.first { it.collection == "bukhari" }
        assertEquals(1L, bukhari.chapter_count)
        assertEquals(2L, bukhari.hadith_count)
    }
    
    @Test fun `selectChaptersByCollection returns chapters for collection`() {
        val chapters = db.hadithQueries.selectChaptersByCollection("bukhari").executeAsList()
        assertEquals(1, chapters.size)
        assertEquals("Revelation", chapters[0].chapter_name)
    }

    @Test fun `selectByChapter returns hadiths for collection`() {
        val hadiths = db.hadithQueries.selectByChapter("bukhari", "Revelation").executeAsList()
        assertEquals(2, hadiths.size)
    }

    @Test fun `selectByChapter returns empty for unknown collection`() {
        val hadiths = db.hadithQueries.selectByChapter("unknown", "Revelation").executeAsList()
        assertTrue(hadiths.isEmpty())
    }

    @Test fun `selectHadithByReference returns exact hadith`() {
        val hadith = db.hadithQueries.selectByReference("bukhari", "Revelation", 1L).executeAsOneOrNull()
        assertNotNull(hadith)
        assertTrue(hadith.translation.contains("intentions"))
    }

    @Test fun `search hadith returns matching results`() {
        val results = db.hadithQueries.search("%intentions%").executeAsList()
        assertTrue(results.isNotEmpty())
        assertEquals("bukhari", results.first().collection)
    }

    // ── User Data — Bookmarks ───────────────────────────────────────────────

    @Test fun `insertBookmark and isBookmarked returns 1`() {
        db.userDataQueries.insertBookmark("ayah", 1L)
        val count = db.userDataQueries.isBookmarked("ayah", 1L).executeAsOne()
        assertEquals(1L, count)
    }

    @Test fun `deleteBookmark removes entry`() {
        db.userDataQueries.insertBookmark("ayah", 2L)
        db.userDataQueries.deleteBookmark("ayah", 2L)
        val count = db.userDataQueries.isBookmarked("ayah", 2L).executeAsOne()
        assertEquals(0L, count)
    }

    @Test fun `insertBookmark is idempotent`() {
        db.userDataQueries.insertBookmark("ayah", 3L)
        db.userDataQueries.insertBookmark("ayah", 3L) // duplicate
        val all = db.userDataQueries.selectAllBookmarks("ayah").executeAsList()
        assertEquals(1, all.filter { it.reference_id == 3L }.size)
    }

    @Test fun `selectAllBookmarks returns by type`() {
        db.userDataQueries.insertBookmark("ayah",   10L)
        db.userDataQueries.insertBookmark("hadith", 20L)
        val ayahBookmarks   = db.userDataQueries.selectAllBookmarks("ayah").executeAsList()
        val hadithBookmarks = db.userDataQueries.selectAllBookmarks("hadith").executeAsList()
        assertTrue(ayahBookmarks.any { it.reference_id == 10L })
        assertTrue(hadithBookmarks.any { it.reference_id == 20L })
        assertFalse(ayahBookmarks.any { it.reference_id == 20L })
    }

    // ── User Data — Highlights ──────────────────────────────────────────────

    @Test fun `upsertHighlight saves color`() {
        db.userDataQueries.upsertHighlight(100L, "#FFD700")
        val color = db.userDataQueries.selectHighlightColor(100L).executeAsOneOrNull()
        assertEquals("#FFD700", color)
    }

    @Test fun `upsertHighlight updates existing color`() {
        db.userDataQueries.upsertHighlight(101L, "#FFD700")
        db.userDataQueries.upsertHighlight(101L, "#90EE90")
        val color = db.userDataQueries.selectHighlightColor(101L).executeAsOneOrNull()
        assertEquals("#90EE90", color)
    }

    @Test fun `deleteHighlight removes entry`() {
        db.userDataQueries.upsertHighlight(102L, "#FFD700")
        db.userDataQueries.deleteHighlight(102L)
        val color = db.userDataQueries.selectHighlightColor(102L).executeAsOneOrNull()
        assertNull(color)
    }

    @Test fun `selectAllHighlights returns all highlights`() {
        db.userDataQueries.upsertHighlight(103L, "#FFD700")
        db.userDataQueries.upsertHighlight(104L, "#90EE90")
        val all = db.userDataQueries.selectAllHighlights().executeAsList()
        assertEquals(2, all.size)
    }

    // ── User Data — Notes ───────────────────────────────────────────────────

    @Test fun `upsertNote saves content`() {
        db.userDataQueries.upsertNote("ayah", 200L, "My reflection on this verse")
        val note = db.userDataQueries.selectNote("ayah", 200L).executeAsOneOrNull()
        assertNotNull(note)
        assertEquals("My reflection on this verse", note.content)
    }

    @Test fun `upsertNote updates existing content`() {
        db.userDataQueries.upsertNote("ayah", 201L, "First draft")
        db.userDataQueries.upsertNote("ayah", 201L, "Updated note")
        val note = db.userDataQueries.selectNote("ayah", 201L).executeAsOneOrNull()
        assertEquals("Updated note", note?.content)
    }

    @Test fun `selectNote returns null when not found`() {
        val note = db.userDataQueries.selectNote("ayah", 9999L).executeAsOneOrNull()
        assertNull(note)
    }

    @Test fun `deleteNote removes entry`() {
        db.userDataQueries.upsertNote("hadith", 50L, "Some note")
        db.userDataQueries.deleteNote("hadith", 50L)
        val note = db.userDataQueries.selectNote("hadith", 50L).executeAsOneOrNull()
        assertNull(note)
    }

    // ── Settings — Reading Position ─────────────────────────────────────────

    @Test fun `getPosition returns default values`() {
        val pos = db.settingsQueries.getPosition().executeAsOneOrNull()
        assertNotNull(pos)
        assertEquals(1L, pos.surah_number)
        assertEquals(1L, pos.ayah_number)
        assertEquals("scroll", pos.mode)
    }

    @Test fun `upsertPosition saves and retrieves`() {
        db.settingsQueries.upsertPosition(2L, 255L, 29L, "page")
        val pos = db.settingsQueries.getPosition().executeAsOneOrNull()
        assertNotNull(pos)
        assertEquals(2L, pos.surah_number)
        assertEquals(255L, pos.ayah_number)
        assertEquals(29L, pos.page_number)
        assertEquals("page", pos.mode)
    }

    @Test fun `upsertPosition overwrites previous position`() {
        db.settingsQueries.upsertPosition(1L, 1L, 1L, "scroll")
        db.settingsQueries.upsertPosition(18L, 1L, 293L, "scroll")
        val pos = db.settingsQueries.getPosition().executeAsOneOrNull()
        assertEquals(18L, pos?.surah_number)
    }

    // ── Settings — App Settings ──────────────────────────────────────────────

    @Test fun `getSetting returns default quran_script`() {
        val script = db.settingsQueries.getSetting("quran_script").executeAsOneOrNull()
        assertEquals("hafs", script)
    }

    @Test fun `upsertSetting updates value`() {
        db.settingsQueries.upsertSetting("quran_script", "warsh")
        val script = db.settingsQueries.getSetting("quran_script").executeAsOneOrNull()
        assertEquals("warsh", script)
    }

    @Test fun `getSetting returns null for unknown key`() {
        val value = db.settingsQueries.getSetting("nonexistent_key").executeAsOneOrNull()
        assertNull(value)
    }
}
