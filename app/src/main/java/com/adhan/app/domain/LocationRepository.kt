package com.adhan.app.domain

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocationRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs = context.getSharedPreferences("adhan_prefs", Context.MODE_PRIVATE)

    private val _location = MutableStateFlow(
        LocationData(
            lat = prefs.getFloat("last_lat", 51.5074f).toDouble(),
            lng = prefs.getFloat("last_lng", -0.1278f).toDouble(),
            name = prefs.getString("last_location_name", "London, UK") ?: "London, UK",
            isOverrideActive = prefs.getBoolean("is_override_active", false)
        )
    )
    val location: StateFlow<LocationData> = _location

    fun updateLocation(lat: Double, lng: Double, name: String, isOverride: Boolean) {
        _location.value = LocationData(lat, lng, name, isOverride)
        
        prefs.edit().apply {
            putFloat("last_lat", lat.toFloat())
            putFloat("last_lng", lng.toFloat())
            putString("last_location_name", name)
            putBoolean("is_override_active", isOverride)
        }.apply()
    }

    data class LocationData(
        val lat: Double,
        val lng: Double,
        val name: String,
        val isOverrideActive: Boolean
    )
}
