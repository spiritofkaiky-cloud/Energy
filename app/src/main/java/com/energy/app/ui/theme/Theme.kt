package com.energy.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import com.energy.app.data.settings.Accent
import com.energy.app.data.settings.ThemeMode

val LocalDarkTheme = staticCompositionLocalOf { false }

/**
 * Energy 2026 themes (§41, §42).
 *
 * Dark: layered blue-black surfaces, restrained orange accent.
 * Light: warm paper surfaces with its own contrast logic — never just an
 * inverted dark theme.
 */
private fun darkScheme(accent: Color) = darkColorScheme(
    primary = accent,
    onPrimary = Color.White,
    primaryContainer = EnergySurfaceHigh,
    onPrimaryContainer = EnergyTextPrimary,
    secondary = EnergyMint,
    onSecondary = EnergyBackground,
    secondaryContainer = Color(0xFF10312A),
    onSecondaryContainer = EnergyMint,
    tertiary = EnergySky,
    background = EnergyBackground,
    onBackground = EnergyTextPrimary,
    surface = EnergySurface,
    onSurface = EnergyTextPrimary,
    surfaceVariant = EnergySurfaceHigh,
    onSurfaceVariant = EnergyTextSecondary,
    surfaceContainer = EnergySurface,
    surfaceContainerHigh = EnergySurfaceHigh,
    surfaceContainerHighest = EnergySurfaceMax,
    outline = EnergyHairlineStrong,
    outlineVariant = EnergyHairline,
    error = EnergyCoral,
    onError = Color.White,
    errorContainer = Color(0xFF3A1A20),
    onErrorContainer = Color(0xFFFFC9CE)
)

private fun lightScheme(accent: Color) = lightColorScheme(
    primary = when (accent) {
        EnergyOrange -> Color(0xFFE8680A)   // deepened for light-theme contrast
        else -> Color(0xFFC93B4F)
    },
    onPrimary = Color.White,
    primaryContainer = when (accent) {
        EnergyOrange -> Color(0xFFFFE3CC)
        else -> Color(0xFFFBDDE2)
    },
    onPrimaryContainer = Color(0xFF3A1B00),
    secondary = Color(0xFF0E7C57),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD6F3E6),
    onSecondaryContainer = Color(0xFF0B3B2B),
    tertiary = Color(0xFF1F6FBF),
    background = LightBackground,
    onBackground = LightTextPrimary,
    surface = LightSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = LightSurfaceHigh,
    onSurfaceVariant = LightTextSecondary,
    surfaceContainer = LightSurface,
    surfaceContainerHigh = LightSurfaceHigh,
    surfaceContainerHighest = Color(0xFFE9E6E0),
    outline = LightHairlineStrong,
    outlineVariant = LightHairline,
    error = Color(0xFFC43B4E),
    onError = Color.White,
    errorContainer = Color(0xFFFADFE3),
    onErrorContainer = Color(0xFF57121E)
)

/** The accent actually in effect (Energy Orange or Energy Coral). */
val LocalAccent = staticCompositionLocalOf { EnergyOrange }

@Composable
fun EnergyTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    accent: Accent = Accent.ORANGE,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val accentColor = when (accent) {
        Accent.ORANGE -> EnergyOrange
        Accent.CORAL -> EnergyCoral
    }

    // Keep status-bar icon contrast in sync with the theme.
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? android.app.Activity)?.window ?: return@SideEffect
            androidx.core.view.WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    CompositionLocalProvider(
        LocalDarkTheme provides darkTheme,
        LocalAccent provides accentColor
    ) {
        MaterialTheme(
            colorScheme = if (darkTheme) darkScheme(accentColor) else lightScheme(accentColor),
            typography = EnergyTypography,
            shapes = EnergyShapes,
            content = content
        )
    }
}
