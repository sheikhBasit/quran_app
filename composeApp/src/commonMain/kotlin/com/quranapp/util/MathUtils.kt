package com.quranapp.util

object MathUtils {
    fun shortestRotation(current: Float, target: Float): Float {
        var diff = (target - current) % 360f
        if (diff > 180f) diff -= 360f
        if (diff < -180f) diff += 360f
        return current + diff
    }
    
    fun getCardinalDirection(bearing: Double): String {
        return when {
            bearing < 22.5 || bearing >= 337.5 -> "N"
            bearing < 67.5 -> "NE"
            bearing < 112.5 -> "E"
            bearing < 157.5 -> "SE"
            bearing < 202.5 -> "S"
            bearing < 247.5 -> "SW"
            bearing < 292.5 -> "W"
            else -> "NW"
        }
    }
}
