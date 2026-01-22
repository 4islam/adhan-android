package com.adhan.app.domain

import android.content.Context
import com.adhan.app.domain.models.PrayerTimesCalculator
import com.adhan.app.infra.LocationService
import com.adhan.app.infra.PrayerAlarmManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PrayerScheduler @Inject constructor(
    private val locationRepository: LocationRepository,
    private val prefsRepository: UserPreferencesRepository,
    private val alarmManager: PrayerAlarmManager,
    private val logRepository: LogRepository
) {

    suspend fun scheduleAlarmsForNext24Hours() {
        withContext(Dispatchers.IO) {
            try {
                // 1. Get Location
                val location = locationRepository.location.value
                if (!location.isSet) {
                    logRepository.log("Scheduler: Location not set, skipping.", true)
                    return@withContext
                }

                // 2. Calculator Setup
                val calculator = PrayerTimesCalculator()
                calculator.setCalcMethod(prefsRepository.getCalcMethod())
                calculator.setAsrMethod(prefsRepository.getAsrJuristic())
                calculator.setCombiningThreshold(prefsRepository.getCombiningThreshold())

                // 3. Calculate for Today
                val today = Date()
                val todayAlarms = calculateAlarmsForDay(today, location.lat, location.lng, calculator)

                // 4. Calculate for Tomorrow
                val tomorrowCal = Calendar.getInstance().apply { 
                    time = today
                    add(Calendar.DAY_OF_YEAR, 1) 
                }
                val tomorrowAlarms = calculateAlarmsForDay(tomorrowCal.time, location.lat, location.lng, calculator)

                // 5. Merge and Schedule
                val allAlarms = todayAlarms + tomorrowAlarms
                
                logRepository.log("Scheduler: Calculated ${allAlarms.size} alarms (Today+Tomorrow).")
                val success = alarmManager.scheduleExactAlarms(allAlarms)
                if (success) {
                    logRepository.log("Scheduler: Scheduler run successful.")
                } else {
                    logRepository.log("Scheduler: Failed to schedule alarms.", true)
                }

            } catch (e: Exception) {
                logRepository.log("Scheduler: Error scheduling alarms: ${e.message}", true)
                e.printStackTrace()
            }
        }
    }

    private fun calculateAlarmsForDay(
        date: Date, 
        lat: Double, 
        lng: Double, 
        calculator: PrayerTimesCalculator
    ): List<Pair<String, Long>> {
        val alarms = mutableListOf<Pair<String, Long>>()
        val combinedTimes = calculator.getCombinedPrayerTimes(date, lat, lng).toMutableList()

        // Apply Combining Logic
        applyShortNightCombining(combinedTimes, date, lat, lng, calculator)
        applyShortAsrCombining(combinedTimes)

        // Tahajjud
        if (prefsRepository.isTahajjudEnabled()) {
             combinedTimes.find { it.name == "Fajr" }?.let { fajr ->
                 val tahajjudOffset = prefsRepository.getTahajjudOffset()
                 // Parse fajr time string to helper or create offset logic
                 // Simplified: relying on existing logic or re-implementing?
                 // Re-implementing briefly for robustness:
                val tahajjudTime = calculateTahajjudTime(fajr.time, tahajjudOffset)
                alarms.add("Tahajjud" to getTimestamp(date, tahajjudTime))
             }
        }

        val isFriday = Calendar.getInstance().apply { time = date }.get(Calendar.DAY_OF_WEEK) == Calendar.FRIDAY

        combinedTimes.forEach { info ->
            if (info.name == "Sunrise" || info.name == "Sunset" || info.name == "Solar Noon") return@forEach
            
            var name = info.name
             if ((name == "Dhuhr" || name == "Dhuhr/Asr") && isFriday) {
                name = "Jummah (or Dhuhr)"
            }

            alarms.add(name to getTimestamp(date, info.time))
        }
        
        return alarms
    }

    private fun applyShortNightCombining(
        times: MutableList<PrayerTimesCalculator.CombinedPrayerInfo>,
        date: Date, lat: Double, lng: Double,
        calculator: PrayerTimesCalculator
    ) {
        if (!prefsRepository.isShortNightEnabled()) return
        val threshold = prefsRepository.getShortNightThreshold()

        val tomorrowCal = Calendar.getInstance().apply { 
            time = date
            add(Calendar.DAY_OF_YEAR, 1) 
        }
        val tomorrowTimes = calculator.getCombinedPrayerTimes(tomorrowCal.time, lat, lng)
        val tomorrowFajr = tomorrowTimes.find { it.name == "Fajr" }
        val todayIsha = times.find { it.name == "Isha" }
        val todayMaghrib = times.find { it.name == "Maghrib" }

        if (tomorrowFajr != null && todayIsha != null && todayMaghrib != null) {
            try {
                val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
                val isha = sdf.parse(todayIsha.time)
                val fajr = sdf.parse(tomorrowFajr.time)
                
                // Diff calculation logic (similar to VM)
                var diffMs = fajr.time - isha.time
                if (diffMs < 0) diffMs += 24 * 3600 * 1000 // Cross midnight
                
                val diffHours = diffMs / (1000.0 * 3600.0)
                if (diffHours < threshold) {
                     val mIdx = times.indexOfFirst { it.name == "Maghrib" }
                     val iIdx = times.indexOfFirst { it.name == "Isha" }
                     if (mIdx != -1 && iIdx != -1) {
                         times[mIdx] = times[mIdx].copy(name = "Maghrib/Isha")
                         times[iIdx] = times[iIdx].copy(name = "Maghrib/Isha", time = times[mIdx].time) // Use Maghrib time
                     }
                }
            } catch (e: Exception) { /* ignore */ }
        }
    }
    
    private fun applyShortAsrCombining(times: MutableList<PrayerTimesCalculator.CombinedPrayerInfo>) {
        if (!prefsRepository.isShortAsrEnabled()) return
        val threshold = prefsRepository.getShortAsrThreshold()
        
        val asr = times.find { it.name == "Asr" }
        val maghrib = times.find { it.name == "Maghrib" }
        
        if (asr != null && maghrib != null) {
              try {
                val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
                val aTime = sdf.parse(asr.time)
                val mTime = sdf.parse(maghrib.time)
                val diffMs = mTime.time - aTime.time
                val diffMins = diffMs / (1000.0 * 60.0)
                
                if (diffMins < threshold) {
                     val dIdx = times.indexOfFirst { it.name == "Dhuhr" }
                     val aIdx = times.indexOfFirst { it.name == "Asr" }
                     if (dIdx != -1 && aIdx != -1) {
                         times[dIdx] = times[dIdx].copy(name = "Dhuhr/Asr")
                         times[aIdx] = times[aIdx].copy(name = "Dhuhr/Asr", time = times[dIdx].time)
                     }
                }
            } catch (e: Exception) { /* ignore */ }
        }
    }

    private fun calculateTahajjudTime(fajrTime: String, offsetMinutes: Int): String {
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        val date = sdf.parse(fajrTime) ?: return fajrTime
        val cal = Calendar.getInstance()
        cal.time = date
        cal.add(Calendar.MINUTE, -offsetMinutes)
        return sdf.format(cal.time)
    }

    private fun getTimestamp(date: Date, timeStr: String): Long {
         val sdfTime = SimpleDateFormat("HH:mm", Locale.getDefault())
         val timeDate = sdfTime.parse(timeStr) ?: return 0L
         
         val calTime = Calendar.getInstance().apply { time = timeDate }
         
         val calFinal = Calendar.getInstance().apply {
             time = date
             set(Calendar.HOUR_OF_DAY, calTime.get(Calendar.HOUR_OF_DAY))
             set(Calendar.MINUTE, calTime.get(Calendar.MINUTE))
             set(Calendar.SECOND, 0)
             set(Calendar.MILLISECOND, 0)
         }
         return calFinal.timeInMillis
    }
}
