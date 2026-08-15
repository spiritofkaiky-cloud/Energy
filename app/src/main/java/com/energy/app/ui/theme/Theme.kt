package com.energy.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.energy.app.data.settings.ThemeMode

private val LightColors = lightColorScheme(
    primary = EnergyOrange,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFE0C7),
    onPrimaryContainer = Color(0xFF4A2400),
    secondary = EnergyCoral,
    onSecondary = Color.White,
    tertiary = EnergyGreen,
    background = SoftWhite,
    onBackground = DeepSpace,
    surface = Color.White,
    onSurface = DeepSpace,
    surfaceVariant = Color(0xFFEFEFF5),
    onSurfaceVariant = Color(0xFF5A5A66),
    error = Color(0xFFD32F2F),
    onError = Color.White
)

private val DarkColors = darkColorScheme(
    primary = EnergyOrange,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF5C2E00),
    onPrimaryContainer = Color(0xFFFFDBC2),
    secondary = EnergyCoral,
    onSecondary = Color(0xFF33100E),
    tertiary = EnergyGreen,
    background = DeepSpace,
    onBackground = SoftWhite,
    surface = DeepSurface,
    onSurface = SoftWhite,
    surfaceVariant = Color(0xFF26262F),
    onSurfaceVariant = MistGray,
    error = Color(0xFFEF5350),
    onError = Color(0xFF3B0000)
)

/** Whether the resolved theme is dark — MapWidget etc. use this for map style. */
val LocalDarkTheme = staticCompositionLocalOf { false }

/**
 * Theme with user-pickable mode: System / Light / Dark (APP_SPEC §6 + v0.2).
 * Selection persists via SettingsRepository; Material You lands in M7.
 */
@Composable
fun EnergyTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    CompositionLocalProvider(LocalDarkTheme provides darkTheme) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColors else LightColors,
            typography = EnergyTypography,
            shapes = EnergyShapes,
            content = content
        )
    }
}
