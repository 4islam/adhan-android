package com.adhan.app.infra

import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import androidx.media3.exoplayer.ExoPlayer
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
open class AudioRouter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val logRepository: com.adhan.app.domain.LogRepository
) {

    fun getAvailableDevices(): List<AudioDeviceInfo> {
        if (getSdkInt() < Build.VERSION_CODES.M) return emptyList()
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        return audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS).toList()
    }

    fun routeAudio(player: ExoPlayer, selectedRouteName: String?): Boolean {
        if (selectedRouteName == null || selectedRouteName == "Default") {
            player.setPreferredAudioDevice(null)
            return false
        }
        
        if (getSdkInt() < Build.VERSION_CODES.M) return false

        try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
            
            // Match using the same format as ViewModel/Service
            val targetDevice = devices.find { 
                val typeName = getDeviceTypeName(it.type)
                "$typeName (${it.productName})" == selectedRouteName 
            }

            if (targetDevice != null) {
                android.util.Log.d("AudioRouter", "Routing to requested device: $selectedRouteName")
                player.setPreferredAudioDevice(targetDevice)
                return true
            } else {
                android.util.Log.w("AudioRouter", "Requested device not found: $selectedRouteName")
                return false
            }
        } catch (e: Exception) {
            android.util.Log.e("AudioRouter", "Routing failed", e)
            return false
        }
    }

    suspend fun routeAudioWithLogging(player: ExoPlayer, selectedRouteName: String?) {
         if (selectedRouteName == null || selectedRouteName == "Default" || getSdkInt() < Build.VERSION_CODES.M) {
            return
        }

        try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
            
            val targetDevice = devices.find { 
                val typeName = getDeviceTypeName(it.type)
                "$typeName (${it.productName})" == selectedRouteName 
            }

            if (targetDevice != null) {
                logRepository.log("Routing to: $selectedRouteName")
                player.setPreferredAudioDevice(targetDevice)
            } else {
                logRepository.log("Routing Failed: Device '$selectedRouteName' not found.", true)
            }
        } catch (e: Exception) {
            logRepository.log("Routing Exception: ${e.message}", true)
        }
    }

    fun getDeviceTypeName(type: Int): String {
        return when (type) {
             AudioDeviceInfo.TYPE_BUILTIN_EARPIECE -> "Earpiece"
             AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> "Speaker"
             AudioDeviceInfo.TYPE_WIRED_HEADSET -> "Wired Headset"
             AudioDeviceInfo.TYPE_WIRED_HEADPHONES -> "Wired Headphones"
             AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> "Bluetooth (Call)"
             AudioDeviceInfo.TYPE_BLUETOOTH_A2DP -> "Bluetooth (Media)"
             AudioDeviceInfo.TYPE_DOCK -> "Dock"
             AudioDeviceInfo.TYPE_USB_ACCESSORY -> "USB Accessory"
             AudioDeviceInfo.TYPE_USB_DEVICE -> "USB Device"
             AudioDeviceInfo.TYPE_USB_HEADSET -> "USB Headset"
             AudioDeviceInfo.TYPE_LINE_ANALOG -> "Line Out"
             AudioDeviceInfo.TYPE_LINE_DIGITAL -> "Digital Out"
             AudioDeviceInfo.TYPE_HDMI -> "HDMI"
             AudioDeviceInfo.TYPE_HDMI_ARC -> "HDMI ARC"
             AudioDeviceInfo.TYPE_AUX_LINE -> "Aux Line"
             18 -> "Telephony"
             23 -> "Hearing Aid"
             24 -> "Bluetooth LE Speaker"
             26 -> "Bluetooth LE Headset"
             else -> "Device (Type $type)"
        }
    }

    open fun getSdkInt(): Int = Build.VERSION.SDK_INT
}
