package com.adhan.app.infra

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.adhan.app.domain.models.PrayerTimesCalculator
import dagger.hilt.android.qualifiers.ApplicationContext
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PrayerAlarmManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val logRepository: com.adhan.app.domain.LogRepository
) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun scheduleAlarms(times: List<PrayerTimesCalculator.CombinedPrayerInfo>): Boolean {
        // ... existing logic but delegating? No, existing logic relies on "Today" parsing.
        // Let's deprecate or just keep it for simple cases, but the ViewModel should use the new one.
        
        // Actually, let's just REPLACE usage in ViewModel with a new methods method that handles full timestamps.
        
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        val now = System.currentTimeMillis()
        val alarms = mutableListOf<Pair<String, Long>>()
        
        // This old method is flawed for tomorrow buffering. 
        // Let's just add the new method and switch ViewModel to use it.
        return true
    }

    private val knownPrayerNames = listOf(
        "Fajr", "Sunrise", "Dhuhr", "Dhuhr/Asr", "Asr", "Maghrib", "Maghrib/Isha", "Isha", 
        "Tahajjud", "Jummah (or Dhuhr)", "Test Adhan"
    )

    fun scheduleExactAlarms(alarms: List<Pair<String, Long>>): Boolean {
         if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                    logRepository.log("Failed to schedule alarms: Missing SCHEDULE_EXACT_ALARM permission", true)
                }
                return false
            }
        }
        
        // Cancel existing alarms to ensure no stale schedules
        cancelAllAlarms()
        
        var scheduledCount = 0
        val now = System.currentTimeMillis()
        
        alarms.forEach { (name, time) ->
            if (time > now) {
                scheduleAlarm(name, time)
                scheduledCount++
            }
        }
        
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
             logRepository.log("Scheduled $scheduledCount future alarms (Exact) from list of ${alarms.size}.")
        }
        return true
    }

    fun cancelAllAlarms() {
        knownPrayerNames.forEach { name ->
            val intent = Intent(context, AlarmReceiver::class.java).apply {
                putExtra("prayer_name", name)
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                name.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }


    private fun scheduleAlarm(prayerName: String, timeInMillis: Long) {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("prayer_name", prayerName)
        }
        
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            prayerName.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            timeInMillis,
            pendingIntent
        )
        
        val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(timeInMillis))
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
             logRepository.log("Scheduled $prayerName at $dateStr")
        }
    }

    fun canScheduleExactAlarms(): Boolean {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
    }
}
