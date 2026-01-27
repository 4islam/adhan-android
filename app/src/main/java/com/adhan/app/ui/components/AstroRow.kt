package com.adhan.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brightness5
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.runtime.*
import androidx.compose.foundation.clickable
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
    var isExpanded by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.Black.copy(alpha = 0.3f))
            .clickable { isExpanded = !isExpanded } // Tap anywhere to toggle
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header (Always Visible) with Toggle Icon
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
             if (isExpanded) {
                 Text(
                    text = "Sun & Moon Info",
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White.copy(alpha = 0.7f)
                )
             } else {
                 Row(verticalAlignment = Alignment.CenterVertically) {
                     Icon(Icons.Default.Brightness5, "Sun", tint = Color(0xFFFFB74D), modifier = Modifier.size(16.dp))
                     Spacer(modifier = Modifier.width(4.dp))
                     Text(text = sunrise, style = MaterialTheme.typography.bodyMedium, color = Color.White, fontWeight = FontWeight.Bold)
                     
                     Spacer(modifier = Modifier.width(16.dp))
                     
                     Icon(Icons.Default.Brightness5, "Sun", tint = Color(0xFFFFB74D), modifier = Modifier.size(16.dp)) // Maybe use different icon for set or rotate
                     Spacer(modifier = Modifier.width(4.dp))
                     Text(text = sunset, style = MaterialTheme.typography.bodyMedium, color = Color.White, fontWeight = FontWeight.Bold)
                 }
             }
             
             Icon(
                imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = if (isExpanded) "Collapse" else "Expand",
                tint = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.size(20.dp)
            )
        }

        if (isExpanded) {
            // Sun Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                AstroItemLarge("Sunrise", sunrise, Icons.Default.Brightness5, Color(0xFFFFB74D))
                AstroItemLarge("Noon", solarNoon, Icons.Default.WbSunny, Color(0xFFFFE082))
                AstroItemLarge("Sunset", sunset, Icons.Default.Brightness5, Color(0xFFFFB74D))
            }
    
            // Divider
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Color.White.copy(alpha = 0.1f))
            )
    
            // Moon Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround, 
                verticalAlignment = Alignment.CenterVertically
            ) {
                AstroItemLarge("Moonrise", moonrise, Icons.Default.NightsStay, Color(0xFF80DEEA))
                AstroItemLarge("Moonset", moonset, Icons.Default.NightsStay, Color(0xFF4DD0E1))
            }
        }
    }
}

@Composable
private fun AstroItemLarge(label: String, time: String, icon: ImageVector, iconColor: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = iconColor,
            modifier = Modifier.size(24.dp) // Larger Icon
        )
        Column(horizontalAlignment = Alignment.Start) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 11.sp
            )
            Text(
                text = time,
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp // Larger Time
            )
        }
    }
}
