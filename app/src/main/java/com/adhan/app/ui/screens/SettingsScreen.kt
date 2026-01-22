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
import com.adhan.app.domain.models.PrayerTimesCalculator
import com.adhan.app.ui.PrayerTimesViewModel

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

        Spacer(modifier = Modifier.height(32.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 200.dp)
        ) {
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
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text(
                                text = "Offset: ${uiState.tahajjudOffset} minutes before Fajr",
                                color = Color.White.copy(alpha = 0.7f),
                                style = MaterialTheme.typography.labelMedium
                            )
                            Slider(
                                value = uiState.tahajjudOffset.toFloat(),
                                onValueChange = { viewModel.setTahajjudOffset(it.toInt()) },
                                valueRange = 30f..90f,
                                steps = 3, // 30, 45, 60, 75, 90
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
                    }
                }
            }

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
                SettingsSection("Audio Configuration") {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text(
                            text = "Audio Output Route",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Select where to play the Adhan (e.g., Bluetooth speakers, phone speaker, or wireless devices).",
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

                        val isPlaying by viewModel.isAdhanPlaying.collectAsState()
                        
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
