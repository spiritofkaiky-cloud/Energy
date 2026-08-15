package com.energy.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.energy.app.ui.theme.DeepSpace
import com.energy.app.ui.theme.EnergyOrange

/**
 * Brand gradient backdrop: deep space fading to warm, with a soft
 * orange glow — used by splash + sign-in (APP_SPEC §5.1, §5.2).
 */
@Composable
fun GradientBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier.background(
            Brush.verticalGradient(
                colors = listOf(DeepSpace, Color(0xFF241013), Color(0xFF1A0F1E))
            )
        )
    ) {
        Box(
            modifier = Modifier
     .fillMaxSize()
     .background(
                    Brush.radialGradient(
                        colors = listOf(EnergyOrange.copy(alpha = 0.16f), Color.Transparent),
                        radius = 900f
                    )
                )
        )
        content()
    }
}
