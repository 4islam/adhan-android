package com.adhan.app.infra

import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import androidx.media3.exoplayer.ExoPlayer
import com.adhan.app.domain.LogRepository
import io.mockk.*
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test

class AudioRouterTest {

    private lateinit var context: Context
    private lateinit var logRepository: LogRepository
    private lateinit var audioManager: AudioManager
    private lateinit var player: ExoPlayer
    private lateinit var audioRouter: AudioRouter

    @Before
    fun setup() {
        context = mockk()
        logRepository = mockk(relaxed = true)
        audioManager = mockk()
        player = mockk(relaxed = true)
        
        every { context.getSystemService(Context.AUDIO_SERVICE) } returns audioManager
        
        // Use spyk to mock the protected/open getSdkInt method
        audioRouter = spyk(AudioRouter(context, logRepository))
        every { audioRouter.getSdkInt() } returns 30
    }

    // setFinalStatic removed

    @Test
    fun `routeAudioWithLogging should setPreferredAudioDevice when device matches`() = runBlocking {
        // Given
        val speakerDevice = mockk<AudioDeviceInfo>()
        every { speakerDevice.type } returns AudioDeviceInfo.TYPE_BUILTIN_SPEAKER
        every { speakerDevice.productName } returns "Pixel Built-in"
        
        val headsetDevice = mockk<AudioDeviceInfo>()
        every { headsetDevice.type } returns AudioDeviceInfo.TYPE_WIRED_HEADSET
        every { headsetDevice.productName } returns "Headphones"

        every { audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS) } returns arrayOf(speakerDevice, headsetDevice)

        // When
        val targetRoute = "Speaker (Pixel Built-in)"
        audioRouter.routeAudioWithLogging(player, targetRoute)

        // Then
        verify { player.setPreferredAudioDevice(speakerDevice) }
        coVerify { logRepository.log("Routing to: $targetRoute") }
    }

    @Test
    fun `routeAudioWithLogging should NOT set device if name mismatch`() = runBlocking {
        // Given
        val speakerDevice = mockk<AudioDeviceInfo>()
        every { speakerDevice.type } returns AudioDeviceInfo.TYPE_BUILTIN_SPEAKER
        every { speakerDevice.productName } returns "Pixel Built-in"

        every { audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS) } returns arrayOf(speakerDevice)

        // When
        val targetRoute = "Bluetooth (Media) (MyBuds)"
        audioRouter.routeAudioWithLogging(player, targetRoute)

        // Then
        verify(exactly = 0) { player.setPreferredAudioDevice(any()) }
        coVerify { logRepository.log("Routing Failed: Device '$targetRoute' not found.", true) }
    }

    @Test
    fun `getDeviceTypeName returns correct strings`() {
        assert(audioRouter.getDeviceTypeName(AudioDeviceInfo.TYPE_BUILTIN_SPEAKER) == "Speaker")
        assert(audioRouter.getDeviceTypeName(AudioDeviceInfo.TYPE_BLUETOOTH_A2DP) == "Bluetooth (Media)")
        assert(audioRouter.getDeviceTypeName(18) == "Telephony")
        assert(audioRouter.getDeviceTypeName(24) == "Bluetooth LE Speaker")
    }
}
