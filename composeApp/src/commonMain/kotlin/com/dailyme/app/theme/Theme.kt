package com.dailyme.app.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val CyanicColorScheme = darkColorScheme(
    primary = CyanicColors.Primary,
    onPrimary = CyanicColors.OnPrimary,
    primaryContainer = CyanicColors.PrimaryContainer,
    onPrimaryContainer = CyanicColors.OnPrimaryContainer,
    inversePrimary = CyanicColors.InversePrimary,
    secondary = CyanicColors.Secondary,
    onSecondary = CyanicColors.OnSecondary,
    secondaryContainer = CyanicColors.SecondaryContainer,
    onSecondaryContainer = CyanicColors.OnSecondaryContainer,
    tertiary = CyanicColors.Tertiary,
    onTertiary = CyanicColors.OnTertiary,
    tertiaryContainer = CyanicColors.TertiaryContainer,
    onTertiaryContainer = CyanicColors.OnTertiaryContainer,
    background = CyanicColors.Background,
    onBackground = CyanicColors.OnBackground,
    surface = CyanicColors.Surface,
    onSurface = CyanicColors.OnSurface,
    surfaceVariant = CyanicColors.SurfaceVariant,
    onSurfaceVariant = CyanicColors.OnSurfaceVariant,
    surfaceTint = CyanicColors.SurfaceTint,
    inverseSurface = CyanicColors.InverseSurface,
    inverseOnSurface = CyanicColors.InverseOnSurface,
    error = CyanicColors.Error,
    onError = CyanicColors.OnError,
    errorContainer = CyanicColors.ErrorContainer,
    onErrorContainer = CyanicColors.OnErrorContainer,
    outline = CyanicColors.Outline,
    outlineVariant = CyanicColors.OutlineVariant,
    surfaceDim = CyanicColors.SurfaceDim,
    surfaceBright = CyanicColors.SurfaceBright,
    surfaceContainerLowest = CyanicColors.SurfaceContainerLowest,
    surfaceContainerLow = CyanicColors.SurfaceContainerLow,
    surfaceContainer = CyanicColors.SurfaceContainer,
    surfaceContainerHigh = CyanicColors.SurfaceContainerHigh,
    surfaceContainerHighest = CyanicColors.SurfaceContainerHighest,
)

private val CyanicLightColorScheme = lightColorScheme(
    primary = CyanicLightColors.Primary,
    onPrimary = CyanicLightColors.OnPrimary,
    primaryContainer = CyanicLightColors.PrimaryContainer,
    onPrimaryContainer = CyanicLightColors.OnPrimaryContainer,
    inversePrimary = CyanicLightColors.InversePrimary,
    secondary = CyanicLightColors.Secondary,
    onSecondary = CyanicLightColors.OnSecondary,
    secondaryContainer = CyanicLightColors.SecondaryContainer,
    onSecondaryContainer = CyanicLightColors.OnSecondaryContainer,
    tertiary = CyanicLightColors.Tertiary,
    onTertiary = CyanicLightColors.OnTertiary,
    tertiaryContainer = CyanicLightColors.TertiaryContainer,
    onTertiaryContainer = CyanicLightColors.OnTertiaryContainer,
    background = CyanicLightColors.Background,
    onBackground = CyanicLightColors.OnBackground,
    surface = CyanicLightColors.Surface,
    onSurface = CyanicLightColors.OnSurface,
    surfaceVariant = CyanicLightColors.SurfaceVariant,
    onSurfaceVariant = CyanicLightColors.OnSurfaceVariant,
    surfaceTint = CyanicLightColors.SurfaceTint,
    inverseSurface = CyanicLightColors.InverseSurface,
    inverseOnSurface = CyanicLightColors.InverseOnSurface,
    error = CyanicLightColors.Error,
    onError = CyanicLightColors.OnError,
    errorContainer = CyanicLightColors.ErrorContainer,
    onErrorContainer = CyanicLightColors.OnErrorContainer,
    outline = CyanicLightColors.Outline,
    outlineVariant = CyanicLightColors.OutlineVariant,
    surfaceDim = CyanicLightColors.SurfaceDim,
    surfaceBright = CyanicLightColors.SurfaceBright,
    surfaceContainerLowest = CyanicLightColors.SurfaceContainerLowest,
    surfaceContainerLow = CyanicLightColors.SurfaceContainerLow,
    surfaceContainer = CyanicLightColors.SurfaceContainer,
    surfaceContainerHigh = CyanicLightColors.SurfaceContainerHigh,
    surfaceContainerHighest = CyanicLightColors.SurfaceContainerHighest,
)

/**
 * "Cyanic Studio" design system: dark turquoise/cyan glassmorphism, per DESIGN.md.
 *
 * @param darkTheme whether to use the dark palette; defaults to the system setting.
 */
@Composable
fun DailyMeTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) CyanicColorScheme else CyanicLightColorScheme,
        typography = cyanicTypography(),
        shapes = CyanicShapes,
        content = content,
    )
}

/**
 * Colors for the top toolbar's "floating glass bar" surface: a tonal layer distinct
 * from the page background, with the title in [CyanicColors.Primary] and icons in
 * [CyanicColors.OnSurface].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun cyanicTopAppBarColors(): TopAppBarColors = TopAppBarDefaults.topAppBarColors(
    containerColor = MaterialTheme.colorScheme.surfaceContainer,
    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
    titleContentColor = MaterialTheme.colorScheme.primary,
    navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
    actionIconContentColor = MaterialTheme.colorScheme.onSurface,
)
