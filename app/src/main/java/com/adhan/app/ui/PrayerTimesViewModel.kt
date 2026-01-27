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

data class FadeConfig(
    val durationSeconds: Int = 0, 

    val initialVolume: Float = 1.0f
)

enum class SkyAnchor {
    Time, // Standard: maintain HH:mm
    Sunrise,
    SolarNoon,
    Sunset
}

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
    val isTahajjudAudioEnabled: Boolean = false,
    val isTahajjudVibrationEnabled: Boolean = false,
    val tahajjudSoundUri: String? = null,
    val tahajjudOffset: Int = 60, // minutes before Fajr
    val combiningThreshold: Int = 70, // minutes
    val use12HourFormat: Boolean = true,
    val adhanSounds: Map<String, String> = emptyMap(), // Maps prayer name to URI string
    val adhanNotificationEnabled: Map<String, Boolean> = emptyMap(),
    val adhanNotificationDays: Map<String, Map<Int, Boolean>> = emptyMap(), // Prayer -> (DayOfWeek -> Enabled)
    val rawPrayerTimes: List<PrayerTimesCalculator.CombinedPrayerInfo> = emptyList(), // 24h for logic
    val isLocationSet: Boolean = true,
    val nextPrayerDateLabel: String = "",
    val audioOutputDevices: List<String> = emptyList(),
    val selectedAudioRoute: String = "Default",
    val fadeConfigs: Map<String, FadeConfig> = emptyMap(),
    val isShortNightCombiningEnabled: Boolean = true,
    val shortNightThresholdHours: Int = 9, // Hours
    val isShortAsrCombiningEnabled: Boolean = true,
    val shortAsrThresholdMinutes: Int = 90,
    val adhanVolume: Int = 80, // Volume percentage 0-100
    val isLoading: Boolean = true,
    val loadingMessage: String = "Initializing...",
    val nextPrayerCountdown: String = "",
    val isAdhanPlaying: Boolean = false,
    val highLatitudeRule: Int = PrayerTimesCalculator.AngleBased,

    val manualOffsets: Map<String, Int> = emptyMap(), // Prayer Name -> Minutes
    val selectedDate: Date = Date(),
    val skyAnchor: SkyAnchor = SkyAnchor.Time
)

@HiltViewModel
class PrayerTimesViewModel @Inject constructor(
    private val alarmManager: com.adhan.app.infra.PrayerAlarmManager, 
    private val repository: com.adhan.app.domain.LocationRepository,
    private val application: android.app.Application,
    private val logRepository: com.adhan.app.domain.LogRepository,
    private val audioRouter: com.adhan.app.infra.AudioRouter,
    private val audioFader: com.adhan.app.infra.AudioFader,
    private val prayerScheduler: com.adhan.app.domain.PrayerScheduler,
    private val userPrefs: com.adhan.app.domain.UserPreferencesRepository,
    private val mediaRouterHelper: com.adhan.app.infra.MediaRouterHelper,
    private val playbackStateRepository: com.adhan.app.domain.PlaybackStateRepository
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
        
        // Observe Playback State
        viewModelScope.launch {
            playbackStateRepository.isPlaying.collect { isPlaying ->
                _uiState.value = _uiState.value.copy(isAdhanPlaying = isPlaying)
            }
        }
    }

    private fun observeLocation() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loadingMessage = "Detecting Location...")
            repository.location.collect { data ->
                _uiState.value = _uiState.value.copy(
                    latitude = data.lat,
                    longitude = data.lng,
                    locationName = data.name,
                    isOverrideActive = data.isOverrideActive,
                    isLocationSet = data.isSet,
                    isLoading = !data.isSet,
                    loadingMessage = if (data.isSet) "" else "Waiting for location..."
                )
                if (data.isSet) updateTimes()
            }
        }
    }

    private fun loadSettings() {
        val calcMethod = userPrefs.getCalcMethod()
        val asrJuristic = userPrefs.getAsrJuristic()
        val isAudioEnabled = prefs.getBoolean("audio_enabled", true)
        val isTahajjudEnabled = userPrefs.isTahajjudEnabled()
        val isTahajjudAudioEnabled = userPrefs.isTahajjudAudioEnabled()
        val isTahajjudVibrationEnabled = userPrefs.isTahajjudVibrationEnabled()
        val tahajjudSoundUri = userPrefs.getTahajjudSoundUri()
        val tahajjudOffset = userPrefs.getTahajjudOffset()
        val combiningThreshold = userPrefs.getCombiningThreshold()
        val use12HourFormat = prefs.getBoolean("use_12_hour", true)

        val loadedSounds = mutableMapOf<String, String>()
        val loadedNotifications = mutableMapOf<String, Boolean>()
        val loadedDays = mutableMapOf<String, MutableMap<Int, Boolean>>()
        
        listOf("Fajr", "Dhuhr", "Asr", "Maghrib", "Isha").forEach { prayer ->
            val uri = prefs.getString("adhan_sound_$prayer", null)
            if (uri != null) loadedSounds[prayer] = uri
            loadedNotifications[prayer] = userPrefs.isAdhanEnabled(prayer)
            
            val daysMap = mutableMapOf<Int, Boolean>()
            for (day in java.util.Calendar.SUNDAY..java.util.Calendar.SATURDAY) {
                daysMap[day] = userPrefs.isAdhanEnabled(prayer, day)
            }
            loadedDays[prayer] = daysMap
        }
        
        val fadeConfigs = userPrefs.getFadeConfigs()
        
        val shortNightEnabled = userPrefs.isShortNightEnabled()
        val shortNightThreshold = userPrefs.getShortNightThreshold()
        
        val shortAsrEnabled = userPrefs.isShortAsrEnabled()
        val shortAsrThreshold = userPrefs.getShortAsrThreshold()
        

        
        val adhanVolume = userPrefs.getAdhanVolume()
        
        val highLatitudeRule = prefs.getInt("high_latitude_rule", PrayerTimesCalculator.AngleBased)
        
        val offsets = mutableMapOf<String, Int>()
        listOf("Fajr", "Sunrise", "Dhuhr", "Asr", "Maghrib", "Isha").forEach { prayer ->
            offsets[prayer] = prefs.getInt("offset_$prayer", 0)
        }

        _uiState.value = _uiState.value.copy(
            calcMethod = calcMethod,
            asrJuristic = asrJuristic,
            isAudioEnabled = isAudioEnabled,
            isTahajjudEnabled = isTahajjudEnabled,
            isTahajjudAudioEnabled = isTahajjudAudioEnabled,
            isTahajjudVibrationEnabled = isTahajjudVibrationEnabled,
            tahajjudSoundUri = tahajjudSoundUri,
            tahajjudOffset = tahajjudOffset,
            combiningThreshold = combiningThreshold,
            use12HourFormat = use12HourFormat,
            adhanSounds = loadedSounds,
            adhanNotificationEnabled = loadedNotifications,
            adhanNotificationDays = loadedDays,
            selectedAudioRoute = prefs.getString("selected_audio_route", "Default") ?: "Default",
            fadeConfigs = fadeConfigs,
            isShortNightCombiningEnabled = shortNightEnabled,
            shortNightThresholdHours = shortNightThreshold,
            isShortAsrCombiningEnabled = shortAsrEnabled,
            shortAsrThresholdMinutes = shortAsrThreshold,

            adhanVolume = adhanVolume,
            highLatitudeRule = highLatitudeRule,
            manualOffsets = offsets
        )
        refreshAudioDevices()
    }

    fun setShortAsrCombiningEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("short_asr_enabled", enabled).apply()
        _uiState.value = _uiState.value.copy(isShortAsrCombiningEnabled = enabled)
        updateTimes()
    }

    fun setShortAsrThreshold(minutes: Int) {
        prefs.edit().putInt("short_asr_threshold", minutes).apply()
        _uiState.value = _uiState.value.copy(shortAsrThresholdMinutes = minutes)
        updateTimes()
    }
    
    fun setAdhanVolume(volume: Int) {
        prefs.edit().putInt("adhan_volume", volume).apply()
        _uiState.value = _uiState.value.copy(adhanVolume = volume)
    }
    
    fun setShortNightCombiningEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("short_night_enabled", enabled).apply()
        _uiState.value = _uiState.value.copy(isShortNightCombiningEnabled = enabled)
        updateTimes()
    }

    fun setShortNightThreshold(hours: Int) {
        prefs.edit().putInt("short_night_threshold", hours).apply()
        _uiState.value = _uiState.value.copy(shortNightThresholdHours = hours)
        updateTimes()
    }
    
    fun setFadeConfig(prayer: String, duration: Int? = null, volume: Float? = null) {
        val currentMap = _uiState.value.fadeConfigs.toMutableMap()
        val currentConfig = currentMap[prayer] ?: FadeConfig()
        
        val newDuration = duration?.coerceIn(0, 30) ?: currentConfig.durationSeconds
        val newVol = volume?.coerceIn(0f, 1f) ?: currentConfig.initialVolume
        
        currentMap[prayer] = FadeConfig(newDuration, newVol)
        
        prefs.edit()
            .putInt("fade_duration_$prayer", newDuration)
            .putFloat("fade_vol_$prayer", newVol)
            .apply()
            
        _uiState.value = _uiState.value.copy(fadeConfigs = currentMap)
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
    
    fun setTahajjudAudioEnabled(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(isTahajjudAudioEnabled = enabled)
        prefs.edit().putBoolean("tahajjud_audio_enabled", enabled).apply()
    }
    
    fun setTahajjudVibrationEnabled(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(isTahajjudVibrationEnabled = enabled)
        prefs.edit().putBoolean("tahajjud_vibration_enabled", enabled).apply()
    }
    
    fun setTahajjudSoundUri(uri: String?) {
        _uiState.value = _uiState.value.copy(tahajjudSoundUri = uri)
        if (uri == null) {
            prefs.edit().remove("tahajjud_sound_uri").apply()
        } else {
            prefs.edit().putString("tahajjud_sound_uri", uri).apply()
        }
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

    fun setHighLatitudeRule(rule: Int) {
        prefs.edit().putInt("high_latitude_rule", rule).apply()
        _uiState.value = _uiState.value.copy(highLatitudeRule = rule)
        updateTimes()
    }
    
    fun setManualOffset(prayer: String, minutes: Int) {
        prefs.edit().putInt("offset_$prayer", minutes).apply()
        val current = _uiState.value.manualOffsets.toMutableMap()
        current[prayer] = minutes
        _uiState.value = _uiState.value.copy(manualOffsets = current)
        updateTimes()
    }

    private var foregroundPlayer: androidx.media3.exoplayer.ExoPlayer? = null
    
    fun playAdhanNow(testRoute: String? = null) {
        val intent = android.content.Intent(application, com.adhan.app.infra.AdhanService::class.java).apply {
            putExtra("prayer_name", "Test Adhan (Foreground)")
            if (testRoute != null && testRoute != "Global Default") {
                putExtra("test_audio_route", testRoute)
            }
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            application.startForegroundService(intent)
        } else {
            application.startService(intent)
        }
        _uiState.value = _uiState.value.copy(isAdhanPlaying = true)
    }

    fun stopAdhan() {
        // Stop foreground test player if any
        foregroundPlayer?.release()
        foregroundPlayer = null
        
        // Stop Service
        val intent = android.content.Intent(application, com.adhan.app.infra.AdhanService::class.java).apply {
            action = "com.adhan.app.action.STOP"
        }
        application.startService(intent)
        
        // Optimistic UI update (Service will also update repo)
        _uiState.value = _uiState.value.copy(isAdhanPlaying = false)
    }

    fun setAdhanNotificationEnabled(prayer: String, enabled: Boolean) {
        // Toggle Master
        val currentMap = _uiState.value.adhanNotificationEnabled.toMutableMap()
        currentMap[prayer] = enabled
        userPrefs.setAdhanEnabled(prayer, enabled)
        
        // Update Days Map to reflect "Select All" / "Select None" logic
        val currentDays = _uiState.value.adhanNotificationDays.toMutableMap()
        val daysForPrayer = currentDays[prayer]?.toMutableMap() ?: mutableMapOf()
        for (day in java.util.Calendar.SUNDAY..java.util.Calendar.SATURDAY) {
            daysForPrayer[day] = enabled
        }
        currentDays[prayer] = daysForPrayer
        
        _uiState.value = _uiState.value.copy(
            adhanNotificationEnabled = currentMap,
            adhanNotificationDays = currentDays
        )
        updateTimes() // Re-schedule alarms
    }

    fun setAdhanDayEnabled(prayer: String, dayOfWeek: Int, enabled: Boolean) {
        userPrefs.setAdhanDayEnabled(prayer, dayOfWeek, enabled)
        
        val currentDays = _uiState.value.adhanNotificationDays.toMutableMap()
        val daysForPrayer = currentDays[prayer]?.toMutableMap() ?: mutableMapOf()
        daysForPrayer[dayOfWeek] = enabled
        currentDays[prayer] = daysForPrayer
        
        _uiState.value = _uiState.value.copy(adhanNotificationDays = currentDays)
        updateTimes()
    }
    
    fun setDayAudioRoute(prayer: String, dayOfWeek: Int, route: String) {
        val finalRoute = if (route == "Global Default") null else route
        userPrefs.setAudioRoute(prayer, dayOfWeek, finalRoute)
        
        // Update local state if we were tracking it in the map (optional for UI reactivity if needed, 
        // but since it's a dialog fetch, maybe less critical? 
        // Better to have it in uiState if we want to show icons etc on the bubble).
        // For now, minimal implementation: just save.
    }
    
    fun applyConfigToAllDays(prayer: String, enabled: Boolean, route: String) {
        val finalRoute = if (route == "Global Default") null else route
        
        val currentDays = _uiState.value.adhanNotificationDays.toMutableMap()
        val daysForPrayer = currentDays[prayer]?.toMutableMap() ?: mutableMapOf()
        
        for (day in java.util.Calendar.SUNDAY..java.util.Calendar.SATURDAY) {
            userPrefs.setAdhanDayEnabled(prayer, day, enabled)
            userPrefs.setAudioRoute(prayer, day, finalRoute)
            daysForPrayer[day] = enabled
        }
        
        currentDays[prayer] = daysForPrayer
        _uiState.value = _uiState.value.copy(adhanNotificationDays = currentDays)
        updateTimes()
    }
    
    fun getDayAudioRoute(prayer: String, dayOfWeek: Int): String? {
        return userPrefs.getAudioRoute(prayer, dayOfWeek)
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

    fun updateHeading(heading: Float) {
        _uiState.value = _uiState.value.copy(deviceHeading = heading)
    }

    fun openAudioOutputPicker() {
        // Handled in UI via selector
    }
    
    val mediaSelector: androidx.mediarouter.media.MediaRouteSelector
        get() = mediaRouterHelper.selector

    private var scanJob: Job? = null

    fun refreshAudioDevices() {
        mediaRouterHelper.startScanning()
        
        scanJob?.cancel()
        scanJob = viewModelScope.launch {
            mediaRouterHelper.availableRoutes.collect { networkRoutes ->
                try {
                    val devices = audioRouter.getAvailableDevices()
                    val allDevices = devices.map { "${audioRouter.getDeviceTypeName(it.type)} (${it.productName})" }.toMutableList()
                    
                    networkRoutes.forEach { route ->
                        allDevices.add("Network: ${route.name}")
                    }
                    
                    _uiState.value = _uiState.value.copy(audioOutputDevices = allDevices.distinct())
                } catch (e: Exception) {
                    logRepository.log("Error refreshing audio devices: ${e.message}")
                }
            }
        }
    }

    fun setSelectedAudioDevice(deviceName: String) {
        if (deviceName.startsWith("Network: ")) {
            val routeName = deviceName.removePrefix("Network: ")
            val routes = mediaRouterHelper.availableRoutes.value
            val match = routes.find { it.name == routeName }
            if (match != null) {
                mediaRouterHelper.selectRoute(match.id)
            }
        }
        
        _uiState.value = _uiState.value.copy(selectedAudioRoute = deviceName)
        prefs.edit().putString("selected_audio_route", deviceName).apply()
    }

    fun testAdhan(delaySeconds: Int = 10, testRoute: String? = null): Boolean {
        val now = Calendar.getInstance()
        now.add(Calendar.SECOND, delaySeconds)
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        val testTime = sdf.format(now.time)
        
        val testPrayerTimestamp = now.timeInMillis
        val finalRoute = if (testRoute == "Global Default") null else testRoute

        val success = alarmManager.scheduleTestAlarm("Test Adhan", testPrayerTimestamp, finalRoute)
        
        if (success) {
            _uiState.value = _uiState.value.copy(
                nextPrayerName = "Test Adhan (${delaySeconds}s)",
                nextPrayerTime = formatDisplayTime(testTime),
                nextPrayerDateLabel = ""
            )
        }
        return success
    }

    fun forceReschedule() {
        viewModelScope.launch {
            logRepository.log("MANUAL RESCHEDULE: User triggered alarm reset.")
            alarmManager.cancelAllAlarms()
            updateTimes()
        }
    }
    
    fun incrementDate(days: Int) {
        val calendar = Calendar.getInstance()
        calendar.time = _uiState.value.selectedDate
        
        // If anchored to simple time, just add days
        if (_uiState.value.skyAnchor == SkyAnchor.Time) {
            calendar.add(Calendar.DAY_OF_YEAR, days)
            _uiState.value = _uiState.value.copy(selectedDate = calendar.time)
        } else {
            // Move the date first, then recalculate time based on anchor
            calendar.add(Calendar.DAY_OF_YEAR, days)
            val newDate = calendar.time
            val time = calculateAnchorTime(_uiState.value.skyAnchor, newDate)
            _uiState.value = _uiState.value.copy(selectedDate = time)
        }
        updateTimes()
    }

    fun setSelectedDate(date: Long) {
        val newDateBase = Date(date)
        if (_uiState.value.skyAnchor == SkyAnchor.Time) {
             // Preserve time of day from parameter? Usually DatePicker returns midnight or set time.
             // If from DatePicker, it likely resets time. Ideally we preserve "Time of Day" from current view if anchored to Time?
             // But setSelectedDate comes from DatePicker which implies "Go to this date".
             // Let's just set it. If user wants specific time they scrub.
             _uiState.value = _uiState.value.copy(selectedDate = newDateBase)
        } else {
             val time = calculateAnchorTime(_uiState.value.skyAnchor, newDateBase)
             _uiState.value = _uiState.value.copy(selectedDate = time)
        }
        updateTimes()
    }
    
    fun setSkyTime(date: Date) {
        // Scrubbing automatically breaks anchor if it was solar based, or we treat it as temporary override?
        // User asked: "allow to peg... allowing user to change date while pegged... or to a time of day"
        // If checking a time, anchor becomes Time.
        _uiState.value = _uiState.value.copy(
            selectedDate = date,
            skyAnchor = SkyAnchor.Time // Scrubbing switches to Manual Time mode
        )
        updateTimes() // Update times/positions
    }
    
    fun setSkyAnchor(anchor: SkyAnchor) {
        // Switch anchor and immediately update time to match that anchor for current day
        val time = calculateAnchorTime(anchor, _uiState.value.selectedDate)
        _uiState.value = _uiState.value.copy(
            skyAnchor = anchor,
            selectedDate = time
        )
        updateTimes()
    }
    
    private fun calculateAnchorTime(anchor: SkyAnchor, date: Date): Date {
        if (anchor == SkyAnchor.Time) return date
        
        val state = _uiState.value
        // Provide calculator to get exact solar events for that day
        val calculator = PrayerTimesCalculator()
        calculator.setCalcMethod(state.calcMethod)
        calculator.setHighLatsMethod(state.highLatitudeRule) 
        
        // We need raw single day calculation
        val times = calculator.getPrayerTimes(date, state.latitude, state.longitude)
        // Times are strings HH:mm. We need to parse relevant ones.
        // indices: 0 Fajr, 1 Sunrise, 2 Dhuhr (Noon ish), 3 Asr, 4 Sunset, 5 Maghrib, 6 Isha
        
        val timeStr = when(anchor) {
            SkyAnchor.Sunrise -> times[1]
            SkyAnchor.SolarNoon -> times[2] // Dhuhr is essentially solar noon
            SkyAnchor.Sunset -> times[4]
            else -> return date // Should not happen
        }
        
        if (timeStr == PrayerTimesCalculator.InvalidTime) return date
        
        return try {
            val parts = timeStr.split(":")
            val cal = Calendar.getInstance()
            cal.time = date
            cal.set(Calendar.HOUR_OF_DAY, parts[0].toInt())
            cal.set(Calendar.MINUTE, parts[1].toInt())
            cal.set(Calendar.SECOND, 0)
            cal.time
        } catch (e: Exception) {
            date
        }
    }

    fun jumpToToday() {
        _uiState.value = _uiState.value.copy(selectedDate = Date(), skyAnchor = SkyAnchor.Time)
        updateTimes()
    }

    private fun updateTimes() {
        updateTimesJob?.cancel()
        updateTimesJob = viewModelScope.launch(Dispatchers.Default) {
            val state = _uiState.value
            val calculator = PrayerTimesCalculator()
            calculator.setCalcMethod(state.calcMethod)
            calculator.setAsrMethod(state.asrJuristic)
            calculator.setCombiningThreshold(state.combiningThreshold)
            calculator.setHighLatsMethod(state.highLatitudeRule)
            
            // Use selectedDate for calculations
            val date = state.selectedDate
            val lat = state.latitude
            val lng = state.longitude
            
            val baseTimes = calculator.getCombinedPrayerTimes(date, lat, lng).toMutableList()
            val allTimesMap = mutableListOf<PrayerTimesCalculator.CombinedPrayerInfo>()
            
            // Apply Manual Offsets
            val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
            
            baseTimes.forEach { info ->
                var finalTime = info.time
                val offset = state.manualOffsets[info.name] ?: 0
                val fixedDhuhrOffset = if (info.name == "Dhuhr") 10 else 0 // Keep existing Dhuhr fix
                val totalOffset = offset + fixedDhuhrOffset
                
                if (totalOffset != 0 && finalTime != PrayerTimesCalculator.InvalidTime) {
                    try {
                        val d = sdf.parse(finalTime)
                        if (d != null) {
                            val newTime = d.time + (totalOffset * 60 * 1000)
                            finalTime = sdf.format(Date(newTime))
                        }
                    } catch (e: Exception) { }
                }
                allTimesMap.add(info.copy(time = finalTime))
            }
            
            // Short Night Combining Logic
            if (state.isShortNightCombiningEnabled) {
                val tomorrowCal = Calendar.getInstance().apply {
                    time = date
                    add(Calendar.DAY_OF_YEAR, 1)
                }
                val tomorrowTimes = calculator.getCombinedPrayerTimes(tomorrowCal.time, lat, lng)
                val tomorrowFajr = tomorrowTimes.find { it.name == "Fajr" }
                val todayIsha = allTimesMap.find { it.name == "Isha" }
                val todayMaghrib = allTimesMap.find { it.name == "Maghrib" }

                if (tomorrowFajr != null && todayIsha != null && todayMaghrib != null) {
                    try {
                        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
                        val ishaTime = sdf.parse(todayIsha.time)
                        val fajrTime = sdf.parse(tomorrowFajr.time)
                        
                        if (ishaTime != null && fajrTime != null) {
                            val fajrMs = fajrTime.time + (24 * 60 * 60 * 1000)
                            val ishaMs = ishaTime.time
                            val diffHours = (fajrMs - ishaMs) / (1000.0 * 60 * 60)
                            
                            if (diffHours < state.shortNightThresholdHours) {
                                val combinedName = "Maghrib/Isha"
                                val combinedTime = todayMaghrib.time
                                
                                val mIndex = allTimesMap.indexOfFirst { it.name == "Maghrib" }
                                if (mIndex != -1) {
                                    allTimesMap[mIndex] = todayMaghrib.copy(name = combinedName, time = combinedTime)
                                }
                                
                                val iIndex = allTimesMap.indexOfFirst { it.name == "Isha" }
                                if (iIndex != -1) {
                                    allTimesMap[iIndex] = todayIsha.copy(name = combinedName, time = combinedTime)
                                }
                            }
                        }
                    } catch (e: Exception) { }
                }
            }

            // Short Asr Window Combining (Dhuhr + Asr if Asr->Maghrib < Threshold)
            if (state.isShortAsrCombiningEnabled) {
                val todayDhuhr = allTimesMap.find { it.name == "Dhuhr" }
                val todayAsr = allTimesMap.find { it.name == "Asr" }
                val maghribForAsr = allTimesMap.find { it.name == "Maghrib" }
                
                if (todayDhuhr != null && todayAsr != null && maghribForAsr != null) {
                    try {
                         val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
                         val asrDate = sdf.parse(todayAsr.time)
                         val magDate = sdf.parse(maghribForAsr.time)
                         
                         if (asrDate != null && magDate != null) {
                             val diffMs = magDate.time - asrDate.time
                             val diffMinutes = diffMs / (1000 * 60)
                             
                             if (diffMinutes < state.shortAsrThresholdMinutes) {
                                  val combinedName = "Dhuhr/Asr"
                                  val combinedTime = todayDhuhr.time
                                  
                                  val dIndex = allTimesMap.indexOfFirst { it.name == "Dhuhr" }
                                  if (dIndex != -1) allTimesMap[dIndex] = todayDhuhr.copy(name = combinedName, time = combinedTime)
                                  
                                  val aIndex = allTimesMap.indexOfFirst { it.name == "Asr" }
                                  if (aIndex != -1) allTimesMap[aIndex] = todayAsr.copy(name = combinedName, time = combinedTime)
                             }
                         }
                    } catch (e: Exception) { }
                }
            }

            val moonTimes = calculator.getMoonTimes(date, lat, lng)
            val hijri = HijriCalendar.fromDate(date)
            val hijriString = "${hijri.day} ${hijri.monthName} ${hijri.year} AH"
            val gregorianString = SimpleDateFormat("EEEE, d MMMM yyyy", Locale.ENGLISH).format(date)

            val prayerList = mutableListOf<PrayerTimesCalculator.CombinedPrayerInfo>()
            val astroList = mutableListOf<PrayerTimesCalculator.CombinedPrayerInfo>()
            
            if (_uiState.value.isTahajjudEnabled) {
                val fajrInfo = allTimesMap.find { it.name == "Fajr" }
                if (fajrInfo != null) {
                    val tahajjudTime = calculateTahajjudTime(fajrInfo.time, _uiState.value.tahajjudOffset)
                    prayerList.add(PrayerTimesCalculator.CombinedPrayerInfo("Tahajjud", tahajjudTime))
                }
            }

            val sunrise = allTimesMap.find { it.name == "Sunrise" }
            val sunset = allTimesMap.find { it.name == "Sunset" }
            val dhuhr = allTimesMap.find { it.name == "Dhuhr" || it.name == "Dhuhr/Asr" }

            if (sunrise != null) astroList.add(sunrise)
            if (dhuhr != null) astroList.add(dhuhr.copy(name = "Solar Noon", isCombined = false, time = dhuhr.time))
            if (sunset != null) astroList.add(sunset)
            
            moonTimes.forEach { (name, time) ->
                astroList.add(PrayerTimesCalculator.CombinedPrayerInfo(name, time))
            }

            val mainCal = Calendar.getInstance().apply { time = date }
            val isFriday = mainCal.get(Calendar.DAY_OF_WEEK) == Calendar.FRIDAY

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
                
                updateNextPrayer(date) // Pass the selected date for context
            }

            prayerScheduler.scheduleAlarmsForNext24Hours()
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

    private fun isSameDay(date1: Date, date2: Date): Boolean {
        val cal1 = Calendar.getInstance().apply { time = date1 }
        val cal2 = Calendar.getInstance().apply { time = date2 }
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
               cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    private fun updateNextPrayer(selectedDate: Date) {
        val now = Date()
        val isToday = isSameDay(selectedDate, now)

        val prayerList = _uiState.value.rawPrayerTimes
        if (prayerList.isEmpty()) return

        var nextName = ""
        var nextTime = ""
        var countdown = ""
        var dateLabel = ""
        var activeName: String? = null

        if (isToday) {
            val currentStr = timeFormat.format(now)
            val nowDate = timeFormat.parse(currentStr) ?: return

            var nextFound = false
            for ((index, info) in prayerList.withIndex()) {
                if (info.time > currentStr) {
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
                
                val currentState = _uiState.value
                val nowMs = nowDate.time
                val nextMs = try {
                    val dateNext = timeFormat.parse(info.time)
                    if (dateNext != null) dateNext.time else nowMs
                } catch (e: Exception) { nowMs }
                
                var diffMs = nextMs - nowMs
                if (diffMs < 0) diffMs += 24 * 60 * 60 * 1000 
                
                val hours = diffMs / (1000 * 60 * 60)
                val minutes = (diffMs / (1000 * 60)) % 60
                
                val countdownString = if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"

                if (currentState.nextPrayerName != info.name || 
                    currentState.activePrayerName != activeName || 
                    currentState.nextPrayerCountdown != countdownString) {
                    
                    _uiState.value = currentState.copy(
                        nextPrayerName = info.name,
                        nextPrayerTime = formatDisplayTime(info.time), 
                        nextPrayerCountdown = countdownString,
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
            val ishaName = prayerList.lastOrNull()?.name ?: ""
            val activeName = if (prayerList.isNotEmpty()) ishaName else null

            // Logic for "Coming up: Fajr Tomorrow"
            // We assume it's Fajr of next day, no calculation here for simplicity as per original code
             if (currentState.nextPrayerName != "Fajr" || currentState.activePrayerName != activeName) {
                _uiState.value = currentState.copy(
                    nextPrayerName = "Fajr",
                    nextPrayerTime = "Tomorrow", // Or actual time if we have next day
                    nextPrayerCountdown = "",
                    activePrayerName = activeName,
                    nextPrayerDateLabel = "Tomorrow"
                )
             }
        }
    } else {
        // Not Today: Just show static "Day Schedule"
        _uiState.value = _uiState.value.copy(
            nextPrayerName = "Schedule",
            nextPrayerTime = "",
            nextPrayerCountdown = "",
            activePrayerName = null,
            nextPrayerDateLabel = ""
        )
    }
}
}
