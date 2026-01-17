package com.adhan.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack

@Composable
fun InteractiveMap(
    initialLat: Double,
    initialLng: Double,
    onLocationOverride: (Double, Double) -> Unit,
    onBack: () -> Unit
) {
    val mecca = LatLng(21.4225, 39.8262)
    val userLocation = LatLng(initialLat, initialLng)
    
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(userLocation, 10f)
    }

    // Keep camera focused on location updates
    LaunchedEffect(initialLat, initialLng) {
        cameraPositionState.animate(
            com.google.android.gms.maps.CameraUpdateFactory.newLatLng(userLocation)
        )
    }

    Box(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(
                mapType = MapType.SATELLITE,
                isMyLocationEnabled = true
            ),
            uiSettings = MapUiSettings(
                zoomControlsEnabled = false, // Disable default zoom controls
                zoomGesturesEnabled = true,
                myLocationButtonEnabled = false // We'll rely on our own overlay if needed
            ),
            onMapClick = { latLng ->
                onLocationOverride(latLng.latitude, latLng.longitude)
            },
            contentPadding = PaddingValues(bottom = 120.dp, top = 60.dp) // Prevent overlap
        ) {
            Polyline(
                points = listOf(userLocation, mecca),
                color = Color(0xFF2196F3),
                width = 8f,
                geodesic = true
            )
            Marker(
                state = MarkerState(position = userLocation),
                title = "Current Location"
            )
            Marker(
                state = MarkerState(position = mecca),
                title = "Kaaba, Mecca"
            )
        }

        // Top Bar with Back Button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.background(Color.Black.copy(alpha = 0.4f), MaterialTheme.shapes.small)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Surface(
                color = Color.Black.copy(alpha = 0.6f),
                shape = MaterialTheme.shapes.medium
            ) {
                Text(
                    text = "Tap any spot to override location",
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    }
}
