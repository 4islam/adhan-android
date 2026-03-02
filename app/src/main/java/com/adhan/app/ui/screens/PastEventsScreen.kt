package com.adhan.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.adhan.app.domain.PrayerEvent
import com.adhan.app.ui.PrayerTimesViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PastEventsScreen(
    viewModel: PrayerTimesViewModel,
    onBack: () -> Unit
) {
    val events by viewModel.prayerEvents.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadPrayerEvents()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Past Adhan Events") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.loadPrayerEvents() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                    IconButton(onClick = { viewModel.clearPrayerEvents() }) {
                        Icon(Icons.Default.Delete, contentDescription = "Clear")
                    }
                }
            )
        }
    ) { padding ->
        if (events.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No events recorded yet.", color = Color.Gray)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(Color.Black),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(events) { event ->
                    EventItem(event)
                }
            }
        }
    }
}

@Composable
fun EventItem(event: PrayerEvent) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = when (event.status) {
                "Error" -> Color(0xFF3E2723)
                "Playing" -> Color(0xFF1B5E20)
                "Triggered" -> Color(0xFF455A64)
                "Scheduled" -> Color(0xFF0D47A1)
                else -> Color(0xFF263238)
            }
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = event.prayerName,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = event.formattedTime,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.LightGray
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Status: ${event.status}",
                style = MaterialTheme.typography.bodySmall,
                color = when (event.status) {
                    "Error" -> Color(0xFFFF8A80)
                    "Playing" -> Color(0xFFA5D6A7)
                    "Triggered" -> Color(0xFFB0BEC5)
                    else -> Color(0xFF81D4FA)
                }
            )
            Row(modifier = Modifier.padding(top = 4.dp)) {
                if (event.speaker != null) {
                    Text(
                        text = "Speaker: ${event.speaker}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray,
                        modifier = Modifier.padding(end = 12.dp)
                    )
                }
                if (event.volume != null) {
                    Text(
                        text = "Vol: ${event.volume}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray
                    )
                }
            }
            if (event.errorMessage != null) {
                Text(
                    text = "Error: ${event.errorMessage}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFFF8A80),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

