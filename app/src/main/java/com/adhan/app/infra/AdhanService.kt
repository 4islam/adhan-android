package com.adhan.app.infra

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.adhan.app.MainActivity
import com.adhan.app.R

@UnstableApi
class AdhanService : Service() {

    private var player: ExoPlayer? = null
    private val NOTIFICATION_ID = 1001
    private val CHANNEL_ID = "adhan_alerts"

    override fun onCreate() {
        super.onCreate()
        player = ExoPlayer.Builder(this).build()
        player?.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                }
            }
        })
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val prayerName = intent?.getStringExtra("prayer_name") ?: "Prayer"
        
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
                    val mediaItem = MediaItem.fromUri("android.resource://$packageName/$rawResourceId")
                    player?.setMediaItem(mediaItem)
                    player?.prepare()
                    player?.play()
                } else {
                    android.util.Log.e("AdhanService", "Audio file $resourceName not found")
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("AdhanService", "Error playing adhan", e)
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
        player?.release()
        player = null
        super.onDestroy()
    }
}
