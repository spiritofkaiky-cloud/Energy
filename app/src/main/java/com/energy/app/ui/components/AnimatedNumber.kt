package com.energy.app.ui.components

import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.spring
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import com.energy.app.ui.theme.Motion

/**
 * Smooth counting numeral — Oura's signature number transitions.
 */
@Composable
fun AnimatedNumber(
    value: Int,
    style: TextStyle,
    fontWeight: FontWeight = FontWeight.Normal
) {
    val animated by animateIntAsState(
        targetValue = value,
        animationSpec = spring(dampingRatio = 0.92f, stiffness = 140f),
        label = "animatedNumber"
    )
    Text(
        text = animated.toString(),
        style = style,
        fontWeight = fontWeight
    )
}
