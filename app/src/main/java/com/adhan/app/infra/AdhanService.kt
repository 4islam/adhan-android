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
        
        android.util.Log.d("AdhanService", "onStartCommand: $prayerName")
        serviceScope.launch { logRepository.log("AdhanService started for: $prayerName") }
        
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
                     // Gentle pulsing pattern for Tahajjud
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
                // Return early, silent notification only (unless vibration happened, but we stop service regardless of active vibration?)
                // Vibration is fire-and-forget usually if non-repeating.
                android.util.Log.d("AdhanService", "Tahajjud audio disabled.")
                serviceScope.launch { logRepository.log("Tahajjud audio disabled.") }
                startForeground(NOTIFICATION_ID, createNotification(prayerName))
                stopSelf() 
                return START_NOT_STICKY
            }
            // If enabled, proceed to playback logic below
        }

        // Volume Override Logic
        try {
            val prefs = getSharedPreferences("adhan_prefs", Context.MODE_PRIVATE)
            val adhanVolumePercent = prefs.getInt("adhan_volume", 80)
            val audioManager = getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
            val maxVol = audioManager.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC)
            val currentVol = audioManager.getStreamVolume(android.media.AudioManager.STREAM_MUSIC)
            
            // Only store initial volume if not already stored (avoid overwriting if service restarts or multiple calls)
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
        
        // Determine URI and Fade settings based on prayer
        try {
            val prefs = getSharedPreferences("adhan_prefs", Context.MODE_PRIVATE)
        var customUri: String? = null
        
        if (prayerName.equals("Tahajjud", ignoreCase = true)) {
            customUri = prefs.getString("tahajjud_sound_uri", null)
            // If custom is null, we will fall back to Fajr logic below or handle explicitly
        } else {
            customUri = prefs.getString("adhan_sound_$prayerName", null)
        }
        
        // Determine fade defaults based on prayer name
        val isFajrOrTahajjud = prayerName.contains("Fajr", ignoreCase = true) || prayerName.equals("Tahajjud", ignoreCase = true)
        val defaultDur = if (isFajrOrTahajjud) 5 else 0
        val defaultVol = if (isFajrOrTahajjud) 0f else 1.0f
        
        val fadeDurationSeconds = prefs.getInt("fade_duration_$prayerName", defaultDur)
        val fadeStartVolume = prefs.getFloat("fade_vol_$prayerName", defaultVol)
            
            val mediaItem: MediaItem? = if (customUri != null) {
                android.util.Log.d("AdhanService", "Using custom URI: $customUri")
                MediaItem.fromUri(android.net.Uri.parse(customUri))
            } else {
                // Default Resources
                if (prayerName.equals("Tahajjud", ignoreCase = true)) {
                     // Force System Notification Sound for Tahajjud defaults
                     val defaultUri = android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION)
                     // If defaultUri is null (some devices?), fallback to alarm
                     val finalUri = defaultUri ?: android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_ALARM)
                     android.util.Log.d("AdhanService", "Using System Notification for Tahajjud: $finalUri")
                     MediaItem.fromUri(finalUri)
                } else {
                    val resourceName = if (prayerName.contains("Fajr", ignoreCase = true)) "adhan_fajr" else "adhan_regular"
                    val rawResourceId = resources.getIdentifier(resourceName, "raw", packageName)
                    if (rawResourceId != 0) {
                        android.util.Log.d("AdhanService", "Using built-in: $resourceName")
                        MediaItem.fromUri("android.resource://$packageName/$rawResourceId")
                    } else {
                        android.util.Log.e("AdhanService", "Resource $resourceName not found!")
                        null
                    }
                }
            }

            // Apply Custom Audio Routing
            val selectedRoute = prefs.getString("selected_audio_route", "Default")
            if (selectedRoute != null && selectedRoute != "Default" && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                 // Use AudioRouter, ensure we run on Main for Player access
                 serviceScope.launch {
                     withContext(kotlinx.coroutines.Dispatchers.Main) {
                         audioRouter.routeAudioWithLogging(player!!, selectedRoute)
                     }
                 }
            }


            if (mediaItem != null) {
                player?.let {
                    if (it.playbackState == Player.STATE_IDLE || it.playbackState == Player.STATE_ENDED) {
                        it.setMediaItem(mediaItem)
                        it.prepare()
                        it.volume = fadeStartVolume // Start at config volume
                        it.play()
                        android.util.Log.d("AdhanService", "Player started, fading in...")
                        
                        serviceScope.launch {
                            withContext(kotlinx.coroutines.Dispatchers.Main) {
                                audioFader.startFadeIn(it, fadeDurationSeconds * 1000L, fadeStartVolume)
                            }
                        }
                        
                        android.util.Log.d("AdhanService", "Player started")
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("AdhanService", "Error in onStartCommand", e)
        }

        return START_NOT_STICKY
    }

    private fun createNotification(prayerName: String): Notification {
        // PendingIntents for actions would require a Receiver, but MediaSession handles media buttons automatically
        // provided we use MediaStyle and connect the session.
        
        val mediaStyle = androidx.media.app.NotificationCompat.MediaStyle()
            .setMediaSession(mediaSession?.sessionCompatToken) // Support lock screen controls?
            .setShowActionsInCompactView(0) // Show Play/Pause

        // Create a 'Stop' action intent if needed, but Media3 handles standard transport controls via Session
        // For simplicity, we just use the MediaStyle decoration for now.

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
