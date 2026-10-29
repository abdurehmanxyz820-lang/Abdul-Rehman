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

private val DarkColorScheme =
  darkColorScheme(
    primary = MedBluePrimaryDark,
    primaryContainer = MedBluePrimaryContainerDark,
    secondary = MedTealSecondaryDark,
    secondaryContainer = MedTealSecondaryContainerDark,
    tertiary = MedAmberTertiary,
    background = MedBackgroundDark,
    surface = MedSurfaceDark,
    surfaceVariant = MedSurfaceVariantDark,
    onPrimary = MedBackgroundDark,
    onSecondary = MedBackgroundDark,
    onBackground = MedOnSurfaceDark,
    onSurface = MedOnSurfaceDark
  )

private val LightColorScheme =
  lightColorScheme(
    primary = MedBluePrimary,
    onPrimary = MedBlueOnPrimary,
    primaryContainer = MedBluePrimaryContainer,
    onPrimaryContainer = MedBlueOnPrimaryContainer,
    secondary = MedTealSecondary,
    onSecondary = MedTealOnSecondary,
    secondaryContainer = MedTealSecondaryContainer,
    onSecondaryContainer = MedTealOnSecondaryContainer,
    tertiary = MedAmberTertiary,
    onTertiary = MedAmberOnTertiary,
    tertiaryContainer = MedAmberTertiaryContainer,
    onTertiaryContainer = MedAmberOnTertiaryContainer,
    background = MedBackgroundLight,
    surface = MedSurfaceLight,
    surfaceVariant = MedSurfaceVariantLight,
    onBackground = MedOnSurfaceLight,
    onSurface = MedOnSurfaceLight,
    outline = MedOutlineLight
  )

@Composable
fun MediSyncTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false, // Healthcare apps prioritize recognizable brand clinical colors
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

// Retain alias for template compatibility
@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  MediSyncTheme(darkTheme = darkTheme, dynamicColor = dynamicColor, content = content)
}
