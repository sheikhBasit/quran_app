package com.quranapp.viewmodel

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.quranapp.domain.model.SearchResult
import com.quranapp.domain.usecase.search.SearchUseCase
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class SearchScreenUiState(
    val query: String = "",
    val quranResults: List<SearchResult.AyahResult> = emptyList(),
    val hadithResults: List<SearchResult.HadithResult> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

@OptIn(FlowPreview::class)
class SearchViewModel(
    private val searchUseCase: SearchUseCase
) : ScreenModel {

    private val _uiState = MutableStateFlow(SearchScreenUiState())
    val uiState: StateFlow<SearchScreenUiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")

    init {
        _searchQuery
            .debounce(500L)
            .distinctUntilChanged()
            .onEach { query ->
                if (query.length >= 3) {
                    performSearch(query)
                } else {
                    _uiState.update { it.copy(quranResults = emptyList(), hadithResults = emptyList(), isLoading = false) }
                }
            }
            .launchIn(screenModelScope)
    }

    fun onQueryChange(newQuery: String) {
        _searchQuery.value = newQuery
        _uiState.update { it.copy(query = newQuery) }
    }

    private fun performSearch(query: String) {
        _uiState.update { it.copy(isLoading = true, error = null) }
        screenModelScope.launch {
            searchUseCase(query)
                .onSuccess { results ->
                    val quran = results.filterIsInstance<SearchResult.AyahResult>()
                    val hadith = results.filterIsInstance<SearchResult.HadithResult>()
                    _uiState.update { it.copy(
                        quranResults = quran,
                        hadithResults = hadith,
                        isLoading = false
                    )}
                }
                .onFailure { error ->
                    _uiState.update { it.copy(error = error.message, isLoading = false) }
                }
        }
    }
}
