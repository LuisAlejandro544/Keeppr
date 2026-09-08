package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

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

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Dynamic color is available on Android 12+
  dynamicColor: Boolean = true,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
