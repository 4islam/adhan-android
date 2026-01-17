package com.adhan.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.adhan.app.domain.models.Astrology
import java.util.Date

@Composable
fun SkylightVisualizer(
    currentTime: Date,
    lat: Double,
    lng: Double,
    onBack: () -> Unit
) {
    val sunPos = Astrology.getSunPosition(currentTime, lat, lng)
    val moonPos = Astrology.getMoonPosition(currentTime, lat, lng)
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(scrollState)
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
                text = "Skylight",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Center)
            )
        }
        
        Spacer(modifier = Modifier.height(32.dp))

        // Celestial Status (Center Area)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center
        ) {
            // Background Arc
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2, size.height * 0.7f)
                val radius = size.width * 0.45f

                drawArc(
                    color = Color.White.copy(alpha = 0.2f),
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = false,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = Size(radius * 2, radius * 2),
                    style = Stroke(width = 2f, pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f))
                )
            }

            // High Quality Icons (Vertically Stacked)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.WbSunny,
                        contentDescription = "Sun",
                        tint = Color(0xFFFFB74D),
                        modifier = Modifier.size(48.dp)
                    )
                    Text("Sun", color = Color.White.copy(alpha = 0.6f), style = MaterialTheme.typography.labelSmall)
                }
                
                Spacer(modifier = Modifier.height(16.dp))

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.NightsStay,
                        contentDescription = "Moon",
                        tint = Color(0xFFB0BEC5),
                        modifier = Modifier.size(40.dp)
                    )
                    Text("Moon", color = Color.White.copy(alpha = 0.6f), style = MaterialTheme.typography.labelSmall)
                }
            }
        }

        // Metrics Grid (2x2)
        Column(
            modifier = Modifier.padding(bottom = 200.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                MetricCard("Sun Altitude", "%.1f°".format(sunPos.altitude))
                MetricCard("Sun Azimuth", "%.1f°".format(sunPos.azimuth))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                MetricCard("Moon Altitude", "%.1f°".format(moonPos.altitude))
                MetricCard("Moon Azimuth", "%.1f°".format(moonPos.azimuth))
            }
        }
    }
}

@Composable
fun MetricCard(label: String, value: String) {
    Surface(
        modifier = Modifier.width(160.dp).height(90.dp),
        shape = RoundedCornerShape(16.dp),
        color = Color.White.copy(alpha = 0.15f)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = label, style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.5f))
            Text(text = value, style = MaterialTheme.typography.headlineSmall, color = Color.White, fontWeight = FontWeight.Bold)
        }
    }
}
