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
import com.adhan.app.ui.screens.SettingsScreen

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
                lng = uiState.longitude,
                onBack = { navController.popBackStack() }
            )
        }
        composable(NavScreen.Map.route) {
            InteractiveMap(
                initialLat = uiState.latitude,
                initialLng = uiState.longitude,
                deviceHeading = uiState.deviceHeading,
                onLocationOverride = { lat, lng, name ->
                    viewModel.overrideLocation(lat, lng, name)
                },
                onBack = { navController.popBackStack() }
            )
        }
        composable(NavScreen.Qibla.route) {
            QiblaCompass(
                userLat = uiState.latitude,
                userLng = uiState.longitude,
                deviceHeading = uiState.deviceHeading,
                onBack = { navController.popBackStack() }
            )
        }
        composable(NavScreen.Settings.route) {
            SettingsScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onNavigateToLogs = { navController.navigate(NavScreen.Logs.route) },
                onNavigateToGuide = { navController.navigate(NavScreen.UserGuide.route) }
            )
        }
        composable(NavScreen.Logs.route) {
            com.adhan.app.ui.screens.LogsScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }
        composable(NavScreen.UserGuide.route) {
            com.adhan.app.ui.screens.UserGuideScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}
