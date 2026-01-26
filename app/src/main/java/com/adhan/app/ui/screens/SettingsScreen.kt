package com.adhan.app.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import com.adhan.app.domain.models.PrayerTimesCalculator
import com.adhan.app.ui.PrayerTimesViewModel
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: PrayerTimesViewModel,
    onBack: () -> Unit,
    onNavigateToLogs: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val calcMethods = mapOf(
        "Ahmadiyya" to PrayerTimesCalculator.Ahmadiyya,
        "MWL" to PrayerTimesCalculator.MWL,
        "ISNA" to PrayerTimesCalculator.ISNA,
        "Egypt" to PrayerTimesCalculator.Egypt,
        "Makkah" to PrayerTimesCalculator.Makkah,
        "Karachi" to PrayerTimesCalculator.Karachi,
        "Tehran" to PrayerTimesCalculator.Tehran,
        "Jafari" to PrayerTimesCalculator.Jafari
    )
    
    val prayers = listOf("Fajr", "Dhuhr", "Asr", "Maghrib", "Isha")
    var selectedPrayerForAudio by remember { mutableStateOf<String?>(null) }
    
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            selectedPrayerForAudio?.let { prayer ->
                viewModel.setAdhanSound(prayer, it.toString())
            }
        }
    }
    
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
                text = "Settings",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Tabs
        var selectedTabIndex by remember { mutableIntStateOf(0) }
        val tabs = listOf("General", "Audio", "Advanced")
        
        TabRow(
            selectedTabIndex = selectedTabIndex,
            containerColor = Color.Transparent,
            contentColor = Color.White,
            indicator = { tabPositions ->
                androidx.compose.foundation.layout.Box(
                    Modifier
                        .tabIndicatorOffset(tabPositions[selectedTabIndex])
                        .height(3.dp)
                        .background(Color.Cyan)
                )
            }
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index },
                    text = { Text(title) }
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 200.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            when (selectedTabIndex) {
                0 -> { // General Tab
                     item {
                        SettingsSection("Notifications") {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Enable Adhan Audio", color = Color.White)
                                Switch(
                                    checked = uiState.isAudioEnabled,
                                    onCheckedChange = { viewModel.setAudioEnabled(it) }
                                )
                            }
                        }
                    }
                    
                    item {
                        val selectedMethodName = calcMethods.entries.find { it.value == uiState.calcMethod }?.key ?: "Select Method"
                        var expanded by remember { mutableStateOf(false) }

                        SettingsSection("Calculation Method") {
                            Box(modifier = Modifier.padding(8.dp)) {
                                ExposedDropdownMenuBox(
                                    expanded = expanded,
                                    onExpandedChange = { expanded = !expanded }
                                ) {
                                    OutlinedTextField(
                                        value = selectedMethodName,
                                        onValueChange = {},
                                        readOnly = true,
                                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White,
                                            focusedBorderColor = Color.White.copy(alpha = 0.5f),
                                            unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                                            focusedContainerColor = Color.Transparent,
                                            unfocusedContainerColor = Color.Transparent
                                        ),
                                        modifier = Modifier
                                            .menuAnchor()
                                            .fillMaxWidth()
                                    )

                                    ExposedDropdownMenu(
                                        expanded = expanded,
                                        onDismissRequest = { expanded = false }
                                    ) {
                                        calcMethods.forEach { (name, id) ->
                                            DropdownMenuItem(
                                                text = { Text(text = name) },
                                                onClick = {
                                                    viewModel.setCalcMethod(id)
                                                    expanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    
                    item {
                        SettingsSection("Madhab (Asr)") {
                            SettingsItem(
                                label = "Standard (Shafi, Maliki, Hanbali)",
                                isSelected = uiState.asrJuristic == PrayerTimesCalculator.Shafii,
                                onClick = { viewModel.setAsrMethod(PrayerTimesCalculator.Shafii) }
                            )
                            SettingsItem(
                                label = "Hanafi",
                                isSelected = uiState.asrJuristic == PrayerTimesCalculator.Hanafi,
                                onClick = { viewModel.setAsrMethod(PrayerTimesCalculator.Hanafi) }
                            )
                        }
                    }
                }
                1 -> { // Audio Tab
                    item {
                        SettingsSection("Audio Configuration") {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text(
                                    text = "Adhan Volume: ${uiState.adhanVolume}%",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Overrides system volume during Adhan playback.",
                                    color = Color.White.copy(alpha = 0.5f),
                                    style = MaterialTheme.typography.labelSmall
                                )
                                Slider(
                                    value = uiState.adhanVolume.toFloat(),
                                    onValueChange = { viewModel.setAdhanVolume(it.toInt()) },
                                    valueRange = 0f..100f,
                                    colors = SliderDefaults.colors(
                                        thumbColor = Color.White,
                                        activeTrackColor = Color.White.copy(alpha = 0.7f)
                                    )
                                )
                                
                                Spacer(modifier = Modifier.height(16.dp))
                                Box(
                                    modifier = Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.1f))
                                )
                                Spacer(modifier = Modifier.height(16.dp))

                                Text(
                                    text = "Audio Output Route",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Select where to play the Adhan (e.g., Bluetooth speakers).",
                                    color = Color.White.copy(alpha = 0.5f),
                                    style = MaterialTheme.typography.labelSmall
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                var audioExpanded by remember { mutableStateOf(false) }
                                
                                Box(modifier = Modifier.fillMaxWidth()) {
                                    ExposedDropdownMenuBox(
                                        expanded = audioExpanded,
                                        onExpandedChange = { audioExpanded = !audioExpanded }
                                    ) {
                                        OutlinedTextField(
                                            value = uiState.selectedAudioRoute,
                                            onValueChange = {},
                                            readOnly = true,
                                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = audioExpanded) },
                                            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(
                                                focusedTextColor = Color.White,
                                                unfocusedTextColor = Color.White,
                                                focusedBorderColor = Color.White.copy(alpha = 0.5f),
                                                unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                                                focusedContainerColor = Color.Transparent,
                                                unfocusedContainerColor = Color.Transparent
                                            ),
                                            modifier = Modifier.menuAnchor().fillMaxWidth()
                                        )

                                        ExposedDropdownMenu(
                                            expanded = audioExpanded,
                                            onDismissRequest = { audioExpanded = false }
                                        ) {
                                            DropdownMenuItem(
                                                text = { Text("Default (System Decision)") },
                                                onClick = {
                                                    viewModel.setSelectedAudioDevice("Default")
                                                    audioExpanded = false
                                                }
                                            )
                                            uiState.audioOutputDevices.forEach { device ->
                                                DropdownMenuItem(
                                                    text = { Text(device) },
                                                    onClick = {
                                                        viewModel.setSelectedAudioDevice(device)
                                                        audioExpanded = false
                                                    }
                                                )
                                            }
                                            DropdownMenuItem(
                                                text = { Text("Refresh Device List", color = Color.Cyan) },
                                                onClick = {
                                                    viewModel.refreshAudioDevices()
                                                }
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = { viewModel.openAudioOutputPicker() },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color.White.copy(alpha = 0.1f)
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Open System Output Switcher", color = Color.White.copy(alpha = 0.7f))
                                }
                            }
                        }
                    }
                    
                    item {
                        SettingsSection("Adhan Sounds") {
                            prayers.forEach { prayer ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(prayer, color = Color.White, fontWeight = FontWeight.Bold)
                                        Text(
                                            text = uiState.adhanSounds[prayer]?.substringAfterLast("/") ?: "Default",
                                            color = Color.White.copy(alpha = 0.5f),
                                            style = MaterialTheme.typography.labelSmall,
                                            maxLines = 1
                                        )
                                    }
                                    
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Switch(
                                            checked = uiState.adhanNotificationEnabled[prayer] ?: true,
                                            onCheckedChange = { viewModel.setAdhanNotificationEnabled(prayer, it) },
                                            colors = SwitchDefaults.colors(
                                                checkedThumbColor = Color.White,
                                                checkedTrackColor = Color.Cyan.copy(alpha = 0.5f)
                                            ),
                                            modifier = Modifier.scale(0.8f)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Button(
                                            onClick = {
                                                selectedPrayerForAudio = prayer
                                                launcher.launch("audio/*")
                                            },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = Color.White.copy(alpha = 0.2f)
                                            ),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                                        ) {
                                            Text("Pick", color = Color.White, style = MaterialTheme.typography.labelMedium)
                                        }
                                    }
                                }
                                
                                // Day Selector Row
                                val daysMap = uiState.adhanNotificationDays[prayer] ?: emptyMap()
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    val days = listOf(
                                        java.util.Calendar.SUNDAY to "S",
                                        java.util.Calendar.MONDAY to "M",
                                        java.util.Calendar.TUESDAY to "T",
                                        java.util.Calendar.WEDNESDAY to "W",
                                        java.util.Calendar.THURSDAY to "T",
                                        java.util.Calendar.FRIDAY to "F",
                                        java.util.Calendar.SATURDAY to "S"
                                    )
                                    
                                    days.forEach { (dayId, label) ->
                                        val isEnabled = daysMap[dayId] ?: true
                                        val isFriday = dayId == java.util.Calendar.FRIDAY
                                        val activeColor = if (isFriday) Color(0xFF00E5FF) else Color.White
                                        
                                        Box(
                                            modifier = Modifier
                                                .size(32.dp)
                                                .clip(CircleShape)
                                                .background(if (isEnabled) activeColor.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.05f))
                                                .clickable { 
                                                    viewModel.setAdhanDayEnabled(prayer, dayId, !isEnabled)
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = label, 
                                                color = if (isEnabled) activeColor else Color.White.copy(alpha = 0.3f),
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.05f)))
                            }
                        }
                    }

                    item {
                        SettingsSection("Audio Fading") {
                            Column(modifier = Modifier.padding(8.dp)) {
                                var fadeExpanded by remember { mutableStateOf(false) }
                                var selectedFadePrayer by remember { mutableStateOf("Fajr") }
                                
                                Text("Target Prayer", color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.labelMedium)
                                Box(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                    ExposedDropdownMenuBox(
                                        expanded = fadeExpanded,
                                        onExpandedChange = { fadeExpanded = !fadeExpanded }
                                    ) {
                                        OutlinedTextField(
                                            value = selectedFadePrayer,
                                            onValueChange = {},
                                            readOnly = true,
                                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = fadeExpanded) },
                                            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(
                                                focusedTextColor = Color.White,
                                                unfocusedTextColor = Color.White,
                                                focusedBorderColor = Color.White.copy(alpha = 0.5f),
                                                unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                                                focusedContainerColor = Color.Transparent,
                                                unfocusedContainerColor = Color.Transparent
                                            ),
                                            modifier = Modifier.menuAnchor().fillMaxWidth()
                                        )

                                        ExposedDropdownMenu(
                                            expanded = fadeExpanded,
                                            onDismissRequest = { fadeExpanded = false }
                                        ) {
                                            prayers.forEach { prayer ->
                                                DropdownMenuItem(
                                                    text = { Text(prayer) },
                                                    onClick = {
                                                        selectedFadePrayer = prayer
                                                        fadeExpanded = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                                
                                val config = uiState.fadeConfigs[selectedFadePrayer] ?: com.adhan.app.ui.FadeConfig(
                                    if(selectedFadePrayer=="Fajr") 5 else 0, 
                                    if(selectedFadePrayer=="Fajr") 0f else 1f
                                )
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                val duration = config.durationSeconds
                                Text(
                                    text = "Fade Duration: ${duration}s ${if(duration==0) "(Instant)" else ""}",
                                    color = Color.White
                                )
                                Slider(
                                    value = duration.toFloat(),
                                    onValueChange = { viewModel.setFadeConfig(selectedFadePrayer, duration = it.toInt()) },
                                    valueRange = 0f..30f,
                                    steps = 29,
                                    colors = SliderDefaults.colors(
                                        thumbColor = Color.White,
                                        activeTrackColor = Color.White.copy(alpha = 0.7f)
                                    )
                                )
                                
                                Spacer(modifier = Modifier.height(16.dp))
                                
                                val startVol = config.initialVolume
                                Text(
                                    text = "Initial Volume: ${(startVol * 100).toInt()}%",
                                    color = Color.White
                                )
                                Slider(
                                    value = startVol,
                                    onValueChange = { viewModel.setFadeConfig(selectedFadePrayer, volume = it) },
                                    valueRange = 0f..1f,
                                    colors = SliderDefaults.colors(
                                        thumbColor = Color.White,
                                        activeTrackColor = Color.White.copy(alpha = 0.7f)
                                    )
                                )
                            }
                        }
                    }
                }
                2 -> { // Advanced Tab
                    item {
                        SettingsSection("Tahajjud Alarm") {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Enable Tahajjud Alarm", color = Color.White)
                                Switch(
                                    checked = uiState.isTahajjudEnabled,
                                    onCheckedChange = { viewModel.setTahajjudEnabled(it) }
                                )
                            }
                            if (uiState.isTahajjudEnabled) {
                                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                                    // Audio Toggle
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Play Audio", color = Color.White)
                                        Switch(
                                            checked = uiState.isTahajjudAudioEnabled,
                                            onCheckedChange = { viewModel.setTahajjudAudioEnabled(it) }
                                        )
                                    }

                                    // Vibration Toggle
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Vibrate", color = Color.White)
                                        Switch(
                                            checked = uiState.isTahajjudVibrationEnabled,
                                            onCheckedChange = { viewModel.setTahajjudVibrationEnabled(it) }
                                        )
                                    }
                                    
                                    // Sound Selector (if Audio Enabled)
                                    if (uiState.isTahajjudAudioEnabled) {
                                        val currentUri = uiState.tahajjudSoundUri
                                        // Simple Sound Selector for Tahajjud (using Picker)
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth().clickable {
                                                // We reuse the selectedPrayerForAudio hack for now, or add specific logic
                                                // Ideally ViewModel handles "setTahajjudSound"
                                                // For now, let's just use the launcher but redirect result?
                                                // Actually, we need to intercept the launcher result.
                                                // Let's simpler: Use a separate button or reuse the hack but set a special key?
                                                // "Tahajjud" isn't in the list of 5 prayers.
                                            },
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text("Alert Sound", color = Color.White)
                                                Text(
                                                    if (currentUri == null) "Default: System Notification" else "Custom: ${currentUri.substringAfterLast("/")}", 
                                                    color = Color.White.copy(alpha = 0.5f),
                                                    style = MaterialTheme.typography.labelSmall
                                                )
                                            }
                                            // TODO: Add Picker for Tahajjud specifically if requested
                                        }
                                    }
        
                                    Spacer(modifier = Modifier.height(16.dp))
                                    
                                    Text(
                                        text = "Offset: ${uiState.tahajjudOffset} minutes before Fajr",
                                        color = Color.White.copy(alpha = 0.7f),
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                    Slider(
                                        value = uiState.tahajjudOffset.toFloat(),
                                        onValueChange = { viewModel.setTahajjudOffset(it.toInt()) },
                                        valueRange = 15f..120f,
                                        steps = 0,
                                        colors = SliderDefaults.colors(
                                            thumbColor = Color.White,
                                            activeTrackColor = Color.White.copy(alpha = 0.7f)
                                        )
                                    )
                                }
                            }
                        }
                    }
        
                    item {
                        SettingsSection("Prayer Combining") {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text(
                                    text = "Combining Threshold: ${uiState.combiningThreshold} minutes",
                                    color = Color.White.copy(alpha = 0.7f),
                                    style = MaterialTheme.typography.labelMedium
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Pairs (Dhuhr/Asr and Maghrib/Isha) will combine if their scheduled gap is within this threshold.",
                                    color = Color.White.copy(alpha = 0.5f),
                                    style = MaterialTheme.typography.labelSmall
                                )
                                Slider(
                                    value = uiState.combiningThreshold.toFloat(),
                                    onValueChange = { viewModel.setCombiningThreshold(it.toInt()) },
                                    valueRange = 30f..90f,
                                    colors = SliderDefaults.colors(
                                        thumbColor = Color.White,
                                        activeTrackColor = Color.White.copy(alpha = 0.7f)
                                    )
                                )
                                
                                Spacer(modifier = Modifier.height(16.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(1.dp)
                                        .background(Color.White.copy(alpha = 0.1f))
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                   Column(modifier = Modifier.weight(1f)) {
                                        Text("Short Night Check", color = Color.White)
                                        Text(
                                            "Combine Maghrib & Isha if night is short",
                                            color = Color.White.copy(alpha = 0.5f),
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                   }
                                   Switch(
                                        checked = uiState.isShortNightCombiningEnabled,
                                        onCheckedChange = { viewModel.setShortNightCombiningEnabled(it) }
                                   )
                                }
                                
                                if (uiState.isShortNightCombiningEnabled) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Is Night < ${uiState.shortNightThresholdHours} hours?",
                                        color = Color.White.copy(alpha = 0.7f),
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                    Slider(
                                        value = uiState.shortNightThresholdHours.toFloat(),
                                        onValueChange = { viewModel.setShortNightThreshold(it.toInt()) },
                                        valueRange = 3f..9f,
                                        steps = 5,
                                        colors = SliderDefaults.colors(
                                            thumbColor = Color.White,
                                            activeTrackColor = Color.White.copy(alpha = 0.7f)
                                        )
                                    )
                                }
        
                                Spacer(modifier = Modifier.height(16.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(1.dp)
                                        .background(Color.White.copy(alpha = 0.1f))
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                   Column(modifier = Modifier.weight(1f)) {
                                        Text("Short Asr Window", color = Color.White)
                                        Text(
                                            "Combine Dhuhr & Asr if Asr is close to Maghrib",
                                            color = Color.White.copy(alpha = 0.5f),
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                   }
                                   Switch(
                                        checked = uiState.isShortAsrCombiningEnabled,
                                        onCheckedChange = { viewModel.setShortAsrCombiningEnabled(it) }
                                   )
                                }
                                
                                if (uiState.isShortAsrCombiningEnabled) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Asr within ${uiState.shortAsrThresholdMinutes}m of Maghrib",
                                        color = Color.White.copy(alpha = 0.7f),
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                    Slider(
                                        value = uiState.shortAsrThresholdMinutes.toFloat(),
                                        onValueChange = { viewModel.setShortAsrThreshold(it.toInt()) },
                                        valueRange = 45f..120f,
                                        colors = SliderDefaults.colors(
                                            thumbColor = Color.White,
                                            activeTrackColor = Color.White.copy(alpha = 0.7f)
                                        )
                                    )
                                }
                            }
                        }
                    }
        
                    item {
                        SettingsSection("Developer Tools") {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text(
                                    text = "Test Background Playback",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Schedules an Adhan in 1 minute. Lock your device after pressing to test background activation.",
                                    color = Color.White.copy(alpha = 0.5f),
                                    style = MaterialTheme.typography.labelSmall
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                    val context = androidx.compose.ui.platform.LocalContext.current
                                Button(
                                    onClick = { 
                                        val success = viewModel.testAdhan()
                                        val msg = if (success) "Test Adhan scheduled in 2 min" else "Failed (Check permission)"
                                        android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
                                        if (!success && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                                             val intent = android.content.Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                                             context.startActivity(intent)
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF00E5FF).copy(alpha = 0.3f)
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Schedule Background Test (10s)", color = Color.White)
                                }
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                Button(
                                    onClick = { 
                                        val success = viewModel.testAdhan(120)
                                        val msg = if (success) "Test Adhan scheduled in 2 min" else "Failed (Check permission)"
                                        android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF00E5FF).copy(alpha = 0.3f)
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Schedule Background Test (2 min)", color = Color.White)
                                }
        
                                Spacer(modifier = Modifier.height(16.dp))
        
                                Button(
                                    onClick = { 
                                        viewModel.forceReschedule()
                                        android.widget.Toast.makeText(context, "Alarms Reset & Rescheduled", android.widget.Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFFFF9800).copy(alpha = 0.3f)
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Reset & Reschedule Alarms", color = Color.White)
                                }
                                
                                Spacer(modifier = Modifier.height(16.dp))
        
                                val isPlaying = uiState.isAdhanPlaying
                                
                                if (isPlaying) {
                                    Button(
                                        onClick = {
                                            viewModel.stopAdhan()
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color.Red.copy(alpha = 0.8f)
                                        ),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("STOP ADHAN", color = Color.White, fontWeight = FontWeight.Bold)
                                    }
                                } else {
                                    Button(
                                        onClick = {
                                            viewModel.playAdhanNow()
                                            android.widget.Toast.makeText(context, "Playing Adhan (Foreground)...", android.widget.Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFF00E5FF).copy(alpha = 0.3f)
                                        ),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("Play Adhan Now (Foreground Test)", color = Color.White)
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(16.dp))
                                
                                Button(
                                    onClick = onNavigateToLogs,
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color.White.copy(alpha = 0.1f)
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("View Debug Logs", color = Color.White)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsSection(title: String, content: @Composable () -> Unit) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = Color.White.copy(alpha = 0.6f),
            modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
        )
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = Color.White.copy(alpha = 0.1f)
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                content()
            }
        }
    }
}

@Composable
fun SettingsItem(label: String, isSelected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = Color.White)
        RadioButton(selected = isSelected, onClick = null)
    }
}
