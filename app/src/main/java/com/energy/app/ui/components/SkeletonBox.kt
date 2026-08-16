package com.energy.app.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Pulsing skeleton placeholder — the "loading page" look of Energy.
 * APP_SPEC §5.1: shimmer placeholders instead of spinners.
 */
@Composable
fun SkeletonBox(
    modifier: Modifier = Modifier,
    height: Dp = 48.dp,
    corner: Dp = 16.dp
) {
    val transition = rememberInfiniteTransition(label = "skeleton")
    val alpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "skeletonAlpha"
    )
    Box(
        modifier = modifier
            .height(height)
            .clip(RoundedCornerShape(corner))
            // Theme-aware shimmer (§12): onSurface adapts — white-on-white in
            // light mode would make the skeleton invisible.
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = alpha * 0.10f))
    )
}
