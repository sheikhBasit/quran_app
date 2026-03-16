package com.quranapp.domain.usecase.hadith

import com.quranapp.domain.model.*
import com.quranapp.domain.repository.HadithRepository

class GetCollectionsUseCase(private val repo: HadithRepository) {
    suspend operator fun invoke(): Result<List<HadithCollection>> =
        runCatching { repo.getCollections() }
}

class GetHadithByBookUseCase(private val repo: HadithRepository) {
    suspend operator fun invoke(collection: String, bookNumber: Int): Result<List<Hadith>> {
        if (collection.isBlank())
            return Result.failure(IllegalArgumentException("Collection name cannot be empty"))
        return runCatching { repo.getHadithByBook(collection, bookNumber) }
    }
}
