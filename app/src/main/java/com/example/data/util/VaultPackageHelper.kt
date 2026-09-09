package com.example.data.util

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import com.example.data.model.Note
import com.example.native.NativeEngine
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.nio.charset.StandardCharsets
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Utilidad de importación y exportación para VaultNotes.
 * Permite exportar notas en formato Markdown plano (.md) universal o en paquetes de
 * bóveda comprimidos (.zip) firmados criptográficamente con SHA-256 en Rust para
 * preservar tipografía, metadatos y asegurar autenticidad.
 */
object VaultPackageHelper {
    private const val TAG = "VaultPackageHelper"
    private const val VAULT_SEPARATOR = "\n---VAULT_SEPARATOR---\n"

    sealed class ImportResult {
        data class Success(val note: Note, val isAuthenticVault: Boolean, val message: String) : ImportResult()
        data class Error(val error: String) : ImportResult()
    }

    /**
     * Exporta la nota como un archivo Markdown (.md) plano universal.
     */
    fun exportMarkdown(context: Context, uri: Uri, note: Note): Boolean {
        return try {
            context.contentResolver.openOutputStream(uri)?.use { os ->
                // Generar contenido Markdown incluyendo título si no está en el cuerpo
                val contentBytes = note.content.toByteArray(StandardCharsets.UTF_8)
                os.write(contentBytes)
                os.flush()
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error exportando Markdown a $uri", e)
            false
        }
    }

    /**
     * Exporta la nota como un paquete seguro de bóveda (.zip) que contiene:
     * 1. note.md: El contenido del documento.
     * 2. vault_meta.json: Metadatos estructurados (título, tipografía, etiquetas, icono, etc.).
     * 3. signature.vault: Firma de autenticidad calculada nativamente en Rust con SHA-256.
     */
    fun exportVaultZip(context: Context, uri: Uri, note: Note): Boolean {
        return try {
            val metaJson = JSONObject().apply {
                put("format", "vaultnotes_v1")
                put("title", note.title)
                put("icon", note.icon)
                put("tags", note.tags)
                put("fontTheme", note.fontTheme)
                put("isPinned", note.isPinned)
                put("createdAt", note.createdAt)
                put("updatedAt", note.updatedAt)
            }.toString()

            val metaBytes = metaJson.toByteArray(StandardCharsets.UTF_8)
            val contentBytes = note.content.toByteArray(StandardCharsets.UTF_8)

            // Carga combinada para computar la firma de integridad en Rust
            val payload = ByteArrayOutputStream().apply {
                write(metaBytes)
                write(VAULT_SEPARATOR.toByteArray(StandardCharsets.UTF_8))
                write(contentBytes)
            }.toByteArray()

            val signature = NativeEngine.generateVaultSignature(payload)

            context.contentResolver.openOutputStream(uri)?.use { os ->
                ZipOutputStream(os).use { zos ->
                    // 1. note.md
                    zos.putNextEntry(ZipEntry("note.md"))
                    zos.write(contentBytes)
                    zos.closeEntry()

                    // 2. vault_meta.json
                    zos.putNextEntry(ZipEntry("vault_meta.json"))
                    zos.write(metaBytes)
                    zos.closeEntry()

                    // 3. signature.vault
                    zos.putNextEntry(ZipEntry("signature.vault"))
                    zos.write(signature.toByteArray(StandardCharsets.UTF_8))
                    zos.closeEntry()

                    zos.finish()
                }
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error exportando paquete ZIP de bóveda a $uri", e)
            false
        }
    }

    /**
     * Importa una nota a partir de un archivo .md, .txt o un paquete .zip de VaultNotes.
     */
    fun importFromUri(context: Context, uri: Uri): ImportResult {
        return try {
            val fileName = queryFileName(context, uri) ?: "nota_importada"
            val isZip = fileName.endsWith(".zip", ignoreCase = true) || isZipContent(context, uri)

            if (isZip) {
                importZipPackage(context, uri, fileName)
            } else {
                importPlainText(context, uri, fileName)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error durante la importación desde $uri", e)
            ImportResult.Error("Error al leer el archivo: ${e.localizedMessage ?: "Formato no compatible"}")
        }
    }

    private fun importZipPackage(context: Context, uri: Uri, fallbackTitle: String): ImportResult {
        var noteContent: String? = null
        var metaJsonStr: String? = null
        var signatureStr: String? = null

        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            ZipInputStream(inputStream).use { zis ->
                var entry: ZipEntry? = zis.nextEntry
                while (entry != null) {
                    when (entry.name) {
                        "note.md", "nota.md", "content.md" -> {
                            noteContent = readStreamToString(zis)
                        }
                        "vault_meta.json", "metadata.json" -> {
                            metaJsonStr = readStreamToString(zis)
                        }
                        "signature.vault", "vault_signature.txt" -> {
                            signatureStr = readStreamToString(zis).trim()
                        }
                        else -> {
                            // Si contiene un archivo de texto cualquiera y aún no tenemos contenido
                            if (noteContent == null && (entry.name.endsWith(".md") || entry.name.endsWith(".txt"))) {
                                noteContent = readStreamToString(zis)
                            }
                        }
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }
        }

        if (noteContent == null) {
            return ImportResult.Error("El archivo comprimido no contiene ningún documento Markdown (.md) válido.")
        }

        val content = noteContent!!

        // Si contiene metadatos y firma, validar mediante el motor Rust
        if (metaJsonStr != null && !signatureStr.isNullOrBlank()) {
            val metaBytes = metaJsonStr!!.toByteArray(StandardCharsets.UTF_8)
            val contentBytes = content.toByteArray(StandardCharsets.UTF_8)

            val payload = ByteArrayOutputStream().apply {
                write(metaBytes)
                write(VAULT_SEPARATOR.toByteArray(StandardCharsets.UTF_8))
                write(contentBytes)
            }.toByteArray()

            val isAuthentic = NativeEngine.verifyVaultSignature(payload, signatureStr!!)

            val json = try {
                JSONObject(metaJsonStr!!)
            } catch (e: Exception) {
                null
            }

            val title = json?.optString("title", fallbackTitle.removeSuffix(".zip")) ?: fallbackTitle.removeSuffix(".zip")
            val icon = json?.optString("icon", "📝") ?: "📝"
            val tags = json?.optString("tags", "") ?: ""
            val fontTheme = json?.optString("fontTheme", "default") ?: "default"
            val isPinned = json?.optBoolean("isPinned", false) ?: false
            val createdAt = json?.optLong("createdAt", System.currentTimeMillis()) ?: System.currentTimeMillis()

            val note = Note(
                title = title.ifBlank { "Nota importada" },
                content = content,
                icon = icon,
                tags = tags,
                fontTheme = fontTheme,
                isPinned = isPinned,
                createdAt = createdAt,
                updatedAt = System.currentTimeMillis()
            )

            return if (isAuthentic) {
                ImportResult.Success(
                    note = note,
                    isAuthenticVault = true,
                    message = "Paquete de bóveda verificado con éxito por Rust. Tipografía y metadatos restaurados."
                )
            } else {
                ImportResult.Success(
                    note = note,
                    isAuthenticVault = false,
                    message = "Nota restaurada desde el paquete ZIP (Firma ausente o no coincidente)."
                )
            }
        } else {
            // ZIP sin firma completa, importar como nota regular
            val cleanTitle = fallbackTitle.removeSuffix(".zip").ifBlank { "Nota importada" }
            val note = Note(
                title = cleanTitle,
                content = content,
                icon = "📦",
                tags = "importado",
                fontTheme = "default",
                isPinned = false
            )
            return ImportResult.Success(
                note = note,
                isAuthenticVault = false,
                message = "Nota extraída de archivo ZIP como texto Markdown."
            )
        }
    }

    private fun importPlainText(context: Context, uri: Uri, fileName: String): ImportResult {
        val rawText = context.contentResolver.openInputStream(uri)?.use { inputStream ->
            readStreamToString(inputStream)
        } ?: return ImportResult.Error("No se pudo leer el archivo seleccionado.")

        // Deducir título: primera línea de encabezado (# Título) o nombre del archivo
        val lines = rawText.lines()
        val firstHeaderLine = lines.firstOrNull { it.trim().startsWith("# ") }
        val title = if (firstHeaderLine != null) {
            firstHeaderLine.trim().removePrefix("# ").trim()
        } else {
            fileName.substringBeforeLast(".").replace("_", " ").replace("-", " ")
        }

        val note = Note(
            title = title.ifBlank { "Nota importada" },
            content = rawText,
            icon = "📄",
            tags = "importado",
            fontTheme = "default",
            isPinned = false
        )

        return ImportResult.Success(
            note = note,
            isAuthenticVault = false,
            message = "Documento de texto importado correctamente como Markdown."
        )
    }

    private fun isZipContent(context: Context, uri: Uri): Boolean {
        return try {
            context.contentResolver.openInputStream(uri)?.use { isStream ->
                val buffer = ByteArray(4)
                val read = isStream.read(buffer)
                read == 4 && buffer[0] == 0x50.toByte() && buffer[1] == 0x4b.toByte() &&
                        buffer[2] == 0x03.toByte() && buffer[3] == 0x04.toByte()
            } ?: false
        } catch (e: Exception) {
            false
        }
    }

    private fun readStreamToString(inputStream: InputStream): String {
        val baos = ByteArrayOutputStream()
        val buffer = ByteArray(4096)
        var length: Int
        while (inputStream.read(buffer).also { length = it } != -1) {
            baos.write(buffer, 0, length)
        }
        return baos.toString(StandardCharsets.UTF_8.name())
    }

    private fun queryFileName(context: Context, uri: Uri): String? {
        if (uri.scheme == "content") {
            try {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1 && cursor.moveToFirst()) {
                        return cursor.getString(nameIndex)
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "No se pudo consultar el nombre del archivo", e)
            }
        }
        return uri.lastPathSegment
    }
}
