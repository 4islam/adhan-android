package com.adhan.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class AdhanApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val adhanChannel = android.app.NotificationChannel(
                "adhan_alerts",
                "Prayer Alerts",
                android.app.NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for prayer times and high-fidelity Adhan playback"
            }
            
            val manager = getSystemService(android.app.NotificationManager::class.java)
            manager.createNotificationChannel(adhanChannel)
        }
    }
}
