package com.quranapp.domain.usecase.prayer

import kotlin.time.ExperimentalTime
import kotlin.time.Duration.Companion.milliseconds
import com.batoulapps.adhan2.CalculationMethod
import com.batoulapps.adhan2.CalculationParameters
import com.batoulapps.adhan2.Coordinates
import com.batoulapps.adhan2.PrayerTimes
import com.batoulapps.adhan2.data.DateComponents
import com.quranapp.domain.model.NextPrayer
import com.quranapp.domain.model.PrayerTimesResult

class GetPrayerTimesUseCase {
    @OptIn(ExperimentalTime::class)
    operator fun invoke(
        latitude: Double,
        longitude: Double,
        dateMs: Long = System.currentTimeMillis(),
    ): Result<PrayerTimesResult> = runCatching {
        val coords = Coordinates(latitude, longitude)
        val params = getCalculationMethod(latitude, longitude)
        val date = DateComponents(
            java.util.Date(dateMs).let {
                val cal = java.util.Calendar.getInstance()
                cal.time = it
                cal.get(java.util.Calendar.YEAR)
            },
            java.util.Date(dateMs).let {
                val cal = java.util.Calendar.getInstance()
                cal.time = it
                cal.get(java.util.Calendar.MONTH) + 1
            },
            java.util.Date(dateMs).let {
                val cal = java.util.Calendar.getInstance()
                cal.time = it
                cal.get(java.util.Calendar.DAY_OF_MONTH)
            }
        )
        val times = PrayerTimes(coords, date, params)
        
        val fajr    = times.fajr.toEpochMilliseconds()
        val sunrise = times.sunrise.toEpochMilliseconds()
        val dhuhr   = times.dhuhr.toEpochMilliseconds()
        val asr     = times.asr.toEpochMilliseconds()
        val maghrib = times.maghrib.toEpochMilliseconds()
        val isha    = times.isha.toEpochMilliseconds()

        // --- Custom Calculations ---
        // Ishraq = Sunrise + 20 minutes
        val ishraq = sunrise + (20 * 60 * 1000L)
        // Chasht (Duha) = Sunrise + 45 minutes
        val chasht = sunrise + (45 * 60 * 1000L)
        // Tahajjud = Isha + ((Fajr - Isha) * 2/3) (Last third of night between Isha and Fajr)
        // Note: Fajr is for the next day for the calculation of the "night"
        val nextFajr = invoke(latitude, longitude, dateMs + 86400000).getOrThrow().fajr
        val tahajjud = isha + ((nextFajr - isha) * 2 / 3)

        PrayerTimesResult(
            fajr    = fajr,
            sunrise = sunrise,
            dhuhr   = dhuhr,
            asr     = asr,
            maghrib = maghrib,
            isha    = isha,
            ishraq  = ishraq,
            chasht  = chasht,
            tahajjud = tahajjud
        )
    }

    private fun getCalculationMethod(lat: Double, lon: Double): CalculationParameters {
        return when {
            // Pakistan, India, Bangladesh, Afghanistan
            lat in 5.0..37.0 && lon in 60.0..100.0 ->
                CalculationMethod.KARACHI.parameters
            // North America
            lon in -170.0..-50.0 ->
                CalculationMethod.NORTH_AMERICA.parameters
            // Egypt and surrounding
            lat in 20.0..32.0 && lon in 25.0..40.0 ->
                CalculationMethod.EGYPTIAN.parameters
            // Default
            else -> CalculationMethod.MUSLIM_WORLD_LEAGUE.parameters
        }
    }
}

class GetNextPrayerUseCase(private val getPrayerTimes: GetPrayerTimesUseCase) {
    @OptIn(ExperimentalTime::class)
    operator fun invoke(latitude: Double, longitude: Double): Result<NextPrayer> = runCatching {
        val now = System.currentTimeMillis()
        val times = getPrayerTimes(latitude, longitude).getOrThrow()
        val prayers = listOf(
            "fajr"    to times.fajr,
            "dhuhr"   to times.dhuhr,
            "asr"     to times.asr,
            "maghrib" to times.maghrib,
            "isha"    to times.isha,
        )
        val displayNames = mapOf(
            "fajr" to "Fajr", "dhuhr" to "Dhuhr", "asr" to "Asr",
            "maghrib" to "Maghrib", "isha" to "Isha",
        )
        val next = prayers.firstOrNull { (_, t) -> t > now }
            ?: ("fajr" to getPrayerTimes(latitude, longitude, now + 86_400_000).getOrThrow().fajr)
        NextPrayer(
            name = next.first,
            displayName = displayNames[next.first] ?: next.first,
            timeEpochMillis = next.second,
            minutesUntil = (next.second - now) / 60_000,
        )
    }
}
