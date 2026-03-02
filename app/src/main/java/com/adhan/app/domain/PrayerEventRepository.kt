package com.adhan.app.domain

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

data class PrayerEvent(
    val timestamp: Long,
    val prayerName: String,
    val status: String, // Scheduled, Triggered, Playing, Success, Error
    val speaker: String? = null,
    val volume: Int? = null,
    val errorMessage: String? = null,
    val formattedTime: String = SimpleDateFormat("MMM dd HH:mm:ss", Locale.getDefault()).format(Date(timestamp))
)

@Singleton
class PrayerEventRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val eventFile = File(context.filesDir, "adhan_prayer_events.txt")
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    
    private val _events = MutableStateFlow<List<PrayerEvent>>(emptyList())
    val events: StateFlow<List<PrayerEvent>> = _events.asStateFlow()

    suspend fun logEvent(
        prayerName: String,
        status: String,
        speaker: String? = null,
        volume: Int? = null,
        errorMessage: String? = null
    ) {
        withContext(Dispatchers.IO) {
            val timestamp = System.currentTimeMillis()
            val dateStr = dateFormat.format(Date(timestamp))
            val line = "$timestamp|$prayerName|$status|${speaker ?: ""}|${volume ?: ""}|${errorMessage ?: ""}\n"
            
            try {
                eventFile.appendText(line)
                limitEvents(100)
                loadEvents()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    suspend fun loadEvents() {
        withContext(Dispatchers.IO) {
            if (!eventFile.exists()) {
                _events.value = emptyList()
                return@withContext
            }
            
            try {
                val loadedEvents = eventFile.readLines().mapNotNull { line ->
                    val parts = line.split("|", limit = 6)
                    if (parts.size >= 3) {
                        PrayerEvent(
                            timestamp = parts[0].toLongOrNull() ?: 0L,
                            prayerName = parts[1],
                            status = parts[2],
                            speaker = parts.getOrNull(3)?.ifEmpty { null },
                            volume = parts.getOrNull(4)?.toIntOrNull(),
                            errorMessage = parts.getOrNull(5)?.ifEmpty { null }
                        )
                    } else null
                }.reversed()
                _events.value = loadedEvents
            } catch (e: Exception) {
                _events.value = emptyList()
            }
        }
    }


    private fun limitEvents(max: Int) {
        if (!eventFile.exists()) return
        val lines = eventFile.readLines()
        if (lines.size > max) {
            eventFile.writeText(lines.takeLast(max).joinToString("\n") + "\n")
        }
    }

    suspend fun clearEvents() {
        withContext(Dispatchers.IO) {
            if (eventFile.exists()) {
                eventFile.delete()
            }
            _events.value = emptyList()
        }
    }
}
