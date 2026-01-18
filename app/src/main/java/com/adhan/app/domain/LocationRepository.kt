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
        if (prefs.contains("last_lat")) {
            val savedName = prefs.getString("last_location_name", "London, UK") ?: "London, UK"
            // If the saved location is the old default (London), treat is as unset to force auto-detection
            if (savedName == "London, UK" || savedName == "Toronto, Canada") {
                LocationData(
                    lat = 0.0,
                    lng = 0.0,
                    name = "Detecting Location...",
                    isOverrideActive = false,
                    isSet = false
                )
            } else {
                LocationData(
                    lat = prefs.getFloat("last_lat", 51.5074f).toDouble(),
                    lng = prefs.getFloat("last_lng", -0.1278f).toDouble(),
                    name = savedName,
                    isOverrideActive = prefs.getBoolean("is_override_active", false),
                    isSet = true
                )
            }
        } else {
            LocationData(
                lat = 0.0,
                lng = 0.0,
                name = "Detecting Location...",
                isOverrideActive = false,
                isSet = false
            )
        }
    )
    val location: StateFlow<LocationData> = _location

    fun updateLocation(lat: Double, lng: Double, name: String, isOverride: Boolean) {
        _location.value = LocationData(lat, lng, name, isOverride, true)
        
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
        val isOverrideActive: Boolean,
        val isSet: Boolean
    )
}
