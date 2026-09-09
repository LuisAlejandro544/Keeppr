package com.example.ui.debug

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.debug.InAppLogCollector
import com.example.debug.LogEntry
import com.example.debug.PerformanceMetrics
import com.example.debug.ThreadInfo
import com.example.native.NativeEngine

/**
 * Panel completo de depuración móvil con pestañas para:
 * 1. Métricas de Rendimiento (FPS, Heap JVM, Heap Nativo de Rust/C++, Garbage Collection)
 * 2. Monitor de Hilos (Inspección de hilos activos, estados, prioridades y daemon)
 * 3. Visor de Logs (Logcat en vivo, búsqueda, filtros de severidad y limpieza)
 * 4. Inspector de Sistema e Hyperion (Arquitectura ABI, CPU, SDK, NDK, Memoria Física y LeakCanary)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebugDashboardDialog(
    metrics: PerformanceMetrics,
    logs: List<LogEntry>,
    onClose: () -> Unit,
    onClearLogs: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Rendimiento", "Hilos", "Logs", "Sistema")

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .testTag("debug_dashboard_dialog"),
            color = MaterialTheme.colorScheme.background
        ) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.BugReport,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Debug Center",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleLarge
                                )
                            }
                        },
                        actions = {
                            IconButton(onClick = onClose) {
                                Icon(imageVector = Icons.Default.Close, contentDescription = "Cerrar")
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    )
                }
            ) { padding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                ) {
                    TabRow(selectedTabIndex = selectedTab) {
                        tabs.forEachIndexed { index, title ->
                            Tab(
                                selected = selectedTab == index,
                                onClick = { selectedTab = index },
                                text = { Text(text = title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold) }
                            )
                        }
                    }

                    when (selectedTab) {
                        0 -> PerformanceTab(metrics = metrics)
                        1 -> ThreadsTab(threads = metrics.activeThreads)
                        2 -> LogsTab(logs = logs, onClearLogs = onClearLogs)
                        3 -> SystemTab()
                    }
                }
            }
        }
    }
}

@Composable
private fun PerformanceTab(metrics: PerformanceMetrics) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Tasa de Refresco y Fluidez",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                        val fpsColor = when {
                            metrics.currentFps >= 55 -> Color(0xFF4CAF50)
                            metrics.currentFps >= 30 -> Color(0xFFFF9800)
                            else -> Color(0xFFF44336)
                        }
                        Text(
                            text = "${metrics.currentFps} FPS",
                            color = fpsColor,
                            fontWeight = FontWeight.Black,
                            fontSize = 20.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (metrics.currentFps >= 55) "Renderizado óptimo (sin jank)" else "Posible caída de fotogramas detectada",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Memoria JVM (Kotlin / Compose)",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    val jvmRatio = if (metrics.jvmHeapTotalMb > 0) (metrics.jvmHeapUsedMb / metrics.jvmHeapTotalMb).coerceIn(0f, 1f) else 0f
                    LinearProgressIndicator(
                        progress = { jvmRatio },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "En uso: %.1f MB".format(metrics.jvmHeapUsedMb),
                            style = MaterialTheme.typography.bodyMedium,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "Total Asignado: %.1f MB".format(metrics.jvmHeapTotalMb),
                            style = MaterialTheme.typography.bodyMedium,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Text(
                        text = "Límite Máximo Heap: %.1f MB".format(metrics.jvmHeapMaxMb),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Native Heap (Rust, C++, Lua)",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "%.2f MB".format(metrics.nativeHeapUsedMb),
                            color = Color(0xFFFF9800),
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 16.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Mide la memoria asignada fuera de la máquina virtual Java/ART mediante malloc/JNI y asignadores de Rust (jemalloc/system). Permite verificar que no existan fugas nativas al ejecutar scripts de Lua o búsquedas Rust.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Acciones de Memoria",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    TextButton(
                        onClick = {
                            System.gc()
                            Runtime.getRuntime().gc()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Default.DeleteSweep, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Forzar Garbage Collection (System.gc)")
                    }
                }
            }
        }
    }
}

@Composable
private fun ThreadsTab(threads: List<ThreadInfo>) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Hilos en ejecución: ${threads.size}",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(threads, key = { it.id }) { thread ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = thread.name,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Prioridad: ${thread.priority} | Daemon: ${if (thread.isDaemon) "Sí" else "No"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        val stateColor = when (thread.state) {
                            Thread.State.RUNNABLE -> Color(0xFF4CAF50)
                            Thread.State.TIMED_WAITING, Thread.State.WAITING -> Color(0xFFFF9800)
                            Thread.State.BLOCKED -> Color(0xFFF44336)
                            else -> Color.Gray
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = stateColor.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = thread.state.name,
                                color = stateColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LogsTab(
    logs: List<LogEntry>,
    onClearLogs: () -> Unit
) {
    var filterQuery by remember { mutableStateOf("") }
    var selectedLevel by remember { mutableStateOf("ALL") }

    val filteredLogs = remember(logs, filterQuery, selectedLevel) {
        logs.filter { entry ->
            val matchesQuery = filterQuery.isEmpty() || entry.message.contains(filterQuery, ignoreCase = true) || entry.tag.contains(filterQuery, ignoreCase = true)
            val matchesLevel = selectedLevel == "ALL" || entry.level.equals(selectedLevel, ignoreCase = true)
            matchesQuery && matchesLevel
        }.reversed()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Barra de búsqueda y limpieza de logs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = filterQuery,
                onValueChange = { filterQuery = it },
                placeholder = { Text("Filtrar logs o tag...", fontSize = 12.sp) },
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(onClick = onClearLogs) {
                Icon(imageVector = Icons.Default.ClearAll, contentDescription = "Limpiar logs")
            }
        }

        // Filtros de severidad
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf("ALL", "E", "W", "I", "D").forEach { lvl ->
                FilterChip(
                    selected = selectedLevel == lvl,
                    onClick = { selectedLevel = lvl },
                    label = { Text(lvl, fontSize = 11.sp) }
                )
            }
        }

        // Lista de logs
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF181825))
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(filteredLogs, key = { it.id }) { log ->
                val color = when (log.level) {
                    "E" -> Color(0xFFF38BA8)
                    "W" -> Color(0xFFFAB387)
                    "I" -> Color(0xFFA6E3A1)
                    "D" -> Color(0xFF89B4FA)
                    else -> Color(0xFFCDD6F4)
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp)
                ) {
                    Text(
                        text = "${log.timestamp} [${log.level}] ${log.tag}: ",
                        color = color,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = log.message,
                        color = Color(0xFFBAC2DE),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

@Composable
private fun SystemTab() {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Dispositivo y Arquitectura",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Modelo: ${Build.MANUFACTURER} ${Build.MODEL}", style = MaterialTheme.typography.bodyMedium)
                    Text("Android SDK: ${Build.VERSION.SDK_INT} (${Build.VERSION.RELEASE})", style = MaterialTheme.typography.bodyMedium)
                    Text("ABIs Soportadas: ${Build.SUPPORTED_ABIS.joinToString(", ")}", style = MaterialTheme.typography.bodyMedium, fontFamily = FontFamily.Monospace)
                    Text("Procesadores Disponibles: ${Runtime.getRuntime().availableProcessors()} núcleos", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Motores Nativos",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Puente Nativo C++: JNI Integrado", style = MaterialTheme.typography.bodyMedium)
                    Text("Runtime Lua: 5.4.6 Compilado en C++", style = MaterialTheme.typography.bodyMedium)
                    Text("Motor Rust: Creado con Cargo NDK (libvault_rust.a)", style = MaterialTheme.typography.bodyMedium)
                    Text("Estado del Motor: ${if (NativeEngine.isAvailable) "Activo (OK)" else "No disponible"}", style = MaterialTheme.typography.bodyMedium, fontFamily = FontFamily.Monospace)
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Herramientas In-App Instaladas",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("• LeakCanary 2.14: Activo en segundo plano. Al detectar una fuga de memoria (Activity, View o Coroutine retenida), enviará una notificación con el icono de fuga a tu pantalla de inicio.", style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("• HUD Flotante: Indicador móvil arrastrable con cálculo de FPS en tiempo real mediante Choreographer, monitor de Heap JVM y Native Heap.", style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("• Visor de Logs e Hilos: Panel táctil para depurar y filtrar trazas sin necesidad de conectar el teléfono a una computadora.", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
