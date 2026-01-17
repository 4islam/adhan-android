package com.adhan.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.rememberNavController
import com.adhan.app.ui.components.CelestialBackground
import com.adhan.app.ui.navigation.BottomDock
import com.adhan.app.ui.navigation.MainNavigation
import androidx.navigation.compose.currentBackStackEntryAsState

@Composable
fun PrayerTimesScreen(viewModel: PrayerTimesViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Box(modifier = Modifier.fillMaxSize()) {
        // Dynamic Background (always present)
        CelestialBackground(
            currentTime = uiState.currentTime,
            latitude = uiState.latitude,
            longitude = uiState.longitude
        )

        // Main Navigation Host
        MainNavigation(
            navController = navController,
            viewModel = viewModel,
            uiState = uiState
        )

        // Floating Bottom Dock
        Box(modifier = Modifier.fillMaxSize().padding(bottom = 32.dp), contentAlignment = Alignment.BottomCenter) {
            BottomDock(
                currentRoute = currentRoute,
                onNavigate = { route ->
                    navController.navigate(route) {
                        popUpTo(navController.graph.startDestinationId) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    }
}

@Composable
fun PrayerTimeRow(name: String, time: String, isCombined: Boolean, isEvent: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = name,
                style = if (isEvent) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.titleMedium,
                color = if (isEvent) Color.White.copy(alpha = 0.7f) else Color.White,
                fontWeight = if (isCombined) FontWeight.Bold else if (isEvent) FontWeight.Normal else FontWeight.Medium
            )
            if (isCombined) {
                Text(
                    text = "Combined",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.6f)
                )
            }
        }
        Text(
            text = time,
            style = if (isEvent) MaterialTheme.typography.titleMedium else MaterialTheme.typography.headlineSmall,
            color = if (isEvent) Color.White.copy(alpha = 0.7f) else Color.White,
            fontWeight = if (isEvent) FontWeight.Medium else FontWeight.Bold
        )
    }
}
