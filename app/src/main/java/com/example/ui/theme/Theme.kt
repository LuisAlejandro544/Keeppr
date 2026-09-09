package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.graphics.Color

private val DarkColorScheme =
  darkColorScheme(
    primary = ObsidianPurple,
    onPrimary = Color(0xFF16082F),
    primaryContainer = Color(0xFF332066),
    onPrimaryContainer = ObsidianPurpleLight,
    secondary = ObsidianCyan,
    onSecondary = Color(0xFF00364A),
    background = ObsidianDarkBg,
    onBackground = ObsidianTextPrimary,
    surface = ObsidianDarkSurface,
    onSurface = ObsidianTextPrimary,
    surfaceVariant = ObsidianDarkSurfaceVariant,
    onSurfaceVariant = ObsidianTextSecondary,
    outline = ObsidianDarkBorder,
    outlineVariant = Color(0xFF3F4254),
  )

private val LightColorScheme =
  lightColorScheme(
    primary = NotionPurple,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0E7FF),
    onPrimaryContainer = Color(0xFF3730A3),
    secondary = Color(0xFF0284C7),
    onSecondary = Color.White,
    background = NotionLightBg,
    onBackground = NotionTextPrimary,
    surface = NotionLightSurface,
    onSurface = NotionTextPrimary,
    surfaceVariant = NotionLightSurfaceVariant,
    onSurfaceVariant = NotionTextSecondary,
    outline = NotionLightBorder,
    outlineVariant = Color(0xFFD1D5DB),
  )

/**
 * Tema principal de VaultNotes.
 *
 * Configura la paleta de colores M3 con soporte para:
 * - Modo Claro, Oscuro o seguimiento del Sistema.
 * - Material You dinámico en Android 12+ (con opción de activarlo/desactivarlo).
 * - Paletas de color personalizadas de acento cuando Material You está desactivado o en Android < 12.
 * - Escala de fuentes fijada a 1.0f para prevenir desbordes en pantalla móvil.
 * - Tipografía activa elegida entre las 5 opciones nativas de Android.
 */
@Composable
fun MyApplicationTheme(
  themeMode: AppThemeMode = AppThemeMode.SYSTEM,
  dynamicColor: Boolean = true,
  accentPalette: AppAccentPalette = AppAccentPalette.PURPLE,
  fontTheme: AppFontTheme = AppFontTheme.DEFAULT,
  content: @Composable () -> Unit,
) {
  val isDark = when (themeMode) {
    AppThemeMode.SYSTEM -> isSystemInDarkTheme()
    AppThemeMode.LIGHT -> false
    AppThemeMode.DARK -> true
  }

  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      isDark -> {
        DarkColorScheme.copy(
          primary = accentPalette.primaryColor,
          secondary = accentPalette.secondaryColor
        )
      }

      else -> {
        LightColorScheme.copy(
          primary = accentPalette.primaryColor,
          secondary = accentPalette.secondaryColor
        )
      }
    }

  // Se crea el conjunto de estilos tipográficos adaptado a la tipografía seleccionada
  val typography = remember(fontTheme) {
    createAppTypography(fontTheme.fontFamily)
  }

  val currentDensity = LocalDensity.current
  CompositionLocalProvider(
    LocalDensity provides Density(
      density = currentDensity.density,
      fontScale = 1.0f
    )
  ) {
    MaterialTheme(colorScheme = colorScheme, typography = typography, content = content)
  }
}

