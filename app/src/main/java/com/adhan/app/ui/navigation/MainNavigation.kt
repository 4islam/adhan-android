package com.adhan.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.adhan.app.ui.PrayerTimesViewModel
import com.adhan.app.ui.components.InteractiveMap
import com.adhan.app.ui.components.QiblaCompass
import com.adhan.app.ui.components.SkylightVisualizer
import com.adhan.app.ui.screens.DashboardScreen

@Composable
fun MainNavigation(
    navController: NavHostController,
    viewModel: PrayerTimesViewModel,
    uiState: com.adhan.app.ui.PrayerTimesState
) {
    NavHost(
        navController = navController,
        startDestination = NavScreen.Dashboard.route
    ) {
        composable(NavScreen.Dashboard.route) {
            DashboardScreen(viewModel)
        }
        composable(NavScreen.Skylight.route) {
            SkylightVisualizer(
                currentTime = uiState.currentTime,
                lat = uiState.latitude,
                lng = uiState.longitude
            )
        }
        composable(NavScreen.Map.route) {
            InteractiveMap(
                initialLat = uiState.latitude,
                initialLng = uiState.longitude,
                onLocationOverride = { lat, lng ->
                    viewModel.overrideLocation(lat, lng)
                }
            )
        }
        composable(NavScreen.Qibla.route) {
            QiblaCompass(
                userLat = uiState.latitude,
                userLng = uiState.longitude,
                deviceHeading = uiState.deviceHeading
            )
        }
    }
}
