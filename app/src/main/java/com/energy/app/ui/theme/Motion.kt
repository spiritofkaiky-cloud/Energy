package com.energy.app.ui.theme

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring

/**
 * Oura-smooth motion: lower-stiffness springs that settle without snap,
 * long soft breathing, quick light press feedback.
 */
object Motion {
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
