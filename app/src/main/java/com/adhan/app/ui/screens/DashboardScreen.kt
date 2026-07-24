package com.adhan.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material.icons.filled.Stop
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.Icon
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
    
    // Date Picker State
    var showDatePicker by remember { mutableStateOf(false) }
    
    val context = LocalContext.current
    
    if (showDatePicker) {
        val calendar = java.util.Calendar.getInstance()
        calendar.time = uiState.selectedDate
        
        android.app.DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val newDate = java.util.Calendar.getInstance()
                newDate.set(year, month, dayOfMonth)
                viewModel.setSelectedDate(newDate.timeInMillis)
                showDatePicker = false
            },
            calendar.get(java.util.Calendar.YEAR),
            calendar.get(java.util.Calendar.MONTH),
            calendar.get(java.util.Calendar.DAY_OF_MONTH)
        ).show()
        
        // Reset state immediately as Dialog handles itself, but in Compose we usually wait onDismiss. 
        // Logic above uses standard Android View dialog which blocks or handles independently.
        // We set showDatePicker = false inside the callback or immediately if we want to just trigger it once.
        // Better pattern for View-based dialog in Compose:
        LaunchedEffect(Unit) { showDatePicker = false } 
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (uiState.isLoading) {
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator(color = Color.White)
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = uiState.loadingMessage,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )
            }
        } else {
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
                    val isToday = android.text.format.DateUtils.isToday(uiState.selectedDate.time)
                    
                    HeroDashboard(
                        nextPrayerName = uiState.nextPrayerName,
                        nextPrayerTime = uiState.nextPrayerTime,
                        nextPrayerCountdown = uiState.nextPrayerCountdown,
                        nextPrayerDateLabel = uiState.nextPrayerDateLabel,
                        hijriDate = uiState.hijriDate,
                        gregorianDate = uiState.gregorianDate,
                        locationName = uiState.locationName,
                        currentTime = uiState.selectedDate, // Show selected date context
                        onPrevDate = { viewModel.incrementDate(-1) },
                        onNextDate = { viewModel.incrementDate(1) },
                        onDateClick = { showDatePicker = true },
                        onJumpToToday = { viewModel.jumpToToday() },
                        isToday = isToday
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
                    
                    // --- 3D Spherical Logic for List Items ---
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
                                    if (viewportHeight > 0) {
                                        val itemCenter = itemInfo.offset + (itemInfo.size / 2f)
                                        val viewportCenter = viewportHeight / 2f
                                        
                                        val distFromCenter = (itemCenter - viewportCenter) / (viewportHeight / 2f)
                                        if (!distFromCenter.isNaN() && !distFromCenter.isInfinite()) {
                                            val absDist = kotlin.math.abs(distFromCenter).coerceAtMost(1f)

                                            rotationX = distFromCenter * -30f
                                            cameraDistance = 12f * density
                                            
                                            val focalScale = if (isActive) 1.25f else 1.0f
                                            val distScale = 1.0f - (absDist * 0.4f)
                                            scaleX = (focalScale * distScale).coerceAtLeast(0.1f)
                                            scaleY = (focalScale * distScale).coerceAtLeast(0.1f)
                                            
                                            alpha = (1.0f - (absDist * 0.3f)).coerceIn(0.4f, 1.0f)
                                            translationY = distFromCenter * -20f
                                        }
                                    }
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
                
                item {
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
            
            // Stop Adhan Button
            if (uiState.isAdhanPlaying) {
                ExtendedFloatingActionButton(
                    onClick = { viewModel.stopAdhan() },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 32.dp),
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = Color.White
                ) {
                    Icon(
                        imageVector = Icons.Default.Stop,
                        contentDescription = "Stop Adhan"
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Stop Adhan")
                }
            }
            

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
