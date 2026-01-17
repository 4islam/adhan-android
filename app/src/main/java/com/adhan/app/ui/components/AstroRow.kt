package com.adhan.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brightness5
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AstroRow(
    sunrise: String,
    solarNoon: String,
    sunset: String,
    moonrise: String,
    moonset: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.1f))
            .padding(vertical = 6.dp, horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        AstroItemCompact("Rise", sunrise, Icons.Default.Brightness5, Color(0xFFFFB74D))
        AstroItemCompact("Noon", solarNoon, Icons.Default.WbSunny, Color(0xFFFFB74D))
        AstroItemCompact("Set", sunset, Icons.Default.Brightness5, Color(0xFFFFB74D))
        
        // Subtle Vertical Divider
        Box(modifier = Modifier.width(1.dp).height(20.dp).background(Color.White.copy(alpha = 0.1f)))
        
        AstroItemCompact("Moon ↑", moonrise, Icons.Default.NightsStay, Color(0xFF80DEEA))
        AstroItemCompact("Moon ↓", moonset, Icons.Default.NightsStay, Color(0xFF80DEEA))
    }
}

@Composable
private fun AstroItemCompact(label: String, time: String, icon: ImageVector, iconColor: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = iconColor,
            modifier = Modifier.size(14.dp)
        )
        Column(horizontalAlignment = Alignment.Start) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 8.sp,
                lineHeight = 8.sp
            )
            Text(
                text = time,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp,
                lineHeight = 10.sp
            )
        }
    }
}
