package com.adhan.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class AdhanApplication : Application(), androidx.work.Configuration.Provider {
    
    @javax.inject.Inject
    lateinit var workerFactory: androidx.hilt.work.HiltWorkerFactory

    override val workManagerConfiguration: androidx.work.Configuration
        get() = androidx.work.Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
        scheduleDailyMaintenance()
    }

    private fun scheduleDailyMaintenance() {
        val workRequest = androidx.work.PeriodicWorkRequestBuilder<com.adhan.app.infra.DailySchedulerWorker>(
            12, java.util.concurrent.TimeUnit.HOURS
        ).setConstraints(
            androidx.work.Constraints.Builder()
                .setRequiresBatteryNotLow(true)
                .build()
        ).build()

        androidx.work.WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "DailyAdhanScheduler",
            androidx.work.ExistingPeriodicWorkPolicy.UPDATE,
            workRequest
        )
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
