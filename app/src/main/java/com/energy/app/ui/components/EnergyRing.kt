package com.energy.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.energy.app.ui.theme.EnergyOrange
import com.energy.app.ui.theme.MetaLabel
import com.energy.app.ui.theme.Motion
import com.energy.app.ui.theme.RingMove
import com.energy.app.ui.theme.RingExercise
import com.energy.app.ui.theme.RingStand
import com.energy.app.ui.theme.Space

/**
 * Energy Ring — our interpretation of the activity-ring concept (§10).
 *
 * ONE integrated circular visualization instead of three Apple-style rings:
 * three arcs (Move / Exercise / Stand) share one track, and the center
 * carries the day's completion percentage. It reads as a single instrument,
 * not three widgets. Animated sweep on first composition; updates settle
 * with spring motion.
 */
@Composable
fun EnergyRing(
    move: Float,
    exercise: Float,
    stand: Float,
    modifier: Modifier = Modifier,
    sizeDp: Int = 168
) {
    // Animated values (spring) so progress changes settle, never snap.
    var moveAnim by remember { mutableFloatStateOf(0f) }
    var exAnim by remember { mutableFloatStateOf(0f) }
    var standAnim by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(move, exercise, stand) {
        moveAnim = move.coerceIn(0f, 1f)
        exAnim = exercise.coerceIn(0f, 1f)
        standAnim = stand.coerceIn(0f, 1f)
    }
    val aMove by animateFloatAsState(moveAnim, Motion.Soft, label = "ringMove")
    val aEx by animateFloatAsState(exAnim, Motion.Soft, label = "ringEx")
    val aStand by animateFloatAsState(standAnim, Motion.Soft, label = "ringStand")

    val pct = (((move + exercise + stand) / 3f).coerceIn(0f, 1f) * 100).toInt()

    // Theme values captured OUTSIDE the draw lambda (not composable there).
    val trackColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.07f)
    val numberColor = MaterialTheme.colorScheme.onBackground
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant

    Box(modifier = modifier.size(sizeDp.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(sizeDp.dp)) {
            val stroke = 11.dp.toPx()
            val inset = stroke / 2 + 2.dp.toPx()
            val arcSize = Size(this.size.width - inset * 2, this.size.height - inset * 2)
            val topLeft = Offset(inset, inset)
            val gapDeg = 6f
            val seg = 360f / 3f

            // Shared track
            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(stroke, cap = StrokeCap.Round)
            )

            data class Seg(val v: Float, val c: Color)
            listOf(Seg(aMove, RingMove), Seg(aEx, RingExercise), Seg(aStand, RingStand))
                .forEachIndexed { i, s ->
                    if (s.v > 0.01f) {
                        drawArc(
                            color = s.c,
                            startAngle = -90f + seg * i + gapDeg / 2f,
                            sweepAngle = (seg - gapDeg) * s.v,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(stroke, cap = StrokeCap.Round)
                        )
                    }
                }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "$pct%",
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Light,
                color = numberColor
            )
            Text(
                text = "TODAY",
                style = MetaLabel,
                color = labelColor
            )
        }
    }
}

/** Legend row beneath the ring: color dot + label + value. */
@Composable
fun EnergyRingLegend(
    entries: List<Triple<Color, String, String>>, // (color, label, detail)
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        entries.forEach { (color, label, detail) ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(horizontal = Space.XS)
            ) {
                Text(
                    text = label,
                    style = MetaLabel,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(7.dp)
                            .background(color, androidx.compose.foundation.shape.CircleShape)
                    )
                    Text(
                        text = "  $detail",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}
