package com.adhan.app.infra

import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.delay
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max
import kotlin.math.min

@Singleton
class AudioFader @Inject constructor() {

    /**
     * Fades in the player volume from 0.0 to 1.0 over the specified duration.
     * This function should be called from a coroutine scope.
     */
    suspend fun startFadeIn(player: ExoPlayer, durationMs: Long = 5000L, startVolume: Float = 0f) {
        if (durationMs <= 0 || startVolume >= 1.0f) {
            player.volume = 1.0f
            return
        }

        val steps = 20 // Number of volume updates
        val stepDuration = durationMs / steps
        val volumeRange = 1.0f - startVolume
        val volumeStep = volumeRange / steps

        // Start at requested volume
        player.volume = startVolume

        for (i in 1..steps) {
            delay(stepDuration)
            val newVolume = min(1.0f, startVolume + (i * volumeStep))
            player.volume = newVolume
        }
        
        // Ensure we end at exact 1.0
        player.volume = 1.0f
    }
}
