package com.adhan.app.infra

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.launch

@dagger.hilt.android.AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @javax.inject.Inject
    lateinit var prayerScheduler: com.adhan.app.domain.PrayerScheduler

    @javax.inject.Inject
    lateinit var logRepository: com.adhan.app.domain.LogRepository

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        android.util.Log.d("BootReceiver", "Received action: $action")
        
        if (action == Intent.ACTION_BOOT_COMPLETED || 
            action == Intent.ACTION_TIME_CHANGED || 
            action == Intent.ACTION_TIMEZONE_CHANGED ||
            action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                logRepository.log("BootReceiver: Triggering reschedule for action $action")
                // Wait briefly for system to stabilize?
                kotlinx.coroutines.delay(2000)
                prayerScheduler.scheduleAlarmsForNext24Hours()
            }
        }
    }
}
