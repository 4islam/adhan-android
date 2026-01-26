package com.adhan.app.infra

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.adhan.app.MainActivity
import com.adhan.app.R
import javax.inject.Inject
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import kotlinx.coroutines.cancel
import kotlinx.coroutines.withContext

@AndroidEntryPoint
class AdhanService : Service() {

    @Inject
    lateinit var logRepository: com.adhan.app.domain.LogRepository

    @Inject
    lateinit var audioRouter: com.adhan.app.infra.AudioRouter

    @Inject
    lateinit var audioFader: com.adhan.app.infra.AudioFader

    @Inject
    lateinit var playbackStateRepository: com.adhan.app.domain.PlaybackStateRepository

    @Inject
    lateinit var mediaRouterHelper: com.adhan.app.infra.MediaRouterHelper

    private var player: ExoPlayer? = null
    private var mediaSession: androidx.media3.session.MediaSession? = null
    private val NOTIFICATION_ID = 1001
    private val CHANNEL_ID = "adhan_alerts_v2"
    private val serviceScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO + kotlinx.coroutines.SupervisorJob())
    private var initialVolume: Int? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_SPEECH)
            .build()

        val simplePlayer = ExoPlayer.Builder(this).build().apply {
            setAudioAttributes(audioAttributes, true)
            setWakeMode(C.WAKE_MODE_LOCAL)
            addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    val stateStr = when(playbackState) {
                        Player.STATE_IDLE -> "IDLE"
                        Player.STATE_BUFFERING -> "BUFFERING"
                        Player.STATE_READY -> "READY"
                        Player.STATE_ENDED -> "ENDED"
                        else -> "UNKNOWN($playbackState)"
                    }
                    val msg = "ExoPlayer State: $stateStr"
                    android.util.Log.d("AdhanService", msg)
                    serviceScope.launch { logRepository.log(msg) }

                    if (playbackState == Player.STATE_READY) {
                        logSystemState("Playback READY")
                    }

                    if (playbackState == Player.STATE_ENDED) {
                        stopForeground(STOP_FOREGROUND_REMOVE)
                        stopSelf()
                    }
                }
                
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                     val msg = "ExoPlayer IsPlaying: $isPlaying"
                     android.util.Log.d("AdhanService", msg)
                     serviceScope.launch { logRepository.log(msg) }
                     serviceScope.launch { playbackStateRepository.setPlaying(isPlaying) }
                }

                override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                     val msg = "ExoPlayer ERROR: ${error.message}"
                     android.util.Log.e("AdhanService", msg)
                     serviceScope.launch { logRepository.log(msg, true) }
                }
            })
        }
        player = simplePlayer
        
        mediaSession = androidx.media3.session.MediaSession.Builder(this, simplePlayer).build()
    }

    private fun createNotificationChannel() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val name = "Adhan Alerts"
            val descriptionText = "Notifications for prayer times"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                setSound(null, null)
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun logSystemState(tag: String) {
        try {
            val audioManager = getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
            val alarmVol = audioManager.getStreamVolume(android.media.AudioManager.STREAM_ALARM)
            val maxAlarmVol = audioManager.getStreamMaxVolume(android.media.AudioManager.STREAM_ALARM)
            val musicVol = audioManager.getStreamVolume(android.media.AudioManager.STREAM_MUSIC)
            val maxMusicVol = audioManager.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC)
            
            val msg = "DIAGNOSTICS($tag): AlarmVol=$alarmVol/$maxAlarmVol, MusicVol=$musicVol/$maxMusicVol"
            android.util.Log.d("AdhanService", msg)
            serviceScope.launch { logRepository.log(msg) }
            
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                val devices = audioManager.getDevices(android.media.AudioManager.GET_DEVICES_OUTPUTS)
                val deviceLog = devices.joinToString(", ") { 
                    "${it.productName}(Type:${it.type}, ID:${it.id})" 
                }
                val routeMsg = "AUDIO ROUTES($tag): $deviceLog"
                android.util.Log.d("AdhanService", routeMsg)
                serviceScope.launch { logRepository.log(routeMsg) }
            }
        } catch (e: Exception) {
             android.util.Log.e("AdhanService", "Log failed", e)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val prayerName = intent?.getStringExtra("prayer_name") ?: "Prayer"
        
        if (intent?.action == "com.adhan.app.action.STOP") {
            android.util.Log.d("AdhanService", "Received STOP command")
            serviceScope.launch { logRepository.log("AdhanService: Received STOP command") }
            player?.stop()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }
        
        android.util.Log.d("AdhanService", "onStartCommand: $prayerName")
        serviceScope.launch { logRepository.log("AdhanService started for: $prayerName") }
        
        // Start MediaRouter Scanning immediately
        mediaRouterHelper.init()
        mediaRouterHelper.startScanning()

        logSystemState("Start: $prayerName")

        val prefs = getSharedPreferences("adhan_prefs", Context.MODE_PRIVATE)
        
        // Tahajjud Special Handling
        if (prayerName.equals("Tahajjud", ignoreCase = true)) {
            val isAudioEnabled = prefs.getBoolean("tahajjud_audio_enabled", false)
            val isVibrationEnabled = prefs.getBoolean("tahajjud_vibration_enabled", false)
            
            // Vibration Logic
            if (isVibrationEnabled) {
                val vibrator = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                     val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as android.os.VibratorManager
                     vibratorManager.defaultVibrator
                } else {
                     getSystemService(Context.VIBRATOR_SERVICE) as android.os.Vibrator
                }
                
                if (vibrator.hasVibrator()) {
                     val timings = longArrayOf(0, 500, 500, 500, 500)
                     val amplitudes = intArrayOf(0, 50, 0, 100, 0)
                     
                     if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                          vibrator.vibrate(android.os.VibrationEffect.createWaveform(timings, amplitudes, -1))
                     } else {
                          vibrator.vibrate(timings, -1)
                     }
                }
                serviceScope.launch { logRepository.log("Tahajjud: Vibrated.") }
            }

            if (!isAudioEnabled) {
                android.util.Log.d("AdhanService", "Tahajjud audio disabled.")
                serviceScope.launch { logRepository.log("Tahajjud audio disabled.") }
                startForeground(NOTIFICATION_ID, createNotification(prayerName))
                stopSelf() 
                return START_NOT_STICKY
            }
        }

        // Volume Override Logic
        try {
            val adhanVolumePercent = prefs.getInt("adhan_volume", 80)
            val audioManager = getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
            val maxVol = audioManager.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC)
            val currentVol = audioManager.getStreamVolume(android.media.AudioManager.STREAM_MUSIC)
            
            if (initialVolume == null) {
                initialVolume = currentVol
            }
            
            val targetVol = (maxVol * (adhanVolumePercent / 100f)).toInt()
            android.util.Log.d("AdhanService", "Setting Volume: Pct=$adhanVolumePercent, Target=$targetVol/$maxVol, Old=$currentVol")
            serviceScope.launch { logRepository.log("Volume Override: Pct=$adhanVolumePercent, Target=$targetVol/$maxVol") }
            
            audioManager.setStreamVolume(android.media.AudioManager.STREAM_MUSIC, targetVol, 0)
        } catch (e: Exception) {
            android.util.Log.e("AdhanService", "Failed to set volume", e)
        }

        // Ensure notification is posted immediately
        startForeground(NOTIFICATION_ID, createNotification(prayerName))
        
        serviceScope.launch {
            // Delay slightly to allow MediaRouter to discover devices if needed
            // This is a tradeoff: delayed playback vs correct routing. 
            // 2 seconds should be enough for cached routes or quick discovery
             if (prefs.getString("selected_audio_route", "Default")?.startsWith("Network:") == true) {
                 kotlinx.coroutines.delay(1500) 
             }
             
             withContext(kotlinx.coroutines.Dispatchers.Main) {
                playAdhan(prayerName, prefs)
             }
        }
        
        return START_NOT_STICKY
    }

    private suspend fun playAdhan(prayerName: String, prefs: android.content.SharedPreferences) {
        try {
            var customUri: String? = null
            
            if (prayerName.equals("Tahajjud", ignoreCase = true)) {
                customUri = prefs.getString("tahajjud_sound_uri", null)
            } else {
                customUri = prefs.getString("adhan_sound_$prayerName", null)
            }
            
            val isFajrOrTahajjud = prayerName.contains("Fajr", ignoreCase = true) || prayerName.equals("Tahajjud", ignoreCase = true)
            val defaultDur = if (isFajrOrTahajjud) 5 else 0
            val defaultVol = if (isFajrOrTahajjud) 0f else 1.0f
            
            val fadeDurationSeconds = prefs.getInt("fade_duration_$prayerName", defaultDur)
            val fadeStartVolume = prefs.getFloat("fade_vol_$prayerName", defaultVol)
            
            val mediaItem: MediaItem? = if (customUri != null) {
                MediaItem.fromUri(android.net.Uri.parse(customUri))
            } else {
                if (prayerName.equals("Tahajjud", ignoreCase = true)) {
                     val defaultUri = android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION)
                     val finalUri = defaultUri ?: android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_ALARM)
                     MediaItem.fromUri(finalUri)
                } else {
                    val resourceName = if (prayerName.contains("Fajr", ignoreCase = true)) "adhan_fajr" else "adhan_regular"
                    val rawResourceId = resources.getIdentifier(resourceName, "raw", packageName)
                    if (rawResourceId != 0) {
                        MediaItem.fromUri("android.resource://$packageName/$rawResourceId")
                    } else {
                        null
                    }
                }
            }

            // Apply Custom Audio Routing
            // Apply Custom Audio Routing
            // 1. Check for Prayer+Day Helper
            val calendar = java.util.Calendar.getInstance()
            calendar.time = java.util.Date()
            val dayOfWeek = calendar.get(java.util.Calendar.DAY_OF_WEEK)
            
            val specificRouteKey = "audio_route_${prayerName}_$dayOfWeek"
            val specificRoute = prefs.getString(specificRouteKey, null)
            
            val selectedRoute = specificRoute ?: prefs.getString("selected_audio_route", "Default")
            
            if (selectedRoute != null && selectedRoute != "Default") {
                 android.util.Log.d("AdhanService", "Routing Decision: Specific=$specificRoute, Global=${prefs.getString("selected_audio_route", "Default")}, Final=$selectedRoute")
                 
                 if (selectedRoute.startsWith("Network: ")) {
                     val routeName = selectedRoute.removePrefix("Network: ")
                     android.util.Log.d("AdhanService", "Attempting to select Network Route: $routeName")
                     mediaRouterHelper.selectRouteByName(routeName)
                     // Give it a moment to connect?
                     kotlinx.coroutines.delay(1000) 
                 } else if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                     audioRouter.routeAudioWithLogging(player!!, selectedRoute)
                 }
            }

            if (mediaItem != null) {
                player?.let {
                    if (it.playbackState == Player.STATE_IDLE || it.playbackState == Player.STATE_ENDED) {
                        it.setMediaItem(mediaItem)
                        it.prepare()
                        it.volume = fadeStartVolume 
                        it.play()
                        
                        // We are already on Main via withContext(Dispatchers.Main) in caller
                        audioFader.startFadeIn(it, fadeDurationSeconds * 1000L, fadeStartVolume)
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("AdhanService", "Error in playAdhan", e)
        }
    }

    private fun createNotification(prayerName: String): Notification {
        val mediaStyle = androidx.media.app.NotificationCompat.MediaStyle()
            .setMediaSession(mediaSession?.sessionCompatToken) 
            .setShowActionsInCompactView(0) 

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Adhan: $prayerName")
            .setContentText("Tap to open")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setStyle(mediaStyle)
            .setOngoing(true)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        // Stop scanning when service dies (if still scanning)
        mediaRouterHelper.stopScanning()
        
        mediaSession?.release()
        mediaSession = null
        player?.release()
        player = null
        serviceScope.cancel()
        
        // Restore Volume
        initialVolume?.let { vol ->
            try {
                val audioManager = getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
                android.util.Log.d("AdhanService", "Restoring Volume to $vol")
                audioManager.setStreamVolume(android.media.AudioManager.STREAM_MUSIC, vol, 0)
            } catch (e: Exception) {
                android.util.Log.e("AdhanService", "Failed to restore volume", e)
            }
        }
        super.onDestroy()
    }
}
