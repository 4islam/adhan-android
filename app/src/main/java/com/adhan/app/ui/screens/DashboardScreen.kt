package com.adhan.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.adhan.app.domain.models.PrayerTimesCalculator
import com.adhan.app.ui.PrayerTimesViewModel
import com.adhan.app.ui.components.HeroDashboard
import com.adhan.app.ui.components.AstroRow
import com.adhan.app.ui.components.PrayerCard
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

@Composable
fun DashboardScreen(viewModel: PrayerTimesViewModel) {
    val uiState by viewModel.uiState.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
        contentPadding = PaddingValues(bottom = 120.dp) // Space for floating dock
    ) {
        // Hero Section
        item {
            HeroDashboard(
                nextPrayerName = uiState.nextPrayerName,
                nextPrayerTime = uiState.nextPrayerTime,
                hijriDate = uiState.hijriDate,
                gregorianDate = uiState.gregorianDate
            )
        }

        // Astro Row
        item {
            val moonrise = uiState.astronomicalEvents.find { it.name == "Moonrise" }?.time ?: "--:--"
            val solarNoon = uiState.astronomicalEvents.find { it.name == "Solar Noon" }?.time ?: "--:--"
            val moonset = uiState.astronomicalEvents.find { it.name == "Moonset" }?.time ?: "--:--"
            
            AstroRow(
                moonrise = moonrise,
                solarNoon = solarNoon,
                moonset = moonset
            )
            Spacer(modifier = Modifier.height(24.dp))
        }

        // Prayer Times List
        items(uiState.prayerTimes) { info ->
            val icon = getPrayerIcon(info.name)
            PrayerCard(
                name = info.name,
                time = info.time,
                icon = icon,
                isCombined = info.isCombined,
                isActive = info.name == uiState.nextPrayerName
            )
        }
    }
}

private fun getPrayerIcon(name: String): ImageVector {
    return when {
        name.contains("Fajr") -> Icons.Default.NightsStay
        name.contains("Sunrise") -> Icons.Default.Brightness5
        name.contains("Dhuhr") || name.contains("Jummah") -> Icons.Default.WbSunny
        name.contains("Asr") -> Icons.Default.Brightness7
        name.contains("Maghrib") -> Icons.Default.Brightness6
        name.contains("Isha") -> Icons.Default.NightsStay
        else -> Icons.Default.Star
    }
}
