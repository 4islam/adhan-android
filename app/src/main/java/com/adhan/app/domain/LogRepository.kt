package com.adhan.app.domain

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

data class LogEntry(
    val timestamp: Long,
    val message: String,
    val isError: Boolean,
    val formattedTime: String
)

@Singleton
class LogRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val logFile = File(context.filesDir, "adhan_debug_logs.txt")
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    private val MAX_LOG_SIZE = 500 * 1024 // 500 KB

    suspend fun log(message: String, isError: Boolean = false) {
        withContext(Dispatchers.IO) {
            synchronized(this) {
                val timestamp = System.currentTimeMillis()
                val dateStr = dateFormat.format(Date(timestamp))
                val types = if (isError) "ERROR" else "INFO"
                val line = "$dateStr|$types|$message\n"
                
                try {
                    if (logFile.exists() && logFile.length() > MAX_LOG_SIZE) {
                        rotateLogs()
                    }
                    logFile.appendText(line)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun rotateLogs() {
        try {
            val lines = logFile.readLines()
            if (lines.size > 1000) {
                logFile.writeText(lines.takeLast(500).joinToString("\n") + "\n")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


    suspend fun getLogs(): List<LogEntry> {
        return withContext(Dispatchers.IO) {
            if (!logFile.exists()) return@withContext emptyList()
            
            try {
                logFile.readLines().mapNotNull { line ->
                    val parts = line.split("|", limit = 3)
                    if (parts.size == 3) {
                        LogEntry(
                            timestamp = 0, // We don't parse it back for sorting if we trust append order, or we could.
                            formattedTime = parts[0],
                            isError = parts[1] == "ERROR",
                            message = parts[2]
                        )
                    } else null
                }.reversed() // Newest first
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    suspend fun clearLogs() {
        withContext(Dispatchers.IO) {
            if (logFile.exists()) {
                logFile.delete()
            }
        }
    }
}
