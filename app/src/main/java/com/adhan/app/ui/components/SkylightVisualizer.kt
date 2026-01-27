package com.adhan.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.PointerInputChange
import com.adhan.app.domain.models.Astrology
import com.adhan.app.ui.SkyAnchor
import java.util.Date

@Composable
fun SkylightVisualizer(
    selectedDate: Date,
    gregorianDate: String,
    hijriDate: String,
    lat: Double,
    lng: Double,
    onBack: () -> Unit,
    onPrevDate: () -> Unit,
    onNextDate: () -> Unit,
    onDateSelected: (Long) -> Unit,
    onJumpToToday: () -> Unit,
    onTimeScrub: (Date) -> Unit,
    currentAnchor: com.adhan.app.ui.SkyAnchor,
    onAnchorSelected: (com.adhan.app.ui.SkyAnchor) -> Unit
) {
    val sunPos = Astrology.getSunPosition(selectedDate, lat, lng)
    val moonPos = Astrology.getMoonPosition(selectedDate, lat, lng)
    val scrollState = rememberScrollState()
    
    // Date Picker State
    var showDatePicker by remember { mutableStateOf(false) }
    val context = androidx.compose.ui.platform.LocalContext.current
    
    if (showDatePicker) {
        val calendar = java.util.Calendar.getInstance()
        calendar.time = selectedDate
        
        android.app.DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val newDate = java.util.Calendar.getInstance()
                newDate.set(year, month, dayOfMonth)
                onDateSelected(newDate.timeInMillis)
                showDatePicker = false
            },
            calendar.get(java.util.Calendar.YEAR),
            calendar.get(java.util.Calendar.MONTH),
            calendar.get(java.util.Calendar.DAY_OF_MONTH)
        ).show()
        
        LaunchedEffect(Unit) { showDatePicker = false }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(scrollState)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Bar
        Box(modifier = Modifier.fillMaxWidth()) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.align(Alignment.CenterStart)
            ) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Text(
                text = "Skylight",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Center)
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Date Navigation
        val isToday = android.text.format.DateUtils.isToday(selectedDate.time)
        DateNavigationRow(
            gregorianDate = gregorianDate,
            isToday = isToday,
            onPrevDate = onPrevDate,
            onNextDate = onNextDate,
            onDateClick = { showDatePicker = true },
            modifier = Modifier.padding(horizontal = 8.dp)
        )
        
        // Hijri Date
        Text(
            text = hijriDate,
            style = MaterialTheme.typography.titleMedium,
            color = Color(0xFF00E5FF), // Cyan
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(top = 4.dp)
        )
        
        if (!isToday) {
             Spacer(modifier = Modifier.height(16.dp))
             Button(
                 onClick = onJumpToToday,
                 colors = ButtonDefaults.buttonColors(
                     containerColor = Color.White.copy(alpha = 0.2f),
                     contentColor = Color.White
                 )
             ) {
                 Text("Return to Today")
             }
        }
        
        Spacer(modifier = Modifier.height(32.dp))

        // Celestial Status (Center Area)
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .pointerInput(Unit) {
                    detectHorizontalDragGestures { change: PointerInputChange, dragAmount: Float ->
                        change.consume()
                        // Sensitivity: 1px = 1 minute? Or screen width = 24 hours?
                        // Let's go with pixel based.
                        val minutesToAdd = (dragAmount / 2f).toInt() // fast scrub
                        if (minutesToAdd != 0) {
                            val cal = java.util.Calendar.getInstance()
                            cal.time = selectedDate
                            cal.add(java.util.Calendar.MINUTE, minutesToAdd)
                            onTimeScrub(cal.time)
                        }
                    }
                },
            contentAlignment = Alignment.TopStart
        ) {
            val width = constraints.maxWidth.toFloat()
            val height = constraints.maxHeight.toFloat()
            val horizonY = height * 0.7f
            
            // Background Arc (Static Reference)
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2, size.height * 0.7f)
                val radius = size.width * 0.45f

                drawArc(
                    color = Color.White.copy(alpha = 0.2f),
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = false,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = Size(radius * 2, radius * 2),
                    style = Stroke(width = 2f, pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f))
                )
                
                // Draw Horizon Line
                drawLine(
                    color = Color.White.copy(alpha = 0.2f),
                    start = Offset(0f, horizonY),
                    end = Offset(size.width, horizonY),
                    strokeWidth = 2f
                )
            }

            // Helper to calculate position
            fun calculatePosition(alt: Double, az: Double): Offset {
                // Altitude: 90 is Zenith (Top), 0 is Horizon, -90 is Nadir
                // Map Altitude 90 -> 20% Height (Space for top bar)
                // Map Altitude 0 -> 70% Height (Horizon)
                // Map Altitude -90 -> Below Horizon
                
                // Scale factor: 50% of screen height covers 0 to 90 degrees
                val altScale = height * 0.5f 
                val y = horizonY - (alt.toFloat() / 90f) * altScale
                
                // Azimuth: 0 to 360 maps across width. 
                // Let's assume standard map: North (0/360) is Center? Or East (90) Right?
                // Visualizer usually implies looking South in Northern Hemisphere context often, but generally:
                // Let's map 0..360 -> 0..Width for linear "Pan" view.
                val x = (az.toFloat() / 360f) * width
                
                return Offset(x, y)
            }
            
            // 1. Draw Sun
            val sunOffset = calculatePosition(sunPos.altitude, sunPos.azimuth)
            val sunSize = 48.dp
            val sunPx = with(androidx.compose.ui.platform.LocalDensity.current) { sunSize.toPx() }
            
            // Only show if reasonably visible or just dim it? 
            // User wants to see "rise/set", so seeing it below horizon with low opacity is cool.
            val sunAlpha = if(sunPos.altitude > -10) 1f else 0.3f
            
            Column(
                modifier = Modifier
                    .offset { androidx.compose.ui.unit.IntOffset(
                        (sunOffset.x - sunPx/2).toInt(), 
                        (sunOffset.y - sunPx/2).toInt()
                    ) },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.WbSunny,
                    contentDescription = "Sun",
                    tint = Color(0xFFFFB74D).copy(alpha = sunAlpha),
                    modifier = Modifier.size(sunSize)
                )
                if (sunAlpha > 0.5f) {
                   Text("Sun", color = Color.White.copy(alpha = 0.6f), style = MaterialTheme.typography.labelSmall)
                }
            }

            // 2. Draw Moon
            val moonOffset = calculatePosition(moonPos.altitude, moonPos.azimuth)
            val moonSize = 40.dp
            val moonPx = with(androidx.compose.ui.platform.LocalDensity.current) { moonSize.toPx() }
            val moonAlpha = if(moonPos.altitude > -10) 1f else 0.3f
            
             Column(
                modifier = Modifier
                     .offset { androidx.compose.ui.unit.IntOffset(
                        (moonOffset.x - moonPx/2).toInt(), 
                        (moonOffset.y - moonPx/2).toInt()
                    ) },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.NightsStay,
                    contentDescription = "Moon",
                    tint = Color(0xFFB0BEC5).copy(alpha = moonAlpha),
                    modifier = Modifier.size(moonSize)
                )
                 if (moonAlpha > 0.5f) {
                    Text("Moon", color = Color.White.copy(alpha = 0.6f), style = MaterialTheme.typography.labelSmall)
                }
            }
        }

        // Metrics & Controls
        Column(
            modifier = Modifier.padding(bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Anchor Controls
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
               AnchorChip("Time", currentAnchor == SkyAnchor.Time) { 
                   if (currentAnchor == SkyAnchor.Time) onJumpToToday() else onAnchorSelected(SkyAnchor.Time)
               }
               AnchorChip("Sunrise", currentAnchor == SkyAnchor.Sunrise) { onAnchorSelected(SkyAnchor.Sunrise) }
               AnchorChip("Noon", currentAnchor == SkyAnchor.SolarNoon) { onAnchorSelected(SkyAnchor.SolarNoon) }
               AnchorChip("Sunset", currentAnchor == SkyAnchor.Sunset) { onAnchorSelected(SkyAnchor.Sunset) }
            }
            
            // Time Display (Big)
            Text(
                text = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(selectedDate),
                style = MaterialTheme.typography.displayLarge,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            
            // Time Slider
            val cal = java.util.Calendar.getInstance()
            cal.time = selectedDate
            val minutes = cal.get(java.util.Calendar.HOUR_OF_DAY) * 60 + cal.get(java.util.Calendar.MINUTE)
            val maxMinutes = 24 * 60f
            
            Slider(
                value = minutes.toFloat(),
                onValueChange = { newMinutes ->
                    val newCal = java.util.Calendar.getInstance()
                    newCal.time = selectedDate
                    val h = (newMinutes / 60).toInt()
                    val m = (newMinutes % 60).toInt()
                    newCal.set(java.util.Calendar.HOUR_OF_DAY, h)
                    newCal.set(java.util.Calendar.MINUTE, m)
                    onTimeScrub(newCal.time)
                },
                valueRange = 0f..maxMinutes,
                modifier = Modifier.fillMaxWidth(0.8f),
                colors = SliderDefaults.colors(
                    thumbColor = Color(0xFF00E5FF),
                    activeTrackColor = Color(0xFF00E5FF),
                    inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                )
            )
            
            // Metrics
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                MetricCard("Sun Altitude", "%.1f°".format(sunPos.altitude))
                MetricCard("Sun Azimuth", "%.1f°".format(sunPos.azimuth))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnchorChip(text: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(text) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = Color(0xFF00E5FF),
            selectedLabelColor = Color.Black,
            containerColor = Color.White.copy(alpha = 0.1f),
            labelColor = Color.White
        ),
        border = null
    )
}

@Composable
fun MetricCard(label: String, value: String) {
    Surface(
        modifier = Modifier.width(160.dp).height(90.dp),
        shape = RoundedCornerShape(16.dp),
        color = Color.White.copy(alpha = 0.15f)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = label, style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.5f))
            Text(text = value, style = MaterialTheme.typography.headlineSmall, color = Color.White, fontWeight = FontWeight.Bold)
        }
    }
}
