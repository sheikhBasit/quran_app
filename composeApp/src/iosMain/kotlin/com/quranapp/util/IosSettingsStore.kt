package com.quranapp.util

import com.russhwolf.settings.NSUserDefaultsSettings
import com.russhwolf.settings.ObservableSettings
import platform.Foundation.NSUserDefaults

actual class SettingsStore {
    actual fun createSettings(): ObservableSettings {
        return NSUserDefaultsSettings(NSUserDefaults.standardUserDefaults)
    }
}
