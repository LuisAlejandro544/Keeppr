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

    private external fun getNativeInfo(): String
    private external fun executeLua(script: String): String
    private external fun calculateReadingTimeInRust(wordCount: Int): Int
}
