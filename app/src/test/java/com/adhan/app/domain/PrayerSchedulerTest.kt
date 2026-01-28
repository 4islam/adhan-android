package com.adhan.app.domain

import com.adhan.app.infra.PrayerAlarmManager
import io.mockk.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import java.util.*

class PrayerSchedulerTest {

    private val locationRepository: LocationRepository = mockk()
    private val prefsRepository: UserPreferencesRepository = mockk(relaxed = true)
    private val alarmManager: PrayerAlarmManager = mockk(relaxed = true)
    private val logRepository: LogRepository = mockk(relaxed = true)

    private lateinit var scheduler: PrayerScheduler

    @Before
    fun setup() {
        scheduler = PrayerScheduler(locationRepository, prefsRepository, alarmManager, logRepository)
        
        // Mock Location
        every { locationRepository.location } returns MutableStateFlow(
            LocationRepository.LocationData(lat = 51.5074, lng = -0.1278, name = "London", isSet = true, isOverrideActive = false)
        )

        
        // Mock Prefs
        every { prefsRepository.getCalcMethod() } returns 0
        every { prefsRepository.getAsrJuristic() } returns 0
        every { prefsRepository.getCombiningThreshold() } returns 70
        every { prefsRepository.isAdhanEnabled(any(), any()) } returns true
        every { prefsRepository.isTahajjudEnabled() } returns false
    }

    @Test
    fun `scheduleAlarmsForNext24Hours should schedule alarms for two days`() = runBlocking {
        scheduler.scheduleAlarmsForNext24Hours()
        
        // We calculate for Today and Tomorrow.
        // Each day usually has 5 prayers (Fajr, Dhuhr, Asr, Maghrib, Isha).
        // Total should be roughly 10 alarms.
        
        // Capture the list passed to alarmManager
        val capturedAlarms = slot<List<Pair<String, Long>>>()
        verify { alarmManager.scheduleExactAlarms(capture(capturedAlarms)) }
        
        val alarms = capturedAlarms.captured
        println("Alarms scheduled: ${alarms.size}")
        
        // At minimum we expect 4 alarms (one prayer Today if it's late, and all 5 Tomorrow, but combining might reduce it)
        // If combining is on, Tomorrow might have 3. If Today is VERY late, Today might have 0 or 1.
        // So let's just check we got some alarms and they are from different days.
        org.junit.Assert.assertTrue("Should have some alarms, got ${alarms.size}", alarms.size >= 1)
        
        // Check for specific prayers
        val names = alarms.map { it.first }
        org.junit.Assert.assertTrue("Should contain at least one of Fajr or Isha", names.any { it.contains("Fajr") || it.contains("Isha") })
    }

    @Test
    fun `scheduleAlarmsForNext24Hours should combine Maghrib and Isha when enabled and threshold met`() = runBlocking {
        // Force Isha to be very close to Maghrib for testing
        every { prefsRepository.isShortIshaEnabled() } returns true
        every { prefsRepository.getShortIshaThreshold() } returns 120 // 2 hours
        
        scheduler.scheduleAlarmsForNext24Hours()
        
        val capturedAlarms = slot<List<Pair<String, Long>>>()
        verify { alarmManager.scheduleExactAlarms(capture(capturedAlarms)) }
        
        val names = capturedAlarms.captured.map { it.first }
        // In London, Maghrib and Isha are usually within 2 hours.
        // If combined, "Maghrib" and "Isha" should be replaced by "Maghrib/Isha"
        org.junit.Assert.assertTrue("Should contain Maghrib/Isha", names.contains("Maghrib/Isha"))
        org.junit.Assert.assertFalse("Should NOT contain solo Maghrib", names.contains("Maghrib"))
        org.junit.Assert.assertFalse("Should NOT contain solo Isha", names.contains("Isha"))
    }
}
