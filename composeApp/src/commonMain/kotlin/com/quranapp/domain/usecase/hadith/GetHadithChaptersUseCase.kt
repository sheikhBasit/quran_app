package com.quranapp.domain.usecase.hadith

import com.quranapp.domain.repository.HadithRepository

class GetHadithChaptersUseCase(private val repo: HadithRepository) {
    suspend operator fun invoke(collection: String): Result<List<String>> {
        if (collection.isBlank())
            return Result.failure(IllegalArgumentException("Collection cannot be empty"))
        return runCatching { repo.getChapterNames(collection) }
    }
}
