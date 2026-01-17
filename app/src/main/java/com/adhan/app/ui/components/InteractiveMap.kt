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
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(initialLat, initialLng), 10f)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(mapType = MapType.SATELLITE),
            uiSettings = MapUiSettings(zoomControlsEnabled = false),
            onMapClick = { latLng ->
                onLocationOverride(latLng.latitude, latLng.longitude)
            }
        )

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
