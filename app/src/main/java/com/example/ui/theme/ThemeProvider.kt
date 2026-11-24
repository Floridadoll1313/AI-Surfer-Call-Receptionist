package com.example.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

enum class OceanPalette(
  val displayName: String,
  val tagline: String,
  val colors: OceanColors
) {
  DEEP_SEA(
    displayName = "Deep Sea",
    tagline = "Abyssal Midnight & Electric Surf Cyan",
    colors = OceanColors(
      abyss = OceanAbyss,
      deepBlue = OceanDeepBlue,
      surface = OceanSurface,
      card = OceanCard,
      cardSecondary = OceanCardSecondary,
      turquoise = SurfTurquoise,
      cyan = SurfCyan,
      teal = SurfTeal,
      waveFoam = SurfWaveFoam,
      goldenSun = GoldenSun,
      sunsetOrange = SunsetOrange,
      coralAlert = CoralAlert,
      seaGreen = SeaGreen,
      textPrimary = SandTextPrimary,
      textSecondary = SandTextSecondary,
      muted = MutedOcean
    )
  ),
  SUNSET_TIDE(
    displayName = "Sunset Tide",
    tagline = "Twilight Plum & Golden Amber Swell",
    colors = OceanColors(
      abyss = Color(0xFF1A0F1F),
      deepBlue = Color(0xFF28172E),
      surface = Color(0xFF361F3E),
      card = Color(0xFF45284E),
      cardSecondary = Color(0xFF56325F),
      turquoise = Color(0xFFFFB703),
      cyan = Color(0xFFFF8C42),
      teal = Color(0xFFD9534F),
      waveFoam = Color(0xFFFFF3E0),
      goldenSun = Color(0xFFFFD166),
      sunsetOrange = Color(0xFFFF6B35),
      coralAlert = Color(0xFFFF3366),
      seaGreen = Color(0xFF2EC4B6),
      textPrimary = Color(0xFFFFF8F0),
      textSecondary = Color(0xFFFFCBA4),
      muted = Color(0xFFA47E93)
    )
  ),
  REEF_GLOW(
    displayName = "Reef Glow",
    tagline = "Bioluminescent Lagoon & Emerald Mint",
    colors = OceanColors(
      abyss = Color(0xFF021A18),
      deepBlue = Color(0xFF052B27),
      surface = Color(0xFF083B35),
      card = Color(0xFF0D4E46),
      cardSecondary = Color(0xFF126359),
      turquoise = Color(0xFF18FFD0),
      cyan = Color(0xFF00D4AA),
      teal = Color(0xFF008F7A),
      waveFoam = Color(0xFFE0FFF8),
      goldenSun = Color(0xFFFFE066),
      sunsetOrange = Color(0xFFFF9F1C),
      coralAlert = Color(0xFFFF5C8A),
      seaGreen = Color(0xFF4DFF88),
      textPrimary = Color(0xFFF0FFF9),
      textSecondary = Color(0xFF96F2E0),
      muted = Color(0xFF539186)
    )
  ),
  ARCTIC_CREST(
    displayName = "Arctic Crest",
    tagline = "Glacial Midnight & Horizon Ice Blue",
    colors = OceanColors(
      abyss = Color(0xFF081226),
      deepBlue = Color(0xFF101E38),
      surface = Color(0xFF16294A),
      card = Color(0xFF1D355C),
      cardSecondary = Color(0xFF254373),
      turquoise = Color(0xFF70D6FF),
      cyan = Color(0xFF90E0EF),
      teal = Color(0xFF2B6CB0),
      waveFoam = Color(0xFFF0F8FF),
      goldenSun = Color(0xFFFFD166),
      sunsetOrange = Color(0xFFF4845F),
      coralAlert = Color(0xFFFF4D6D),
      seaGreen = Color(0xFF38D9A9),
      textPrimary = Color(0xFFF8FBFF),
      textSecondary = Color(0xFFBBD7F5),
      muted = Color(0xFF6B85A8)
    )
  );

  fun next(): OceanPalette {
    val all = entries
    return all[(ordinal + 1) % all.size]
  }
}

@Immutable
data class OceanColors(
  val abyss: Color,
  val deepBlue: Color,
  val surface: Color,
  val card: Color,
  val cardSecondary: Color,
  val turquoise: Color,
  val cyan: Color,
  val teal: Color,
  val waveFoam: Color,
  val goldenSun: Color,
  val sunsetOrange: Color,
  val coralAlert: Color,
  val seaGreen: Color,
  val textPrimary: Color,
  val textSecondary: Color,
  val muted: Color
) {
  fun toMaterialColorScheme(): ColorScheme = darkColorScheme(
    primary = turquoise,
    onPrimary = abyss,
    primaryContainer = cardSecondary,
    onPrimaryContainer = waveFoam,
    secondary = cyan,
    onSecondary = abyss,
    secondaryContainer = card,
    onSecondaryContainer = textSecondary,
    tertiary = goldenSun,
    onTertiary = abyss,
    tertiaryContainer = sunsetOrange,
    onTertiaryContainer = textPrimary,
    error = coralAlert,
    onError = Color.White,
    background = abyss,
    onBackground = textPrimary,
    surface = deepBlue,
    onSurface = textPrimary,
    surfaceVariant = surface,
    onSurfaceVariant = textSecondary,
    outline = teal,
    outlineVariant = card
  )
}

@Stable
data class OceanThemeController(
  val currentPalette: OceanPalette = OceanPalette.DEEP_SEA,
  val selectPalette: (OceanPalette) -> Unit = {},
  val cyclePalette: () -> Unit = {}
)

val LocalOceanColors = staticCompositionLocalOf { OceanPalette.DEEP_SEA.colors }
val LocalOceanThemeController = staticCompositionLocalOf { OceanThemeController() }

object OceanTheme {
  val colors: OceanColors
    @Composable
    @ReadOnlyComposable
    get() = LocalOceanColors.current

  val controller: OceanThemeController
    @Composable
    @ReadOnlyComposable
    get() = LocalOceanThemeController.current
}

@Composable
fun ThemeProvider(
  palette: OceanPalette = OceanPalette.DEEP_SEA,
  onSelectPalette: (OceanPalette) -> Unit = {},
  animateTransitions: Boolean = true,
  content: @Composable () -> Unit
) {
  val target = palette.colors
  val animatedColors = if (animateTransitions) {
    val spec = tween<Color>(durationMillis = 360)
    val abyss by animateColorAsState(target.abyss, spec, label = "abyss")
    val deepBlue by animateColorAsState(target.deepBlue, spec, label = "deepBlue")
    val surface by animateColorAsState(target.surface, spec, label = "surface")
    val card by animateColorAsState(target.card, spec, label = "card")
    val cardSecondary by animateColorAsState(target.cardSecondary, spec, label = "cardSecondary")
    val turquoise by animateColorAsState(target.turquoise, spec, label = "turquoise")
    val cyan by animateColorAsState(target.cyan, spec, label = "cyan")
    val teal by animateColorAsState(target.teal, spec, label = "teal")
    val waveFoam by animateColorAsState(target.waveFoam, spec, label = "waveFoam")
    val goldenSun by animateColorAsState(target.goldenSun, spec, label = "goldenSun")
    val sunsetOrange by animateColorAsState(target.sunsetOrange, spec, label = "sunsetOrange")
    val coralAlert by animateColorAsState(target.coralAlert, spec, label = "coralAlert")
    val seaGreen by animateColorAsState(target.seaGreen, spec, label = "seaGreen")
    val textPrimary by animateColorAsState(target.textPrimary, spec, label = "textPrimary")
    val textSecondary by animateColorAsState(target.textSecondary, spec, label = "textSecondary")
    val muted by animateColorAsState(target.muted, spec, label = "muted")
    OceanColors(
      abyss = abyss,
      deepBlue = deepBlue,
      surface = surface,
      card = card,
      cardSecondary = cardSecondary,
      turquoise = turquoise,
      cyan = cyan,
      teal = teal,
      waveFoam = waveFoam,
      goldenSun = goldenSun,
      sunsetOrange = sunsetOrange,
      coralAlert = coralAlert,
      seaGreen = seaGreen,
      textPrimary = textPrimary,
      textSecondary = textSecondary,
      muted = muted
    )
  } else {
    target
  }

  val controller = OceanThemeController(
    currentPalette = palette,
    selectPalette = onSelectPalette,
    cyclePalette = { onSelectPalette(palette.next()) }
  )

  CompositionLocalProvider(
    LocalOceanColors provides animatedColors,
    LocalOceanThemeController provides controller
  ) {
    MaterialTheme(
      colorScheme = animatedColors.toMaterialColorScheme(),
      typography = Typography,
      content = content
    )
  }
}
