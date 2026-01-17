package com.adhan.app.domain.models

import java.util.*
import kotlin.math.floor

/**
 * Hijri Calendar converter (Arithmetic / Kuwaiti algorithm).
 * Provides a reliable approximation of the Hijri date.
 */
object HijriCalendar {

    data class HijriDate(
        val day: Int,
        val month: Int,
        val year: Int,
        val monthName: String
    )

    private val MONTH_NAMES = listOf(
        "Muharram", "Safar", "Rabi' al-Awwal", "Rabi' al-Thani",
        "Jumada al-Ula", "Jumada al-Akhira", "Rajab", "Sha'ban",
        "Ramadan", "Shawwal", "Dhu al-Qi'dah", "Dhu al-Hijjah"
    )

    /**
     * Converts a standard Date to a HijriDate.
     * Based on the Kuwaiti calendar algorithm.
     */
    fun fromDate(date: Date, adjustment: Int = 0): HijriDate {
        val calendar = Calendar.getInstance()
        calendar.time = date
        
        var day = calendar.get(Calendar.DAY_OF_MONTH)
        var month = calendar.get(Calendar.MONTH) + 1
        var year = calendar.get(Calendar.YEAR)

        if (month < 3) {
            year -= 1
            month += 12
        }

        val a = floor(year / 100.0).toInt()
        val b = 2 - a + floor(a / 4.0).toInt()
        val jd = floor(365.25 * (year + 4716)).toInt() + floor(30.6001 * (month + 1)).toInt() + day + b - 1524

        // Apply adjustment
        val jdAdj = jd + adjustment
        
        val l = jdAdj - 1948440 + 10632
        val n = ((l - 1) / 10631).toInt()
        val lRemaining = l - 10631 * n + 354
        val j = ((10985 - lRemaining) / 5316).toInt() * ((50 * lRemaining) / 17719).toInt() +
                (lRemaining / 5670).toInt() * ((43 * lRemaining) / 15238).toInt()
        val lFinal = lRemaining - ((30 - j) / 15).toInt() * ((17719 * j) / 50).toInt() -
                (j / 16).toInt() * ((15238 * j) / 43).toInt() + 29
        
        var hMonth = ((24 * lFinal) / 709).toInt()
        var hDay = lFinal - ((709 * hMonth) / 24).toInt()
        var hYear = 30 * n + j - 30

        // Handle month/day overflows
        if (hMonth > 12) {
            hMonth = 12
        }
        if (hMonth < 1) {
            hMonth = 1
        }

        return HijriDate(
            day = hDay,
            month = hMonth,
            year = hYear,
            monthName = MONTH_NAMES.getOrElse(hMonth - 1) { "Unknown" }
        )
    }
}
