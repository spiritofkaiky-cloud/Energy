package com.energy.app.ui.theme

import androidx.compose.ui.unit.dp

/**
 * Centralized spacing + shape tokens (§43). Screens use these instead of
 * magic numbers so rhythm stays consistent across the whole app.
 */
object Space {
    val XXS = 4.dp
    val XS = 8.dp
    val SM = 12.dp
    val MD = 16.dp
    val LG = 20.dp
    val XL = 24.dp
    val XXL = 32.dp
    val XXXL = 40.dp
    val HERO = 48.dp
}

/** Corner-radius hierarchy (§33): large surfaces, medium controls, small controls. */
object Radius {
    val XL = 28.dp      // hero surfaces (score, map container)
    val LG = 24.dp      // large sections
    val MD = 18.dp      // medium cards/controls
    val SM = 12.dp      // small controls, chips, inputs
}

/** Icon-size hierarchy for a coherent icon language (§36). */
object IconSize {
    val XS = 14.dp
    val SM = 18.dp
    val MD = 22.dp
    val LG = 28.dp
    val XL = 34.dp
}
