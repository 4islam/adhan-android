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
import androidx.compose.ui.graphics.graphicsLayer
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
    moonPhase: com.adhan.app.domain.models.Astrology.MoonPhase?,
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
            val horizonY = height * 0.8f
            
            // Background Arc (Static Reference)
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2, size.height * 0.8f)
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
                // Scale factor: 85% of screen height covers 0 to 90 degrees (Very high arc)
                val altScale = height * 0.85f 
                val y = horizonY - (alt.toFloat() / 90f) * altScale
                val x = (az.toFloat() / 360f) * width
                return Offset(x, y)
            }
            
            // 1. Draw Sun
            val sunOffset = calculatePosition(sunPos.altitude, sunPos.azimuth)
            val sunSize = 100.dp
            val sunPx = with(androidx.compose.ui.platform.LocalDensity.current) { sunSize.toPx() }
            val sunAlpha = if(sunPos.altitude > -10) 1f else 0.3f
            
            Column(
                modifier = Modifier
                    .offset { androidx.compose.ui.unit.IntOffset(
                        (sunOffset.x - sunPx/2).toInt(), 
                        (sunOffset.y - sunPx/2).toInt()
                    ) },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Realistic Sun: Gradient Circle
                Canvas(modifier = Modifier.size(sunSize).graphicsLayer(alpha = sunAlpha)) {
                    val radius = size.minDimension / 2
                    val center = center
                    
                    val brush = androidx.compose.ui.graphics.Brush.radialGradient(
                        colors = listOf(Color(0xFFFFEE58), Color(0xFFFFB74D), Color(0xFFFF8F00)),
                        center = center,
                        radius = radius
                    )
                    drawCircle(brush = brush, radius = radius, center = center)
                }
                if (sunAlpha > 0.5f) {
                   Text("Sun", color = Color.White.copy(alpha = 0.6f), style = MaterialTheme.typography.labelSmall)
                }
            }

            // 2. Draw Moon
            val moonOffset = calculatePosition(moonPos.altitude, moonPos.azimuth)
            val moonSize = 100.dp
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
                // Dynamic Moon Phase Visual
                Box(modifier = Modifier.size(moonSize)) {
                    Canvas(modifier = Modifier.matchParentSize().graphicsLayer(alpha = moonAlpha)) {
                         val radius = size.minDimension / 2
                         val center = center
                         
                         // 1. Draw Dark Moon Base
                         drawCircle(color = Color(0xFF263238), radius = radius, center = center)
                         
                         val phase = moonPhase
                         if (phase != null) {
                             val litColor = Color(0xFFEEEEEE)
                             val darkColor = Color(0xFF263238)
                             
                             val age = phase.age
                             val synodic = 29.53
                             
                             // Phase Angle: 0 (New) -> PI (Full) -> 0 (New)
                             // Actually we want 0 -> 2PI for full cycle calculation
                             val phaseAngle = (age / synodic) * 2 * Math.PI
                             
                             // Waxing (Right Lit) vs Waning (Left Lit)
                             val isWaxing = age < (synodic / 2)
                             
                             // Terminator Width (-1 to 1) 
                             // 1 = New, 0 = Quarter, -1 = Full (Relative to lit side width)
                             // Actually terminator X offset from center is R * cos(phaseAngle)
                             val terminatorX = (radius * kotlin.math.cos(phaseAngle)).toFloat()
                             
                             val path = androidx.compose.ui.graphics.Path()
                             
                             if (isWaxing) {
                                 // WAXING
                                 // 1. Draw Right Semicircle (Lit)
                                 drawArc(
                                     color = litColor,
                                     startAngle = -90f,
                                     sweepAngle = 180f,
                                     useCenter = true,
                                     topLeft = Offset(center.x - radius, center.y - radius),
                                     size = Size(radius * 2, radius * 2)
                                 )
                                 
                                 // 2. Draw Terminator Ellipse
                                 // If Crescent (Age < 7.4): Terminator bulges Right (Concave Lit). Ellipse is DARK.
                                 // If Gibbous (Age > 7.4): Terminator bulges Left (Convex Lit). Ellipse is LIT.
                                 
                                 // cos(0) = 1 (New). Rect width radius.
                                 // cos(PI/2) = 0 (Quarter). Rect width 0.
                                 // cos(PI) = -1 (Full).
                                 
                                 val w = kotlin.math.abs(terminatorX)
                                 val ellipseRect = androidx.compose.ui.geometry.Rect(
                                     center.x - w, center.y - radius,
                                     center.x + w, center.y + radius
                                 )
                                 
                                 if (age < (synodic / 4)) {
                                     // Waxing Crescent: Dark Ellipse on Right
                                     drawOval(color = darkColor, topLeft = ellipseRect.topLeft, size = ellipseRect.size)
                                 } else {
                                     // Waxing Gibbous: Lit Ellipse on Left
                                     drawOval(color = litColor, topLeft = ellipseRect.topLeft, size = ellipseRect.size)
                                 }
                                 
                             } else {
                                 // WANING
                                 // 1. Draw Left Semicircle (Lit)
                                 drawArc(
                                     color = litColor,
                                     startAngle = 90f,
                                     sweepAngle = 180f,
                                     useCenter = true,
                                     topLeft = Offset(center.x - radius, center.y - radius),
                                     size = Size(radius * 2, radius * 2)
                                 )
                                 
                                 val w = kotlin.math.abs(terminatorX)
                                 val ellipseRect = androidx.compose.ui.geometry.Rect(
                                     center.x - w, center.y - radius,
                                     center.x + w, center.y + radius
                                 )
                                 
                                 if (age < (synodic * 0.75)) {
                                      // Waning Gibbous (Age 14.8..22.1). Terminator bulges Right. Lit Ellipse.
                                      drawOval(color = litColor, topLeft = ellipseRect.topLeft, size = ellipseRect.size)
                                 } else {
                                      // Waning Crescent (Age 22.1..29.5). Terminator bulges Left. Dark Ellipse.
                                      drawOval(color = darkColor, topLeft = ellipseRect.topLeft, size = ellipseRect.size)
                                 }
                             }
                         } else {
                             // Fallback
                             drawCircle(color = Color(0xFFCFD8DC), radius = radius, center = center)
                         }
                    }
                }
                
                if (moonAlpha > 0.5f) {
                   Text("Moon", color = Color.White.copy(alpha = 0.6f), style = MaterialTheme.typography.labelSmall)
                }
            }
        }

        // Metrics & Controls
        Column(
            modifier = Modifier.padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
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
                style = MaterialTheme.typography.displayMedium, // Smaller than Large
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
                modifier = Modifier.fillMaxWidth(0.9f).height(20.dp), // Compact height
                colors = SliderDefaults.colors(
                    thumbColor = Color(0xFF00E5FF),
                    activeTrackColor = Color(0xFF00E5FF),
                    inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                )
            )
            
            // Metrics
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricCard("Sun Alt", "%.1f°".format(sunPos.altitude))
                if (moonPhase != null) {
                     MetricCard("Moon Phase", moonPhase.phaseName)
                     MetricCard("Illum", "%.0f%%".format(moonPhase.illumination * 100))
                }
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
        label = { Text(text, style = MaterialTheme.typography.labelSmall) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = Color(0xFF00E5FF),
            selectedLabelColor = Color.Black,
            containerColor = Color.White.copy(alpha = 0.1f),
            labelColor = Color.White
        ),
        border = null,
        modifier = Modifier.height(32.dp)
    )
}

@Composable
fun MetricCard(label: String, value: String) {
    Surface(
        modifier = Modifier.height(60.dp),
        shape = RoundedCornerShape(12.dp),
        color = Color.White.copy(alpha = 0.15f)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = label, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.5f), maxLines = 1)
            Text(text = value, style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.Bold)
        }
    }
}
