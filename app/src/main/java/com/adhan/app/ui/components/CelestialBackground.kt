package com.adhan.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.adhan.app.domain.models.Astrology
import java.util.*
import kotlin.math.abs
import kotlin.random.Random

@Composable
fun CelestialBackground(
    latitude: Double,
    longitude: Double,
    currentTime: Date
) {
    val sunPos = Astrology.getSunPosition(currentTime, latitude, longitude)
    val moonPos = Astrology.getMoonPosition(currentTime, latitude, longitude)

    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        val horizonY = height * 0.75f

        // 1. Sky Gradient based on Sun Altitude
        val sunAlt = sunPos.altitude
        val skyBrush = when {
            sunAlt > 10 -> Brush.verticalGradient(listOf(Color(0xFF1E88E5), Color(0xFF90CAF9))) // Day
            sunAlt > 0 -> Brush.verticalGradient(listOf(Color(0xFF0D47A1), Color(0xFFF06292), Color(0xFFFFB74D))) // Sunset/Rise
            sunAlt > -6 -> Brush.verticalGradient(listOf(Color(0xFF1A237E), Color(0xFF4A148C), Color(0xFFE91E63))) // Twilight
            else -> Brush.verticalGradient(listOf(Color(0xFF000428), Color(0xFF004E92))) // Night
        }
        drawRect(brush = skyBrush, size = size)

        // 2. Stars for Night/Twilight
        if (sunAlt < 0) {
            val starCount = 100
            val alpha = if (sunAlt < -6) 1f else (abs(sunAlt) / 6f).toFloat()
            val random = Random(currentTime.time / 86400000) // Consistent per day
            repeat(starCount) {
                val x = random.nextFloat() * width
                val y = random.nextFloat() * horizonY
                val starSize = random.nextFloat() * 2.dp.toPx()
                drawCircle(
                    color = Color.White.copy(alpha = alpha * 0.6f),
                    radius = starSize,
                    center = Offset(x, y)
                )
            }
        }

        // 3. Horizon Atmospheric Glow
        drawRect(
            brush = Brush.verticalGradient(
                listOf(Color.White.copy(alpha = 0.1f), Color.Transparent),
                startY = horizonY - 100.dp.toPx(),
                endY = horizonY
            ),
            size = androidx.compose.ui.geometry.Size(width, 100.dp.toPx()),
            topLeft = Offset(0f, horizonY - 100.dp.toPx())
        )

        // 4. Horizon Line
        drawLine(
            color = Color.White.copy(alpha = 0.15f),
            start = Offset(0f, horizonY),
            end = Offset(width, horizonY),
            strokeWidth = 1.dp.toPx()
        )

        // 5. Draw Sun
        if (sunAlt > -15) {
            val sunX = (sunPos.azimuth.toFloat() / 360f) * width
            val sunY = horizonY - (sunAlt.toFloat() / 90f) * (height * 0.5f)
            
            // Outer Glow
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(Color(0xFFFFD54F).copy(alpha = 0.3f), Color.Transparent),
                    center = Offset(sunX, sunY),
                    radius = 120.dp.toPx()
                ),
                radius = 120.dp.toPx(),
                center = Offset(sunX, sunY)
            )
            // Inner Body
            drawCircle(
                color = Color(0xFFFFD54F),
                radius = 45.dp.toPx(),
                center = Offset(sunX, sunY),
                alpha = if (sunAlt > 0) 1f else (sunAlt + 15).toFloat() / 15f
            )
        }

        // 6. Draw Moon
        val moonAlt = moonPos.altitude
        if (moonAlt > -15) {
            val moonX = (moonPos.azimuth.toFloat() / 360f) * width
            val moonY = horizonY - (moonAlt.toFloat() / 90f) * (height * 0.5f)
            
            // Moon Glow
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(Color(0xFFE0F7FA).copy(alpha = 0.2f), Color.Transparent),
                    center = Offset(moonX, moonY),
                    radius = 80.dp.toPx()
                ),
                radius = 80.dp.toPx(),
                center = Offset(moonX, moonY)
            )
            // Moon Body
            drawCircle(
                color = Color(0xFFE0F7FA),
                radius = 35.dp.toPx(),
                center = Offset(moonX, moonY),
                alpha = if (moonAlt > 0) 0.8f else (moonAlt + 15).toFloat() / 15f * 0.8f
            )
            // Crescent cutout
            drawCircle(
                color = Color.Black.copy(alpha = 0.01f), // Subtle cutout
                radius = 30.dp.toPx(),
                center = Offset(moonX - 8.dp.toPx(), moonY - 4.dp.toPx()),
                blendMode = androidx.compose.ui.graphics.BlendMode.DstOut
            )
        }
    }
}
