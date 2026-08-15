package com.energy.app.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.energy.app.ui.theme.Motion

/**
 * Apple-Fitness-style activity ring with an optional Oura-style soft
 * breathing glow halo behind it.
 */
@Composable
fun ActivityRing(
    progress: Float,
    color: Color,
    modifier: Modifier = Modifier,
    sizeDp: Int = 120,
    glow: Boolean = false
) {
    var animated by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(progress) { animated = progress.coerceIn(0f, 1f) }
    val sweep by animateFloatAsState(
        targetValue = animated,
        animationSpec = Motion.Lively,
        label = "ringSweep"
    )

    val halo = rememberInfiniteTransition(label = "ringHalo")
    val haloPulse by halo.animateFloat(
        initialValue = 0.08f,
        targetValue = 0.20f,
        animationSpec = infiniteRepeatable(tween(2600), RepeatMode.Reverse),
        label = "haloPulse"
    )

    Canvas(modifier = modifier) {
        val stroke = 9.dp.toPx()
        val inset = stroke / 2 + (if (glow) 8.dp.toPx() else 0f)
        val arcSize = Size(this.size.width - inset * 2, this.size.height - inset * 2)
        val topLeft = Offset(inset, inset)

        // Breathing glow halo
        if (glow) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(color.copy(alpha = haloPulse * 1.4f), color.copy(alpha = 0f)),
                    center = Offset(this.size.width / 2, this.size.height / 2),
                    radius = this.size.width / 2
                ),
                radius = this.size.width / 2,
                center = Offset(this.size.width / 2, this.size.height / 2)
            )
        }

        // Track
        drawArc(
            color = Color.White.copy(alpha = 0.08f),
            startAngle = -90f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(stroke, cap = StrokeCap.Round)
        )
        // Progress
        if (sweep > 0.002f) {
            drawArc(
                brush = Brush.linearGradient(listOf(color, color.copy(alpha = 0.75f))),
                startAngle = -90f,
                sweepAngle = 360f * sweep,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(stroke, cap = StrokeCap.Round)
            )
        }
    }
}
