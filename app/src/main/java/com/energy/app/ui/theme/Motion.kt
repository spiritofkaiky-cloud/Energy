package com.energy.app.ui.theme

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring

/**
 * Shared motion specs — APP_SPEC §6: 200–350 ms spring animations,
 * haptics and staggered entrances ride on top of these.
 */
object EnergyMotion {
    val Bouncy = spring<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMedium
    )
    val Snappy = spring<Float>(
        dampingRatio = Spring.DampingRatioLowBouncy,
        stiffness = Spring.StiffnessMediumLow
    )
    const val FAST_MS = 250
    const val MEDIUM_MS = 350
}
