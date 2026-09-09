package com.example.debug

import android.os.Debug
import android.os.SystemClock
import android.view.Choreographer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Métricas de rendimiento recolectadas en tiempo real para la variante Debug.
 * Incluye FPS, memoria JVM (heap asignado y libre), memoria nativa (Rust, C++, Lua) e hilos.
 */
data class PerformanceMetrics(
    val currentFps: Int = 60,
    val jvmHeapUsedMb: Float = 0f,
    val jvmHeapTotalMb: Float = 0f,
    val jvmHeapMaxMb: Float = 0f,
    val nativeHeapUsedMb: Float = 0f,
    val activeThreadCount: Int = 0,
    val activeThreads: List<ThreadInfo> = emptyList()
)

data class ThreadInfo(
    val id: Long,
    val name: String,
    val state: Thread.State,
    val priority: Int,
    val isDaemon: Boolean
)

/**
 * Monitor de rendimiento global para inspección desde el dispositivo móvil.
 */
object PerformanceMonitor {
    private val _metrics = MutableStateFlow(PerformanceMetrics())
    val metrics: StateFlow<PerformanceMetrics> = _metrics.asStateFlow()

    private val _isOverlayVisible = MutableStateFlow(true)
    val isOverlayVisible: StateFlow<Boolean> = _isOverlayVisible.asStateFlow()

    private var monitorJob: Job? = null
    private var isFrameCallbackRegistered = false

    private var frameCount = 0
    private var lastFpsTimestamp = 0L
    private var currentCalculatedFps = 60

    private val frameCallback = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            val nowMs = SystemClock.uptimeMillis()
            if (lastFpsTimestamp == 0L) {
                lastFpsTimestamp = nowMs
            }
            frameCount++
            val elapsed = nowMs - lastFpsTimestamp
            if (elapsed >= 1000L) {
                currentCalculatedFps = ((frameCount * 1000L) / elapsed).toInt()
                frameCount = 0
                lastFpsTimestamp = nowMs
            }
            Choreographer.getInstance().postFrameCallback(this)
        }
    }

    fun startMonitoring(scope: CoroutineScope) {
        if (!isFrameCallbackRegistered) {
            try {
                Choreographer.getInstance().postFrameCallback(frameCallback)
                isFrameCallbackRegistered = true
            } catch (_: Exception) {
                // Ignore if not running on main looper
            }
        }

        if (monitorJob?.isActive == true) return

        monitorJob = scope.launch(Dispatchers.Default) {
            while (isActive) {
                val runtime = Runtime.getRuntime()
                val totalMemory = runtime.totalMemory()
                val freeMemory = runtime.freeMemory()
                val usedMemory = totalMemory - freeMemory
                val maxMemory = runtime.maxMemory()

                val nativeAllocatedBytes = Debug.getNativeHeapAllocatedSize()

                val allThreads = Thread.getAllStackTraces().keys.map { t ->
                    ThreadInfo(
                        id = t.id,
                        name = t.name,
                        state = t.state,
                        priority = t.priority,
                        isDaemon = t.isDaemon
                    )
                }.sortedBy { it.name }

                _metrics.value = PerformanceMetrics(
                    currentFps = currentCalculatedFps.coerceIn(0, 165),
                    jvmHeapUsedMb = usedMemory / (1024f * 1024f),
                    jvmHeapTotalMb = totalMemory / (1024f * 1024f),
                    jvmHeapMaxMb = maxMemory / (1024f * 1024f),
                    nativeHeapUsedMb = nativeAllocatedBytes / (1024f * 1024f),
                    activeThreadCount = allThreads.size,
                    activeThreads = allThreads
                )

                delay(500L)
            }
        }
    }

    fun toggleOverlay() {
        _isOverlayVisible.value = !_isOverlayVisible.value
    }

    fun setOverlayVisible(visible: Boolean) {
        _isOverlayVisible.value = visible
    }
}
