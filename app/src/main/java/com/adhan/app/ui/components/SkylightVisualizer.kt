package com.adhan.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.adhan.app.domain.models.Astrology
import java.util.*
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun SkylightVisualizer(
    currentTime: Date,
    lat: Double,
    lng: Double
) {
    val sunPos = Astrology.getSunPosition(currentTime, lat, lng)
    val moonPos = Astrology.getMoonPosition(currentTime, lat, lng)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Skylight",
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(32.dp))

        // Celestial Arc Canvas
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2, size.height * 0.8f)
                val radius = size.width * 0.4f

                // Draw Ground Line
                drawLine(
                    color = Color.White.copy(alpha = 0.3f),
                    start = Offset(0f, center.y),
                    end = Offset(size.width, center.y),
                    strokeWidth = 2f
                )

                // Draw Arc (Visual Path)
                drawArc(
                    color = Color.White.copy(alpha = 0.1f),
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = false,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = Size(radius * 2, radius * 2),
                    style = Stroke(width = 4f)
                )

                // Sun Position
                val sunAngle = (sunPos.altitude % 180).toFloat()
                val sunRad = Math.toRadians((180 - sunAngle).toDouble())
                val sunX = center.x + radius * cos(sunRad).toFloat()
                val sunY = center.y - radius * sin(sunRad).toFloat()

                if (sunPos.altitude > -5) {
                    drawCircle(
                        color = Color(0xFFFFD700),
                        radius = 20f,
                        center = Offset(sunX, sunY)
                    )
                }

                // Moon Position
                val moonAngle = (moonPos.altitude % 180).toFloat()
                val moonRad = Math.toRadians((180 - moonAngle).toDouble())
                val moonX = center.x + radius * cos(moonRad).toFloat()
                val moonY = center.y - radius * sin(moonRad).toFloat()

                if (moonPos.altitude > -5) {
                    drawCircle(
                        color = Color(0xFFF5F5F5),
                        radius = 15f,
                        center = Offset(moonX, moonY)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Metrics Table
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            MetricCard("Sun Altitude", "%.1f°".format(sunPos.altitude))
            MetricCard("Moon Altitude", "%.1f°".format(moonPos.altitude))
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            MetricCard("Sun Azimuth", "%.1f°".format(sunPos.azimuth))
            MetricCard("Moon Azimuth", "%.1f°".format(moonPos.azimuth))
        }
    }
}

@Composable
fun MetricCard(label: String, value: String) {
    Column(
        modifier = Modifier
            .width(150.dp)
            .height(80.dp)
            .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.6f))
        Text(text = value, style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.Bold)
    }
}
