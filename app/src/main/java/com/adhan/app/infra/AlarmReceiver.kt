package com.adhan.app.infra

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import kotlinx.coroutines.launch

@dagger.hilt.android.AndroidEntryPoint
class AlarmReceiver : BroadcastReceiver() {

    @javax.inject.Inject
    lateinit var logRepository: com.adhan.app.domain.LogRepository

    override fun onReceive(context: Context, intent: Intent) {
        val prayerName = intent.getStringExtra("prayer_name") ?: "Prayer"
        android.util.Log.d("AlarmReceiver", "onReceive: $prayerName")
        
        // Wake up CPU to ensure service starts
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Adhan:AlarmReceiver")
        wakeLock.acquire(10000L) // 10 seconds should be enough to start service
        
        val pendingResult = goAsync()
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            try {
                logRepository.log("AlarmReceiver received alarm for: $prayerName")
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                pendingResult.finish()
            }
        }
        
        val serviceIntent = Intent(context, AdhanService::class.java).apply {
            putExtra("prayer_name", prayerName)
        }
        
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
        } catch (e: Exception) {
             kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                logRepository.log("Failed to start service: ${e.message}", true)
             }
        }
    }
}
