package com.quranapp.domain.model

// ─── Quran ────────────────────────────────────────────────────────────────────

enum class QuranScript { HAFS, WARSH }

enum class ReadingMode { PAGE, SCROLL }

data class Surah(
    val number: Int,
    val nameArabic: String,
    val nameEnglish: String,
    val nameTransliteration: String,
    val revelationType: String,
    val ayahCount: Int,
)

data class Ayah(
    val id: Long,
    val surahNumber: Int,
    val ayahNumber: Int,
    val pageNumber: Int,
    val juzNumber: Int,
    val arabicTextHafs: String,
    val arabicTextWarsh: String,
    val translationEnglish: String,
) {
    fun arabicText(script: QuranScript): String = when (script) {
        QuranScript.HAFS -> arabicTextHafs
        QuranScript.WARSH -> arabicTextWarsh
    }
}

data class TafsirEntry(
    val bookName: String,
    val content: String,
)

// ─── Hadith ───────────────────────────────────────────────────────────────────

data class Hadith(
    val id: Long,
    val collection: String,
    val chapterName: String,
    val hadithNumber: Int,
    val arabicText: String,
    val translation: String,
    val narrator: String,
)

data class HadithCollection(
    val collection: String,
    val displayName: String,
    val bookCount: Int,
    val hadithCount: Int,
)

// ─── User Data ────────────────────────────────────────────────────────────────

data class Bookmark(
    val id: Long,
    val type: String,        // "ayah" | "hadith"
    val referenceId: Long,
    val createdAt: String,
)

data class Highlight(
    val ayahId: Long,
    val color: String,       // hex e.g. "#FFD700"
)

data class Note(
    val id: Long,
    val type: String,
    val referenceId: Long,
    val content: String,
    val updatedAt: String,
)

// ─── Prayer ───────────────────────────────────────────────────────────────────

data class PrayerTimesResult(
    val fajr: Long,
    val sunrise: Long,
    val dhuhr: Long,
    val asr: Long,
    val maghrib: Long,
    val isha: Long,
    val tahajjud: Long,
    val ishraq: Long,
    val chasht: Long,
)

data class NextPrayer(
    val name: String,
    val displayName: String,
    val timeEpochMillis: Long,
    val minutesUntil: Long,
)

// ─── Search ───────────────────────────────────────────────────────────────────

sealed class SearchResult {
    data class AyahResult(val ayah: Ayah, val query: String) : SearchResult()
    data class HadithResult(val hadith: Hadith, val query: String) : SearchResult()
}

// ─── Chatbot ──────────────────────────────────────────────────────────────────

enum class ChatRole { USER, ASSISTANT }

data class AyahReference(val surah: Int, val ayah: Int)
data class HadithReference(val collection: String, val number: Int)
data class TafsirReference(val surah: Int, val ayah: Int, val book: String)

data class ChatSources(
    val ayahs: List<AyahReference> = emptyList(),
    val hadiths: List<HadithReference> = emptyList(),
    val tafsir: List<TafsirReference> = emptyList(),
)

data class ChatResponse(
    val answer: String,
    val sources: ChatSources,
)

data class ChatMessage(
    val id: String,
    val role: ChatRole,
    val content: String,
    val sources: ChatSources? = null,
    val isLoading: Boolean = false,
    val isStreaming: Boolean = false,
    val timestamp: Long = 0L
)

data class ChatSession(
    val id: String,
    val title: String,
    val createdAt: Long,
    val lastMessage: String,
    val lastUpdated: Long
)
