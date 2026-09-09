package com.example.ui.debug

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.debug.PerformanceMetrics
import kotlin.math.roundToInt

/**
 * HUD Flotante interactivo y arrastrable para monitoreo en vivo de FPS, RAM e Hilos.
 * Diseñado especialmente para pantallas táctiles móviles.
 */
@Composable
fun PerformanceFloatingHud(
    metrics: PerformanceMetrics,
    onOpenFullDashboard: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    var offsetX by remember { mutableFloatStateOf(16f) }
    var offsetY by remember { mutableFloatStateOf(120f) }
    var isExpanded by remember { mutableStateOf(false) }

    // Colores dinámicos según el rendimiento
    val fpsColor = when {
        metrics.currentFps >= 55 -> Color(0xFF4CAF50) // Verde
        metrics.currentFps >= 30 -> Color(0xFFFF9800) // Naranja
        else -> Color(0xFFF44336) // Rojo (jank)
    }

    Box(
        modifier = modifier
            .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    offsetX = (offsetX + dragAmount.x).coerceIn(0f, 600f)
                    offsetY = (offsetY + dragAmount.y).coerceIn(60f, 1600f)
                }
            }
            .shadow(10.dp, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xE61E1E2E)) // Fondo oscuro translúcido
            .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(16.dp))
            .padding(8.dp)
            .testTag("floating_performance_hud")
    ) {
        Column {
            // Barra superior compacta con métricas clave
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Indicador de FPS
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(fpsColor.copy(alpha = 0.2f))
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(fpsColor)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${metrics.currentFps} FPS",
                        color = fpsColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // RAM total (JVM + Nativa)
                val totalUsed = metrics.jvmHeapUsedMb + metrics.nativeHeapUsedMb
                Text(
                    text = "${totalUsed.roundToInt()} MB",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.Monospace
                )

                // Hilos activos
                Text(
                    text = "${metrics.activeThreadCount} Th",
                    color = Color(0xFF80DEEA),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )

                // Botón expandir/contraer
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(Color(0x33FFFFFF))
                        .clickable { isExpanded = !isExpanded },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = "Expandir HUD",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Botón cerrar
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(Color(0x33FFFFFF))
                        .clickable { onClose() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Ocultar HUD",
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            // Detalles desplegables
            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .width(220.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "JVM Heap:",
                            color = Color(0xFFB0B0C0),
                            fontSize = 11.sp
                        )
                        Text(
                            text = "%.1f / %.1f MB".format(metrics.jvmHeapUsedMb, metrics.jvmHeapTotalMb),
                            color = Color.White,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Native Heap (Rust/C++):",
                            color = Color(0xFFFFB74D),
                            fontSize = 11.sp
                        )
                        Text(
                            text = "%.1f MB".format(metrics.nativeHeapUsedMb),
                            color = Color(0xFFFFB74D),
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Botón para abrir panel completo de diagnóstico
                    Surface(
                        onClick = onOpenFullDashboard,
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(30.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Speed,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Panel de Debug Completo",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
