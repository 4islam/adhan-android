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
    
    fun isTahajjudVibrationEnabled(): Boolean = prefs.getBoolean("tahajjud_vibration_enabled", false)
    
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
}
