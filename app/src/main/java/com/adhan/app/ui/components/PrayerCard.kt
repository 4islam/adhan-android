package com.adhan.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun PrayerCard(
    name: String,
    time: String,
    icon: ImageVector = Icons.Default.Star,
    isCombined: Boolean = false,
    isActive: Boolean = false,
    isTahajjud: Boolean = false,
    scale: Float = 1f
) {
    val backgroundColor = when {
        isActive -> Color.White.copy(alpha = 0.3f)
        isTahajjud -> Color(0xFF9575CD).copy(alpha = 0.15f) // Distinct purple for Tahajjud
        else -> Color.White.copy(alpha = 0.1f)
    }

    val iconTint = when {
        isActive -> Color(0xFFFFD54F) 
        isTahajjud -> Color(0xFFB39DDB)
        else -> Color.White.copy(alpha = 0.7f)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .graphicsLayer(
                scaleX = scale,
                scaleY = scale,
                alpha = if (isActive) 1f else 0.85f
            )
            .clip(RoundedCornerShape(24.dp))
            .background(backgroundColor)
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = name,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (isTahajjud) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF9575CD).copy(alpha = 0.3f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "SUNNAH",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFD1C4E9),
                                fontWeight = FontWeight.Bold,
                                fontSize = 8.sp
                            )
                        }
                    }
                }
                if (isCombined) {
                    Text(
                        text = "Combined",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                }
            }
        }
        Text(
            text = time,
            style = MaterialTheme.typography.titleLarge,
            color = if (isActive) Color.White else Color.White.copy(alpha = 0.9f),
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Light,
            fontSize = 20.sp
        )
    }
}
