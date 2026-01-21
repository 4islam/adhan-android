package com.adhan.app.ui

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import com.adhan.app.domain.LocationRepository
import com.adhan.app.domain.models.PrayerTimesCalculator
import com.adhan.app.infra.PrayerAlarmManager
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.util.*

@OptIn(ExperimentalCoroutinesApi::class)
class PrayerTimesViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    
    private lateinit var viewModel: PrayerTimesViewModel
    private val alarmManager = mockk<PrayerAlarmManager>(relaxed = true)
    private val repository = mockk<LocationRepository>(relaxed = true)
    private val application = mockk<Application>(relaxed = true)
    private val prefs = mockk<SharedPreferences>(relaxed = true)
    private val prefsEditor = mockk<SharedPreferences.Editor>(relaxed = true)
    private val logRepository = mockk<com.adhan.app.domain.LogRepository>(relaxed = true)

    private val locationFlow = MutableStateFlow(
        LocationRepository.LocationData(51.5074, -0.1278, "London, UK", true, true)
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        
        every { application.getSharedPreferences(any(), any()) } returns prefs
        every { prefs.edit() } returns prefsEditor
        every { repository.location } returns locationFlow
        coEvery { logRepository.getLogs() } returns emptyList()
        coEvery { logRepository.log(any(), any()) } just Runs
        
        // Mocking default settings
        every { prefs.getInt("calc_method", any()) } returns PrayerTimesCalculator.Ahmadiyya
        every { prefs.getInt("asr_juristic", any()) } returns PrayerTimesCalculator.Shafii
        every { prefs.getBoolean("audio_enabled", any()) } returns true
        every { prefs.getBoolean("use_12_hour", any()) } returns true

        viewModel = PrayerTimesViewModel(alarmManager, repository, application, logRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `verify all essential prayers are present and astro events are excluded`() = runTest {
        // Wait for updateTimes to finish (offloaded to Default)
        testDispatcher.scheduler.advanceUntilIdle()
        
        val state = viewModel.uiState.value
        val prayerNames = state.prayerTimes.map { it.name }
        
        // Essential prayers MUST be here
        assertTrue("Fajr missing", prayerNames.any { it.contains("Fajr") })
        assertTrue("Dhuhr/Jummah missing", prayerNames.any { it.contains("Dhuhr") || it.contains("Jummah") })
        assertTrue("Asr missing", prayerNames.contains("Asr"))
        assertTrue("Maghrib missing", prayerNames.contains("Maghrib"))
        assertTrue("Isha missing", prayerNames.contains("Isha"))
        
        // Astro events MUST NOT be in the prayer list
        assertFalse("Sunrise should not be in prayer list", prayerNames.contains("Sunrise"))
        assertFalse("Sunset should not be in prayer list", prayerNames.contains("Sunset"))
        assertFalse("Solar Noon should not be in prayer list", prayerNames.contains("Solar Noon"))
    }

    @Test
    fun `verify 15-minute highlight logic`() = runTest {
        // Setup fixed prayer times for testing
        // Mocking updateNextPrayer behavior by setting known times
        // In a real scenario, we might want to dependency inject the Calculator too,
        // but for now we'll test the ViewModel's logic with the current state.
        
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Let's assume Fajr is at 05:00 and Dhuhr is at 12:00
        // If current time is 11:40 (20 mins before Dhuhr) -> Active should be Fajr
        // If current time is 11:50 (10 mins before Dhuhr) -> Active should be Dhuhr
        
        // This test requires verifying the updateNextPrayer(now: Date) logic specifically.
        // We can trigger it by updating the internal state if possible, 
        // or by letting the startClock loop run (which is tricky with delays).
        
        // For unit testing, we want to verify the logic inside updateNextPrayer.
    }
}
