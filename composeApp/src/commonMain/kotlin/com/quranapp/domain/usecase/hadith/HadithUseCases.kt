package com.quranapp.domain.usecase.hadith

import com.quranapp.domain.model.*
import com.quranapp.domain.repository.HadithRepository

class GetCollectionsUseCase(private val repo: HadithRepository) {
    suspend operator fun invoke(): Result<List<HadithCollection>> =
        runCatching { repo.getCollections() }
}

class GetHadithByChapterUseCase(private val repo: HadithRepository) {
    suspend operator fun invoke(collection: String, chapterName: String): Result<List<Hadith>> {
        if (collection.isBlank())
            return Result.failure(IllegalArgumentException("Collection name cannot be empty"))
        if (chapterName.isBlank())
            return Result.failure(IllegalArgumentException("Chapter name cannot be empty"))
        return runCatching { repo.getHadithByChapter(collection, chapterName) }
    }
}
