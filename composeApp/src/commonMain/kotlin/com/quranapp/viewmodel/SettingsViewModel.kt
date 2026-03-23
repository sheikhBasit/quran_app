package com.quranapp.viewmodel

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.quranapp.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val repository: SettingsRepository
) : ScreenModel {

    val themeMode: StateFlow<String> = repository.themeMode
        .stateIn(screenModelScope, SharingStarted.WhileSubscribed(5000), "system")

    val arabicFontSize: StateFlow<Float> = repository.arabicFontSize
        .stateIn(screenModelScope, SharingStarted.WhileSubscribed(5000), 28f)

    val showTranslation: StateFlow<Boolean> = repository.showTranslation
        .stateIn(screenModelScope, SharingStarted.WhileSubscribed(5000), true)

    val notificationsEnabled: StateFlow<Boolean> = repository.notificationsEnabled
        .stateIn(screenModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun setThemeMode(mode: String) {
        screenModelScope.launch {
            repository.setThemeMode(mode)
        }
    }

    fun setArabicFontSize(size: Float) {
        screenModelScope.launch {
            repository.setArabicFontSize(size)
        }
    }

    fun setShowTranslation(show: Boolean) {
        screenModelScope.launch {
            repository.setShowTranslation(show)
        }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        screenModelScope.launch {
            repository.setNotificationsEnabled(enabled)
        }
    }
}
