package com.adhan.app.domain.models

import java.util.Date
import kotlin.math.*

data class AstroPosition(
    val altitude: Double, // in degrees
    val azimuth: Double   // in degrees
)

/**
 * Simplified astronomical calculations for Sun/Moon position.
 * Base logic for a beautiful horizon visualizer.
 * Ported from Astrology.ts
 */
object Astrology {

    private const val RAD = PI / 180.0
    private const val DEG = 180.0 / PI

    /**
     * Accurate Sun altitude calculation.
     * Based on NOAA's Solar Calculation equations.
     */
    fun getSunPosition(date: Date, lat: Double, lng: Double): AstroPosition {
        // 1. Julian Date
        val julianDate = (date.time / 86400000.0) + 2440587.5
        val d = julianDate - 2451545.0

        // 2. Solar coordinates
        val L = (280.460 + 0.9856474 * d) % 360
        val g = (357.528 + 0.9856003 * d) % 360
        val radG = g * RAD
        val lambda = (L + 1.915 * sin(radG) + 0.020 * sin(2 * radG)) % 360
        val epsilon = (23.439 - 0.0000004 * d) % 360

        // 3. Right Ascension / Declination
        val radLambda = lambda * RAD
        val radEpsilon = epsilon * RAD
        
        val alpha = atan2(cos(radEpsilon) * sin(radLambda), cos(radLambda)) * DEG
        val delta = asin(sin(radEpsilon) * sin(radLambda)) * DEG

        // 4. Local Sidereal Time
        // GMST in hours = 18.697374558 + 24.06570982441908 * d
        val gmst = (18.697374558 + 24.06570982441908 * d) % 24
        val lst = (gmst + lng / 15 + 24) % 24

        // 5. Hour Angle
        var ha = (lst * 15 - alpha) // in degrees
        while (ha < -180) ha += 360
        while (ha > 180) ha -= 360

        // 6. Altitude
        val phi = lat * RAD
        val radDelta = delta * RAD
        val radHa = ha * RAD

        val sinAlt = sin(phi) * sin(radDelta) + cos(phi) * cos(radDelta) * cos(radHa)
        val altitude = asin(sinAlt) * DEG

        // 7. Azimuth
        val denom = cos(phi) * cos(asin(max(-1.0, min(1.0, sinAlt))))
        var azimuth = 0.0
        if (abs(denom) > 0.0001) {
            val cosAz = (sin(radDelta) - sin(phi) * sinAlt) / denom
            azimuth = acos(max(-1.0, min(1.0, cosAz))) * DEG
        }
        if (sin(radHa) > 0) azimuth = 360 - azimuth

        return AstroPosition(altitude, azimuth)
    }

    fun getMoonPosition(date: Date, lat: Double, lng: Double): AstroPosition {
        val julianDate = (date.time / 86400000.0) + 2440587.5
        val d = julianDate - 2451545.0

        // Simplified orbital elements for the Moon
        val L = (218.316 + 13.176396 * d) % 360 // Mean longitude
        val M = (134.963 + 13.064993 * d) % 360 // Mean anomaly
        val F = (93.272 + 13.229350 * d) % 360  // Mean distance from node

        val radM = M * RAD
        val lambda = (L + 6.289 * sin(radM)) % 360 // Ecliptic longitude
        val radF = F * RAD
        val beta = 5.128 * sin(radF)               // Ecliptic latitude
        val epsilon = 23.439 * RAD                 // Obliquity

        // Right Ascension / Declination
        val radLambda = lambda * RAD
        val radBeta = beta * RAD
        
        val alpha = atan2(
            sin(radLambda) * cos(epsilon) - tan(radBeta) * sin(epsilon),
            cos(radLambda)
        ) * DEG
        
        val delta = asin(
            sin(radBeta) * cos(epsilon) + cos(radBeta) * sin(epsilon) * sin(radLambda)
        ) * DEG

        // Local Sidereal Time
        val gmst = (18.697374558 + 24.06570982441908 * d) % 24
        val lst = (gmst + lng / 15 + 24) % 24

        // Hour Angle
        var ha = (lst * 15 - alpha)
        while (ha < -180) ha += 360
        while (ha > 180) ha -= 360

        // Altitude
        val phi = lat * RAD
        val radDelta = delta * RAD
        val radHa = ha * RAD

        val sinAlt = sin(phi) * sin(radDelta) + cos(phi) * cos(radDelta) * cos(radHa)
        val altitude = asin(max(-1.0, min(1.0, sinAlt))) * DEG

        // Azimuth
        val denom = cos(phi) * cos(asin(max(-1.0, min(1.0, sinAlt))))
        var azimuth = 0.0
        if (abs(denom) > 0.0001) {
            val cosAz = (sin(radDelta) - sin(phi) * sinAlt) / denom
            azimuth = acos(max(-1.0, min(1.0, cosAz))) * DEG
        }
        if (sin(radHa) > 0) azimuth = 360 - azimuth

        return AstroPosition(altitude, azimuth)
    }

    /**
     * Calculates Moonset time for a given day.
     * Uses sampling to find when altitude crosses 0.
     */
    fun getMoonset(date: Date, lat: Double, lng: Double): Date? {
        val startOfDay = getStartOfDay(date)

        // Sample every hour to find transition
        for (i in 0 until 24) {
            val d1 = Date(startOfDay.time + i * 3600000)
            val d2 = Date(startOfDay.time + (i + 1) * 3600000)

            val pos1 = getMoonPosition(d1, lat, lng)
            val pos2 = getMoonPosition(d2, lat, lng)

            // Moonset: going from positive altitude to negative
            if (pos1.altitude > 0 && pos2.altitude <= 0) {
                val fraction = pos1.altitude / (pos1.altitude - pos2.altitude)
                return Date(d1.time + (fraction * 3600000).toLong())
            }
        }
        return null
    }

    /**
     * Calculates Moonrise time for a given day.
     */
    fun getMoonrise(date: Date, lat: Double, lng: Double): Date? {
        val startOfDay = getStartOfDay(date)

        for (i in 0 until 24) {
            val d1 = Date(startOfDay.time + i * 3600000)
            val d2 = Date(startOfDay.time + (i + 1) * 3600000)

            val pos1 = getMoonPosition(d1, lat, lng)
            val pos2 = getMoonPosition(d2, lat, lng)

            // Moonrise: going from negative altitude to positive
            if (pos1.altitude <= 0 && pos2.altitude > 0) {
                val fraction = -pos1.altitude / (pos2.altitude - pos1.altitude)
                return Date(d1.time + (fraction * 3600000).toLong())
            }
        }
        return null
    }
    
    // Helper since we can't easily construct a Date copy with setHours return value in one expression before Date extension functions
    private fun getStartOfDay(date: Date): Date {
        val cal = java.util.Calendar.getInstance()
        cal.time = date
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
        cal.set(java.util.Calendar.MINUTE, 0)
        cal.set(java.util.Calendar.SECOND, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)
        return cal.time
    }
}
