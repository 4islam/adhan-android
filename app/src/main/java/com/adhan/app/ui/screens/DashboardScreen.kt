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
import com.adhan.app.ui.PrayerTimeRow
import com.adhan.app.ui.PrayerTimesViewModel
import com.adhan.app.ui.components.HeroDashboard

@Composable
fun DashboardScreen(viewModel: PrayerTimesViewModel) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(bottom = 80.dp), // Leave space for bottom dock
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Hero Dashboard
        HeroDashboard(
            nextPrayerName = uiState.nextPrayerName,
            nextPrayerTime = uiState.nextPrayerTime,
            hijriDate = uiState.hijriDate
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Prayer Times Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .weight(1f),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White.copy(alpha = 0.15f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Prayer Times Section
                item {
                    Text(
                        "Prayer Times",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.5f),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                items(uiState.prayerTimes) { info ->
                    PrayerTimeRow(info.name, info.time, info.isCombined)
                    if (info != uiState.prayerTimes.last()) {
                        Divider(
                            color = Color.White.copy(alpha = 0.1f),
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                    }
                }

                item { Spacer(modifier = Modifier.height(16.dp)) }

                // Astronomical Events Section
                item {
                    Text(
                        "Astronomical Events (Not Prayers)",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.5f),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                items(uiState.astronomicalEvents) { info ->
                    PrayerTimeRow(info.name, info.time, false, isEvent = true)
                    if (info != uiState.astronomicalEvents.last()) {
                        Divider(
                            color = Color.White.copy(alpha = 0.05f),
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                }
            }
        }
    }
}
