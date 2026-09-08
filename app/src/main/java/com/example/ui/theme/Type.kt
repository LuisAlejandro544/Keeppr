package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Catálogo de las 5 tipografías disponibles en VaultNotes.
 *
 * Se utilizan familias tipográficas estándar del sistema Android para asegurar:
 * 1. Funcionamiento 100% offline (sin requerir Google Play Services o descargas de red).
 * 2. Cero latencia o parpadeos de carga de fuentes.
 * 3. Máxima compatibilidad en dispositivos de 32 y 64 bits (Android 8.0+ / Oreo en adelante).
 */
enum class AppFontTheme(
    val id: String,
    val displayName: String,
    val subtitle: String,
    val previewSample: String,
    val fontFamily: FontFamily
) {
    DEFAULT(
        id = "default",
        displayName = "Predeterminada",
        subtitle = "Tipografía estándar del sistema Android (Roboto / Variable)",
        previewSample = "La agilidad del pensamiento libre y seguro.",
        fontFamily = FontFamily.Default
    ),
    SANS_SERIF(
        id = "sans_serif",
        displayName = "Sans-Serif Moderna",
        subtitle = "Limpia, geométrica y con máxima legibilidad en pantalla",
        previewSample = "Claridad absoluta en cada idea y estructura.",
        fontFamily = FontFamily.SansSerif
    ),
    SERIF(
        id = "serif",
        displayName = "Serif Clásica",
        subtitle = "Estilo editorial tradicional, ideal para lectura inmersiva",
        previewSample = "La elegancia y serenidad de la palabra escrita.",
        fontFamily = FontFamily.Serif
    ),
    MONOSPACE(
        id = "monospace",
        displayName = "Monoespaciada (Código)",
        subtitle = "Anchura fija para bloques técnicos, markdown y tablas",
        previewSample = "printf(\"VaultNotes::Core::Encrypted\");",
        fontFamily = FontFamily.Monospace
    ),
    CURSIVE(
        id = "cursive",
        displayName = "Cursiva Manuscrita",
        subtitle = "Expresiva y creativa, evoca la caligrafía de una libreta personal",
        previewSample = "Inspiración espontánea y reflexiones diarias.",
        fontFamily = FontFamily.Cursive
    );

    companion object {
        /**
         * Recupera la tipografía correspondiente por su identificador persistente.
         * En caso de valor nulo o desconocido, recurre de forma segura a [DEFAULT].
         */
        fun fromId(id: String?): AppFontTheme {
            return entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: DEFAULT
        }
    }
}

/**
 * Genera el conjunto de estilos tipográficos de Material Design 3 aplicando la [FontFamily] seleccionada
 * de manera consistente a todas las escalas de texto (títulos, encabezados, cuerpo y etiquetas).
 */
fun createAppTypography(fontFamily: FontFamily): Typography {
    return Typography(
        displayLarge = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 32.sp,
            lineHeight = 40.sp,
            letterSpacing = (-0.25).sp
        ),
        displayMedium = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 28.sp,
            lineHeight = 36.sp
        ),
        headlineLarge = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp,
            lineHeight = 32.sp
        ),
        headlineMedium = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 20.sp,
            lineHeight = 28.sp
        ),
        headlineSmall = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 18.sp,
            lineHeight = 24.sp
        ),
        titleLarge = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            lineHeight = 26.sp
        ),
        titleMedium = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp,
            lineHeight = 24.sp
        ),
        titleSmall = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            lineHeight = 20.sp
        ),
        bodyLarge = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 16.sp,
            lineHeight = 24.sp,
            letterSpacing = 0.3.sp
        ),
        bodyMedium = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.25.sp
        ),
        bodySmall = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 12.sp,
            lineHeight = 16.sp
        ),
        labelLarge = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            lineHeight = 20.sp
        ),
        labelMedium = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            lineHeight = 16.sp
        ),
        labelSmall = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 11.sp,
            lineHeight = 16.sp
        )
    )
}

// Tipografía predeterminada del sistema expuesta para compatibilidad inicial
val Typography = createAppTypography(FontFamily.Default)
