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
    val times: List<PrayerTimesCalculator.CombinedPrayerInfo> = emptyList(),
    val locationName: String = "London, UK",
    val latitude: Double = 51.5074,
    val longitude: Double = -0.1278,
    val hijriDate: String = "",
    val nextPrayerName: String = "",
    val nextPrayerTime: String = "",
    val currentTime: Date = Date(),
    val deviceHeading: Float = 0f,
    val isOverrideActive: Boolean = false
)

@HiltViewModel
class PrayerTimesViewModel @Inject constructor(
    private val alarmManager: com.adhan.app.infra.PrayerAlarmManager
) : ViewModel() {
    private val _uiState = MutableStateFlow(PrayerTimesState())
    val uiState: StateFlow<PrayerTimesState> = _uiState.asStateFlow()

    init {
        updateTimes()
        startClock()
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
        calculator.setCalcMethod(PrayerTimesCalculator.Ahmadiyya)
        
        val date = _uiState.value.currentTime
        val lat = _uiState.value.latitude
        val lng = _uiState.value.longitude
        
        val combinedTimes = calculator.getCombinedPrayerTimes(date, lat, lng)
        val hijri = HijriCalendar.fromDate(date)
        val hijriString = "${hijri.day} ${hijri.monthName} ${hijri.year} AH"

        _uiState.value = _uiState.value.copy(
            times = combinedTimes,
            hijriDate = hijriString
        )
        
        // Schedule alarms for the new times
        alarmManager.scheduleAlarms(combinedTimes)
        
        updateNextPrayer()
    }

    private fun updateNextPrayer() {
        val now = _uiState.value.currentTime
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        val currentStr = sdf.format(now)

        var nextFound = false
        for (info in _uiState.value.times) {
            if (info.time > currentStr) {
                _uiState.value = _uiState.value.copy(
                    nextPrayerName = info.name,
                    nextPrayerTime = info.time
                )
                nextFound = true
                break
            }
        }
        
        if (!nextFound && _uiState.value.times.isNotEmpty()) {
            _uiState.value = _uiState.value.copy(
                nextPrayerName = _uiState.value.times[0].name,
                nextPrayerTime = _uiState.value.times[0].time
            )
        }
    }
}
