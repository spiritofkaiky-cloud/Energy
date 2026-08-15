package com.energy.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Energy 2026 type scale (§5): typographic confidence over bold-everything.
 *
 *  - Display numerals: huge, Light weight, tight tracking, tabular figures
 *    ("tnum") so changing numbers never jitter — numbers ARE the identity.
 *  - Metadata labels: tiny, uppercase, wide tracking — the quiet counterpart
 *    to the display numerals.
 *  - Body weights stay Regular/Medium; SemiBold is reserved for real
 *    emphasis.
 */
private val TabularFigures = "'tnum'"

val EnergyTypography = Typography(
    // ── Display numerals (hero) ──────────────────────────────────────────
    displayLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Light,
        fontSize = 72.sp,
        letterSpacing = (-2).sp,
        fontFeatureSettings = "'tnum'"
    ),
    displayMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Light,
        fontSize = 52.sp,
        letterSpacing = (-1.5).sp,
        fontFeatureSettings = "'tnum'"
    ),
    displaySmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Light,
        fontSize = 38.sp,
        letterSpacing = (-0.5).sp,
        fontFeatureSettings = "'tnum'"
    ),
    // ── Headings ─────────────────────────────────────────────────────────
    headlineLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        letterSpacing = (-0.3).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp
    ),
    // ── Titles ───────────────────────────────────────────────────────────
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 17.sp,
        fontFeatureSettings = "'tnum'"
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        fontFeatureSettings = "'tnum'"
    ),
    titleSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        fontFeatureSettings = "'tnum'"
    ),
    // ── Body ─────────────────────────────────────────────────────────────
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 21.sp
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 18.sp
    ),
    // ── Labels ───────────────────────────────────────────────────────────
    labelLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        letterSpacing = 0.8.sp
    )
)

/** Uppercase metadata label — "ENERGY SCORE", "TODAY'S MOVEMENT", etc. */
val MetaLabel = TextStyle(
    fontFamily = FontFamily.Default,
    fontWeight = FontWeight.Medium,
    fontSize = 11.sp,
    letterSpacing = 1.6.sp,
    fontFeatureSettings = "'tnum'"
)
