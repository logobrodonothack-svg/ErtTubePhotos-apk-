package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme =
  darkColorScheme(
    primary = ErtViolet,
    onPrimary = Color(0xFF1A0938),
    primaryContainer = ErtVioletContainerDark,
    onPrimaryContainer = ErtVioletBright,
    secondary = ErtMint,
    onSecondary = Color(0xFF03291E),
    secondaryContainer = Color(0xFF17362D),
    onSecondaryContainer = Color(0xFFB4F7E0),
    tertiary = ErtCoral,
    onTertiary = Color(0xFF3D061A),
    tertiaryContainer = ErtCoralContainerDark,
    onTertiaryContainer = Color(0xFFFFD9E2),
    background = ErtObsidian,
    onBackground = ErtTextPrimaryDark,
    surface = ErtSurfaceDark,
    onSurface = ErtTextPrimaryDark,
    surfaceVariant = ErtSurfaceCardDark,
    onSurfaceVariant = ErtTextSecondaryDark,
    surfaceContainerHigh = ErtSurfaceElevatedDark,
    outline = ErtOutlineDark,
    outlineVariant = Color(0xFF2B2242),
  )

private val LightColorScheme =
  lightColorScheme(
    primary = ErtVioletDeep,
    onPrimary = Color.White,
    primaryContainer = ErtVioletContainerLight,
    onPrimaryContainer = Color(0xFF280B5C),
    secondary = Color(0xFF0E7C5D),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD5F9EC),
    onSecondaryContainer = Color(0xFF043023),
    tertiary = Color(0xFFD6336C),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFDCE5),
    onTertiaryContainer = Color(0xFF45071E),
    background = ErtIvory,
    onBackground = ErtTextPrimaryLight,
    surface = ErtSurfaceLight,
    onSurface = ErtTextPrimaryLight,
    surfaceVariant = ErtSurfaceCardLight,
    onSurfaceVariant = ErtTextSecondaryLight,
    surfaceContainerHigh = ErtSurfaceElevatedLight,
    outline = ErtOutlineLight,
    outlineVariant = Color(0xFFE7DEFC),
  )

@Composable
fun ErtTubePhotosTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
