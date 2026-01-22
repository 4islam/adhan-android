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

@AndroidEntryPoint
class AdhanService : Service() {

    @Inject
    lateinit var logRepository: com.adhan.app.domain.LogRepository

    private var player: ExoPlayer? = null
    private var mediaSession: androidx.media3.session.MediaSession? = null
    private val NOTIFICATION_ID = 1001
    private val CHANNEL_ID = "adhan_alerts_v2"
    private val serviceScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO + kotlinx.coroutines.SupervisorJob())

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

        // Ensure notification is posted immediately
        startForeground(NOTIFICATION_ID, createNotification(prayerName))
        
        if (prayerName.equals("Tahajjud", ignoreCase = true)) {
            return START_NOT_STICKY
        }

        try {
            val prefs = getSharedPreferences("adhan_prefs", Context.MODE_PRIVATE)
            val customUri = prefs.getString("adhan_sound_$prayerName", null)
            
            val mediaItem: MediaItem? = if (customUri != null) {
                android.util.Log.d("AdhanService", "Using custom URI: $customUri")
                MediaItem.fromUri(customUri)
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

            // Apply Custom Audio Routing
            val selectedRoute = prefs.getString("selected_audio_route", "Default")
            if (selectedRoute != null && selectedRoute != "Default" && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                 val audioManager = getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
                 val devices = audioManager.getDevices(android.media.AudioManager.GET_DEVICES_OUTPUTS)
                 val targetDevice = devices.find { "${it.productName} [${it.type}]" == selectedRoute }
                 
                 if (targetDevice != null) {
                     android.util.Log.d("AdhanService", "Routing to requested device: $selectedRoute")
                     serviceScope.launch { logRepository.log("Routing to: $selectedRoute") }
                     player?.setPreferredAudioDevice(targetDevice)
                 } else {
                     android.util.Log.w("AdhanService", "Requested device not found: $selectedRoute")
                     serviceScope.launch { logRepository.log("Routing Failed: Device '$selectedRoute' not found.", true) }
                 }
            }


            if (mediaItem != null) {
                player?.let {
                    if (it.playbackState == Player.STATE_IDLE || it.playbackState == Player.STATE_ENDED) {
                        it.setMediaItem(mediaItem)
                        it.prepare()
                        it.play()
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
        super.onDestroy()
    }
}
