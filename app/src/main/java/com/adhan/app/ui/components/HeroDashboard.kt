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
    currentTime: java.util.Date = java.util.Date()
) {
    val isFriday = java.util.Calendar.getInstance().apply { time = currentTime }
        .get(java.util.Calendar.DAY_OF_WEEK) == java.util.Calendar.FRIDAY

    val (arabicVerse, englishVerse) = if (isFriday) {
        "يَٰٓأَيُّهَا ٱلَّذِينَ ءَامَنُوٓا۟ إِذَا نُودِىَ لِلصَّلَوٰةِ مِن يَوْمِ ٱلْجُمُعَةِ فَٱسْعَوْا۟ إِلَىٰ ذِكْرِ ٱللَّهِ وَذَرُوا۟ ٱلْبَيْعَ ۚ ذَٰلِكُمْ خَيْرٌ لَّكُمْ إِن كُنتُمْ تَعْلَمُونَ" to
        "O ye who believe! when the call is made for Prayer on Friday, hasten to the remembrance of Allah, and leave off all business. That is better for you, if you only knew. 62:10"
    } else {
        "...إِنَّ ٱلصَّلَوٰةَ كَانَتْ عَلَى ٱلْمُؤْمِنِينَ كِتَٰبًا مَّوْقُوتًا" to
        "...verily Prayer is enjoined on the believers to be performed at fixed hours. 4:104"
    }

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
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = arabicVerse,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Right, // Traditional right-align for Arabic
                    lineHeight = 28.sp,
                    fontSize = 18.sp,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = englishVerse,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.8f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    lineHeight = 18.sp,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                )
            }
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
