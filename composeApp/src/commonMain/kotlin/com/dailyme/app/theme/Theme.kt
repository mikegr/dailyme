package com.dailyme.app.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
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

/** "Cyanic Studio" design system: dark turquoise/cyan glassmorphism, per DESIGN.md. */
@Composable
fun DailyMeTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = CyanicColorScheme,
        typography = cyanicTypography(),
        shapes = CyanicShapes,
        content = content,
    )
}
