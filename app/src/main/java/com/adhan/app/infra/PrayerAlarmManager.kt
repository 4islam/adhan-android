package com.adhan.app.infra

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.adhan.app.domain.models.PrayerTimesCalculator
import dagger.hilt.android.qualifiers.ApplicationContext
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PrayerAlarmManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun scheduleAlarms(times: List<PrayerTimesCalculator.CombinedPrayerInfo>) {
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        val now = Calendar.getInstance()

        times.forEach { info ->
            val prayerTime = Calendar.getInstance().apply {
                val parsedDate = sdf.parse(info.time) ?: return@forEach
                val prayerCal = Calendar.getInstance().apply { time = parsedDate }
                set(Calendar.HOUR_OF_DAY, prayerCal.get(Calendar.HOUR_OF_DAY))
                set(Calendar.MINUTE, prayerCal.get(Calendar.MINUTE))
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            if (prayerTime.after(now)) {
                scheduleAlarm(info.name, prayerTime.timeInMillis)
            }
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
    }
}
