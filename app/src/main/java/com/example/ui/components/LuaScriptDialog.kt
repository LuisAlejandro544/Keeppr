package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.VerticalAlignBottom
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.native.NativeEngine

/**
 * Modo de inserción del resultado generado por un script de Lua en el editor de la nota.
 */
enum class LuaInsertMode {
    CURSOR,   // Inserta en la posición actual del cursor de texto
    APPEND,   // Añade al final del documento con salto de línea
    REPLACE   // Sustituye el contenido completo de la nota
}

/**
 * Diálogo modal para ejecutar scripts y plantillas de Lua directamente sobre la nota activa.
 * 
 * Permite dos flujos de trabajo clave:
 * 1. Plantilla Diaria: Genera un formato estructurado en Markdown con fecha, hora y listas interactivas.
 * 2. Script Personalizado: Permite al usuario escribir código Lua 5.4 con acceso a 'content' y 'title',
 *    probar la salida en tiempo real e insertarla en la nota con el modo de inserción deseado.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LuaScriptDialog(
    currentContent: String,
    currentTitle: String,
    onDismiss: () -> Unit,
    onApplyResult: (generatedText: String, insertMode: LuaInsertMode) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }

    // Script oficial de Plantilla Diaria en Lua 5.4
    val dailyTemplateLuaScript = remember {
        """
        local fecha = os.date("%d/%m/%Y")
        local hora = os.date("%H:%M")

        return string.format([[# 📅 Registro Diario - %s

> Creado a las %s

## 🎯 Objetivos Principales
- [ ] 
- [ ] 
- [ ] 

## 📝 Notas y Apuntes Rápidos


## 💡 Reflexión del Día
]], fecha, hora)
        """.trimIndent()
    }

    // Estado del editor de script personalizado
    var customScript by remember {
        mutableStateOf(
            """
            -- Variables globales disponibles: `content` y `title`
            -- Devuelve el texto deseado usando `return`
            
            local fechaHora = os.date("%d/%m/%Y %H:%M")
            return content .. "\n\n> ⚡ Nota actualizada con Lua: " .. fechaHora
            """.trimIndent()
        )
    }

    var executionOutput by remember { mutableStateOf<String?>(null) }
    var isExecutionError by remember { mutableStateOf(false) }
    var selectedInsertMode by remember { mutableStateOf(LuaInsertMode.APPEND) }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier
            .fillMaxWidth()
            .widthIn(max = 500.dp)
            .padding(vertical = 16.dp),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = stringResource(R.string.lua_scripts_title),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.lua_scripts_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Selector de Pestañas: Plantillas vs Script Personalizado
                PrimaryTabRow(
                    selectedTabIndex = selectedTab,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = {
                            Text(
                                text = stringResource(R.string.lua_tab_templates),
                                fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.CalendarToday,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        modifier = Modifier.testTag("lua_tab_templates")
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = {
                            Text(
                                text = stringResource(R.string.lua_tab_custom),
                                fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Code,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        modifier = Modifier.testTag("lua_tab_custom")
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                when (selectedTab) {
                    0 -> {
                        // PESTAÑA 1: PLANTILLAS RÁPIDAS
                        DailyTemplateSection(
                            onInsert = { mode ->
                                val generated = NativeEngine.evalLuaWithContext(
                                    script = dailyTemplateLuaScript,
                                    content = currentContent,
                                    title = currentTitle
                                )
                                onApplyResult(generated, mode)
                                onDismiss()
                            }
                        )
                    }
                    1 -> {
                        // PESTAÑA 2: SCRIPT PERSONALIZADO
                        CustomScriptSection(
                            script = customScript,
                            onScriptChange = { customScript = it },
                            output = executionOutput,
                            isError = isExecutionError,
                            selectedMode = selectedInsertMode,
                            onModeSelect = { selectedInsertMode = it },
                            onTestRun = {
                                val res = NativeEngine.evalLuaWithContext(
                                    script = customScript,
                                    content = currentContent,
                                    title = currentTitle
                                )
                                isExecutionError = res.startsWith("Error Lua:")
                                executionOutput = res
                            },
                            onApply = {
                                val res = NativeEngine.evalLuaWithContext(
                                    script = customScript,
                                    content = currentContent,
                                    title = currentTitle
                                )
                                if (!res.startsWith("Error Lua:")) {
                                    onApplyResult(res, selectedInsertMode)
                                    onDismiss()
                                } else {
                                    isExecutionError = true
                                    executionOutput = res
                                }
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("lua_dialog_close")
            ) {
                Text(stringResource(R.string.close))
            }
        }
    )
}

/**
 * Sección de Plantilla Diaria generada mediante el motor nativo de Lua.
 */
@Composable
private fun DailyTemplateSection(
    onInsert: (LuaInsertMode) -> Unit
) {
    ElevatedCard(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CalendarToday,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = stringResource(R.string.lua_daily_template_title),
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                )
            }

            Text(
                text = stringResource(R.string.lua_daily_template_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            Text(
                text = "Modo de Inserción:",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
            )

            // Botones de acción directa con áreas táctiles accesibles (>= 48dp)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                OutlinedButton(
                    onClick = { onInsert(LuaInsertMode.CURSOR) },
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .testTag("daily_template_insert_cursor")
                ) {
                    Text(
                        text = stringResource(R.string.lua_insert_cursor),
                        style = MaterialTheme.typography.labelSmall
                    )
                }

                Button(
                    onClick = { onInsert(LuaInsertMode.APPEND) },
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .testTag("daily_template_insert_append")
                ) {
                    Text(
                        text = stringResource(R.string.lua_insert_append),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }

            OutlinedButton(
                onClick = { onInsert(LuaInsertMode.REPLACE) },
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .testTag("daily_template_insert_replace")
            ) {
                Text(
                    text = stringResource(R.string.lua_replace_content),
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}

/**
 * Sección de edición y prueba de scripts de Lua personalizados.
 */
@Composable
private fun CustomScriptSection(
    script: String,
    onScriptChange: (String) -> Unit,
    output: String?,
    isError: Boolean,
    selectedMode: LuaInsertMode,
    onModeSelect: (LuaInsertMode) -> Unit,
    onTestRun: () -> Unit,
    onApply: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = stringResource(R.string.lua_custom_script_title),
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
        )

        // Píldoras de atajos y variables disponibles para facilitar la edición en móvil
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            VariableChip(label = "content (texto de nota)") {
                onScriptChange(script + " content")
            }
            VariableChip(label = "title (título)") {
                onScriptChange(script + " title")
            }
            VariableChip(label = "os.date()") {
                onScriptChange(script + " os.date(\"%d/%m/%Y %H:%M\")")
            }
            VariableChip(label = "return ...") {
                onScriptChange(script + "\nreturn ")
            }
        }

        // Editor de código con fuente monoespaciada
        OutlinedTextField(
            value = script,
            onValueChange = onScriptChange,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 130.dp, max = 220.dp)
                .testTag("lua_custom_script_input"),
            shape = RoundedCornerShape(8.dp),
            textStyle = MaterialTheme.typography.bodySmall.copy(
                fontFamily = FontFamily.Monospace,
                lineHeight = 18.sp
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
            ),
            placeholder = {
                Text(
                    text = stringResource(R.string.lua_custom_script_hint),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp
                )
            }
        )

        // Botón para probar script sin alterar la nota
        OutlinedButton(
            onClick = onTestRun,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .testTag("lua_test_run_button")
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(stringResource(R.string.lua_run_test))
        }

        // Caja de Salida / Resultado de la ejecución
        AnimatedVisibility(visible = output != null) {
            output?.let { textOutput ->
                val boxBorderColor = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                val boxBgColor = if (isError) {
                    MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                } else {
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .border(1.dp, boxBorderColor, RoundedCornerShape(8.dp))
                        .background(boxBgColor)
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = stringResource(R.string.lua_output_title),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                        )
                    )
                    Text(
                        text = textOutput,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp
                        )
                    )
                }
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

        // Selector de Modo de Inserción
        Text(
            text = "Insertar resultado en la nota:",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            FilterChip(
                selected = selectedMode == LuaInsertMode.CURSOR,
                onClick = { onModeSelect(LuaInsertMode.CURSOR) },
                label = { Text(stringResource(R.string.lua_insert_cursor)) },
                modifier = Modifier.testTag("lua_mode_cursor")
            )
            FilterChip(
                selected = selectedMode == LuaInsertMode.APPEND,
                onClick = { onModeSelect(LuaInsertMode.APPEND) },
                label = { Text(stringResource(R.string.lua_insert_append)) },
                modifier = Modifier.testTag("lua_mode_append")
            )
            FilterChip(
                selected = selectedMode == LuaInsertMode.REPLACE,
                onClick = { onModeSelect(LuaInsertMode.REPLACE) },
                label = { Text(stringResource(R.string.lua_replace_content)) },
                modifier = Modifier.testTag("lua_mode_replace")
            )
        }

        // Botón Principal para aplicar el script
        Button(
            onClick = onApply,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .testTag("lua_apply_to_note_button")
        ) {
            Icon(
                imageVector = Icons.Default.VerticalAlignBottom,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(stringResource(R.string.lua_insert_action))
        }
    }
}

/**
 * Chip interactivo para insertar fragmentos de código rápido con un toque.
 */
@Composable
private fun VariableChip(
    label: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 5.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.primary,
                fontSize = 11.sp
            )
        )
    }
}
