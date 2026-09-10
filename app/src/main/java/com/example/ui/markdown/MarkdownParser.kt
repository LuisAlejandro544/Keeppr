package com.example.ui.markdown

import android.util.LruCache

sealed class MarkdownBlock {
    data class Header(val level: Int, val text: String, val lineIndex: Int) : MarkdownBlock()
    data class Callout(val type: CalloutType, val title: String, val content: String, val lineIndex: Int) : MarkdownBlock()
    data class TaskItem(val checked: Boolean, val text: String, val lineIndex: Int) : MarkdownBlock()
    data class BulletItem(val text: String, val lineIndex: Int) : MarkdownBlock()
    data class NumberedItem(val number: String, val text: String, val lineIndex: Int) : MarkdownBlock()
    data class Quote(val text: String, val lineIndex: Int) : MarkdownBlock()
    data class CodeBlock(val language: String, val code: String, val lineIndex: Int) : MarkdownBlock()
    data class Divider(val lineIndex: Int) : MarkdownBlock()
    data class Paragraph(val text: String, val lineIndex: Int) : MarkdownBlock()
    data class BlankLine(val lineIndex: Int) : MarkdownBlock()
}

enum class CalloutType {
    NOTE,
    INFO,
    TIP,
    WARNING,
    SUCCESS
}

object MarkdownParser {

    // Caché LRU de bloques Markdown en memoria RAM (máximo 30 notas o versiones recientes).
    // Evita recalcular expresiones regulares y recorrido de líneas cuando el usuario alterna
    // entre el editor y la vista previa, ahorrando batería y ciclos de CPU.
    private val parsedBlocksCache = LruCache<Int, List<MarkdownBlock>>(30)

    /**
     * Libera la memoria de la caché de parseo cuando se requiera liberar RAM.
     */
    fun clearCache() {
        parsedBlocksCache.evictAll()
    }

    fun parse(content: String): List<MarkdownBlock> {
        if (content.isEmpty()) return emptyList()

        val cacheKey = content.hashCode()
        parsedBlocksCache.get(cacheKey)?.let { cachedBlocks ->
            return cachedBlocks
        }

        val lines = content.lines()
        val blocks = mutableListOf<MarkdownBlock>()
        var i = 0

        while (i < lines.size) {
            val line = lines[i]
            val trimmed = line.trim()

            when {
                // Empty line: preservar el espacio vertical para que los saltos entre párrafos funcionen fielmente
                trimmed.isEmpty() -> {
                    blocks.add(MarkdownBlock.BlankLine(i))
                    i++
                }

                // Code block start
                trimmed.startsWith("```") -> {
                    val lang = trimmed.removePrefix("```").trim()
                    val codeLines = mutableListOf<String>()
                    val startIndex = i
                    i++
                    while (i < lines.size && !lines[i].trim().startsWith("```")) {
                        codeLines.add(lines[i])
                        i++
                    }
                    if (i < lines.size) {
                        i++ // consume closing ```
                    }
                    blocks.add(MarkdownBlock.CodeBlock(lang, codeLines.joinToString("\n"), startIndex))
                }

                // Divider
                trimmed == "---" || trimmed == "***" || trimmed == "___" -> {
                    blocks.add(MarkdownBlock.Divider(i))
                    i++
                }

                // Headings
                trimmed.startsWith("### ") -> {
                    blocks.add(MarkdownBlock.Header(3, trimmed.removePrefix("### "), i))
                    i++
                }
                trimmed.startsWith("## ") -> {
                    blocks.add(MarkdownBlock.Header(2, trimmed.removePrefix("## "), i))
                    i++
                }
                trimmed.startsWith("# ") -> {
                    blocks.add(MarkdownBlock.Header(1, trimmed.removePrefix("# "), i))
                    i++
                }

                // Callouts: > [!NOTE], > [!TIP], > [!WARNING], > [!INFO], > [!SUCCESS]
                trimmed.startsWith("> [!") -> {
                    val startIndex = i
                    val calloutHeader = trimmed.removePrefix("> [!").substringBefore("]").uppercase()
                    val calloutType = when (calloutHeader) {
                        "NOTE" -> CalloutType.NOTE
                        "INFO" -> CalloutType.INFO
                        "TIP", "IDEA" -> CalloutType.TIP
                        "WARNING", "DANGER", "CAUTION" -> CalloutType.WARNING
                        "SUCCESS", "CHECK" -> CalloutType.SUCCESS
                        else -> CalloutType.NOTE
                    }
                    val defaultTitle = when (calloutType) {
                        CalloutType.NOTE -> "Nota"
                        CalloutType.INFO -> "Información"
                        CalloutType.TIP -> "Consejo"
                        CalloutType.WARNING -> "Atención"
                        CalloutType.SUCCESS -> "Éxito"
                    }
                    val contentLines = mutableListOf<String>()
                    i++
                    while (i < lines.size && lines[i].trim().startsWith(">")) {
                        val calloutLine = lines[i].trim().removePrefix(">").trim()
                        if (calloutLine.isNotEmpty()) {
                            contentLines.add(calloutLine)
                        }
                        i++
                    }
                    blocks.add(
                        MarkdownBlock.Callout(
                            type = calloutType,
                            title = defaultTitle,
                            content = contentLines.joinToString("\n"),
                            lineIndex = startIndex
                        )
                    )
                }

                // Regular Blockquotes: > quote
                trimmed.startsWith(">") -> {
                    val startIndex = i
                    val quoteLines = mutableListOf<String>()
                    while (i < lines.size && lines[i].trim().startsWith(">")) {
                        quoteLines.add(lines[i].trim().removePrefix(">").trim())
                        i++
                    }
                    blocks.add(MarkdownBlock.Quote(quoteLines.joinToString("\n"), startIndex))
                }

                // Checklists: - [ ] or - [x]
                trimmed.startsWith("- [ ] ") || trimmed.startsWith("* [ ] ") -> {
                    val taskText = trimmed.substring(6)
                    blocks.add(MarkdownBlock.TaskItem(checked = false, text = taskText, lineIndex = i))
                    i++
                }
                trimmed.startsWith("- [x] ") || trimmed.startsWith("- [X] ") ||
                trimmed.startsWith("* [x] ") || trimmed.startsWith("* [X] ") -> {
                    val taskText = trimmed.substring(6)
                    blocks.add(MarkdownBlock.TaskItem(checked = true, text = taskText, lineIndex = i))
                    i++
                }

                // Bullet Lists: - or *
                trimmed.startsWith("- ") || trimmed.startsWith("* ") -> {
                    val itemText = trimmed.substring(2)
                    blocks.add(MarkdownBlock.BulletItem(itemText, i))
                    i++
                }

                // Numbered Lists: 1. or 2.
                Regex("^\\d+\\.\\s+.*").matches(trimmed) -> {
                    val number = trimmed.substringBefore(".")
                    val itemText = trimmed.substringAfter(".").trim()
                    blocks.add(MarkdownBlock.NumberedItem(number, itemText, i))
                    i++
                }

                // Regular Paragraph: agrupa líneas consecutivas no vacías en un solo bloque con salto natural
                else -> {
                    val startIndex = i
                    val paragraphLines = mutableListOf<String>()
                    while (i < lines.size) {
                        val currLine = lines[i]
                        val currTrimmed = currLine.trim()
                        if (currTrimmed.isEmpty() || isSpecialBlockStart(currTrimmed)) {
                            break
                        }
                        paragraphLines.add(currLine)
                        i++
                    }
                    if (paragraphLines.isNotEmpty()) {
                        blocks.add(MarkdownBlock.Paragraph(paragraphLines.joinToString("\n"), startIndex))
                    }
                }
            }
        }
        parsedBlocksCache.put(cacheKey, blocks)
        return blocks
    }

    /**
     * Comprueba si una línea inicia una entidad de bloque Markdown específica.
     */
    private fun isSpecialBlockStart(trimmed: String): Boolean {
        return trimmed.startsWith("```") ||
                trimmed == "---" || trimmed == "***" || trimmed == "___" ||
                trimmed.startsWith("# ") || trimmed.startsWith("## ") || trimmed.startsWith("### ") ||
                trimmed.startsWith(">") ||
                trimmed.startsWith("- [ ] ") || trimmed.startsWith("- [x] ") || trimmed.startsWith("- [X] ") ||
                trimmed.startsWith("* [ ] ") || trimmed.startsWith("* [x] ") || trimmed.startsWith("* [X] ") ||
                trimmed.startsWith("- ") || trimmed.startsWith("* ") ||
                Regex("^\\d+\\.\\s+.*").matches(trimmed)
    }

    /**
     * Toggles a checklist item at lineIndex in the markdown content string.
     */
    fun toggleChecklistAt(content: String, lineIndex: Int): String {
        val lines = content.lines().toMutableList()
        if (lineIndex in lines.indices) {
            val line = lines[lineIndex]
            val newLine = when {
                line.contains("- [ ]") -> line.replace("- [ ]", "- [x]")
                line.contains("- [x]") -> line.replace("- [x]", "- [ ]")
                line.contains("- [X]") -> line.replace("- [X]", "- [ ]")
                line.contains("* [ ]") -> line.replace("* [ ]", "* [x]")
                line.contains("* [x]") -> line.replace("* [x]", "* [ ]")
                line.contains("* [X]") -> line.replace("* [X]", "* [ ]")
                else -> line
            }
            lines[lineIndex] = newLine
            return lines.joinToString("\n")
        }
        return content
    }
}
