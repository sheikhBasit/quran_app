package com.quranapp.util

import com.russhwolf.settings.ObservableSettings

expect class SettingsStore {
    fun createSettings(): ObservableSettings
}
