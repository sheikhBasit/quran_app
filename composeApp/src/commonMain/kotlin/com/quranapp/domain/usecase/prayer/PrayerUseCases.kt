package com.quranapp.domain.usecase.prayer

import com.batoulapps.adhan.CalculationMethod
import com.batoulapps.adhan.Coordinates
import com.batoulapps.adhan.PrayerTimes
import com.batoulapps.adhan.data.DateComponents
import com.quranapp.domain.model.NextPrayer
import com.quranapp.domain.model.PrayerTimesResult

class GetPrayerTimesUseCase {
    operator fun invoke(
        latitude: Double,
        longitude: Double,
        dateMs: Long = System.currentTimeMillis(),
    ): Result<PrayerTimesResult> = runCatching {
        val coords = Coordinates(latitude, longitude)
        val params = CalculationMethod.MUSLIM_WORLD_LEAGUE.parameters
        val date = DateComponents.fromUtcDate(
            java.util.Date(dateMs).let {
                val cal = java.util.Calendar.getInstance()
                cal.time = it
                DateComponents(cal.get(java.util.Calendar.YEAR),
                    cal.get(java.util.Calendar.MONTH) + 1,
                    cal.get(java.util.Calendar.DAY_OF_MONTH))
            }
        )
        val times = PrayerTimes(coords, date, params)
        PrayerTimesResult(
            fajr    = times.fajr.time,
            sunrise = times.sunrise.time,
            dhuhr   = times.dhuhr.time,
            asr     = times.asr.time,
            maghrib = times.maghrib.time,
            isha    = times.isha.time,
        )
    }
}

class GetNextPrayerUseCase(private val getPrayerTimes: GetPrayerTimesUseCase) {
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
