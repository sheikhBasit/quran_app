package com.quranapp.domain.usecase.search

import com.quranapp.domain.model.SearchResult
import com.quranapp.domain.repository.SearchRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

class SearchUseCase(private val repo: SearchRepository) {
    suspend operator fun invoke(query: String): Result<List<SearchResult>> {
        if (query.trim().length < 3)
            return Result.failure(IllegalArgumentException("Query must be at least 3 characters"))
        return runCatching {
            coroutineScope {
                val ayahs  = async { repo.searchAyahs(query) }
                val hadith = async { repo.searchHadith(query) }
                ayahs.await() + hadith.await()
            }
        }
    }
}
