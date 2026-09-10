package com.example.updater

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.content.FileProvider
import com.example.native.NativeEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.concurrent.TimeUnit

/**
 * Información estructurada de un Release Beta disponible para descarga.
 */
data class BetaReleaseInfo(
    val tagName: String,
    val title: String,
    val releaseNotes: String,
    val isPrerelease: Boolean,
    val apkUrl: String,
    val apkFileName: String,
    val apkSize: Long,
    val publishedAt: String
)

/**
 * Estados del flujo de actualización gestionado por el motor nativo de Lua y Android.
 */
sealed class UpdateStatus {
    object Idle : UpdateStatus()
    object Checking : UpdateStatus()
    data class UpToDate(val currentVersion: String, val lastCheckedTime: Long = System.currentTimeMillis()) : UpdateStatus()
    data class UpdateAvailable(val releaseInfo: BetaReleaseInfo) : UpdateStatus()
    data class Downloading(
        val progressPercent: Int,
        val downloadedMb: Float,
        val totalMb: Float
    ) : UpdateStatus()
    data class ReadyToInstall(val apkFile: File, val releaseInfo: BetaReleaseInfo) : UpdateStatus()
    data class Error(val message: String) : UpdateStatus()
}

/**
 * Gestor central de actualizaciones automáticas de Keeppr.
 *
 * Utiliza el script nativo de Lua 5.4 (`updater_config.lua`) para desacoplar endpoints,
 * filtrar pre-releases beta terminadas en `-b` y descargar el APK directamente
 * al almacenamiento local sin requerir navegación externa.
 */
class AppUpdateManager(private val context: Context) {

    private val tag = "AppUpdateManager"

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val _status = MutableStateFlow<UpdateStatus>(UpdateStatus.Idle)
    val status: StateFlow<UpdateStatus> = _status.asStateFlow()

    private var activeLuaScript: String? = null

    /**
     * Carga el script de Lua desde assets locales o descarga la versión más reciente
     * desde GitHub Raw para aplicar cambios dinámicos sin recompilar.
     */
    private suspend fun obtainLuaScript(): String = withContext(Dispatchers.IO) {
        // 1. Si ya se cargó en memoria, reutilizar
        activeLuaScript?.let { return@withContext it }

        // 2. Cargar primero el script base empaquetado en assets
        var scriptContent = ""
        try {
            context.assets.open("updater_config.lua").use { inputStream ->
                scriptContent = inputStream.bufferedReader().use { it.readText() }
            }
        } catch (e: Exception) {
            Log.w(tag, "No se pudo leer assets/updater_config.lua local: ${e.message}")
        }

        // 3. Intentar consultar la versión más reciente en GitHub Raw para obtener URLs dinámicas
        try {
            val rawUrl = "https://raw.githubusercontent.com/LuisAlejandro544/Keeppr/main/updater_config.lua"
            val request = Request.Builder()
                .url(rawUrl)
                .addHeader("User-Agent", "Keeppr-Android-App")
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val remoteContent = response.body?.string()
                    if (!remoteContent.isNullOrBlank() && remoteContent.contains("UpdaterConfig")) {
                        Log.i(tag, "Configuración Lua remota de GitHub Raw cargada exitosamente.")
                        scriptContent = remoteContent
                    }
                }
            }
        } catch (e: Exception) {
            Log.d(tag, "Uso de script Lua local (sin conexión a GitHub Raw): ${e.message}")
        }

        activeLuaScript = scriptContent
        scriptContent
    }

    /**
     * Consulta la API de GitHub y filtra las pre-releases de acuerdo con las reglas
     * programadas en el script nativo de Lua 5.4.
     */
    suspend fun checkForUpdates(currentVersion: String) = withContext(Dispatchers.IO) {
        _status.value = UpdateStatus.Checking

        // Limpieza preventiva de APKs antiguos o ya instalados para no malgastar espacio en disco
        cleanInstalledOrStaleApks(currentVersion)

        try {
            val luaScript = obtainLuaScript()
            if (luaScript.isBlank()) {
                _status.value = UpdateStatus.Error("No se pudo inicializar la configuración de Lua.")
                return@withContext
            }

            // Ejecutar el script en el motor nativo C++/Lua para extraer la configuración JSON
            val executionCode = "$luaScript\nreturn UpdaterConfig.get_config_json()"
            val jsonConfigRaw = NativeEngine.evalLua(executionCode)

            if (jsonConfigRaw.startsWith("Error")) {
                Log.e(tag, "Error evaluando script Lua: $jsonConfigRaw")
                _status.value = UpdateStatus.Error("Error nativo en configuración Lua: $jsonConfigRaw")
                return@withContext
            }

            val configJson = JSONObject(jsonConfigRaw)
            val releasesApiUrl = configJson.optString(
                "releases_api",
                "https://api.github.com/repos/LuisAlejandro544/Keeppr/releases"
            )

            // Consultar la lista de releases en GitHub
            val request = Request.Builder()
                .url(releasesApiUrl)
                .addHeader("Accept", "application/vnd.github.v3+json")
                .addHeader("User-Agent", "Keeppr-Beta-Updater")
                .build()

            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                _status.value = UpdateStatus.Error("Error al consultar releases (HTTP ${response.code})")
                return@withContext
            }

            val responseBody = response.body?.string() ?: ""
            val releasesArray = JSONArray(responseBody)

            if (releasesArray.length() == 0) {
                _status.value = UpdateStatus.UpToDate(currentVersion)
                return@withContext
            }

            var candidateRelease: BetaReleaseInfo? = null

            // Iterar releases de GitHub y validar con el motor nativo de Lua
            for (i in 0 until releasesArray.length()) {
                val releaseObj = releasesArray.getJSONObject(i)
                val tagName = releaseObj.optString("tag_name", "")
                val isPrerelease = releaseObj.optBoolean("prerelease", false)
                val isDraft = releaseObj.optBoolean("draft", false)

                if (isDraft) continue

                // Ejecutar filtro lógico de Lua nativo: debe ser pre-release y contener '-b'
                val luaFilterCall = "$luaScript\nreturn tostring(UpdaterConfig.filter_beta_release(\"$tagName\", $isPrerelease))"
                val luaFilterResult = NativeEngine.evalLua(luaFilterCall).trim()

                val isBetaEligible = luaFilterResult.equals("true", ignoreCase = true) ||
                        (isPrerelease && tagName.contains("-b"))

                if (isBetaEligible) {
                    // Buscar el archivo APK en los assets del release
                    val assetsArray = releaseObj.optJSONArray("assets") ?: JSONArray()
                    var apkDownloadUrl = ""
                    var apkFileName = ""
                    var apkSize = 0L

                    for (j in 0 until assetsArray.length()) {
                        val assetObj = assetsArray.getJSONObject(j)
                        val name = assetObj.optString("name", "")
                        if (name.endsWith(".apk", ignoreCase = true)) {
                            apkDownloadUrl = assetObj.optString("browser_download_url", "")
                            apkFileName = name
                            apkSize = assetObj.optLong("size", 0L)
                            break
                        }
                    }

                    if (apkDownloadUrl.isNotBlank()) {
                        candidateRelease = BetaReleaseInfo(
                            tagName = tagName,
                            title = releaseObj.optString("name", tagName),
                            releaseNotes = releaseObj.optString("body", "Nueva actualización beta disponible."),
                            isPrerelease = isPrerelease,
                            apkUrl = apkDownloadUrl,
                            apkFileName = if (apkFileName.isNotBlank()) apkFileName else "Keeppr-$tagName.apk",
                            apkSize = apkSize,
                            publishedAt = releaseObj.optString("published_at", "")
                        )
                        break // Tomar el pre-release beta más reciente
                    }
                }
            }

            if (candidateRelease == null) {
                _status.value = UpdateStatus.UpToDate(currentVersion)
                return@withContext
            }

            // Comparar si el release remoto es más nuevo que la versión instalada usando Lua
            val luaCompareCall = "$luaScript\nreturn tostring(UpdaterConfig.is_newer_version(\"$currentVersion\", \"${candidateRelease.tagName}\"))"
            val isNewer = NativeEngine.evalLua(luaCompareCall).trim().equals("true", ignoreCase = true)

            if (isNewer) {
                _status.value = UpdateStatus.UpdateAvailable(candidateRelease)
            } else {
                _status.value = UpdateStatus.UpToDate(currentVersion)
            }

        } catch (e: Exception) {
            Log.e(tag, "Fallo durante la búsqueda de actualizaciones", e)
            _status.value = UpdateStatus.Error("Error de conexión: ${e.localizedMessage ?: "No se pudo conectar"}")
        }
    }

    /**
     * Limpia de forma proactiva archivos APK obsoletos o huérfanos en la caché de disco
     * para evitar acumulación de memoria basura (archivos de 25-35MB).
     *
     * @param preserveFileName Nombre opcional de un APK específico que se desea conservar.
     */
    fun purgeUpdatesCache(preserveFileName: String? = null) {
        try {
            val updatesDir = File(context.cacheDir, "updates")
            if (updatesDir.exists() && updatesDir.isDirectory) {
                updatesDir.listFiles()?.forEach { file ->
                    if (file.isFile && (preserveFileName == null || file.name != preserveFileName)) {
                        val deleted = file.delete()
                        if (deleted) {
                            Log.d(tag, "Caché de disco liberada: eliminado APK antiguo ${file.name}")
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(tag, "No se pudo purgar la caché de APKs", e)
        }
    }

    /**
     * Elimina automáticamente cualquier APK en caché si su versión es igual o inferior
     * a la versión actualmente instalada en el dispositivo móvil.
     */
    private fun cleanInstalledOrStaleApks(currentVersion: String) {
        try {
            val updatesDir = File(context.cacheDir, "updates")
            if (!updatesDir.exists()) return

            val maxAgeMs = 24 * 60 * 60 * 1000L // 24 horas de expiración para APKs sin instalar
            val now = System.currentTimeMillis()

            updatesDir.listFiles()?.forEach { file ->
                if (file.isFile && file.name.endsWith(".apk")) {
                    val isStale = (now - file.lastModified()) > maxAgeMs
                    val matchesCurrent = file.name.contains(currentVersion, ignoreCase = true)
                    if (isStale || matchesCurrent) {
                        file.delete()
                        Log.i(tag, "Limpieza automática de caché: eliminado APK ${file.name} (stale=$isStale, current=$matchesCurrent)")
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(tag, "Error en limpieza preventiva de caché APK", e)
        }
    }

    /**
     * Descarga el archivo APK directamente en la memoria caché privada de la app
     * reportando el progreso en tiempo real.
     * Pega antes una purga inteligente para no acumular APKs de versiones pasadas.
     */
    suspend fun downloadApk(releaseInfo: BetaReleaseInfo) = withContext(Dispatchers.IO) {
        try {
            _status.value = UpdateStatus.Downloading(0, 0f, 0f)

            // Purgar de inmediato APKs antiguos antes de comenzar una nueva descarga
            purgeUpdatesCache(preserveFileName = releaseInfo.apkFileName)

            val updatesDir = File(context.cacheDir, "updates")
            if (!updatesDir.exists()) {
                updatesDir.mkdirs()
            }

            val targetFile = File(updatesDir, releaseInfo.apkFileName)
            if (targetFile.exists()) {
                targetFile.delete()
            }

            val request = Request.Builder()
                .url(releaseInfo.apkUrl)
                .addHeader("User-Agent", "Keeppr-Beta-Downloader")
                .build()

            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                _status.value = UpdateStatus.Error("Error de descarga (HTTP ${response.code})")
                return@withContext
            }

            val body = response.body
            if (body == null) {
                _status.value = UpdateStatus.Error("Cuerpo de descarga vacío")
                return@withContext
            }

            val totalBytes = body.contentLength().let { if (it > 0) it else releaseInfo.apkSize }
            val totalMb = if (totalBytes > 0) totalBytes / (1024f * 1024f) else 0f

            var downloadedBytes = 0L
            val buffer = ByteArray(8 * 1024)

            val inputStream: InputStream = body.byteStream()
            val outputStream = FileOutputStream(targetFile)

            var lastReportTime = System.currentTimeMillis()

            inputStream.use { input ->
                outputStream.use { output ->
                    var bytesRead: Int
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        downloadedBytes += bytesRead

                        val now = System.currentTimeMillis()
                        if (now - lastReportTime > 100 || downloadedBytes == totalBytes) {
                            lastReportTime = now
                            val progressPercent = if (totalBytes > 0) {
                                ((downloadedBytes * 100) / totalBytes).toInt().coerceIn(0, 100)
                            } else 0
                            val downloadedMb = downloadedBytes / (1024f * 1024f)
                            _status.value = UpdateStatus.Downloading(progressPercent, downloadedMb, totalMb)
                        }
                    }
                    output.flush()
                }
            }

            Log.i(tag, "APK descargado exitosamente: ${targetFile.absolutePath} (${targetFile.length()} bytes)")
            _status.value = UpdateStatus.ReadyToInstall(targetFile, releaseInfo)

        } catch (e: Exception) {
            Log.e(tag, "Error durante la descarga del APK", e)
            _status.value = UpdateStatus.Error("Fallo al descargar: ${e.localizedMessage}")
        }
    }

    /**
     * Comprueba si la aplicación tiene autorización del sistema operativo para solicitar instalaciones de APK.
     */
    fun canRequestPackageInstalls(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            true
        }
    }

    /**
     * Inicia la instalación del archivo APK descargado a través del instalador de paquetes
     * nativo de Android sin salir a navegadores externos.
     */
    fun installApk(activityContext: Context, apkFile: File) {
        try {
            if (!apkFile.exists()) {
                _status.value = UpdateStatus.Error("El archivo APK descargado no existe.")
                return
            }

            // Comprobar permiso de instalación de paquetes desconocidos en Android 8.0+ (API 26+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val canInstall = activityContext.packageManager.canRequestPackageInstalls()
                if (!canInstall) {
                    android.widget.Toast.makeText(
                        activityContext,
                        "Activa 'Permitir desde esta fuente' para que Keeppr pueda instalar la actualización",
                        android.widget.Toast.LENGTH_LONG
                    ).show()

                    val settingsIntent = Intent(
                        Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                        Uri.parse("package:${activityContext.packageName}")
                    ).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    activityContext.startActivity(settingsIntent)
                    return
                }
            }

            val apkUri = FileProvider.getUriForFile(
                activityContext,
                "${activityContext.packageName}.fileprovider",
                apkFile
            )

            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            activityContext.startActivity(installIntent)

        } catch (e: Exception) {
            Log.e(tag, "Error al lanzar instalador de APK", e)
            _status.value = UpdateStatus.Error("No se pudo iniciar la instalación: ${e.localizedMessage}")
        }
    }

    /**
     * Restablece el estado del gestor de actualizaciones a inactivo.
     */
    fun resetStatus() {
        _status.value = UpdateStatus.Idle
    }
}
