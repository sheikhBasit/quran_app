package com.quranapp.domain.usecase.prayer

import kotlin.time.ExperimentalTime
import kotlin.time.Duration.Companion.milliseconds
import com.batoulapps.adhan2.CalculationMethod
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
        val params = CalculationMethod.MUSLIM_WORLD_LEAGUE.parameters
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
        PrayerTimesResult(
            fajr    = times.fajr.toEpochMilliseconds(),
            sunrise = times.sunrise.toEpochMilliseconds(),
            dhuhr   = times.dhuhr.toEpochMilliseconds(),
            asr     = times.asr.toEpochMilliseconds(),
            maghrib = times.maghrib.toEpochMilliseconds(),
            isha    = times.isha.toEpochMilliseconds(),
        )
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
