package com.quranapp.util

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import com.quranapp.db.QuranDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

// ─── Database Driver ──────────────────────────────────────────────────────────

actual class DatabaseDriverFactory {
    actual fun createDriver(): SqlDriver =
        NativeSqliteDriver(QuranDatabase.Schema, "quran.db")
}

// ─── Location — stub returns null until iOS implemented ───────────────────────

actual class LocationProvider {
    actual fun getLocationFlow(): Flow<Coordinates?> = flow { emit(null) }
}

// ─── Compass — stub returns 0 until iOS implemented ──────────────────────────

actual class CompassSensor {
    actual fun getBearingFlow(): Flow<Float> = flow { emit(0f) }
}

// ─── Notifications — no-op stubs ─────────────────────────────────────────────

actual class NotificationScheduler {
    actual fun schedulePrayerAlarm(prayerName: String, timeMs: Long, soundUri: String?) {
        // TODO: Implement for iOS post-MVP using UNUserNotificationCenter
    }

    actual fun cancelPrayerAlarm(prayerName: String) {
        // TODO: Implement for iOS post-MVP
    }

    actual fun cancelAllAlarms() {
        // TODO: Implement for iOS post-MVP
    }
}

actual val platformModule = org.koin.dsl.module {
    single(org.koin.core.qualifier.named("baseUrl")) { "http://localhost:8000" }
    single { SettingsStore() }
}

actual fun randomUUID(): String = platform.Foundation.NSUUID().UUIDString()
actual fun currentTimeMillis(): Long = (platform.Foundation.NSDate().timeIntervalSince1970 * 1000).toLong()
