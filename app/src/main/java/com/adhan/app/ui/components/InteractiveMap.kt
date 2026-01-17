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
import com.google.android.gms.maps.CameraUpdateFactory
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Layers
import android.location.Geocoder
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import java.util.*

@Composable
fun InteractiveMap(
    initialLat: Double,
    initialLng: Double,
    onLocationOverride: (Double, Double, String?) -> Unit,
    onBack: () -> Unit
) {
    val mecca = LatLng(21.4225, 39.8262)
    val userLocation = LatLng(initialLat, initialLng)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    
    var searchQuery by remember { mutableStateOf("") }
    var currentMapType by remember { mutableStateOf(MapType.SATELLITE) }
    
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(userLocation, 10f)
    }

    // Geocoding function
    fun performSearch(query: String) {
        if (query.isBlank()) return
        
        scope.launch {
            try {
                val geocoder = Geocoder(context, Locale.getDefault())
                val addresses = withContext(Dispatchers.IO) {
                    geocoder.getFromLocationName(query, 1)
                }
                
                if (!addresses.isNullOrEmpty()) {
                    val address = addresses[0]
                    val target = LatLng(address.latitude, address.longitude)
                    val locName = address.locality ?: address.featureName ?: query
                    onLocationOverride(address.latitude, address.longitude, locName)
                    cameraPositionState.animate(
                        CameraUpdateFactory.newLatLngZoom(target, 12f)
                    )
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Location not found", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Search failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Keep camera focused on location updates (only if not searching)
    LaunchedEffect(initialLat, initialLng) {
        cameraPositionState.animate(
            CameraUpdateFactory.newLatLng(userLocation)
        )
    }

    Box(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(
                mapType = currentMapType,
                isMyLocationEnabled = true
            ),
            uiSettings = MapUiSettings(
                zoomControlsEnabled = false,
                zoomGesturesEnabled = true,
                myLocationButtonEnabled = false
            ),
            onMapClick = { latLng ->
                onLocationOverride(latLng.latitude, latLng.longitude, null)
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

        // Top Bar with Extensions
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Row 1: Back + Instructions
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.background(Color.Black.copy(alpha = 0.4f), MaterialTheme.shapes.small)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Surface(
                    color = Color.Black.copy(alpha = 0.6f),
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Tap to override location",
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.labelMedium
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Map Type Toggle
                IconButton(
                    onClick = {
                        currentMapType = when (currentMapType) {
                            MapType.SATELLITE -> MapType.NORMAL
                            MapType.NORMAL -> MapType.TERRAIN
                            else -> MapType.SATELLITE
                        }
                    },
                    modifier = Modifier.background(Color.Black.copy(alpha = 0.4f), MaterialTheme.shapes.small)
                ) {
                    Icon(Icons.Default.Layers, contentDescription = "Map Layers", tint = Color.White)
                }
            }

            // Row 2: Search Bar
            Surface(
                color = Color.Black.copy(alpha = 0.6f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Search location...", color = Color.White.copy(alpha = 0.5f)) },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            cursorColor = Color.White,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyMedium
                    )
                    IconButton(onClick = { performSearch(searchQuery) }) {
                        Icon(Icons.Default.Search, contentDescription = "Search", tint = Color.White)
                    }
                }
            }
        }

        // Custom Zoom Controls (Bottom Right)
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 140.dp), // Elevated above navigation
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FloatingActionButton(
                onClick = {
                    scope.launch {
                        cameraPositionState.animate(CameraUpdateFactory.zoomIn())
                    }
                },
                containerColor = Color.Black.copy(alpha = 0.6f),
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Zoom In")
            }
            
            FloatingActionButton(
                onClick = {
                    scope.launch {
                        cameraPositionState.animate(CameraUpdateFactory.zoomOut())
                    }
                },
                containerColor = Color.Black.copy(alpha = 0.6f),
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(Icons.Default.Remove, contentDescription = "Zoom Out")
            }
        }
    }
}
