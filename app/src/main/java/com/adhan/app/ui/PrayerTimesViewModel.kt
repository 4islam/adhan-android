package com.adhan.app.ui

import androidx.lifecycle.ViewModel
import com.adhan.app.domain.models.PrayerTimesCalculator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.*
import javax.inject.Inject

data class PrayerTimesState(
    val times: List<Pair<String, String>> = emptyList(),
    val locationName: String = "London, UK" // Default for now
)

@HiltViewModel
class PrayerTimesViewModel @Inject constructor() : ViewModel() {
    private val _uiState = MutableStateFlow(PrayerTimesState())
    val uiState: StateFlow<PrayerTimesState> = _uiState.asStateFlow()

    init {
        calculateTimes()
    }

    private fun calculateTimes() {
        val calculator = PrayerTimesCalculator()
        calculator.setCalcMethod(PrayerTimesCalculator.Ahmadiyya)
        
        // Default location: London (51.5074 N, 0.1278 W)
        val lat = 51.5074
        val lng = -0.1278
        val date = Date()
        
        val times = calculator.getPrayerTimes(date, lat, lng)
        val names = PrayerTimesCalculator.timeNames
        
        val pairedTimes = names.zip(times)
        _uiState.value = _uiState.value.copy(times = pairedTimes)
    }
}
