package com.example.debug

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Modelo de una línea de log capturada.
 */
data class LogEntry(
    val id: Long,
    val timestamp: String,
    val level: String,
    val tag: String,
    val message: String
)

/**
 * Colector de logs de Logcat en memoria para visualización móvil dentro de la aplicación.
 */
object InAppLogCollector {
    private val _logs = MutableStateFlow<List<LogEntry>>(emptyList())
    val logs: StateFlow<List<LogEntry>> = _logs.asStateFlow()

    private var collectionJob: Job? = null
    private var sequenceId = 0L

    fun addManualLog(tag: String, message: String, level: String = "I") {
        val time = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(Date())
        val entry = LogEntry(
            id = ++sequenceId,
            timestamp = time,
            level = level,
            tag = tag,
            message = message
        )
        val currentList = _logs.value.toMutableList()
        if (currentList.size > 500) {
            currentList.removeAt(0)
        }
        currentList.add(entry)
        _logs.value = currentList
    }

    fun startCollecting(scope: CoroutineScope) {
        if (collectionJob?.isActive == true) return

        collectionJob = scope.launch(Dispatchers.IO) {
            try {
                // Ejecuta logcat filtrando por el proceso actual
                val process = ProcessBuilder("logcat", "-v", "time", "-d").start()
                val reader = BufferedReader(InputStreamReader(process.inputStream))
                var line = reader.readLine()
                val initialList = mutableListOf<LogEntry>()

                while (line != null) {
                    val parsed = parseLogLine(line)
                    if (parsed != null) {
                        initialList.add(parsed)
                    }
                    if (initialList.size > 300) {
                        initialList.removeAt(0)
                    }
                    line = reader.readLine()
                }
                reader.close()
                _logs.value = initialList
            } catch (e: Exception) {
                addManualLog("LogcatCollector", "No se pudo leer logcat automáticamente: ${e.localizedMessage}", "W")
            }

            // Monitoreo continuo de buffer
            while (isActive) {
                delay(2000L)
            }
        }
    }

    private fun parseLogLine(line: String): LogEntry? {
        if (line.isBlank() || line.startsWith("---------")) return null
        return try {
            // Formato estándar: MM-dd HH:mm:ss.SSS D/Tag(pid): Mensaje
            val parts = line.split(" ", limit = 3)
            val time = if (parts.size >= 2) parts[1] else SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
            val rest = if (parts.size >= 3) parts[2] else line
            val level = if (rest.length > 2 && rest[1] == '/') rest[0].toString() else "I"
            val tagAndMsg = if (rest.contains(": ")) rest.split(": ", limit = 2) else listOf("App", rest)
            val tag = tagAndMsg[0].trim()
            val msg = if (tagAndMsg.size > 1) tagAndMsg[1].trim() else ""

            LogEntry(
                id = ++sequenceId,
                timestamp = time,
                level = level,
                tag = tag,
                message = msg
            )
        } catch (_: Exception) {
            LogEntry(
                id = ++sequenceId,
                timestamp = "",
                level = "D",
                tag = "Log",
                message = line
            )
        }
    }

    fun clear() {
        _logs.value = emptyList()
    }
}
