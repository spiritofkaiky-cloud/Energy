package com.energy.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Shape hierarchy (§33): XL for hero surfaces, MD for cards/controls,
 * SM for chips and inputs. Pills only where semantically appropriate.
 */
val EnergyShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(Radius.SM),
    medium = RoundedCornerShape(Radius.MD),
    large = RoundedCornerShape(Radius.LG),
    extraLarge = RoundedCornerShape(Radius.XL)
)
