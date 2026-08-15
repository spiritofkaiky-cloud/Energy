package com.energy.app.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.energy.app.ui.theme.EnergyOrange
import com.energy.app.ui.theme.Motion
import androidx.compose.animation.core.animateFloatAsState

/**
 * Oura-style score gauge: soft glowing arc sweeping to [score] (0-100)
 * with a huge light-weight numeral in the center.
 */
@Composable
fun ScoreGauge(
    score: Int,
    modifier: Modifier = Modifier,
    label: String = "Energy Score",
    size: Int = 220
) {
    val glow = rememberInfiniteTransition(label = "gaugeGlow")
    val glowAlpha by glow.animateFloat(
        initialValue = 0.14f,
        targetValue = 0.30f,
        animationSpec = infiniteRepeatable(tween(2400), RepeatMode.Reverse),
        label = "glowPulse"
    )
    var animated by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(score) {
        animated = score / 100f
    }
    val sweep by animateFloatAsState(
        targetValue = animated,
        animationSpec = Motion.Lively,
        label = "gaugeSweep"
    )

    Box(modifier = modifier.size(size.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(size.dp)) {
            val stroke = 14.dp.toPx()
            val inset = stroke / 2 + 6.dp.toPx()
            val arcSize = Size(this.size.width - inset * 2, this.size.height - inset * 2)
            val topLeft = Offset(inset, inset)
            // Track
            drawArc(
                color = Color.White.copy(alpha = 0.08f),
                startAngle = 150f,
                sweepAngle = 240f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(stroke, cap = StrokeCap.Round)
            )
            // Glow
            drawArc(
                brush = Brush.sweepGradient(
                    colors = listOf(
                        EnergyOrange.copy(alpha = 0f),
                        EnergyOrange.copy(alpha = glowAlpha),
                        EnergyOrange.copy(alpha = glowAlpha * 1.6f),
                        EnergyOrange.copy(alpha = 0f)
                    )
                ),
                startAngle = 150f,
                sweepAngle = 240f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(stroke + 10.dp.toPx(), cap = StrokeCap.Round)
            )
            // Value arc
            drawArc(
                brush = Brush.linearGradient(
                    listOf(EnergyOrange, Color(0xFFFFB37A))
                ),
                startAngle = 150f,
                sweepAngle = 240f * sweep,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(stroke, cap = StrokeCap.Round)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            AnimatedNumber(
                value = score,
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.Light
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
