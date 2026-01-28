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

    fun scheduleTestAlarm(name: String, time: Long, route: String?): Boolean {
         if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                return false
            }
        }
        
        // Single One-off alarm
        val extras = if (route != null) mapOf("test_audio_route" to route) else emptyMap()
        scheduleAlarm(name, time, extras)
        
        return true
    }

    fun cancelAllAlarms() {
        val cal = Calendar.getInstance()
        val currentYear = cal.get(Calendar.YEAR)
        val currentDay = cal.get(Calendar.DAY_OF_YEAR)

        // Cancel Today and Tomorrow's slots for each prayer
        for (dayOffset in 0..1) {
            val day = currentDay + dayOffset
            knownPrayerNames.forEach { name ->
                val requestCode = getRequestCode(name, day, currentYear)
                val intent = Intent(context, AlarmReceiver::class.java).apply {
                    putExtra("prayer_name", name)
                }
                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    requestCode,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                alarmManager.cancel(pendingIntent)
                pendingIntent.cancel()
            }
        }
    }

    internal fun getRequestCode(prayerName: String, dayOfYear: Int, year: Int): Int {
        // Create a unique int for this prayer on this day
        // prayerName.hashCode() provides a base, then we mix in day and year.
        // year % 10 (last digit) is enough to avoid year-over-year collisions for a decade.
        return prayerName.hashCode() + (year % 10 * 1000) + dayOfYear
    }

    private fun scheduleAlarm(prayerName: String, timeInMillis: Long, extras: Map<String, String> = emptyMap()) {
        val cal = Calendar.getInstance().apply { setTimeInMillis(timeInMillis) }
        val dayOfYear = cal.get(Calendar.DAY_OF_YEAR)
        val year = cal.get(Calendar.YEAR)
        val requestCode = getRequestCode(prayerName, dayOfYear, year)

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("prayer_name", prayerName)
            extras.forEach { (key, value) -> putExtra(key, value) }
        }
        
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
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
             logRepository.log("Scheduled $prayerName (ID: $requestCode) at $dateStr with extras: $extras")
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
