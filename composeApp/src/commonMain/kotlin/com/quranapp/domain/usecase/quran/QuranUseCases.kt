package com.quranapp.domain.usecase.quran

import com.quranapp.domain.model.*
import com.quranapp.domain.repository.QuranRepository

class GetSurahListUseCase(private val repo: QuranRepository) {
    suspend operator fun invoke(): Result<List<Surah>> = runCatching { repo.getAllSurahs() }
}

class GetAyahsBySurahUseCase(private val repo: QuranRepository) {
    suspend operator fun invoke(surahNumber: Int): Result<List<Ayah>> {
        if (surahNumber !in 1..114)
            return Result.failure(IllegalArgumentException("Surah must be 1–114, got $surahNumber"))
        return runCatching { repo.getAyahsBySurah(surahNumber) }
    }
}

class GetAyahsForPageUseCase(private val repo: QuranRepository) {
    suspend operator fun invoke(pageNumber: Int): Result<List<Ayah>> {
        if (pageNumber !in 1..604)
            return Result.failure(IllegalArgumentException("Page must be 1–604, got $pageNumber"))
        return runCatching { repo.getAyahsByPage(pageNumber) }
    }
}

class GetTafsirUseCase(private val repo: QuranRepository) {
    suspend operator fun invoke(surahNumber: Int, ayahNumber: Int): Result<List<TafsirEntry>> {
        if (surahNumber !in 1..114)
            return Result.failure(IllegalArgumentException("Invalid surah: $surahNumber"))
        return runCatching { repo.getTafsir(surahNumber, ayahNumber) }
    }
}
