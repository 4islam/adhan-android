package com.adhan.app.infra

import android.annotation.SuppressLint
import android.app.*
import android.content.Context
import android.content.Intent
import android.location.Geocoder
import android.os.*
import androidx.core.app.NotificationCompat
import com.adhan.app.MainActivity
import com.google.android.gms.location.*
import dagger.hilt.android.AndroidEntryPoint
import java.util.*
import javax.inject.Inject
import com.adhan.app.domain.LocationRepository

@AndroidEntryPoint
class LocationService : Service() {

    @Inject
    lateinit var repository: LocationRepository

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback

    private var lastUpdateTime: Long = 0
    private val NOTIFICATION_ID = 1002
    private val CHANNEL_ID = "location_updates"

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        
        createNotificationChannel()
        
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    updateLocationData(location.latitude, location.longitude)
                }
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Location Tracking",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun updateLocationData(lat: Double, lng: Double) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastUpdateTime < 3600000L) return // Not sooner than once an hour

        val lastLoc = repository.location.value
        val distance = FloatArray(1)
        android.location.Location.distanceBetween(lat, lng, lastLoc.lat, lastLoc.lng, distance)
        
        // Only update if significant change (> 2km) or if no name set (first run)
        if (distance[0] > 2000 || lastLoc.name == "London, UK") {
            lastUpdateTime = currentTime
            val geocoder = Geocoder(this, Locale.getDefault())
            val addresses = geocoder.getFromLocation(lat, lng, 1)
            val name = if (!addresses.isNullOrEmpty()) {
                addresses[0].locality ?: addresses[0].subAdminArea ?: "Current Location"
            } else {
                "Location (%.2f, %.2f)".format(lat, lng)
            }
            repository.updateLocation(lat, lng, name, false)
        }
    }

    @SuppressLint("MissingPermission")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Adhan Location Tracking")
            .setContentText("Monitoring location for accurate prayer times.")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()

        startForeground(NOTIFICATION_ID, notification)

        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_BALANCED_POWER_ACCURACY, 3600000L) // 1 hour
            .setMinUpdateDistanceMeters(2000f) // 2 km
            .build()

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )

        return START_STICKY
    }

    override fun onDestroy() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
