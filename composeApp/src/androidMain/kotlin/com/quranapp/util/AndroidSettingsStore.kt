package com.quranapp.util

import android.content.Context
import com.russhwolf.settings.ObservableSettings
import com.russhwolf.settings.SharedPreferencesSettings

actual class SettingsStore(private val context: Context) {
    actual fun createSettings(): ObservableSettings {
        val sharedPrefs = context.getSharedPreferences("quran_settings", Context.MODE_PRIVATE)
        return SharedPreferencesSettings(sharedPrefs)
    }
}
