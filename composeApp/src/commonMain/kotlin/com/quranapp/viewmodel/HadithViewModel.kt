package com.quranapp.viewmodel

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.quranapp.domain.model.Hadith
import com.quranapp.domain.model.HadithCollection
import com.quranapp.domain.usecase.hadith.GetCollectionsUseCase
import com.quranapp.domain.usecase.hadith.GetHadithByChapterUseCase
import com.quranapp.domain.usecase.hadith.GetHadithChaptersUseCase
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class HadithUiState(
    val collections: List<HadithCollection> = emptyList(),
    val chapters: List<String> = emptyList(),
    val hadiths: List<Hadith> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class HadithViewModel(
    private val getCollectionsUseCase: GetCollectionsUseCase,
    private val getHadithChaptersUseCase: GetHadithChaptersUseCase,
    private val getHadithByChapterUseCase: GetHadithByChapterUseCase
) : ScreenModel {

    private val _uiState = MutableStateFlow(HadithUiState())
    val uiState: StateFlow<HadithUiState> = _uiState.asStateFlow()

    init {
        loadCollections()
    }

    fun loadCollections() {
        _uiState.update { it.copy(isLoading = true, error = null) }
        screenModelScope.launch {
            getCollectionsUseCase()
                .onSuccess { collections ->
                    _uiState.update { it.copy(collections = collections, isLoading = false) }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(error = error.message, isLoading = false) }
                }
        }
    }

    fun loadChapters(collection: String) {
        _uiState.update { it.copy(isLoading = true, error = null, chapters = emptyList()) }
        screenModelScope.launch {
            getHadithChaptersUseCase(collection)
                .onSuccess { chapters ->
                    _uiState.update { it.copy(chapters = chapters, isLoading = false) }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(error = error.message, isLoading = false) }
                }
        }
    }

    fun loadHadiths(collection: String, chapterName: String) {
        _uiState.update { it.copy(isLoading = true, error = null, hadiths = emptyList()) }
        screenModelScope.launch {
            getHadithByChapterUseCase(collection, chapterName)
                .onSuccess { hadiths ->
                    _uiState.update { it.copy(hadiths = hadiths, isLoading = false) }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(error = error.message, isLoading = false) }
                }
        }
    }
}
