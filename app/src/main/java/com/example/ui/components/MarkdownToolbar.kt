package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MarkdownToolbar(
    onInsertText: (prefix: String, suffix: String) -> Unit,
    onOpenFontDialog: (() -> Unit)? = null,
    onOpenLuaDialog: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 3.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onOpenLuaDialog != null) {
                ToolbarButton(label = "🪄 Lua", testTag = "toolbar_lua") { onOpenLuaDialog() }
            }
            ToolbarButton(label = "H1", testTag = "toolbar_h1") { onInsertText("\n# ", "") }
            ToolbarButton(label = "H2", testTag = "toolbar_h2") { onInsertText("\n## ", "") }
            ToolbarButton(label = "H3", testTag = "toolbar_h3") { onInsertText("\n### ", "") }
            ToolbarButton(label = "B", isBold = true, testTag = "toolbar_bold") { onInsertText("**", "**") }
            ToolbarButton(label = "I", isItalic = true, testTag = "toolbar_italic") { onInsertText("*", "*") }
            if (onOpenFontDialog != null) {
                ToolbarButton(label = "Aa Fuente", testTag = "toolbar_font") { onOpenFontDialog() }
            }
            ToolbarButton(label = "☑ Tarea", testTag = "toolbar_task") { onInsertText("\n- [ ] ", "") }
            ToolbarButton(label = "• Lista", testTag = "toolbar_bullet") { onInsertText("\n- ", "") }
            ToolbarButton(label = "1. Num", testTag = "toolbar_num") { onInsertText("\n1. ", "") }
            ToolbarButton(label = "❝ Cita", testTag = "toolbar_quote") { onInsertText("\n> ", "") }
            ToolbarButton(label = "💡 Bloque", testTag = "toolbar_callout") { onInsertText("\n> [!NOTE]\n> ", "") }
            ToolbarButton(label = "</> Código", isMono = true, testTag = "toolbar_code") { onInsertText("\n```\n", "\n```") }
            ToolbarButton(label = "— Línea", testTag = "toolbar_divider") { onInsertText("\n---\n", "") }
        }
    }
}

@Composable
private fun ToolbarButton(
    label: String,
    modifier: Modifier = Modifier,
    isBold: Boolean = false,
    isItalic: Boolean = false,
    isMono: Boolean = false,
    testTag: String = "",
    onClick: () -> Unit
) {
    TextButton(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        modifier = modifier
            .height(38.dp)
            .testTag(testTag)
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Medium,
            fontFamily = if (isMono) FontFamily.Monospace else FontFamily.Default,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
