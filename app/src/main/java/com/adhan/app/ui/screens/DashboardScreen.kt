package com.adhan.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.graphics.graphicsLayer
import kotlin.math.*
import com.adhan.app.ui.PrayerTimesViewModel
import com.adhan.app.ui.components.*

@Composable
fun DashboardScreen(viewModel: PrayerTimesViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()

    Box(modifier = Modifier.fillMaxSize()) {
        // 1. Scrollable Content (Hero & Prayers)
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
            contentPadding = PaddingValues(top = 16.dp, bottom = 250.dp)
        ) {
            // Hero Section
            item(key = "hero") {
                HeroDashboard(
                    nextPrayerName = uiState.nextPrayerName,
                    nextPrayerTime = uiState.nextPrayerTime,
                    hijriDate = uiState.hijriDate,
                    gregorianDate = uiState.gregorianDate,
                    locationName = uiState.locationName,
                    currentTime = uiState.currentTime
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Prayer Times List
            items(
                items = uiState.prayerTimes,
                key = { it.name },
                contentType = { "prayer" }
            ) { info ->
                val icon = getPrayerIcon(info.name)
                val isActive = info.name == uiState.activePrayerName
                val isTahajjud = info.name.contains("Tahajjud")
                
                // --- Phase 14: 3D Spherical Logic ---
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .graphicsLayer {
                            val infoKey = info.name
                            val layoutInfo = listState.layoutInfo
                            val itemInfo = layoutInfo.visibleItemsInfo.find { it.key == infoKey }
                            
                            if (itemInfo != null) {
                                val viewportHeight = layoutInfo.viewportEndOffset - layoutInfo.viewportStartOffset
                                val itemCenter = itemInfo.offset + (itemInfo.size / 2f)
                                val viewportCenter = viewportHeight / 2f
                                
                                val distFromCenter = (itemCenter - viewportCenter) / (viewportHeight / 2f)
                                val absDist = kotlin.math.abs(distFromCenter).coerceAtMost(1f)

                                rotationX = distFromCenter * -30f
                                cameraDistance = 12f * density
                                
                                val focalScale = if (isActive) 1.25f else 1.0f
                                val distScale = 1.0f - (absDist * 0.4f)
                                scaleX = focalScale * distScale
                                scaleY = focalScale * distScale
                                
                                alpha = (1.0f - (absDist * 0.3f)).coerceIn(0.4f, 1.0f)
                                translationY = distFromCenter * -20f
                            }
                        }
                ) {
                    PrayerCard(
                        name = info.name,
                        time = info.time,
                        icon = icon,
                        isCombined = info.isCombined,
                        isActive = isActive,
                        isTahajjud = isTahajjud,
                        scale = 1f
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
        }

        // 3. Fixed Astro Panel at bottom
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 115.dp)
        ) {
            val events = uiState.astronomicalEvents
            val sunrise = events.find { it.name == "Sunrise" }?.time ?: "--:--"
            val solarNoon = events.find { it.name == "Solar Noon" }?.time ?: "--:--"
            val sunset = events.find { it.name == "Sunset" }?.time ?: "--:--"
            val moonrise = events.find { it.name == "Moonrise" }?.time ?: "--:--"
            val moonset = events.find { it.name == "Moonset" }?.time ?: "--:--"
            
            AstroRow(
                sunrise = sunrise,
                solarNoon = solarNoon,
                sunset = sunset,
                moonrise = moonrise,
                moonset = moonset
            )
        }
    }
}

private fun getPrayerIcon(name: String): ImageVector {
    return when {
        name.contains("Fajr") -> Icons.Default.NightsStay
        name.contains("Sunrise") || name.contains("Solar Noon") || name.contains("Sunset") -> Icons.Default.AutoAwesome
        name.contains("Dhuhr") || name.contains("Jummah") -> Icons.Default.WbSunny
        name.contains("Asr") -> Icons.Default.Brightness7
        name.contains("Maghrib") -> Icons.Default.Brightness6
        name.contains("Isha") -> Icons.Default.NightsStay
        else -> Icons.Default.Star
    }
}
