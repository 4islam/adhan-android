package com.adhan.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import com.adhan.app.domain.models.Astrology
import java.util.*
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun CelestialBackground(
    currentTime: Date,
    lat: Double,
    lng: Double
) {
    val sunPos = Astrology.getSunPosition(currentTime, lat, lng)
    val moonPos = Astrology.getMoonPosition(currentTime, lat, lng)

    // Dynamic colors based on sun altitude
    val skyColor = when {
        sunPos.altitude > 0 -> Color(0xFF4A90E2) // Day
        sunPos.altitude > -6 -> Color(0xFFFFA07A) // Golden hour/Twilight
        sunPos.altitude > -12 -> Color(0xFF483D8B) // Nautical twilight
        else -> Color(0xFF191970) // Night
    }

    val animatedSkyColor by animateColorAsState(
        targetValue = skyColor,
        animationSpec = tween(durationMillis = 1000)
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        animatedSkyColor,
                        animatedSkyColor.copy(alpha = 0.7f),
                        animatedSkyColor.copy(alpha = 0.5f)
                    )
                )
            )
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            // Render Sun if above horizon
            if (sunPos.altitude > -5) {
                val sunX = (sunPos.azimuth / 360.0) * width
                val sunY = height * (1.0 - (sunPos.altitude + 20) / 110.0)
                
                drawCircle(
                    color = Color(0xFFFFD700),
                    radius = 40f,
                    center = Offset(sunX.toFloat(), sunY.toFloat())
                )
            }

            // Render Moon if above horizon
            if (moonPos.altitude > -5) {
                val moonX = (moonPos.azimuth / 360.0) * width
                val moonY = height * (1.0 - (moonPos.altitude + 20) / 110.0)
                
                drawCircle(
                    color = Color(0xFFF5F5F5),
                    radius = 30f,
                    center = Offset(moonX.toFloat(), moonY.toFloat())
                )
            }
        }
    }
}
