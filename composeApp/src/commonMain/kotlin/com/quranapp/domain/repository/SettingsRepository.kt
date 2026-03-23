package com.quranapp.domain.repository

import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val themeMode: Flow<String>
    val arabicFontSize: Flow<Float>
    val showTranslation: Flow<Boolean>
    val notificationsEnabled: Flow<Boolean>

    suspend fun setThemeMode(mode: String)
    suspend fun setArabicFontSize(size: Float)
    suspend fun setShowTranslation(show: Boolean)
    suspend fun setNotificationsEnabled(enabled: Boolean)
}
