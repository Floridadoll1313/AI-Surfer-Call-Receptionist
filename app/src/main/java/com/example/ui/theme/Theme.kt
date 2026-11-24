package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val OceanDarkColorScheme = darkColorScheme(
  primary = SurfTurquoise,
  onPrimary = OceanAbyss,
  primaryContainer = OceanCardSecondary,
  onPrimaryContainer = SurfWaveFoam,
  secondary = SurfCyan,
  onSecondary = OceanAbyss,
  secondaryContainer = OceanCard,
  onSecondaryContainer = SandTextSecondary,
  tertiary = GoldenSun,
  onTertiary = OceanAbyss,
  tertiaryContainer = SunsetOrange,
  onTertiaryContainer = SandTextPrimary,
  error = CoralAlert,
  onError = Color.White,
  background = OceanAbyss,
  onBackground = SandTextPrimary,
  surface = OceanDeepBlue,
  onSurface = SandTextPrimary,
  surfaceVariant = OceanSurface,
  onSurfaceVariant = SandTextSecondary,
  outline = SurfTeal,
  outlineVariant = OceanCard
)

private val OceanLightColorScheme = lightColorScheme(
  primary = SurfTeal,
  onPrimary = Color.White,
  primaryContainer = Color(0xFFD4F1F9),
  onPrimaryContainer = OceanDeepBlue,
  secondary = SurfCyan,
  onSecondary = Color.White,
  secondaryContainer = Color(0xFFCAF0F8),
  onSecondaryContainer = OceanDeepBlue,
  tertiary = SunsetOrange,
  onTertiary = Color.White,
  tertiaryContainer = Color(0xFFFFE5B4),
  onTertiaryContainer = OceanAbyss,
  error = CoralAlert,
  onError = Color.White,
  background = Color(0xFFF0F9FB),
  onBackground = OceanAbyss,
  surface = Color(0xFFE3F2FD),
  onSurface = OceanAbyss,
  surfaceVariant = Color(0xFFD0EBF5),
  onSurfaceVariant = OceanDeepBlue,
  outline = SurfCyan,
  outlineVariant = Color(0xFFBEE3F8)
)

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true,
  dynamicColor: Boolean = false,
  palette: OceanPalette = OceanPalette.DEEP_SEA,
  onSelectPalette: (OceanPalette) -> Unit = {},
  content: @Composable () -> Unit,
) {
  if (darkTheme) {
    ThemeProvider(
      palette = palette,
      onSelectPalette = onSelectPalette,
      content = content
    )
  } else {
    MaterialTheme(colorScheme = OceanLightColorScheme, typography = Typography, content = content)
  }
}

