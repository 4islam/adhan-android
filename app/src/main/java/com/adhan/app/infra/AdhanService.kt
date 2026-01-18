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
import androidx.media3.common.util.UnstableApi
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
    private val NOTIFICATION_ID = 1001
    private val CHANNEL_ID = "adhan_alerts"
    private val serviceScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO + kotlinx.coroutines.SupervisorJob())

    override fun onCreate() {
        super.onCreate()
        
        createNotificationChannel()

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_ALARM)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()

        player = ExoPlayer.Builder(this).build().apply {
            setAudioAttributes(audioAttributes, true)
            setWakeMode(C.WAKE_MODE_LOCAL)
            addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_ENDED) {
                        stopForeground(STOP_FOREGROUND_REMOVE)
                        stopSelf()
                    }
                }
                
                override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                     serviceScope.launch {
                        logRepository.log("ExoPlayer error: ${error.message}", true)
                    }
                }
            })
        }
    }

    private fun createNotificationChannel() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val name = "Adhan Alerts"
            val descriptionText = "Notifications for prayer times"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                setSound(null, null) // Audio is handled by Service, not Notification
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val prayerName = intent?.getStringExtra("prayer_name") ?: "Prayer"
        
        serviceScope.launch {
            logRepository.log("AdhanService started for: $prayerName")
        }

        startForeground(NOTIFICATION_ID, createNotification(prayerName))
        
        // Tahajjud doesn't have an adhan
        if (prayerName.equals("Tahajjud", ignoreCase = true)) {
            return START_NOT_STICKY
        }

        // Load and play Adhan
        try {
            val prefs = getSharedPreferences("adhan_prefs", Context.MODE_PRIVATE)
            val customUri = prefs.getString("adhan_sound_$prayerName", null)

            if (customUri != null) {
                serviceScope.launch { logRepository.log("Attempting to play custom URI: $customUri") }
                val mediaItem = MediaItem.fromUri(customUri)
                player?.setMediaItem(mediaItem)
                player?.prepare()
                player?.play()
            } else {
                val resourceName = if (prayerName.contains("Fajr", ignoreCase = true)) {
                    "adhan_fajr"
                } else {
                    "adhan_regular"
                }

                val rawResourceId = resources.getIdentifier(resourceName, "raw", packageName)
                if (rawResourceId != 0) {
                    serviceScope.launch { logRepository.log("Playing built-in resource: $resourceName") }
                    val mediaItem = MediaItem.fromUri("android.resource://$packageName/$rawResourceId")
                    player?.setMediaItem(mediaItem)
                    player?.prepare()
                    player?.play()
                } else {
                    val msg = "Audio file $resourceName not found"
                    android.util.Log.e("AdhanService", msg)
                    serviceScope.launch { logRepository.log(msg, true) }
                }
            }
        } catch (e: Exception) {
            val msg = "Error playing adhan: ${e.message}"
            android.util.Log.e("AdhanService", msg, e)
            serviceScope.launch { logRepository.log(msg, true) }
        }

        return START_NOT_STICKY
    }

    private fun createNotification(prayerName: String): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Adhan: $prayerName")
            .setContentText("It's time for $prayerName prayer.")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        try {
            serviceScope.cancel()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        player?.release()
        player = null
        super.onDestroy()
    }
}
