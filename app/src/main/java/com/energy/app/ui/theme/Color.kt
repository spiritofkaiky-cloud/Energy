package com.energy.app.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Energy 2026 semantic color system (§6, §42).
 *
 * Dark is the primary experience: deep blue-black canvas with layered
 * tonal surfaces. Energy Orange is an ACCENT, not a default surface color —
 * large dark areas with occasional high-impact accent moments.
 *
 * Every color used by components must be referenced from here or from the
 * Material scheme in Theme.kt. No raw hex in screens.
 */

// ── Dark surfaces (layered, never pure black) ────────────────────────────
val EnergyBackground = Color(0xFF0A0A0F)      // canvas
val EnergySurface = Color(0xFF12121A)         // default surface
val EnergySurfaceHigh = Color(0xFF1A1A24)     // elevated / interactive
val EnergySurfaceMax = Color(0xFF23232E)      // hover/pressed, chips

// ── Dark text — ALL WHITE (user requirement) ─────────────────────────────
// Every text level is pure #FFFFFF in dark mode: primary, secondary and
// tertiary alike. 19:1 contrast on the dark canvas.
val EnergyTextPrimary = Color(0xFFFFFFFF)
val EnergyTextSecondary = Color(0xFFFFFFFF)
val EnergyTextTertiary = Color(0xFFFFFFFF)

// ── Hairlines / dividers ─────────────────────────────────────────────────
val EnergyHairline = Color(0x12FFFFFF)        // subtle
val EnergyHairlineStrong = Color(0x24FFFFFF)  // emphasized

// ── Accents — used sparingly, one at a time ──────────────────────────────
val EnergyOrange = Color(0xFFFF7A1A)
val EnergyOrangeSoft = Color(0xFFFF9A4D)      // gradient partner
val EnergyCoral = Color(0xFFFF5F6D)
val EnergyMint = Color(0xFF4CD9A5)            // success / recovery
val EnergySky = Color(0xFF54B8FF)             // information / stand
val EnergyGold = Color(0xFFF2C94C)            // PRs / achievements
val EnergyWarning = Color(0xFFE8A23D)

// ── Light theme surfaces (warm paper, not white-everywhere) ──────────────
val LightBackground = Color(0xFFF7F5F2)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceHigh = Color(0xFFF0EEE9)
val LightTextPrimary = Color(0xFF17171C)
val LightTextSecondary = Color(0x8A17171C)
val LightTextTertiary = Color(0x5717171C)
val LightHairline = Color(0x12000000)
val LightHairlineStrong = Color(0x1F000000)

// ── Workout-type palette ─────────────────────────────────────────────────
val RunColor = EnergyOrange
val WalkColor = EnergyMint
val CycleColor = EnergySky
val HikeColor = Color(0xFFB98CF5)

// ── Ring palette (activity ring arcs, softened) ──────────────────────────
val RingMove = Color(0xFFFF5F6D)
val RingExercise = Color(0xFF4CD9A5)
val RingStand = Color(0xFF54B8FF)

// ── Chart palette (ordered) ──────────────────────────────────────────────
val ChartColors = listOf(EnergyOrange, EnergyMint, EnergySky, EnergyCoral, EnergyGold)
