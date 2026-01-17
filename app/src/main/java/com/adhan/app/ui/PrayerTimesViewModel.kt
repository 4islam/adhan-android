package com.adhan.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adhan.app.domain.models.HijriCalendar
import com.adhan.app.domain.models.PrayerTimesCalculator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

data class PrayerTimesState(
    val prayerTimes: List<PrayerTimesCalculator.CombinedPrayerInfo> = emptyList(),
    val astronomicalEvents: List<PrayerTimesCalculator.CombinedPrayerInfo> = emptyList(),
    val locationName: String = "London, UK",
    val latitude: Double = 51.5074,
    val longitude: Double = -0.1278,
    val hijriDate: String = "",
    val gregorianDate: String = "",
    val nextPrayerName: String = "",
    val nextPrayerTime: String = "",
    val currentTime: Date = Date(),
    val deviceHeading: Float = 0f,
    val isOverrideActive: Boolean = false,
    val calcMethod: Int = PrayerTimesCalculator.Ahmadiyya,
    val asrJuristic: Int = PrayerTimesCalculator.Shafii,
    val isAudioEnabled: Boolean = true,
    val isTahajjudEnabled: Boolean = false,
    val tahajjudOffset: Int = 60, // minutes before Fajr
    val combiningThreshold: Int = 90, // minutes
    val use12HourFormat: Boolean = true,
    val adhanSounds: Map<String, String> = emptyMap(), // Maps prayer name to URI string
    val rawPrayerTimes: List<PrayerTimesCalculator.CombinedPrayerInfo> = emptyList() // 24h for logic
)

@HiltViewModel
class PrayerTimesViewModel @Inject constructor(
    private val alarmManager: com.adhan.app.infra.PrayerAlarmManager,
    private val application: android.app.Application
) : ViewModel() {
    private val prefs = application.getSharedPreferences("adhan_prefs", android.content.Context.MODE_PRIVATE)
    private val _uiState = MutableStateFlow(PrayerTimesState())
    val uiState: StateFlow<PrayerTimesState> = _uiState.asStateFlow()

    init {
        loadSettings()
        updateTimes()
        startClock()
    }

    private fun loadSettings() {
        val calcMethod = prefs.getInt("calc_method", PrayerTimesCalculator.Ahmadiyya)
        val asrJuristic = prefs.getInt("asr_juristic", PrayerTimesCalculator.Shafii)
        val isAudioEnabled = prefs.getBoolean("audio_enabled", true)
        val isTahajjudEnabled = prefs.getBoolean("tahajjud_enabled", false)
        val tahajjudOffset = prefs.getInt("tahajjud_offset", 60)
        val combiningThreshold = prefs.getInt("combining_threshold", 90)
        val use12HourFormat = prefs.getBoolean("use_12_hour", true)
        
        val adhanSounds = mutableMapOf<String, String>()
        listOf("Fajr", "Dhuhr", "Asr", "Maghrib", "Isha").forEach { prayer ->
            prefs.getString("adhan_sound_$prayer", null)?.let { adhanSounds[prayer] = it }
        }

        _uiState.value = _uiState.value.copy(
            calcMethod = calcMethod,
            asrJuristic = asrJuristic,
            isAudioEnabled = isAudioEnabled,
            isTahajjudEnabled = isTahajjudEnabled,
            tahajjudOffset = tahajjudOffset,
            combiningThreshold = combiningThreshold,
            use12HourFormat = use12HourFormat,
            adhanSounds = adhanSounds
        )
    }

    fun setCalcMethod(method: Int) {
        _uiState.value = _uiState.value.copy(calcMethod = method)
        prefs.edit().putInt("calc_method", method).apply()
        updateTimes()
    }

    fun setAsrMethod(method: Int) {
        _uiState.value = _uiState.value.copy(asrJuristic = method)
        prefs.edit().putInt("asr_juristic", method).apply()
        updateTimes()
    }

    fun setAudioEnabled(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(isAudioEnabled = enabled)
        prefs.edit().putBoolean("audio_enabled", enabled).apply()
    }

    fun setTahajjudEnabled(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(isTahajjudEnabled = enabled)
        prefs.edit().putBoolean("tahajjud_enabled", enabled).apply()
        updateTimes()
    }

    fun setTahajjudOffset(offset: Int) {
        _uiState.value = _uiState.value.copy(tahajjudOffset = offset)
        prefs.edit().putInt("tahajjud_offset", offset).apply()
        updateTimes()
    }

    fun setCombiningThreshold(threshold: Int) {
        _uiState.value = _uiState.value.copy(combiningThreshold = threshold)
        prefs.edit().putInt("combining_threshold", threshold).apply()
        updateTimes()
    }

    fun setUse12HourFormat(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(use12HourFormat = enabled)
        prefs.edit().putBoolean("use_12_hour", enabled).apply()
        updateTimes()
    }

    fun setAdhanSound(prayerName: String, uri: String?) {
        val currentSounds = _uiState.value.adhanSounds.toMutableMap()
        if (uri == null) {
            currentSounds.remove(prayerName)
            prefs.edit().remove("adhan_sound_$prayerName").apply()
        } else {
            currentSounds[prayerName] = uri
            prefs.edit().putString("adhan_sound_$prayerName", uri).apply()
        }
        _uiState.value = _uiState.value.copy(adhanSounds = currentSounds)
    }

    private fun startClock() {
        viewModelScope.launch {
            while (true) {
                _uiState.value = _uiState.value.copy(currentTime = Date())
                updateNextPrayer()
                delay(1000)
            }
        }
    }

    fun overrideLocation(lat: Double, lng: Double) {
        _uiState.value = _uiState.value.copy(
            latitude = lat,
            longitude = lng,
            locationName = "Custom Location (%.4f, %.4f)".format(lat, lng),
            isOverrideActive = true
        )
        updateTimes()
    }

    fun updateHeading(heading: Float) {
        _uiState.value = _uiState.value.copy(deviceHeading = heading)
    }


    private fun updateTimes() {
        val calculator = PrayerTimesCalculator()
        calculator.setCalcMethod(_uiState.value.calcMethod)
        calculator.setAsrMethod(_uiState.value.asrJuristic)
        calculator.setCombiningThreshold(_uiState.value.combiningThreshold)
        
        val date = _uiState.value.currentTime
        val lat = _uiState.value.latitude
        val lng = _uiState.value.longitude
        
        val allTimesMap = calculator.getCombinedPrayerTimes(date, lat, lng)
        val moonTimes = calculator.getMoonTimes(date, lat, lng)
        val hijri = HijriCalendar.fromDate(date)
        val hijriString = "${hijri.day} ${hijri.monthName} ${hijri.year} AH"
        val gregorianString = SimpleDateFormat("d MMMM yyyy", Locale.ENGLISH).format(date)

        // Separate Prayer Times and Astronomical Events
        val prayerList = mutableListOf<PrayerTimesCalculator.CombinedPrayerInfo>()
        val astroList = mutableListOf<PrayerTimesCalculator.CombinedPrayerInfo>()
        
        // Tahajjud Calculation
        if (_uiState.value.isTahajjudEnabled) {
            val fajrInfo = allTimesMap.find { it.name == "Fajr" }
            if (fajrInfo != null) {
                val tahajjudTime = calculateTahajjudTime(fajrInfo.time, _uiState.value.tahajjudOffset)
                prayerList.add(PrayerTimesCalculator.CombinedPrayerInfo("Tahajjud", tahajjudTime))
            }
        }

        // Create explicit astronomical events list
        val sunrise = allTimesMap.find { it.name == "Sunrise" }
        val sunset = allTimesMap.find { it.name == "Sunset" }
        val dhuhr = allTimesMap.find { it.name == "Dhuhr" || it.name == "Dhuhr/Asr" }

        if (sunrise != null) astroList.add(sunrise)
        if (dhuhr != null) astroList.add(dhuhr.copy(name = "Solar Noon", isCombined = false, time = dhuhr.time))
        if (sunset != null) astroList.add(sunset)
        
        // Add Moon timings
        moonTimes.forEach { (name, time) ->
            astroList.add(PrayerTimesCalculator.CombinedPrayerInfo(name, time))
        }

        val mainCal = Calendar.getInstance().apply { time = date }
        val isFriday = mainCal.get(Calendar.DAY_OF_WEEK) == Calendar.FRIDAY

        // Filter for actual prayers only
        val prayersOnly = allTimesMap.filter { 
            it.name !in listOf("Sunrise", "Sunset", "Solar Noon") 
        }

        prayersOnly.forEach { info ->
            var finalInfo = info
            if ((info.name == "Dhuhr" || info.name == "Dhuhr/Asr") && isFriday) {
                finalInfo = info.copy(name = "Jummah (or Dhuhr)")
            }
            prayerList.add(finalInfo)
        }
        
        prayerList.sortBy { it.time }

        // Format for display: Filter out astronomical repetitions (Rise/Noon/Set)
        // because they are now uniquely displayed in the AstroRow panel.
        val displayPrayerList = prayerList
            .filter { info -> 
                info.name !in listOf("Sunrise", "Sunset", "Solar Noon", "Dhuhr", "Dhuhr/Asr", "Jummah (or Dhuhr)") 
            }
            .map { it.copy(time = formatDisplayTime(it.time)) }
        val displayAstroList = astroList.map { it.copy(time = formatDisplayTime(it.time)) }

        _uiState.value = _uiState.value.copy(
            prayerTimes = displayPrayerList,
            astronomicalEvents = displayAstroList,
            rawPrayerTimes = prayerList,
            hijriDate = hijriString,
            gregorianDate = gregorianString
        )
        
        // Schedule alarms for prayers only
        alarmManager.scheduleAlarms(prayerList)
        
        updateNextPrayer()
    }

    private fun calculateTahajjudTime(fajrTime: String, offsetMinutes: Int): String {
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        val date = sdf.parse(fajrTime) ?: return fajrTime
        val cal = Calendar.getInstance()
        cal.time = date
        cal.add(Calendar.MINUTE, -offsetMinutes)
        return sdf.format(cal.time)
    }

    private fun formatDisplayTime(time24: String): String {
        if (!_uiState.value.use12HourFormat) return time24
        if (time24 == PrayerTimesCalculator.InvalidTime) return time24
        return try {
            val sdf24 = SimpleDateFormat("HH:mm", Locale.getDefault())
            val sdf12 = SimpleDateFormat("h:mm a", Locale.getDefault())
            val date = sdf24.parse(time24)
            sdf12.format(date!!).lowercase()
        } catch (e: Exception) {
            time24
        }
    }

    private fun updateNextPrayer() {
        val now = _uiState.value.currentTime
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        val currentStr = sdf.format(now)

        var nextFound = false
        val prayerList = _uiState.value.rawPrayerTimes
        for (info in prayerList) {
            if (info.time > currentStr) {
                _uiState.value = _uiState.value.copy(
                    nextPrayerName = info.name,
                    nextPrayerTime = formatDisplayTime(info.time)
                )
                nextFound = true
                break
            }
        }
        
        if (!nextFound && prayerList.isNotEmpty()) {
            _uiState.value = _uiState.value.copy(
                nextPrayerName = prayerList[0].name,
                nextPrayerTime = formatDisplayTime(prayerList[0].time)
            )
        }
    }
}
