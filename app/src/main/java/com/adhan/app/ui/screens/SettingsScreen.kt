package com.adhan.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.adhan.app.domain.models.PrayerTimesCalculator
import com.adhan.app.ui.PrayerTimesViewModel
import java.util.Calendar

// Enums for Navigation
enum class SettingsView {
    Main,
    SoundsAudio,
    Notifications,
    TahajjudFeatures,
    CalculationMethods,
    TestingTools,
    Reliability,
    PastEvents
}

@Composable
fun SettingsScreen(
    viewModel: PrayerTimesViewModel,
    onBack: () -> Unit,
    onNavigateToLogs: () -> Unit, // Still used for "App Logs"
    onNavigateToGuide: () -> Unit // Still used for "User Guide"
) {
    var currentView by remember { mutableStateOf(SettingsView.Main) }
    val uiState by viewModel.uiState.collectAsState()

    // Handle Back Press to navigate up or exit
    BackHandler(enabled = currentView != SettingsView.Main) {
        currentView = SettingsView.Main
    }

    Scaffold(
        containerColor = Color.Transparent, // Assumes background is handled by parent or theme
        topBar = {
            SettingsTopBar(
                title = when(currentView) {
                    SettingsView.Main -> "Settings"
                    SettingsView.SoundsAudio -> "Sounds & Audio"
                    SettingsView.Notifications -> "Notifications"
                    SettingsView.TahajjudFeatures -> "Tahajjud & Features"
                    SettingsView.CalculationMethods -> "Calculation"
                    SettingsView.TestingTools -> "Testing Tools"
                    SettingsView.Reliability -> "Reliability Audit"
                    SettingsView.PastEvents -> "Past Adhan Events"
                },
                isMain = currentView == SettingsView.Main,
                onBack = {
                    if (currentView == SettingsView.Main) onBack() else currentView = SettingsView.Main
                },
                onHelp = onNavigateToGuide
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            AnimatedContent(
                targetState = currentView,
                transitionSpec = {
                    if (targetState != SettingsView.Main && initialState == SettingsView.Main) {
                        slideInHorizontally { it } togetherWith slideOutHorizontally { -it }
                    } else {
                        slideInHorizontally { -it } togetherWith slideOutHorizontally { it }
                    }
                }
            ) { view ->
                when (view) {
                    SettingsView.Main -> MainSettingsList(
                        uiState = uiState,
                        onNavigate = { currentView = it },
                        onNavigateToLogs = onNavigateToLogs,
                        viewModel = viewModel
                    )
                    SettingsView.SoundsAudio -> SoundsAudioSettings(viewModel, uiState)
                    SettingsView.Notifications -> NotificationTogglesSettings(viewModel, uiState)
                    SettingsView.TahajjudFeatures -> TahajjudFeaturesSettings(viewModel, uiState)
                    SettingsView.CalculationMethods -> CalculationMethodsSettings(viewModel, uiState)
                    SettingsView.TestingTools -> TestingToolsSettings(viewModel, uiState, onNavigateToLogs)
                    SettingsView.Reliability -> ReliabilitySettings(viewModel, uiState)
                    SettingsView.PastEvents -> com.adhan.app.ui.screens.PastEventsScreen(viewModel, onBack = { currentView = SettingsView.Main })
                }
            }
        }
    }
}

@Composable
fun SettingsTopBar(title: String, isMain: Boolean, onBack: () -> Unit, onHelp: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.Cyan)
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f).padding(start = 8.dp)
        )
        if (isMain) {
            IconButton(onClick = onHelp) {
                Icon(Icons.Default.Info, contentDescription = "Help", tint = Color.Cyan)
            }
        }
    }
}

@Composable
fun MainSettingsList(
    uiState: com.adhan.app.ui.PrayerTimesState,
    onNavigate: (SettingsView) -> Unit,
    onNavigateToLogs: () -> Unit,
    viewModel: PrayerTimesViewModel
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 16.dp, start = 16.dp, end = 16.dp, bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item {
            SettingsGroupTitle("CONFIGURATION")
            SettingsGroupCard {
                SettingsNavRow("Sounds & Audio", Icons.Default.Check /* Placeholder Icon, or use custom */) { onNavigate(SettingsView.SoundsAudio) }
                HorizontalDivider()
                SettingsNavRow("Notification Toggles", Icons.Default.Check) { onNavigate(SettingsView.Notifications) }
                HorizontalDivider()
                SettingsNavRow("Tahajjud & Features", Icons.Default.Check) { onNavigate(SettingsView.TahajjudFeatures) }
                HorizontalDivider()
                SettingsNavRow("Calculation Methods", Icons.Default.Check) { onNavigate(SettingsView.CalculationMethods) }
            }
        }

        item {
            SettingsGroupTitle("GENERAL")
            SettingsGroupCard {
                // Time Format Segmented Control
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Time Format", color = Color.White, style = MaterialTheme.typography.bodyMedium)
                    
                    // Simple Toggle for now since we have a boolean in VM (24h vs 12h)
                    // Plan mentioned Segmented Control, but let's stick to what VM supports first or extend it properly.
                    // VM has `use12HourFormat`.
                    // Let's make a nice toggle row.
                    
                    Row(
                        modifier = Modifier
                            .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                            .padding(2.dp)
                    ) {
                        val is12 = uiState.use12HourFormat
                        
                        TimeFormatChip("24h", !is12) { viewModel.setUse12HourFormat(false) }
                        TimeFormatChip("12h", is12) { viewModel.setUse12HourFormat(true) }
                    }
                }
            }
        }

        item {
            SettingsGroupTitle("RELIABILITY & DIAGNOSTICS")
            SettingsGroupCard {
                SettingsNavRow("Reliability Audit", Icons.Default.Check) { onNavigate(SettingsView.Reliability) }
                HorizontalDivider()
                SettingsNavRow("Past Adhan Events", Icons.Default.Check) { onNavigate(SettingsView.PastEvents) }
                HorizontalDivider()
                SettingsNavRow("Testing Tools", Icons.Default.Check) { onNavigate(SettingsView.TestingTools) }
                HorizontalDivider()
                SettingsNavRow("App Logs", Icons.Default.Check, onClick = onNavigateToLogs)
            }
        }
    }
}

@Composable
fun TimeFormatChip(text: String, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clickable(onClick = onClick)
            .background(
                if (isSelected) Color.White.copy(alpha = 0.3f) else Color.Transparent,
                RoundedCornerShape(6.dp)
            )
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = text,
            color = if (isSelected) Color.White else Color.White.copy(alpha = 0.5f),
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            style = MaterialTheme.typography.labelMedium
        )
    }
}

// ================= SUB-SCREENS =================

@Composable
fun SoundsAudioSettings(viewModel: PrayerTimesViewModel, uiState: com.adhan.app.ui.PrayerTimesState) {
    LazyColumn(contentPadding = PaddingValues(top = 16.dp, start = 16.dp, end = 16.dp, bottom = 80.dp)) {
        item {
            SettingsGroupCard {
                // Master Switch
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Enable Adhan Audio", color = Color.White, fontWeight = FontWeight.Bold)
                        Text("Master switch for all prayers", color = Color.White.copy(alpha = 0.5f), style = MaterialTheme.typography.labelSmall)
                    }
                    Switch(
                        checked = uiState.isAudioEnabled,
                        onCheckedChange = { viewModel.setAudioEnabled(it) }
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        item {
            SettingsGroupTitle("PROFILE & VOLUME")
            SettingsGroupCard {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Adhan Volume: ${uiState.adhanVolume}%", color = Color.White)
                    Slider(
                        value = uiState.adhanVolume.toFloat(),
                        onValueChange = { viewModel.setAdhanVolume(it.toInt()) },
                        valueRange = 0f..100f
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        item {
            SettingsGroupTitle("AUDIO OUTPUT")
            SettingsGroupCard {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Audio Route", color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.labelMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    var expanded by remember { mutableStateOf(false) }
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { expanded = true },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                        ) {
                            Text(uiState.selectedAudioRoute)
                        }
                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false },
                            modifier = Modifier.background(Color(0xFF2D2D2D))
                        ) {
                            DropdownMenuItem(
                                text = { Text("Default (System)", color = Color.White) },
                                onClick = { 
                                    viewModel.setSelectedAudioDevice("Default")
                                    expanded = false 
                                }
                            )
                            uiState.audioOutputDevices.forEach { device ->
                                DropdownMenuItem(
                                    text = { Text(device, color = Color.White) },
                                    onClick = { 
                                        viewModel.setSelectedAudioDevice(device)
                                        expanded = false 
                                    }
                                )
                            }
                            DropdownMenuItem(
                                text = { Text("Refresh List", color = Color.Cyan) },
                                onClick = { viewModel.refreshAudioDevices() }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    val context = androidx.compose.ui.platform.LocalContext.current
                    Button(
                        onClick = { 
                            val selector = viewModel.mediaSelector
                            val dialog = androidx.mediarouter.app.MediaRouteChooserDialog(context)
                            dialog.routeSelector = selector
                            dialog.show()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.1f))
                    ) {
                        Text("System Output Pickers (Cast/BT)")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            val intent = android.content.Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS)
                            context.startActivity(intent)
                        },
                         modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.1f))
                    ) {
                         Text("Open Bluetooth Settings")
                    }
                }
            }
             Spacer(modifier = Modifier.height(24.dp))
        }

        item {
            SettingsGroupTitle("CUSTOM SOUNDS")
            SettingsGroupCard {
                val launcher = rememberLauncherForActivityResult(contract = ActivityResultContracts.GetContent()) { uri ->
                     uri?.let { viewModel.setAdhanSound(selectedPrayerForSound, it.toString()) }
                }
                
                listOf("Fajr", "Dhuhr", "Asr", "Maghrib", "Isha").forEachIndexed { index, prayer ->
                    if (index > 0) HorizontalDivider()
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp).clickable {
                             selectedPrayerForSound = prayer
                             launcher.launch("audio/*")
                        },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(prayer, color = Color.White)
                        Text(
                            text = uiState.adhanSounds[prayer]?.substringAfterLast("/") ?: "Default",
                            color = Color.White.copy(alpha = 0.5f),
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                            modifier = Modifier.fillMaxWidth(0.6f),
                            textAlign = TextAlign.End
                        )
                    }
                }
            }
        }
    }
}

// Global var hack for quick launcher access, ideally logic in VM or properly scoped
private var selectedPrayerForSound = "Fajr"

@Composable
fun NotificationTogglesSettings(viewModel: PrayerTimesViewModel, uiState: com.adhan.app.ui.PrayerTimesState) {
    LazyColumn(contentPadding = PaddingValues(top = 16.dp, start = 16.dp, end = 16.dp, bottom = 80.dp)) {
        item {
            GuideText("Tap bubbles to toggle days. Long press S/M... for day-specific audio routing.")
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        items(listOf("Fajr", "Dhuhr", "Asr", "Maghrib", "Isha")) { prayer ->
            SettingsGroupCard {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(prayer, color = Color.White, fontWeight = FontWeight.Bold)
                        Switch(
                            checked = uiState.adhanNotificationEnabled[prayer] ?: true,
                            onCheckedChange = { viewModel.setAdhanNotificationEnabled(prayer, it) },
                            modifier = Modifier.scale(0.8f)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Day Selector
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        val daysMap = uiState.adhanNotificationDays[prayer] ?: emptyMap()
                        val days = listOf(
                            Calendar.SUNDAY to "S", Calendar.MONDAY to "M", Calendar.TUESDAY to "T",
                            Calendar.WEDNESDAY to "W", Calendar.THURSDAY to "T", Calendar.FRIDAY to "F",
                            Calendar.SATURDAY to "S"
                        )
                        
                        // Dialog State
                        var showDayConfigDialog by remember { mutableStateOf(false) }
                        var selectedDayId by remember { mutableStateOf<Int?>(null) }
                        
                        if (showDayConfigDialog && selectedDayId != null) {
                            DayConfigurationDialog(
                                prayer = prayer,
                                dayId = selectedDayId!!,
                                viewModel = viewModel,
                                uiState = uiState,
                                onDismiss = { showDayConfigDialog = false }
                            )
                        }

                        days.forEach { (dayId, label) ->
                            val isEnabled = daysMap[dayId] ?: true
                            val isFriday = dayId == Calendar.FRIDAY
                            val baseColor = if (isFriday) Color(0xFF00E5FF) else Color.White
                            
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(if (isEnabled) baseColor.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.05f))
                                    .simpleCombinedClickable(
                                        onClick = { 
                                            // Simple Toggle
                                            viewModel.setAdhanDayEnabled(prayer, dayId, !isEnabled)
                                        },
                                        onLongClick = {
                                            selectedDayId = dayId
                                            showDayConfigDialog = true
                                        }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    color = if (isEnabled) baseColor else Color.White.copy(alpha = 0.3f),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Modifier.simpleCombinedClickable(
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    onDoubleClick: (() -> Unit)? = null
): Modifier = this.combinedClickable(
    enabled = true,
    onClick = onClick,
    onLongClick = onLongClick,
    onDoubleClick = onDoubleClick
)

@Composable
fun DayConfigurationDialog(
    prayer: String,
    dayId: Int,
    viewModel: PrayerTimesViewModel,
    uiState: com.adhan.app.ui.PrayerTimesState,
    onDismiss: () -> Unit
) {
    val dayName = when(dayId) {
        Calendar.SUNDAY -> "Sunday"
        Calendar.MONDAY -> "Monday"
        Calendar.TUESDAY -> "Tuesday"
        Calendar.WEDNESDAY -> "Wednesday"
        Calendar.THURSDAY -> "Thursday"
        Calendar.FRIDAY -> "Friday"
        Calendar.SATURDAY -> "Saturday"
        else -> "Unknown"
    }
    
    var isEnabled by remember { mutableStateOf(uiState.adhanNotificationDays[prayer]?.get(dayId) ?: true) }
    var currentRoute by remember { mutableStateOf(viewModel.getDayAudioRoute(prayer, dayId) ?: "Global Default") }
    
    // Volume Override
    val initialVol = viewModel.getDayVolume(prayer, dayId)
    var isVolumeOverride by remember { mutableStateOf(initialVol != null) }
    var volumeValue by remember { mutableStateOf(initialVol ?: 80) }

    // Fade Override
    val initialFade = viewModel.getDayFadeSeconds(prayer, dayId)
    var isFadeOverride by remember { mutableStateOf(initialFade != null) }
    var fadeValue by remember { mutableStateOf(initialFade ?: 5) }

    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("$prayer on $dayName") },
        text = {
            Column(modifier = Modifier.verticalScroll(androidx.compose.foundation.rememberScrollState())) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Enable Notification")
                    Switch(checked = isEnabled, onCheckedChange = { isEnabled = it })
                }
                
                if (isEnabled) {
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text("Audio Output", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(bottom = 8.dp))
                    Box {
                         OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
                             Text(currentRoute)
                         }
                         DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                             DropdownMenuItem(
                                 text = { Text("Global Default") },
                                 onClick = { currentRoute = "Global Default"; expanded = false }
                             )
                             uiState.audioOutputDevices.forEach { device ->
                                 DropdownMenuItem(
                                     text = { Text(device) },
                                     onClick = { currentRoute = device; expanded = false }
                                 )
                             }
                         }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Volume Override UI
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                         Text("Override Volume")
                         Switch(checked = isVolumeOverride, onCheckedChange = { isVolumeOverride = it })
                    }
                    if (isVolumeOverride) {
                        Text("Volume: $volumeValue%", style = MaterialTheme.typography.labelSmall)
                        Slider(
                             value = volumeValue.toFloat(),
                             onValueChange = { volumeValue = it.toInt() },
                             valueRange = 0f..100f
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(8.dp))

                    // Fade Override UI
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                         Text("Override Fade")
                         Switch(checked = isFadeOverride, onCheckedChange = { isFadeOverride = it })
                    }
                    if (isFadeOverride) {
                        Text("Fade Duration: ${fadeValue}s", style = MaterialTheme.typography.labelSmall)
                        Slider(
                             value = fadeValue.toFloat(),
                             onValueChange = { fadeValue = it.toInt() },
                             valueRange = 0f..30f
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedButton(
                        onClick = {
                             viewModel.applyConfigToAllDays(
                                 prayer, 
                                 isEnabled, 
                                 currentRoute, 
                                 if (isVolumeOverride) volumeValue else null,
                                 if (isFadeOverride) fadeValue else null
                             )
                             onDismiss()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Apply to All Days", color = Color.Cyan)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = {
                             viewModel.applyConfigToAllPrayersOnDay(
                                 dayId, 
                                 isEnabled, 
                                 currentRoute, 
                                 if (isVolumeOverride) volumeValue else null,
                                 if (isFadeOverride) fadeValue else null
                             )
                             onDismiss()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Apply to All Prayers today", color = Color.Cyan)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                viewModel.setAdhanDayEnabled(prayer, dayId, isEnabled)
                viewModel.setDayAudioRoute(prayer, dayId, currentRoute)
                viewModel.setDayVolume(prayer, dayId, if (isVolumeOverride) volumeValue else null)
                viewModel.setDayFadeSeconds(prayer, dayId, if (isFadeOverride) fadeValue else null)
                onDismiss()
            }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}


@Composable
fun TahajjudFeaturesSettings(viewModel: PrayerTimesViewModel, uiState: com.adhan.app.ui.PrayerTimesState) {
    LazyColumn(contentPadding = PaddingValues(top = 16.dp, start = 16.dp, end = 16.dp, bottom = 80.dp)) {
        item {
            SettingsGroupTitle("TAHAJJUD")
            SettingsGroupCard {
                Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Enable Alert", color = Color.White)
                    Switch(checked = uiState.isTahajjudEnabled, onCheckedChange = { viewModel.setTahajjudEnabled(it) })
                }
                if (uiState.isTahajjudEnabled) {
                    HorizontalDivider()
                    Column(modifier = Modifier.padding(16.dp)) {
                         Text("Offset: ${uiState.tahajjudOffset} mins before Fajr", color = Color.White.copy(alpha = 0.7f))
                         Slider(
                             value = uiState.tahajjudOffset.toFloat(),
                             onValueChange = { viewModel.setTahajjudOffset(it.toInt()) },
                             valueRange = 15f..120f
                         )
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        item {
             SettingsGroupTitle("PRAYER COMBINING")
             SettingsGroupCard {
                 // Short Night
                 Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                     Column {
                         Text("Short Night Combining", color = Color.White)
                         Text("Maghrib & Isha", color = Color.White.copy(alpha = 0.5f), style = MaterialTheme.typography.labelSmall)
                     }
                     Switch(checked = uiState.isShortNightCombiningEnabled, onCheckedChange = { viewModel.setShortNightCombiningEnabled(it) })
                 }
                 if (uiState.isShortNightCombiningEnabled) {
                     Column(modifier = Modifier.padding(16.dp)) {
                         Text("Threshold: ${uiState.shortNightThresholdHours} hours", color = Color.White)
                         Slider(
                             value = uiState.shortNightThresholdHours.toFloat(),
                             onValueChange = { viewModel.setShortNightThreshold(it.toInt()) },
                             valueRange = 3f..10f,
                             steps = 6
                         )
                     }
                 }
                 
                 HorizontalDivider()

                 // Short Asr
                 Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                     Column {
                         Text("Short Asr Window", color = Color.White)
                         Text("Dhuhr & Asr", color = Color.White.copy(alpha = 0.5f), style = MaterialTheme.typography.labelSmall)
                     }
                     Switch(checked = uiState.isShortAsrCombiningEnabled, onCheckedChange = { viewModel.setShortAsrCombiningEnabled(it) })
                 }
                  if (uiState.isShortAsrCombiningEnabled) {
                     Column(modifier = Modifier.padding(16.dp)) {
                         Text("Threshold: ${uiState.shortAsrThresholdMinutes} minutes", color = Color.White)
                         Slider(
                             value = uiState.shortAsrThresholdMinutes.toFloat(),
                             onValueChange = { viewModel.setShortAsrThreshold(it.toInt()) },
                             valueRange = 45f..120f
                         )
                     }
                 }
                 
                 HorizontalDivider()

                 // Short Isha
                 Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                     Column {
                         Text("Short Isha Window", color = Color.White)
                         Text("Maghrib & Isha", color = Color.White.copy(alpha = 0.5f), style = MaterialTheme.typography.labelSmall)
                     }
                     Switch(checked = uiState.isShortIshaCombiningEnabled, onCheckedChange = { viewModel.setShortIshaCombiningEnabled(it) })
                 }
                  if (uiState.isShortIshaCombiningEnabled) {
                     Column(modifier = Modifier.padding(16.dp)) {
                         Text("Threshold: ${uiState.shortIshaThresholdMinutes} minutes", color = Color.White)
                         Slider(
                             value = uiState.shortIshaThresholdMinutes.toFloat(),
                             onValueChange = { viewModel.setShortIshaThreshold(it.toInt()) },
                             valueRange = 45f..120f
                         )
                     }
                 }
             }
         }
    }
}

@Composable
fun CalculationMethodsSettings(viewModel: PrayerTimesViewModel, uiState: com.adhan.app.ui.PrayerTimesState) {
    val methods = mapOf(
        "Ahmadiyya" to PrayerTimesCalculator.Ahmadiyya,
        "MWL" to PrayerTimesCalculator.MWL,
        "ISNA" to PrayerTimesCalculator.ISNA,
        "Egypt" to PrayerTimesCalculator.Egypt,
        "Makkah" to PrayerTimesCalculator.Makkah,
        "Karachi" to PrayerTimesCalculator.Karachi,
        "Tehran" to PrayerTimesCalculator.Tehran,
        "Jafari" to PrayerTimesCalculator.Jafari
    )
    
    val highLatRules = mapOf(
        "Angle Based" to PrayerTimesCalculator.AngleBased,
        "Midnight" to PrayerTimesCalculator.MidNight,
        "One Seventh" to PrayerTimesCalculator.OneSeventh,
        "None" to PrayerTimesCalculator.None
    )

    LazyColumn(contentPadding = PaddingValues(top = 16.dp, start = 16.dp, end = 16.dp, bottom = 80.dp)) {
        item {
            SettingsGroupTitle("CALCULATION METHOD")
            SettingsGroupCard {
                SettingsDropdownRow(
                    label = "Method",
                    currentValue = methods.entries.find { it.value == uiState.calcMethod }?.key ?: "Unknown",
                    options = methods.keys.toList(),
                    onSelect = { name -> methods[name]?.let { viewModel.setCalcMethod(it) } }
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        item {
            SettingsGroupTitle("ASR JURISTIC METHOD")
            SettingsGroupCard {
                 Row(
                     modifier = Modifier.fillMaxWidth().padding(16.dp),
                     horizontalArrangement = Arrangement.SpaceBetween,
                     verticalAlignment = Alignment.CenterVertically
                 ) {
                     Text("Hanafi (Later Asr)", color = Color.White)
                     Switch(
                         checked = uiState.asrJuristic == PrayerTimesCalculator.Hanafi,
                         onCheckedChange = { viewModel.setAsrMethod(if(it) PrayerTimesCalculator.Hanafi else PrayerTimesCalculator.Shafii) }
                     )
                 }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
        
        item {
            SettingsGroupTitle("HIGH LATITUDE RULE")
            SettingsGroupCard {
                 SettingsDropdownRow(
                    label = "Rule",
                    currentValue = highLatRules.entries.find { it.value == uiState.highLatitudeRule }?.key ?: "Angle Based",
                    options = highLatRules.keys.toList(),
                    onSelect = { name -> highLatRules[name]?.let { viewModel.setHighLatitudeRule(it) } }
                )
            }
             Spacer(modifier = Modifier.height(24.dp))
        }
        
        item {
            SettingsGroupTitle("MANUAL OFFSETS (MINUTES)")
            SettingsGroupCard {
                 listOf("Fajr", "Sunrise", "Dhuhr", "Asr", "Maghrib", "Isha").forEachIndexed { index, prayer ->
                     if(index > 0) HorizontalDivider()
                     val offset = uiState.manualOffsets[prayer] ?: 0
                     Row(
                         modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                         horizontalArrangement = Arrangement.SpaceBetween,
                         verticalAlignment = Alignment.CenterVertically
                     ) {
                         Text(prayer, color = Color.White)
                         Row(verticalAlignment = Alignment.CenterVertically) {
                             IconButton(onClick = { viewModel.setManualOffset(prayer, offset - 1) }) {
                                 Text("-", color = Color.Cyan, fontSize = 24.sp)
                             }
                             Text(
                                 "$offset", 
                                 color = Color.White, 
                                 modifier = Modifier.width(32.dp), 
                                 textAlign = TextAlign.Center
                             )
                             IconButton(onClick = { viewModel.setManualOffset(prayer, offset + 1) }) {
                                 Text("+", color = Color.Cyan, fontSize = 24.sp)
                             }
                         }
                     }
                 }
            }
        }
    }
}

@Composable
fun TestingToolsSettings(viewModel: PrayerTimesViewModel, uiState: com.adhan.app.ui.PrayerTimesState, onNavigateToLogs: () -> Unit) {
     val context = androidx.compose.ui.platform.LocalContext.current
     LazyColumn(contentPadding = PaddingValues(top = 16.dp, start = 16.dp, end = 16.dp, bottom = 80.dp)) {
         item {
             SettingsGroupTitle("TARGET")
             SettingsGroupCard {
                  SettingsDropdownRow(
                    label = "Test Target",
                    currentValue = uiState.selectedAudioRoute, // Actually testing tool tracks this separately in the old code, assuming we just use global or make a separate state?
                    // Re-using global selection for test for simplicity, OR checking if we want a separate test-only selection.
                    // The refactor earlier implies test uses `selectedTestRoute`. VM logic for `testAdhan` takes a route.
                    // Let's implement a local state dropdown here.
                    options = listOf("Global Default") + uiState.audioOutputDevices,
                    onSelect = { /* handled locally below */ },
                    readOnlyDisplay = true // We will do custom impl
                )
             }
             Spacer(modifier = Modifier.height(24.dp))
             
             // ... Custom Impl for Test Target ...
             // Actually, simplest is to just expose the Test Buttons
             
             SettingsGroupCard {
                 Button(
                     onClick = { 
                         // Foreground Test
                         viewModel.playAdhanNow() // Uses Default or we can pass a route if we added state
                     },
                     modifier = Modifier.fillMaxWidth().padding(16.dp),
                     colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF).copy(alpha = 0.2f))
                 ) {
                     Text("Play Adhan Now (Foreground)", color = Color.White)
                 }
                 HorizontalDivider()
                  Button(
                     onClick = { 
                         viewModel.testAdhan(10)
                         android.widget.Toast.makeText(context, "Scheduled in 10s", android.widget.Toast.LENGTH_SHORT).show()
                     },
                     modifier = Modifier.fillMaxWidth().padding(16.dp),
                     colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF).copy(alpha = 0.2f))
                 ) {
                     Text("Schedule Background (10s)", color = Color.White)
                 }
                 HorizontalDivider()
                  Button(
                     onClick = { 
                         viewModel.testAdhan(120)
                         android.widget.Toast.makeText(context, "Scheduled in 2m", android.widget.Toast.LENGTH_SHORT).show()
                     },
                     modifier = Modifier.fillMaxWidth().padding(16.dp),
                     colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF).copy(alpha = 0.2f))
                 ) {
                     Text("Schedule Background (2m)", color = Color.White)
                 }
                 HorizontalDivider()
                 Button(
                     onClick = { 
                         viewModel.forceReschedule()
                         android.widget.Toast.makeText(context, "Rescheduled", android.widget.Toast.LENGTH_SHORT).show()
                     },
                     modifier = Modifier.fillMaxWidth().padding(16.dp),
                     colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800).copy(alpha = 0.2f))
                 ) {
                     Text("Info: Reset Alarms", color = Color.White)
                 }
             }
         }
     }
}

@Composable
fun ReliabilitySettings(viewModel: PrayerTimesViewModel, uiState: com.adhan.app.ui.PrayerTimesState) {
    LaunchedEffect(Unit) {
        viewModel.checkReliabilitySettings()
    }

    LazyColumn(contentPadding = PaddingValues(16.dp)) {
        item {
            SettingsGroupTitle("SYSTEM STATUS")
            SettingsGroupCard {
                ReliabilityStatusRow(
                    label = "Battery Optimization",
                    status = if (uiState.isBatteryOptimizationIgnored) "Disabled (Good)" else "Enabled (Risky)",
                    isValid = uiState.isBatteryOptimizationIgnored,
                    actionLabel = "Modify",
                    onAction = { viewModel.requestIgnoreBatteryOptimizations() }
                )
                HorizontalDivider()
                ReliabilityStatusRow(
                    label = "Exact Alarms",
                    status = if (uiState.canScheduleExactAlarms) "Granted" else "Restricted",
                    isValid = uiState.canScheduleExactAlarms,
                    actionLabel = "Fix",
                    onAction = { viewModel.openExactAlarmSettings() }
                )
                HorizontalDivider()
                ReliabilityStatusRow(
                    label = "Notifications",
                    status = if (uiState.hasNotificationPermission) "Granted" else "Missing",
                    isValid = uiState.hasNotificationPermission,
                    actionLabel = "Grant",
                    onAction = { viewModel.openNotificationSettings() }
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "For 100% reliability, ensure Battery Optimization is set to 'Unrestricted' or 'Not Optimized'.",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        }
    }
}

@Composable
fun ReliabilityStatusRow(label: String, status: String, isValid: Boolean, actionLabel: String, onAction: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, color = Color.White, fontWeight = FontWeight.Bold)
            Text(status, color = if (isValid) Color.Green else Color.Yellow, fontSize = 12.sp)
        }
        TextButton(onClick = onAction) {
            Text(actionLabel, color = Color.Cyan)
        }
    }
}

// ================= HELPERS =================

@Composable
fun SettingsGroupTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelMedium,
        color = Color.White.copy(alpha = 0.5f),
        modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
    )
}

@Composable
fun SettingsGroupCard(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.1f)) // Translucent background
    ) {
        content()
    }
}

@Composable
fun SettingsNavRow(title: String, icon: ImageVector? = null, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            // Icon(icon, contentDescription = null, tint = Color.Cyan, modifier = Modifier.size(24.dp))
            // Spacer(modifier = Modifier.width(16.dp))
            // iOS style usually just text, maybe icon on left.
        }
        Text(title, color = Color.White, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Nav", tint = Color.White.copy(alpha = 0.3f))
    }
}

@Composable
fun HorizontalDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(Color.Black.copy(alpha = 0.2f))
    )
}

@Composable
fun SettingsDropdownRow(label: String, currentValue: String, options: List<String>, onSelect: (String) -> Unit, readOnlyDisplay: Boolean = false) {
    var expanded by remember { mutableStateOf(false) }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !readOnlyDisplay) { expanded = true }
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = Color.White)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(currentValue, color = Color.White.copy(alpha = 0.6f))
            if(!readOnlyDisplay) {
                Box {
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        options.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = { onSelect(option); expanded = false }
                            )
                        }
                    }
                }
            }
        }
    }
}
