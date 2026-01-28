package com.adhan.app.infra

import android.app.AlarmManager
import android.content.Context
import com.adhan.app.domain.LogRepository
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Before
import org.junit.Test

class PrayerAlarmManagerTest {

    private lateinit var prayerAlarmManager: PrayerAlarmManager
    private val context: Context = mockk()
    private val logRepository: LogRepository = mockk(relaxed = true)
    private val alarmManager: AlarmManager = mockk(relaxed = true)

    @Before
    fun setup() {
        io.mockk.every { context.getSystemService(Context.ALARM_SERVICE) } returns alarmManager
        prayerAlarmManager = PrayerAlarmManager(context, logRepository)
    }

    @Test
    fun `getRequestCode should be unique for different prayers on same day`() {
        val fajrCode = prayerAlarmManager.getRequestCode("Fajr", 100, 2026)
        val dhuhrCode = prayerAlarmManager.getRequestCode("Dhuhr", 100, 2026)
        
        assertNotEquals("Fajr and Dhuhr should have different codes", fajrCode, dhuhrCode)
    }

    @Test
    fun `getRequestCode should be unique for same prayer on different days`() {
        val todayCode = prayerAlarmManager.getRequestCode("Asr", 100, 2026)
        val tomorrowCode = prayerAlarmManager.getRequestCode("Asr", 101, 2026)
        
        assertNotEquals("Today and Tomorrow Asr should have different codes", todayCode, tomorrowCode)
    }

    @Test
    fun `getRequestCode should be unique for same prayer across year boundaries`() {
        val endOfYear = prayerAlarmManager.getRequestCode("Isha", 365, 2025)
        val startOfNextYear = prayerAlarmManager.getRequestCode("Isha", 1, 2026)
        
        assertNotEquals("Isha codes should differ across year boundary", endOfYear, startOfNextYear)
    }

    @Test
    fun `getRequestCode should be deterministic`() {
        val code1 = prayerAlarmManager.getRequestCode("Maghrib", 150, 2026)
        val code2 = prayerAlarmManager.getRequestCode("Maghrib", 150, 2026)
        
        assertEquals("Identical inputs must yield identical codes", code1, code2)
    }
}
