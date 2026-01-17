package com.adhan.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.*

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*

@Composable
fun QiblaCompass(
    userLat: Double,
    userLng: Double,
    deviceHeading: Float,
    onBack: () -> Unit
) {
    val qiblaDirection = calculateQibla(userLat, userLng)
    val rotation by animateFloatAsState(targetValue = qiblaDirection.toFloat() - deviceHeading)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Bar
        Box(modifier = Modifier.fillMaxWidth()) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.align(Alignment.CenterStart)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Text(
                text = "Qibla",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Center)
            )
        }
        
        Spacer(modifier = Modifier.height(48.dp))

        Box(
            modifier = Modifier.size(280.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2, size.height / 2)
                val radius = size.width / 2

                // Outer Ring
                drawCircle(
                    color = Color.White.copy(alpha = 0.2f),
                    radius = radius,
                    center = center
                )

                // Navigation Needle
                rotate(degrees = rotation, pivot = center) {
                    val path = Path().apply {
                        moveTo(center.x, center.y - radius * 0.8f) // Tip
                        lineTo(center.x - 20f, center.y)
                        lineTo(center.x + 20f, center.y)
                        close()
                    }
                    drawPath(path, Color(0xFFC19A6B)) // Gold color for Kaaba direction

                    val backPath = Path().apply {
                        moveTo(center.x, center.y + radius * 0.8f) // Back
                        lineTo(center.x - 20f, center.y)
                        lineTo(center.x + 20f, center.y)
                        close()
                    }
                    drawPath(backPath, Color.White.copy(alpha = 0.5f))
                }
            }
        }

        Spacer(modifier = Modifier.height(48.dp))
        
        Text(
            text = "%.1f°".format(qiblaDirection),
            style = MaterialTheme.typography.displaySmall,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "QIBLA DIRECTION",
            style = MaterialTheme.typography.labelMedium,
            color = Color.White.copy(alpha = 0.6f)
        )
    }
}

private fun calculateQibla(lat: Double, lng: Double): Double {
    val mekkaLat = Math.toRadians(21.4225)
    val mekkaLng = Math.toRadians(39.8262)
    val phi1 = Math.toRadians(lat)
    val lambda1 = Math.toRadians(lng)

    val deltaL = mekkaLng - lambda1
    
    val y = sin(deltaL)
    val x = cos(phi1) * tan(mekkaLat) - sin(phi1) * cos(deltaL)
    
    var qibla = Math.toDegrees(atan2(y, x))
    return (qibla + 360) % 360
}
