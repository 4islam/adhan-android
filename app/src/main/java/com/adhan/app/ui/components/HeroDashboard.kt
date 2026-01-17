package com.adhan.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun HeroDashboard(
    nextPrayerName: String,
    nextPrayerTime: String,
    hijriDate: String,
    gregorianDate: String,
    quranVerse: String = "O ye who believe! When the call is made for Prayer on Friday, hasten to the remembrance of Allah..."
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Quran Verse Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White.copy(alpha = 0.12f))
                .padding(horizontal = 24.dp, vertical = 20.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = quranVerse,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.8f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                lineHeight = 22.sp,
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Gregorian Date
        Text(
            text = gregorianDate,
            style = MaterialTheme.typography.headlineSmall,
            color = Color.White,
            fontWeight = FontWeight.Medium,
            fontSize = 26.sp
        )

        // Hijri Date (Cyan)
        Text(
            text = hijriDate,
            style = MaterialTheme.typography.labelLarge,
            color = Color(0xFF00E5FF),
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(top = 4.dp)
        )

        // Location Pill
        Box(
            modifier = Modifier
                .padding(top = 12.dp)
                .clip(RoundedCornerShape(50))
                .background(Color.White.copy(alpha = 0.2f))
                .padding(horizontal = 16.dp, vertical = 4.dp)
        ) {
            Text(
                text = "London",
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.7f)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Circular Countdown
        Box(contentAlignment = Alignment.Center) {
            // Glow Circle (Static)
            Canvas(modifier = Modifier.size(260.dp)) {
                drawCircle(
                    color = Color(0xFF00E5FF).copy(alpha = 0.05f),
                    radius = size.minDimension / 2
                )
            }
            
            // Progress Ring
            Canvas(modifier = Modifier.size(240.dp)) {
                // Background Track
                drawCircle(
                    color = Color.White.copy(alpha = 0.1f),
                    style = Stroke(width = 8.dp.toPx())
                )
                
                // Cyan Active Ring (70% for demo)
                drawArc(
                    color = Color(0xFF00E5FF),
                    startAngle = -90f,
                    sweepAngle = 260f,
                    useCenter = false,
                    style = Stroke(
                        width = 8.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "$nextPrayerName (Tomorrow)",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Light,
                    fontSize = 28.sp
                )
                Text(
                    text = nextPrayerTime,
                    style = MaterialTheme.typography.displayMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 52.sp,
                    letterSpacing = 2.sp
                )
            }
        }
    }
}
