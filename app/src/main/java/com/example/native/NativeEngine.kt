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

    // Expresiones regulares precompiladas para evitar sobrecarga en el Garbage Collector de la JVM
    private val FONT_TAG_REGEX = Regex("\\[/?font(:[^\\]]+)?\\]")
    private val WHITESPACE_REGEX = Regex("\\s+")

    // Caché LRU de resúmenes de notas en memoria para evitar llamadas JNI repetidas durante el renderizado de listas
    private val summaryCache = android.util.LruCache<Int, NoteSummary>(200)

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
        val cacheKey = 31 * content.hashCode() + maxSnippetLen
        summaryCache.get(cacheKey)?.let { return it }

        val summary = if (!isLoaded) {
            val words = if (content.isBlank()) 0 else content.trim().split(WHITESPACE_REGEX).size
            val readingTime = (words / 3.3).toInt()
            val snippet = content.lines().filter { it.isNotBlank() && !it.startsWith("#") }.joinToString(" ").take(maxSnippetLen)
            val cleanedSnippet = snippet.replace(FONT_TAG_REGEX, "")
            NoteSummary(words, readingTime, cleanedSnippet)
        } else {
            try {
                val raw = processNoteSummaryInRust(content, maxSnippetLen)
                val parts = raw.split("|", limit = 3)
                val words = parts.getOrNull(0)?.toIntOrNull() ?: 0
                val readingTime = parts.getOrNull(1)?.toIntOrNull() ?: 0
                val snippet = parts.getOrNull(2) ?: ""
                val cleanedSnippet = snippet.replace(FONT_TAG_REGEX, "")
                NoteSummary(words, readingTime, cleanedSnippet)
            } catch (e: Throwable) {
                Log.e(TAG, "Error procesando resumen en Rust", e)
                val words = if (content.isBlank()) 0 else content.trim().split(WHITESPACE_REGEX).size
                val readingTime = (words / 3.3).toInt()
                val snippet = content.lines().filter { it.isNotBlank() && !it.startsWith("#") }.joinToString(" ").take(maxSnippetLen)
                val cleanedSnippet = snippet.replace(FONT_TAG_REGEX, "")
                NoteSummary(words, readingTime, cleanedSnippet)
            }
        }
        summaryCache.put(cacheKey, summary)
        return summary
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

    /**
     * Genera una firma criptográfica nativa en Rust con SHA-256 autenticado
     * para empaquetar notas en archivos .zip de VaultNotes.
     */
    fun generateVaultSignature(data: ByteArray): String {
        return if (isLoaded) {
            try {
                generateVaultSignatureInRust(data)
            } catch (e: Throwable) {
                Log.e(TAG, "Error generando firma de bóveda en Rust", e)
                ""
            }
        } else {
            ""
        }
    }

    /**
     * Verifica en tiempo constante mediante Rust si una firma criptográfica
     * coincide exactamente con los datos del paquete de la bóveda.
     */
    fun verifyVaultSignature(data: ByteArray, signature: String): Boolean {
        if (signature.isBlank()) return false
        return if (isLoaded) {
            try {
                verifyVaultSignatureInRust(data, signature)
            } catch (e: Throwable) {
                Log.e(TAG, "Error verificando firma de bóveda en Rust", e)
                false
            }
        } else {
            false
        }
    }

    private external fun getNativeInfo(): String
    private external fun executeLua(script: String): String
    private external fun calculateReadingTimeInRust(wordCount: Int): Int
    private external fun processNoteSummaryInRust(content: String, maxSnippetLen: Int): String
    private external fun matchNoteInRust(query: String, title: String, content: String, tags: String): Boolean
    private external fun generateVaultSignatureInRust(data: ByteArray): String
    private external fun verifyVaultSignatureInRust(data: ByteArray, signature: String): Boolean
}

/**
 * Representa las métricas y el extracto de texto calculados de manera nativa por Rust.
 */
data class NoteSummary(
    val wordCount: Int,
    val readingTimeSecs: Int,
    val snippet: String
)
