package com.adhan.app.domain

import android.content.Context
import android.content.SharedPreferences
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LocationRepositoryTest {

    private val context = mockk<Context>(relaxed = true)
    private val prefs = mockk<SharedPreferences>(relaxed = true)
    private val editor = mockk<SharedPreferences.Editor>(relaxed = true)
    
    private lateinit var repository: LocationRepository

    @Before
    fun setup() {
        every { context.getSharedPreferences("adhan_prefs", Context.MODE_PRIVATE) } returns prefs
        every { prefs.edit() } returns editor
        every { editor.putFloat(any(), any()) } returns editor
        every { editor.putString(any(), any()) } returns editor
        every { editor.putBoolean(any(), any()) } returns editor
        
        // Default values in prefs
        every { prefs.contains("last_lat") } returns true
        every { prefs.getFloat("last_lat", any()) } returns 48.8566f
        every { prefs.getFloat("last_lng", any()) } returns 2.3522f
        every { prefs.getString("last_location_name", any()) } returns "Paris, France"
        every { prefs.getBoolean("is_override_active", any()) } returns false
        
        repository = LocationRepository(context)
    }

    @Test
    fun `initial location is loaded from preferences`() = runTest {
        val location = repository.location.value
        assertEquals(48.8566, location.lat, 0.0001)
        assertEquals(2.3522, location.lng, 0.0001)
        assertEquals("Paris, France", location.name)
        assertEquals(false, location.isOverrideActive)
        assertEquals(true, location.isSet)
    }

    @Test
    fun `updateLocation updates flow and persists to preferences`() = runTest {
        repository.updateLocation(40.7128, -74.0060, "New York", true)
        
        val location = repository.location.value
        assertEquals(40.7128, location.lat, 0.0001)
        assertEquals(-74.0060, location.lng, 0.0001)
        assertEquals("New York", location.name)
        assertEquals(true, location.isOverrideActive)
        
        verify {
            editor.putFloat("last_lat", 40.7128f)
            editor.putFloat("last_lng", -74.0060f)
            editor.putString("last_location_name", "New York")
            editor.putBoolean("is_override_active", true)
            editor.apply()
        }
    }
}
