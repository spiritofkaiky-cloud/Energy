package com.energy.app.ui.theme

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring

/**
 * Energy motion language (§30): alive, not busy.
 *
 *  Durations: Instant (80ms — button feedback) → Fast (160 — micro
 *  interactions) → Medium (280 — navigation) → Slow (480 — hero
 *  transitions). Springs carry the physicality.
 */
object Motion {
    /** Durations (ms). */
    const val Instant = 80
    const val Fast = 160
    const val Medium = 280
    const val Slow = 480

    /** Gentle settle — screens and cards glide in. */
    val Soft = spring<Float>(
        dampingRatio = 0.92f,
        stiffness = 140f
    )

    /** Bouncier but never jarring — ring fills, gauge sweeps. */
    val Lively = spring<Float>(
        dampingRatio = 0.78f,
        stiffness = 220f
    )

    /** Press feedback: quick, subtle spring. */
    val Press: SpringSpec<Float> = spring(
        dampingRatio = 0.72f,
        stiffness = 600f
    )
}
