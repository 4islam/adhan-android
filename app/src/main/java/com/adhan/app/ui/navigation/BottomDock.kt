package com.adhan.app.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CompassCalibration
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

sealed class NavScreen(val route: String, val icon: ImageVector, val label: String) {
    object Dashboard : NavScreen("dashboard", Icons.Filled.Home, "Home")
    object Skylight : NavScreen("skylight", Icons.Filled.WbSunny, "Sky")
    object Map : NavScreen("map", Icons.Filled.Map, "Map")
    object Qibla : NavScreen("qibla", Icons.Filled.CompassCalibration, "Qibla")
}

@Composable
fun BottomDock(
    currentRoute: String?,
    onNavigate: (String) -> Unit
) {
    val screens = listOf(
        NavScreen.Dashboard,
        NavScreen.Skylight,
        NavScreen.Map,
        NavScreen.Qibla
    )

    Surface(
        modifier = Modifier
            .height(64.dp)
            .fillMaxWidth(),
        shape = RoundedCornerShape(32.dp),
        color = Color.White.copy(alpha = 0.15f),
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            screens.forEach { screen ->
                val isSelected = currentRoute == screen.route
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) Color.White.copy(alpha = 0.2f) else Color.Transparent)
                        .clickable { onNavigate(screen.route) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = screen.icon,
                        contentDescription = screen.label,
                        tint = if (isSelected) Color.White else Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}
