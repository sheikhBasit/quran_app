package com.quranapp

import com.quranapp.domain.model.*

/**
 * Shared fake data for all test files.
 * Import in any test: import com.quranapp.TestFixtures
 */
object TestFixtures {

    // ─── Quran ────────────────────────────────────────────────────────────────

    val fakeAyah = Ayah(
        id = 1L,
        surahNumber = 1,
        ayahNumber = 1,
        pageNumber = 1,
        juzNumber = 1,
        arabicTextHafs = "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ",
        arabicTextWarsh = "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ",
        translationEnglish = "In the name of Allah, the Entirely Merciful, the Especially Merciful."
    )

    val fakeAyah2 = Ayah(
        id = 2L,
        surahNumber = 1,
        ayahNumber = 2,
        pageNumber = 1,
        juzNumber = 1,
        arabicTextHafs = "الْحَمْدُ لِلَّهِ رَبِّ الْعَالَمِينَ",
        arabicTextWarsh = "الْحَمْدُ لِلَّهِ رَبِّ الْعَالَمِينَ",
        translationEnglish = "All praise is due to Allah, Lord of the worlds."
    )

    val fakeSurah = Surah(
        number = 1,
        nameArabic = "الْفَاتِحَة",
        nameEnglish = "The Opening",
        nameTransliteration = "Al-Fatihah",
        revelationType = "Meccan",
        ayahCount = 7
    )

    val fakeTafsirEntry = TafsirEntry(
        bookName = "ibn_kathir",
        content = "This verse is the opening of the Quran and contains the essence of the entire book. It begins with the name of Allah, the Most Gracious, the Most Merciful."
    )

    val fakeTafsirEntries = mapOf(
        "ibn_kathir" to fakeTafsirEntry,
        "maarif" to TafsirEntry("maarif", "Bismillah is the opening phrase of the Quran and is recited before every Surah."),
        "ibn_abbas" to TafsirEntry("ibn_abbas", "In the name of Allah means seeking blessings and help from Allah.")
    )

    // ─── Hadith ───────────────────────────────────────────────────────────────

    val fakeHadith = Hadith(
        id = 1L,
        collection = "bukhari",
        chapterName = "Revelation",
        hadithNumber = 1,
        arabicText = "إِنَّمَا الأَعْمَالُ بِالنِّيَّاتِ",
        translation = "Actions are judged by intentions, and every person will get the reward according to what he has intended.",
        narrator = "Umar ibn al-Khattab"
    )

    val fakeHadith2 = Hadith(
        id = 2L,
        collection = "muslim",
        chapterName = "Faith",
        hadithNumber = 1,
        arabicText = "",
        translation = "Islam is built upon five pillars.",
        narrator = "Ibn Umar"
    )

    // ─── Chatbot ──────────────────────────────────────────────────────────────

    val fakeChatResponse = ChatResponse(
        answer = "Zakat is the third pillar of Islam (Surah 9:60). It is an obligatory annual payment. For personal guidance, please consult a qualified Islamic scholar.",
        sources = ChatSources(
            ayahs = listOf(AyahReference(surah = 9, ayah = 60)),
            hadiths = listOf(HadithReference(collection = "bukhari", number = 1395)),
            tafsir = listOf(TafsirReference(surah = 9, ayah = 60, book = "ibn_kathir"))
        )
    )

    // ─── Prayer ───────────────────────────────────────────────────────────────

    val fakePrayerTimes = PrayerTimesResult(
        fajr     = 1718420400000L,
        sunrise  = 1718424000000L,
        dhuhr    = 1718445600000L,
        asr      = 1718456400000L,
        maghrib  = 1718467200000L,
        isha     = 1718473200000L,
        tahajjud = 1718410000000L,
        ishraq   = 1718425800000L,
        chasht   = 1718434200000L,
    )
}
