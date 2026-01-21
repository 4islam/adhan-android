package com.adhan.app.domain

import com.adhan.app.domain.models.PrayerTimesCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.*

class PrayerTimesHighLatTest {

    private val calculator = PrayerTimesCalculator()

    @Test
    fun `calculatePrayerTimes_HighLatitude_Reykjavik`() {
        // Reykjavik, Iceland
        val lat = 64.1466
        val lng = -21.9426
        
        // Mid-summer (Extreme long days)
        val calendar = Calendar.getInstance().apply {
            set(2024, Calendar.JUNE, 21, 12, 0, 0)
        }
        val date = calendar.time

        val times = calculator.getCombinedPrayerTimes(date, lat, lng)
        
        // Verify Fajr and Isha exist even in extreme conditions (methods usually switch to angle-based or 1/7th rule)
        val fajr = times.find { it.name == "Fajr" }
        val isha = times.find { it.name == "Isha" }
        
        assertNotEquals("Should have Fajr time", PrayerTimesCalculator.InvalidTime, fajr?.time)
        assertNotEquals("Should have Isha time", PrayerTimesCalculator.InvalidTime, isha?.time)
        
        println("Reykjavik Summer Times: $times")
    }

    @Test
    fun `calculatePrayerTimes_HighLatitude_Yellowknife`() {
        // Yellowknife, Canada
        val lat = 62.4540
        val lng = -114.3718
        
        // Mid-winter (Short days)
        val calendar = Calendar.getInstance().apply {
            set(2024, Calendar.DECEMBER, 21, 12, 0, 0)
        }
        val date = calendar.time

        val times = calculator.getCombinedPrayerTimes(date, lat, lng)
        
        val sunrise = times.find { it.name == "Sunrise" }
        val sunset = times.find { it.name == "Sunset" }
        
        assertNotEquals("Should have sunrise", PrayerTimesCalculator.InvalidTime, sunrise?.time)
        assertNotEquals("Should have sunset", PrayerTimesCalculator.InvalidTime, sunset?.time)
        
        println("Yellowknife Winter Times: $times")
    }
}
