package com.adhan.app.ui.components

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

@Composable
fun InteractiveMap(
    initialLat: Double,
    initialLng: Double,
    onLocationOverride: (Double, Double) -> Unit
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

    Box(modifier = Modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(
                mapType = MapType.SATELLITE,
                isMyLocationEnabled = true
            ),
            uiSettings = MapUiSettings(
                zoomControlsEnabled = true,
                zoomGesturesEnabled = true,
                myLocationButtonEnabled = true
            ),
            onMapClick = { latLng ->
                onLocationOverride(latLng.latitude, latLng.longitude)
            }
        ) {
            // Shortest distance (geodesic) line to Mecca
            Polyline(
                points = listOf(userLocation, mecca),
                color = Color(0xFF2196F3), // Material Blue
                width = 8f,
                geodesic = true
            )

            // Marker for current/override location
            Marker(
                state = MarkerState(position = userLocation),
                title = "Current Location",
                snippet = "Shortest path to Mecca: Blue Line"
            )

            // Marker for Mecca
            Marker(
                state = MarkerState(position = mecca),
                title = "Kaaba, Mecca",
                snippet = "The Qibla Direction"
            )
        }

        // Overlay Instructions
        Surface(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(16.dp),
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
