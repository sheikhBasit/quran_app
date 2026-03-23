package com.quranapp.util

import app.cash.sqldelight.db.SqlDriver
import kotlinx.coroutines.flow.Flow

// ─── Database Driver ──────────────────────────────────────────────────────────

expect class DatabaseDriverFactory {
    fun createDriver(): SqlDriver
}

// ─── Location ─────────────────────────────────────────────────────────────────

data class Coordinates(val latitude: Double, val longitude: Double)

expect class LocationProvider {
    fun getLocationFlow(): Flow<Coordinates?>
}

// ─── Compass ──────────────────────────────────────────────────────────────────

expect class CompassSensor {
    fun getBearingFlow(): Flow<Float>
}

// ─── Notifications ────────────────────────────────────────────────────────────

expect class NotificationScheduler {
    fun schedulePrayerAlarm(prayerName: String, timeMs: Long, soundUri: String?)
    fun cancelPrayerAlarm(prayerName: String)
    fun cancelAllAlarms()
}

expect fun randomUUID(): String
expect fun currentTimeMillis(): Long
