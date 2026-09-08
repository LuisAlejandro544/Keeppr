package com.example.native

import android.util.Log

object NativeEngine {
    private const val TAG = "NativeEngine"
    private var isLoaded = false

    init {
        try {
            System.loadLibrary("vaultnotes_native")
            isLoaded = true
            Log.i(TAG, "Motor nativo 'vaultnotes_native' cargado exitosamente.")
        } catch (e: UnsatisfiedLinkError) {
            Log.e(TAG, "No se pudo cargar la librería nativa", e)
        }
    }

    val isAvailable: Boolean
        get() = isLoaded

    fun getEngineInfo(): String {
        return if (isLoaded) {
            try {
                getNativeInfo()
            } catch (e: Throwable) {
                "Error nativo: ${e.message}"
            }
        } else {
            "Motor nativo no cargado"
        }
    }

    fun evalLua(script: String): String {
        return if (isLoaded) {
            try {
                executeLua(script)
            } catch (e: Throwable) {
                "Error Lua: ${e.message}"
            }
        } else {
            "Motor Lua no disponible"
        }
    }

    /**
     * Calcula el tiempo estimado de lectura en segundos utilizando el núcleo nativo de Rust.
     */
    fun calculateReadingTimeSecs(wordCount: Int): Int {
        return if (isLoaded) {
            try {
                calculateReadingTimeInRust(wordCount)
            } catch (e: Throwable) {
                (wordCount / 3.3).toInt()
            }
        } else {
            (wordCount / 3.3).toInt()
        }
    }

    /**
     * Procesa en una sola pasada en Rust nativo el conteo de palabras, tiempo de lectura y
     * extracto limpio (snippet) de Markdown sin asignar objetos pesados en el Garbage Collector de la JVM.
     */
    fun processNoteSummary(content: String, maxSnippetLen: Int = 120): NoteSummary {
        if (!isLoaded) {
            val words = if (content.isBlank()) 0 else content.trim().split(Regex("\\s+")).size
            val readingTime = (words / 3.3).toInt()
            val snippet = content.lines().filter { it.isNotBlank() && !it.startsWith("#") }.joinToString(" ").take(maxSnippetLen)
            return NoteSummary(words, readingTime, snippet)
        }

        return try {
            val raw = processNoteSummaryInRust(content, maxSnippetLen)
            val parts = raw.split("|", limit = 3)
            val words = parts.getOrNull(0)?.toIntOrNull() ?: 0
            val readingTime = parts.getOrNull(1)?.toIntOrNull() ?: 0
            val snippet = parts.getOrNull(2) ?: ""
            val cleanedSnippet = snippet.replace(Regex("\\[/?font(:[^\\]]+)?\\]"), "")
            NoteSummary(words, readingTime, cleanedSnippet)
        } catch (e: Throwable) {
            Log.e(TAG, "Error procesando resumen en Rust", e)
            val words = if (content.isBlank()) 0 else content.trim().split(Regex("\\s+")).size
            val readingTime = (words / 3.3).toInt()
            val snippet = content.lines().filter { it.isNotBlank() && !it.startsWith("#") }.joinToString(" ").take(maxSnippetLen)
            val cleanedSnippet = snippet.replace(Regex("\\[/?font(:[^\\]]+)?\\]"), "")
            NoteSummary(words, readingTime, cleanedSnippet)
        }
    }

    /**
     * Búsqueda y filtrado de notas acelerado por Rust nativo.
     * Evalúa coincidencia insensible a mayúsculas en título, contenido y etiquetas.
     */
    fun matchNote(query: String, title: String, content: String, tags: String): Boolean {
        if (!isLoaded) {
            val q = query.trim().lowercase()
            return title.lowercase().contains(q) || content.lowercase().contains(q) || tags.lowercase().contains(q)
        }
        return try {
            matchNoteInRust(query, title, content, tags)
        } catch (e: Throwable) {
            Log.e(TAG, "Error en coincidencia Rust", e)
            val q = query.trim().lowercase()
            title.lowercase().contains(q) || content.lowercase().contains(q) || tags.lowercase().contains(q)
        }
    }

    private external fun getNativeInfo(): String
    private external fun executeLua(script: String): String
    private external fun calculateReadingTimeInRust(wordCount: Int): Int
    private external fun processNoteSummaryInRust(content: String, maxSnippetLen: Int): String
    private external fun matchNoteInRust(query: String, title: String, content: String, tags: String): Boolean
}

/**
 * Representa las métricas y el extracto de texto calculados de manera nativa por Rust.
 */
data class NoteSummary(
    val wordCount: Int,
    val readingTimeSecs: Int,
    val snippet: String
)
