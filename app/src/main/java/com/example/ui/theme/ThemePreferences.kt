package com.example.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Representa los diferentes modos de tema visual soportados por VaultNotes.
 */
enum class AppThemeMode(val id: String, val displayName: String) {
    SYSTEM("system", "Sistema"),
    LIGHT("light", "Claro"),
    DARK("dark", "Oscuro");

    companion object {
        fun fromId(id: String?): AppThemeMode =
            entries.find { it.id == id } ?: SYSTEM
    }
}

/**
 * Paletas de color de acento disponibles cuando Material You está desactivado
 * o en dispositivos con versiones de Android anteriores a Android 12.
 */
enum class AppAccentPalette(
    val id: String,
    val displayName: String,
    val primaryColor: Color,
    val secondaryColor: Color
) {
    PURPLE("purple", "Obsidian Púrpura", ObsidianPurple, ObsidianCyan),
    EMERALD("emerald", "Esmeralda Cripto", Color(0xFF10B981), Color(0xFF34D399)),
    AMBER("amber", "Ámbar Cálido", Color(0xFFF59E0B), Color(0xFFFBBF24)),
    BLUE("blue", "Azul Zafiro", Color(0xFF3B82F6), Color(0xFF60A5FA)),
    ROSE("rose", "Rosa Neón", Color(0xFFF43F5E), Color(0xFFFB7185));

    companion object {
        fun fromId(id: String?): AppAccentPalette =
            entries.find { it.id == id } ?: PURPLE
    }
}
