package com.quranapp.domain.usecase.qibla

import kotlin.math.*

// Kaaba coordinates
private const val KAABA_LAT = 21.4225
private const val KAABA_LNG = 39.8262

class GetQiblaDirectionUseCase {
    operator fun invoke(latitude: Double, longitude: Double): Double {
        val lat1 = Math.toRadians(latitude)
        val lat2 = Math.toRadians(KAABA_LAT)
        val dLng = Math.toRadians(KAABA_LNG - longitude)
        val x = sin(dLng) * cos(lat2)
        val y = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(dLng)
        val bearing = Math.toDegrees(atan2(x, y))
        return (bearing + 360) % 360
    }
}
