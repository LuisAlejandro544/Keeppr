package com.example.ui.markdown

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AppFontTheme
import com.example.ui.theme.CalloutAmberBg
import com.example.ui.theme.CalloutAmberBorder
import com.example.ui.theme.CalloutBlueBg
import com.example.ui.theme.CalloutBlueBorder
import com.example.ui.theme.CalloutGreenBg
import com.example.ui.theme.CalloutGreenBorder
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun MarkdownPreview(
    content: String,
    onToggleTask: (lineIndex: Int) -> Unit,
    baseFontTheme: AppFontTheme = AppFontTheme.DEFAULT,
    modifier: Modifier = Modifier
) {
    val blocks = remember(content) { MarkdownParser.parse(content) }
    val baseFontFamily = baseFontTheme.fontFamily

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (blocks.isEmpty()) {
            Text(
                text = "Sin contenido para previsualizar.",
                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = baseFontFamily),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.padding(16.dp)
            )
        }

        blocks.forEach { block ->
            when (block) {
                is MarkdownBlock.Header -> RenderHeader(block, baseFontFamily)
                is MarkdownBlock.Callout -> RenderCallout(block, baseFontFamily)
                is MarkdownBlock.TaskItem -> RenderTaskItem(block, baseFontFamily, onToggleTask)
                is MarkdownBlock.BulletItem -> RenderBulletItem(block, baseFontFamily)
                is MarkdownBlock.NumberedItem -> RenderNumberedItem(block, baseFontFamily)
                is MarkdownBlock.Quote -> RenderQuote(block, baseFontFamily)
                is MarkdownBlock.CodeBlock -> RenderCodeBlock(block)
                is MarkdownBlock.Divider -> RenderDivider()
                is MarkdownBlock.Paragraph -> RenderParagraph(block, baseFontFamily)
            }
        }
    }
}

@Composable
private fun RenderHeader(header: MarkdownBlock.Header, baseFontFamily: FontFamily) {
    val (style, topPadding) = when (header.level) {
        1 -> MaterialTheme.typography.headlineMedium.copy(
            fontWeight = FontWeight.Bold,
            fontFamily = baseFontFamily,
            color = MaterialTheme.colorScheme.onSurface
        ) to 12.dp
        2 -> MaterialTheme.typography.titleLarge.copy(
            fontWeight = FontWeight.SemiBold,
            fontFamily = baseFontFamily,
            color = MaterialTheme.colorScheme.onSurface
        ) to 10.dp
        else -> MaterialTheme.typography.titleMedium.copy(
            fontWeight = FontWeight.SemiBold,
            fontFamily = baseFontFamily,
            color = MaterialTheme.colorScheme.primary
        ) to 6.dp
    }

    Column(modifier = Modifier.padding(top = topPadding, bottom = 2.dp)) {
        Text(
            text = formatInlineMarkdown(header.text),
            style = style
        )
        if (header.level == 1) {
            HorizontalDivider(
                modifier = Modifier.padding(top = 4.dp),
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
private fun RenderCallout(callout: MarkdownBlock.Callout, baseFontFamily: FontFamily) {
    val (bgColor, borderColor, icon) = when (callout.type) {
        CalloutType.NOTE, CalloutType.INFO -> Triple(
            CalloutBlueBg,
            CalloutBlueBorder,
            Icons.Default.Info
        )
        CalloutType.TIP -> Triple(
            CalloutAmberBg,
            CalloutAmberBorder,
            Icons.Default.Lightbulb
        )
        CalloutType.WARNING -> Triple(
            Color(0x22EF4444),
            Color(0xFFEF4444),
            Icons.Default.Warning
        )
        CalloutType.SUCCESS -> Triple(
            CalloutGreenBg,
            CalloutGreenBorder,
            Icons.Default.Check
        )
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, borderColor.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
            .clip(RoundedCornerShape(10.dp)),
        color = bgColor
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = icon,
                contentDescription = callout.title,
                tint = borderColor,
                modifier = Modifier
                    .size(20.dp)
                    .padding(top = 2.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = callout.title,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontFamily = baseFontFamily,
                        color = borderColor
                    )
                )
                if (callout.content.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = formatInlineMarkdown(callout.content),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = baseFontFamily,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun RenderTaskItem(
    item: MarkdownBlock.TaskItem,
    baseFontFamily: FontFamily,
    onToggle: (lineIndex: Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle(item.lineIndex) }
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = item.checked,
            onCheckedChange = { onToggle(item.lineIndex) },
            colors = CheckboxDefaults.colors(
                checkedColor = MaterialTheme.colorScheme.primary,
                uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            modifier = Modifier.size(36.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = formatInlineMarkdown(item.text),
            style = MaterialTheme.typography.bodyMedium.copy(
                fontFamily = baseFontFamily,
                textDecoration = if (item.checked) TextDecoration.LineThrough else TextDecoration.None,
                color = if (item.checked) {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
        )
    }
}

@Composable
private fun RenderBulletItem(item: MarkdownBlock.BulletItem, baseFontFamily: FontFamily) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .padding(top = 8.dp, end = 10.dp)
                .size(6.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
        )
        Text(
            text = formatInlineMarkdown(item.text),
            style = MaterialTheme.typography.bodyMedium.copy(
                fontFamily = baseFontFamily,
                color = MaterialTheme.colorScheme.onSurface
            )
        )
    }
}

@Composable
private fun RenderNumberedItem(item: MarkdownBlock.NumberedItem, baseFontFamily: FontFamily) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = "${item.number}.",
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Bold,
                fontFamily = baseFontFamily,
                color = MaterialTheme.colorScheme.primary
            ),
            modifier = Modifier.width(24.dp)
        )
        Text(
            text = formatInlineMarkdown(item.text),
            style = MaterialTheme.typography.bodyMedium.copy(
                fontFamily = baseFontFamily,
                color = MaterialTheme.colorScheme.onSurface
            )
        )
    }
}

@Composable
private fun RenderQuote(quote: MarkdownBlock.Quote, baseFontFamily: FontFamily) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            .padding(start = 12.dp, top = 8.dp, bottom = 8.dp, end = 12.dp)
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(36.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(MaterialTheme.colorScheme.primary)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = formatInlineMarkdown(quote.text),
            style = MaterialTheme.typography.bodyMedium.copy(
                fontFamily = baseFontFamily,
                fontStyle = FontStyle.Italic,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
            )
        )
    }
}

@Composable
private fun RenderCodeBlock(codeBlock: MarkdownBlock.CodeBlock) {
    val context = LocalContext.current
    var copied by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), RoundedCornerShape(10.dp)),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (codeBlock.language.isNotBlank()) codeBlock.language.uppercase() else "CÓDIGO",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                )

                IconButton(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("code", codeBlock.code)
                        clipboard.setPrimaryClip(clip)
                        copied = true
                        Toast.makeText(context, "Código copiado", Toast.LENGTH_SHORT).show()
                        scope.launch {
                            delay(2000)
                            copied = false
                        }
                    },
                    modifier = Modifier.size(32.dp).testTag("copy_code_button")
                ) {
                    Icon(
                        imageVector = if (copied) Icons.Default.Check else Icons.Default.ContentCopy,
                        contentDescription = "Copiar código",
                        tint = if (copied) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = codeBlock.code,
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
private fun RenderDivider() {
    HorizontalDivider(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        thickness = 1.dp,
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    )
}

@Composable
private fun RenderParagraph(paragraph: MarkdownBlock.Paragraph, baseFontFamily: FontFamily) {
    Text(
        text = formatInlineMarkdown(paragraph.text),
        style = MaterialTheme.typography.bodyMedium.copy(
            fontFamily = baseFontFamily,
            lineHeight = 22.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
    )
}

/**
 * Parses inline markdown: **bold**, *italic*, `code`, ~~strike~~, [font:id]...[/font], #tags
 */
@Composable
fun formatInlineMarkdown(rawText: String): AnnotatedString {
    val primaryColor = MaterialTheme.colorScheme.primary
    val codeBgColor = MaterialTheme.colorScheme.surfaceVariant
    val codeTextColor = MaterialTheme.colorScheme.onSurfaceVariant

    return remember(rawText, primaryColor, codeBgColor) {
        buildAnnotatedString {
            var i = 0
            val len = rawText.length

            while (i < len) {
                when {
                    // Inline custom font tags: [font:serif]text[/font]
                    rawText.startsWith("[font:", i) && rawText.indexOf(']', i) != -1 && rawText.indexOf("[/font]", i) != -1 -> {
                        val closeBracket = rawText.indexOf(']', i)
                        val endTag = rawText.indexOf("[/font]", closeBracket + 1)
                        if (closeBracket != -1 && endTag != -1) {
                            val fontId = rawText.substring(i + 6, closeBracket).trim()
                            val fontTarget = AppFontTheme.fromId(fontId)
                            val innerText = rawText.substring(closeBracket + 1, endTag)
                            pushStyle(SpanStyle(fontFamily = fontTarget.fontFamily))
                            append(innerText)
                            pop()
                            i = endTag + 7
                        } else {
                            append(rawText[i])
                            i++
                        }
                    }

                    // Inline code `code`
                    rawText[i] == '`' && rawText.indexOf('`', i + 1) != -1 -> {
                        val end = rawText.indexOf('`', i + 1)
                        val codeText = rawText.substring(i + 1, end)
                        pushStyle(
                            SpanStyle(
                                fontFamily = FontFamily.Monospace,
                                background = codeBgColor,
                                color = codeTextColor,
                                fontSize = 13.sp
                            )
                        )
                        append(" $codeText ")
                        pop()
                        i = end + 1
                    }

                    // Bold **text**
                    rawText.startsWith("**", i) && rawText.indexOf("**", i + 2) != -1 -> {
                        val end = rawText.indexOf("**", i + 2)
                        val boldText = rawText.substring(i + 2, end)
                        pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                        append(boldText)
                        pop()
                        i = end + 2
                    }

                    // Strikethrough ~~text~~
                    rawText.startsWith("~~", i) && rawText.indexOf("~~", i + 2) != -1 -> {
                        val end = rawText.indexOf("~~", i + 2)
                        val strikeText = rawText.substring(i + 2, end)
                        pushStyle(SpanStyle(textDecoration = TextDecoration.LineThrough))
                        append(strikeText)
                        pop()
                        i = end + 2
                    }

                    // Italic *text*
                    rawText[i] == '*' && rawText.indexOf('*', i + 1) != -1 -> {
                        val end = rawText.indexOf('*', i + 1)
                        val italicText = rawText.substring(i + 1, end)
                        pushStyle(SpanStyle(fontStyle = FontStyle.Italic))
                        append(italicText)
                        pop()
                        i = end + 1
                    }

                    // Tag #tag
                    rawText[i] == '#' && (i == 0 || rawText[i - 1].isWhitespace()) -> {
                        var end = i + 1
                        while (end < len && !rawText[end].isWhitespace() && rawText[end] !in listOf(',', '.', '!', '?')) {
                            end++
                        }
                        val tag = rawText.substring(i, end)
                        pushStyle(
                            SpanStyle(
                                color = primaryColor,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                        append(tag)
                        pop()
                        i = end
                    }

                    else -> {
                        append(rawText[i])
                        i++
                    }
                }
            }
        }
    }
}
