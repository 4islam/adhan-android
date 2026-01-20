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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import kotlinx.coroutines.Job

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
    val activePrayerName: String? = null,
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
    val rawPrayerTimes: List<PrayerTimesCalculator.CombinedPrayerInfo> = emptyList(), // 24h for logic
    val isLocationSet: Boolean = true,
    val nextPrayerDateLabel: String = ""
)

@HiltViewModel
class PrayerTimesViewModel @Inject constructor(
    private val alarmManager: com.adhan.app.infra.PrayerAlarmManager,
    private val repository: com.adhan.app.domain.LocationRepository,
    private val application: android.app.Application,
    private val logRepository: com.adhan.app.domain.LogRepository
) : ViewModel() {
    private val prefs = application.getSharedPreferences("adhan_prefs", android.content.Context.MODE_PRIVATE)
    private val _uiState = MutableStateFlow(PrayerTimesState())
    val uiState: StateFlow<PrayerTimesState> = _uiState.asStateFlow()

    val logs = MutableStateFlow<List<com.adhan.app.domain.LogEntry>>(emptyList())

    fun loadLogs() {
        viewModelScope.launch {
            logs.value = logRepository.getLogs()
        }
    }

    fun clearLogs() {
        viewModelScope.launch {
            logRepository.clearLogs()
            loadLogs()
        }
    }

    private var updateTimesJob: Job? = null
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    init {
        loadSettings()
        observeLocation()
        startClock()
    }

    private fun observeLocation() {
        viewModelScope.launch {
            repository.location.collect { data ->
                _uiState.value = _uiState.value.copy(
                    latitude = data.lat,
                    longitude = data.lng,
                    locationName = data.name,
                    isOverrideActive = data.isOverrideActive,
                    isLocationSet = data.isSet
                )
                updateTimes()
            }
        }
    }

    private fun loadSettings() {
        val calcMethod = prefs.getInt("calc_method", PrayerTimesCalculator.Ahmadiyya)
        val asrJuristic = prefs.getInt("asr_juristic", PrayerTimesCalculator.Shafii)
        val isAudioEnabled = prefs.getBoolean("audio_enabled", true)
        val isTahajjudEnabled = prefs.getBoolean("tahajjud_enabled", false)
        val tahajjudOffset = prefs.getInt("tahajjud_offset", 60)
        val combiningThreshold = prefs.getInt("combining_threshold", 90)
        val use12HourFormat = prefs.getBoolean("use_12_hour", true)

        val loadedSounds = mutableMapOf<String, String>()
        listOf("Fajr", "Dhuhr", "Asr", "Maghrib", "Isha").forEach { prayer ->
            val uri = prefs.getString("adhan_sound_$prayer", null)
            if (uri != null) loadedSounds[prayer] = uri
        }
        
        _uiState.value = _uiState.value.copy(
            calcMethod = calcMethod,
            asrJuristic = asrJuristic,
            isAudioEnabled = isAudioEnabled,
            isTahajjudEnabled = isTahajjudEnabled,
            tahajjudOffset = tahajjudOffset,
            combiningThreshold = combiningThreshold,
            use12HourFormat = use12HourFormat,
            adhanSounds = loadedSounds
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
                val now = Date()
                _uiState.value = _uiState.value.copy(currentTime = now)
                updateNextPrayer(now)
                delay(1000)
            }
        }
    }

    fun overrideLocation(lat: Double, lng: Double, name: String? = null) {
        val finalName = name ?: "Location (%.2f, %.2f)".format(lat, lng)
        repository.updateLocation(lat, lng, finalName, true)
    }

    fun testAdhan(): Boolean {
        val now = Calendar.getInstance()
        now.add(Calendar.MINUTE, 2)
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        val testTime = sdf.format(now.time)
        
        val testPrayerTimestamp = now.timeInMillis
        val success = alarmManager.scheduleExactAlarms(listOf("Test Adhan" to testPrayerTimestamp))
        
        if (success) {
            // Update UI to show we scheduled it
            _uiState.value = _uiState.value.copy(
                nextPrayerName = "Test Adhan",
                nextPrayerTime = formatDisplayTime(testTime),
                nextPrayerDateLabel = ""
            )
        }
        return success
    }

    private var foregroundPlayer: androidx.media3.exoplayer.ExoPlayer? = null
    
    private val _isAdhanPlaying = kotlinx.coroutines.flow.MutableStateFlow(false)
    val isAdhanPlaying = _isAdhanPlaying.asStateFlow()

    fun playAdhanNow() {
        try {
            stopAdhan() // clear existing
            
            foregroundPlayer = androidx.media3.exoplayer.ExoPlayer.Builder(application).build().apply {
                val resourceName = "adhan_fajr" // Default to Fajr for test
                val rawResourceId = application.resources.getIdentifier(resourceName, "raw", application.packageName)
                
                if (rawResourceId != 0) {
                     val mediaItem = androidx.media3.common.MediaItem.fromUri("android.resource://${application.packageName}/$rawResourceId")
                     setMediaItem(mediaItem)
                     prepare()
                     play()
                     _isAdhanPlaying.value = true
                     viewModelScope.launch { logRepository.log("FOREGROUND TEST: Playing $resourceName") }
                     
                     addListener(object : androidx.media3.common.Player.Listener {
                         override fun onPlaybackStateChanged(playbackState: Int) {
                             if (playbackState == androidx.media3.common.Player.STATE_ENDED) {
                                 viewModelScope.launch { logRepository.log("FOREGROUND TEST: Playback Ended") }
                                 stopAdhan()
                             }
                         }
                         override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                              viewModelScope.launch { logRepository.log("FOREGROUND TEST ERROR: ${error.message}", true) }
                              stopAdhan()
                         }
                     })
                } else {
                    viewModelScope.launch { logRepository.log("FOREGROUND TEST ERROR: Resource $resourceName not found", true) }
                }
            }
        } catch (e: Exception) {
            viewModelScope.launch { logRepository.log("FOREGROUND TEST EXCEPTION: ${e.message}", true) }
            stopAdhan()
        }
    }
    
    fun stopAdhan() {
        foregroundPlayer?.release()
        foregroundPlayer = null
        _isAdhanPlaying.value = false
    }

    fun updateHeading(heading: Float) {
        _uiState.value = _uiState.value.copy(deviceHeading = heading)
    }

    fun openAudioOutputPicker() {
        try {
            val intent = android.content.Intent("com.android.settings.panel.action.MEDIA_OUTPUT").apply {
                putExtra("com.android.settings.panel.extra.PACKAGE_NAME", application.packageName)
                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            application.startActivity(intent)
        } catch (e: Exception) {
            // Fallback for older Android versions or if panel is not available
            val intent = android.content.Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS).apply {
                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            application.startActivity(intent)
        }
    }


    private fun updateTimes() {
        updateTimesJob?.cancel()
        updateTimesJob = viewModelScope.launch(Dispatchers.Default) {
            val state = _uiState.value
            val calculator = PrayerTimesCalculator()
            calculator.setCalcMethod(state.calcMethod)
            calculator.setAsrMethod(state.asrJuristic)
            calculator.setCombiningThreshold(state.combiningThreshold)
            
            val date = state.currentTime
            val lat = state.latitude
            val lng = state.longitude
            
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

        // Format for display: Inclusive list of prayers only
        val allowedPrayers = listOf("Tahajjud", "Fajr", "Dhuhr", "Dhuhr/Asr", "Jummah (or Dhuhr)", "Asr", "Maghrib", "Maghrib/Isha", "Isha")
        val displayPrayerList = prayerList
            .filter { it.name in allowedPrayers }
            .map { it.copy(time = formatDisplayTime(it.time)) }
        val displayAstroList = astroList.map { it.copy(time = formatDisplayTime(it.time)) }

            withContext(Dispatchers.Main) {
                _uiState.value = _uiState.value.copy(
                    prayerTimes = displayPrayerList,
                    astronomicalEvents = displayAstroList,
                    rawPrayerTimes = prayerList,
                    hijriDate = hijriString,
                    gregorianDate = gregorianString
                )
                
                updateNextPrayer(date)
            }


            // Calculate timestamps for Today
            val scheduleList = mutableListOf<Pair<String, Long>>()
            val todayCal = Calendar.getInstance().apply { time = date }
            
            prayerList.forEach { info ->
                val timeParts = info.time.split(":")
                if (timeParts.size == 2) {
                     val pCal = todayCal.clone() as Calendar
                     pCal.set(Calendar.HOUR_OF_DAY, timeParts[0].toInt())
                     pCal.set(Calendar.MINUTE, timeParts[1].toInt())
                     pCal.set(Calendar.SECOND, 0)
                     pCal.set(Calendar.MILLISECOND, 0)
                     scheduleList.add(info.name to pCal.timeInMillis)
                }
            }
            
            // Calculate timestamps for Tomorrow
            val tomorrowCal = Calendar.getInstance().apply { 
                time = date
                add(Calendar.DAY_OF_YEAR, 1)
            }
            val tomorrowDate = tomorrowCal.time
            val tomorrowTimes = calculator.getCombinedPrayerTimes(tomorrowDate, lat, lng)
            
            // Tahajjud for Tomorrow
             if (_uiState.value.isTahajjudEnabled) {
                val fajrInfo = tomorrowTimes.find { it.name == "Fajr" }
                if (fajrInfo != null) {
                    val tahajjudTime = calculateTahajjudTime(fajrInfo.time, _uiState.value.tahajjudOffset)
                    val tParts = tahajjudTime.split(":")
                    if (tParts.size == 2) {
                         val pCal = tomorrowCal.clone() as Calendar
                         pCal.set(Calendar.HOUR_OF_DAY, tParts[0].toInt())
                         pCal.set(Calendar.MINUTE, tParts[1].toInt())
                         pCal.set(Calendar.SECOND, 0)
                         pCal.set(Calendar.MILLISECOND, 0)
                         scheduleList.add("Tahajjud" to pCal.timeInMillis)
                    }
                }
            }
            
            tomorrowTimes.filter { it.name !in listOf("Sunrise", "Sunset", "Solar Noon") }.forEach { info ->
                var name = info.name
                val isTomorrowFri = tomorrowCal.get(Calendar.DAY_OF_WEEK) == Calendar.FRIDAY
                if ((name == "Dhuhr" || name == "Dhuhr/Asr") && isTomorrowFri) {
                    name = "Jummah (or Dhuhr)"
                }
                
                if (name in allowedPrayers) {
                    val timeParts = info.time.split(":")
                     if (timeParts.size == 2) {
                         val pCal = tomorrowCal.clone() as Calendar
                         pCal.set(Calendar.HOUR_OF_DAY, timeParts[0].toInt())
                         pCal.set(Calendar.MINUTE, timeParts[1].toInt())
                         pCal.set(Calendar.SECOND, 0)
                         pCal.set(Calendar.MILLISECOND, 0)
                         scheduleList.add(name to pCal.timeInMillis)
                    }
                }
            }

            // Move scheduling to background
            alarmManager.scheduleExactAlarms(scheduleList)
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

    private fun updateNextPrayer(now: Date) {
        val currentStr = timeFormat.format(now)
        val prayerList = _uiState.value.rawPrayerTimes
        if (prayerList.isEmpty()) return

        var nextFound = false
        val nowDate = timeFormat.parse(currentStr) ?: return
        
        for ((index, info) in prayerList.withIndex()) {
            if (info.time > currentStr) {
                var activeName: String? = null
                
                val diffMinutes = try {
                    val dateNext = timeFormat.parse(info.time)
                    if (dateNext != null) {
                        (dateNext.time - nowDate.time) / (60 * 1000)
                    } else {
                        Long.MAX_VALUE
                    }
                } catch (e: Exception) {
                    Long.MAX_VALUE
                }
                
                if (diffMinutes <= 15) {
                    activeName = info.name
                } else if (index > 0) {
                    activeName = prayerList[index - 1].name
                }
                
                // Compare before updating to avoid unnecessary recompositions
                val currentState = _uiState.value
                if (currentState.nextPrayerName != info.name || currentState.activePrayerName != activeName) {
                    _uiState.value = currentState.copy(
                        nextPrayerName = info.name,
                        nextPrayerTime = formatDisplayTime(info.time),
                        activePrayerName = activeName,
                        nextPrayerDateLabel = ""
                    )
                }
                nextFound = true
                break
            }
        }
        
        if (!nextFound) {
            val currentState = _uiState.value
            val ishaName = prayerList.last().name
            if (currentState.activePrayerName != ishaName) {
                _uiState.value = currentState.copy(
                    nextPrayerName = prayerList[0].name,
                    nextPrayerTime = formatDisplayTime(prayerList[0].time),
                    activePrayerName = ishaName,
                    nextPrayerDateLabel = "(Tomorrow)"
                )
            }
        }
    }
}
