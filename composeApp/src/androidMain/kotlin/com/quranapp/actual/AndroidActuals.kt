package com.quranapp.util

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.google.android.gms.location.LocationServices
import com.quranapp.db.QuranDatabase
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import java.io.File

// ─── Database Driver ──────────────────────────────────────────────────────────

actual class DatabaseDriverFactory(private val context: Context) {
    actual fun createDriver(): SqlDriver {
        copyDatabaseIfNeeded()
        return AndroidSqliteDriver(QuranDatabase.Schema, context, "quran.db")
    }

    private fun copyDatabaseIfNeeded() {
        val dbFile = context.getDatabasePath("quran.db")
        if (dbFile.exists()) return
        dbFile.parentFile?.mkdirs()
        context.assets.open("quran.db").use { input ->
            dbFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }
    }
}

// ─── Location ─────────────────────────────────────────────────────────────────

actual class LocationProvider(private val context: Context) {
    actual fun getLocationFlow(): Flow<Coordinates?> = callbackFlow {
        val client = LocationServices.getFusedLocationProviderClient(context)
        try {
            client.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    trySend(Coordinates(location.latitude, location.longitude))
                } else {
                    trySend(null)
                }
            }.addOnFailureListener {
                trySend(null)
            }
        } catch (e: SecurityException) {
            trySend(null)
        }
        awaitClose()
    }
}

// ─── Compass ──────────────────────────────────────────────────────────────────

actual class CompassSensor(private val context: Context) {
    actual fun getBearingFlow(): Flow<Float> = callbackFlow {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

        if (sensor == null) {
            trySend(0f)
            awaitClose()
            return@callbackFlow
        }

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val rotationMatrix = FloatArray(9)
                val orientationAngles = FloatArray(3)
                SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                SensorManager.getOrientation(rotationMatrix, orientationAngles)
                val azimuth = Math.toDegrees(orientationAngles[0].toDouble()).toFloat()
                trySend((azimuth + 360f) % 360f)
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI)
        awaitClose { sensorManager.unregisterListener(listener) }
    }
}

// ─── Notifications ────────────────────────────────────────────────────────────

actual class NotificationScheduler(private val context: Context) {
    actual fun schedulePrayerAlarm(prayerName: String, timeMs: Long, soundUri: String?) {
        val alarmManager = context.getSystemService(android.app.AlarmManager::class.java)
        val intent = android.content.Intent(context, Class.forName("com.quranapp.actual.PrayerAlarmReceiver")).apply {
            putExtra("prayer_name", prayerName)
            if (soundUri != null) putExtra("sound_uri", soundUri)
        }
        val pendingIntent = android.app.PendingIntent.getBroadcast(
            context, prayerName.hashCode(), intent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.setExactAndAllowWhileIdle(
            android.app.AlarmManager.RTC_WAKEUP, timeMs, pendingIntent
        )
    }

    actual fun cancelPrayerAlarm(prayerName: String) {
        val alarmManager = context.getSystemService(android.app.AlarmManager::class.java)
        val intent = android.content.Intent(context, Class.forName("com.quranapp.actual.PrayerAlarmReceiver"))
        val pendingIntent = android.app.PendingIntent.getBroadcast(
            context, prayerName.hashCode(), intent,
            android.app.PendingIntent.FLAG_NO_CREATE or android.app.PendingIntent.FLAG_IMMUTABLE
        )
        pendingIntent?.let { alarmManager.cancel(it) }
    }

    actual fun cancelAllAlarms() {
        listOf("fajr", "dhuhr", "asr", "maghrib", "isha").forEach { cancelPrayerAlarm(it) }
    }
}

actual fun randomUUID(): String = java.util.UUID.randomUUID().toString()
actual fun currentTimeMillis(): Long = System.currentTimeMillis()
