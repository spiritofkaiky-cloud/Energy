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

val LocalDarkTheme = staticCompositionLocalOf { false }

// Oura-style: cards read as slightly-raised planes with hairline borders,
// not elevated shadow boxes.
private val DarkColors = darkColorScheme(
    primary = EnergyOrange,
    onPrimary = Color.White,
    primaryContainer = EnergySurfaceHigh,
    onPrimaryContainer = EnergyTextPrimary,
    secondary = EnergyMint,
    background = EnergyBackground,
    onBackground = EnergyTextPrimary,
    surface = EnergySurface,
    onSurface = EnergyTextPrimary,
    surfaceVariant = EnergySurfaceHigh,
    onSurfaceVariant = EnergyTextSecondary,
    outline = EnergyHairlineStrong
)

private val LightColors = lightColorScheme(
    primary = EnergyOrange,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFE8D8),
    onPrimaryContainer = Color(0xFF3A1B00),
    secondary = Color(0xFF1B8A64),
    background = Color(0xFFF7F6F4),
    onBackground = Color(0xFF19191D),
    surface = Color.White,
    onSurface = Color(0xFF19191D),
    surfaceVariant = Color(0xFFF0EFEC),
    onSurfaceVariant = Color(0x8A19191D),
    outline = Color(0x1A000000)
)

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
