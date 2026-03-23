package com.quranapp.data.repository

import com.quranapp.domain.repository.SettingsRepository
import com.russhwolf.settings.ObservableSettings
import com.russhwolf.settings.coroutines.getBooleanFlow
import com.russhwolf.settings.coroutines.getFloatFlow
import com.russhwolf.settings.coroutines.getStringFlow
import com.russhwolf.settings.set
import kotlinx.coroutines.flow.Flow

class SettingsRepositoryImpl(
    private val settings: ObservableSettings
) : SettingsRepository {

    override val themeMode: Flow<String> = 
        settings.getStringFlow("theme_mode", "system")

    override val arabicFontSize: Flow<Float> = 
        settings.getFloatFlow("arabic_font_size", 28f)

    override val showTranslation: Flow<Boolean> = 
        settings.getBooleanFlow("show_translation", true)

    override val notificationsEnabled: Flow<Boolean> = 
        settings.getBooleanFlow("notifications_enabled", false)

    override suspend fun setThemeMode(mode: String) {
        settings["theme_mode"] = mode
    }

    override suspend fun setArabicFontSize(size: Float) {
        settings["arabic_font_size"] = size
    }

    override suspend fun setShowTranslation(show: Boolean) {
        settings["show_translation"] = show
    }

    override suspend fun setNotificationsEnabled(enabled: Boolean) {
        settings["notifications_enabled"] = enabled
    }
}
