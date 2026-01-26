package com.adhan.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun UserGuideScreen(onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(16.dp)
    ) {
        // Header
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Text(
                text = "User Guide",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 32.dp)
        ) {
            GuideSection("Weekly Schedule & Audio Routing")
            GuideText("You can configure specific settings for each prayer on each day of the week.")
            GuideStep(1, "Go to Settings -> Notification Toggles.")
            GuideStep(2, "Tap any day bubble (S M T W...) to toggle notifications On/Off.")
            GuideStep(3, "Long Press a day bubble to configure specific Audio Routing for that day.")
            GuideStep(4, "In the Day Config dialog, you can override the global audio output for that specific prayer instance.")
            
            Spacer(modifier = Modifier.height(24.dp))
            
            GuideSection("Audio Output Priority")
            GuideText("The app decides which audio device to use in this order:")
            GuideStep(1, "Specific Day Route: If you set a route for a specific day (Long Press), it takes highest priority.")
            GuideStep(2, "Global Default: If no specific route is set, it uses the route selected in 'Sounds & Audio'.")
            
            Spacer(modifier = Modifier.height(24.dp))
            
            GuideSection("Testing")
            GuideText("You can test your audio setup in 'Testing Tools'.")
            GuideStep(1, "Select a 'Test Audio Target' (or leave as Global Default).")
            GuideStep(2, "Use 'Play Adhan Now' for an immediate foreground test.")
            GuideStep(3, "Use 'Schedule Background Test' to verify background execution (starts in 10s or 2 mins).")
        }
    }
}

@Composable
fun GuideSection(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = Color(0xFF00E5FF),
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

@Composable
fun GuideText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = Color.White,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@Composable
fun GuideStep(number: Int, text: String) {
    Row(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(
            text = "$number. ",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Cyan,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White
        )
    }
}

@Composable
fun GuideBullet(text: String) {
    Row(modifier = Modifier.padding(start = 16.dp, top = 2.dp, bottom = 2.dp)) {
        Text(
            text = "• ",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.7f)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.9f)
        )
    }
}
