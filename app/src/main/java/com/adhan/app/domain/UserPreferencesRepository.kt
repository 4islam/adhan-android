package com.adhan.app.domain

import android.content.Context
import android.content.SharedPreferences
import com.adhan.app.domain.models.PrayerTimesCalculator
import com.adhan.app.ui.FadeConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserPreferencesRepository @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences("adhan_prefs", Context.MODE_PRIVATE)

    fun getCalcMethod(): Int = prefs.getInt("calc_method", PrayerTimesCalculator.Ahmadiyya)
    
    fun getAsrJuristic(): Int = prefs.getInt("asr_juristic", PrayerTimesCalculator.Shafii)
    
    fun isTahajjudEnabled(): Boolean = prefs.getBoolean("tahajjud_enabled", false)
    
    fun getTahajjudOffset(): Int = prefs.getInt("tahajjud_offset", 60)
    
    fun isTahajjudAudioEnabled(): Boolean = prefs.getBoolean("tahajjud_audio_enabled", false)
    
    fun isTahajjudVibrationEnabled(): Boolean = prefs.getBoolean("tahajjud_vibration_enabled", true)
    
    fun getTahajjudSoundUri(): String? = prefs.getString("tahajjud_sound_uri", null)
    
    fun getCombiningThreshold(): Int = prefs.getInt("combining_threshold", 70)
    
    fun isShortNightEnabled(): Boolean = prefs.getBoolean("short_night_enabled", true)
    
    fun getShortNightThreshold(): Int = prefs.getInt("short_night_threshold", 9)
    
    fun isShortAsrEnabled(): Boolean = prefs.getBoolean("short_asr_enabled", true)
    
    fun getShortAsrThreshold(): Int = prefs.getInt("short_asr_threshold", 90)
    
    fun getAdhanVolume(): Int = prefs.getInt("adhan_volume", 80)
    
    fun getFadeConfigs(): Map<String, FadeConfig> {
        val fadeConfigs = mutableMapOf<String, FadeConfig>()
        val prayers = listOf("Fajr", "Dhuhr", "Asr", "Maghrib", "Isha")
        
        prayers.forEach { prayer ->
            val isFajr = prayer == "Fajr"
            val defaultDur = if (isFajr) 5 else 0
            val defaultVol = if (isFajr) 0f else 1.0f
            
            val dur = prefs.getInt("fade_duration_$prayer", defaultDur)
            val vol = prefs.getFloat("fade_vol_$prayer", defaultVol)
            fadeConfigs[prayer] = FadeConfig(dur, vol)
        }
        return fadeConfigs
    }
    
    fun isAdhanEnabled(prayer: String, dayOfWeek: Int = -1): Boolean {
        if (dayOfWeek != -1) {
            return prefs.getBoolean("adhan_enabled_${prayer}_$dayOfWeek", true)
        }
        // Legacy/Master fallback (or assume true if we migrated completely)
        // For robustness, if NO day is specified (legacy call), we return true ?? 
        // Actually, let's keep the master key as well? Or just return "Always true" and let the scheduler ask for strict days.
        // Scheduler will now ALWAYS pass a day. ViewModel might check "is ANY day enabled?"
        return prefs.getBoolean("adhan_enabled_$prayer", true)
    }
    
    fun setAdhanEnabled(prayer: String, enabled: Boolean) {
        // Master switch (optional use)
        prefs.edit().putBoolean("adhan_enabled_$prayer", enabled).apply()
        // Also toggle ALL days?
        val editor = prefs.edit()
        for (i in java.util.Calendar.SUNDAY..java.util.Calendar.SATURDAY) {
            editor.putBoolean("adhan_enabled_${prayer}_$i", enabled)
        }
        editor.apply()
    }
    
    fun setAdhanDayEnabled(prayer: String, dayOfWeek: Int, enabled: Boolean) {
        prefs.edit().putBoolean("adhan_enabled_${prayer}_$dayOfWeek", enabled).apply()
    }
    
    fun getAudioRoute(prayer: String, dayOfWeek: Int): String? {
        return prefs.getString("audio_route_${prayer}_$dayOfWeek", null)
    }
    
    fun setAudioRoute(prayer: String, dayOfWeek: Int, route: String?) {
        if (route == null) {
            prefs.edit().remove("audio_route_${prayer}_$dayOfWeek").apply()
        } else {
            prefs.edit().putString("audio_route_${prayer}_$dayOfWeek", route).apply()
        }
    }
}
