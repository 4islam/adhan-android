package com.adhan.app.domain.models

import org.junit.Assert.assertEquals
import org.junit.Test

class PrayerTimesParityTest {

    @Test
    fun testMakkahParity() {
        val calculator = PrayerTimesCalculator()
        calculator.setCalcMethod(PrayerTimesCalculator.Makkah)
        
        // Makkah_2024_01_01: Lat 21.4225, Lng 39.8262, Zone 3.0
        val times = calculator.getDatePrayerTimes(2024, 1, 1, 21.4225, 39.8262, 3.0)
        
        val expected = listOf(
            "05:35",
            "06:58",
            "12:24",
            "15:29",
            "17:50",
            "17:50",
            "19:20"
        )
        
        assertEquals("Makkah Parity Failed", expected, times)
    }

    @Test
    fun testNYCParity() {
        val calculator = PrayerTimesCalculator()
        calculator.setCalcMethod(PrayerTimesCalculator.ISNA)
        
        // NYC_2024_06_15: Lat 40.7128, Lng -74.0060, Zone -4.0
        val times = calculator.getDatePrayerTimes(2024, 6, 15, 40.7128, -74.0060, -4.0)
        
        val expected = listOf(
            "03:45",
            "05:24",
            "12:57",
            "16:57",
            "20:29",
            "20:30",
            "22:09"
        )
        assertEquals("NYC Parity Failed", expected, times)
    }

    @Test
    fun testLondonParity() {
        val calculator = PrayerTimesCalculator()
        calculator.setCalcMethod(PrayerTimesCalculator.MWL)
        
        // London_2024_12_21: Lat 51.5074, Lng -0.1278, Zone 0.0
        val times = calculator.getDatePrayerTimes(2024, 12, 21, 51.5074, -0.1278, 0.0)
        
        val expected = listOf(
            "06:00",
            "08:04",
            "11:59",
            "13:38",
            "15:54",
            "15:54",
            "17:51"
        )
        assertEquals("London Parity Failed", expected, times)
    }
}
