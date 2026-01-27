package com.adhan.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun DateNavigationRow(
    gregorianDate: String,
    isToday: Boolean,
    onPrevDate: () -> Unit,
    onNextDate: () -> Unit,
    onDateClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = modifier.fillMaxWidth()
    ) {
        androidx.compose.material3.IconButton(onClick = onPrevDate) {
            Icon(
                imageVector = Icons.Default.ChevronLeft,
                contentDescription = "Previous Day",
                tint = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.size(32.dp)
            )
        }
        
        Text(
            text = gregorianDate,
            style = MaterialTheme.typography.headlineSmall,
            color = if(isToday) Color.White else Color(0xFFFFB74D), // Highlight if not Today
            fontWeight = FontWeight.Medium,
            fontSize = 26.sp,
            modifier = Modifier
                .padding(horizontal = 8.dp)
                .clickable(onClick = onDateClick)
        )

        androidx.compose.material3.IconButton(onClick = onNextDate) {
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Next Day",
                tint = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.size(32.dp)
            )
        }
    }
}
