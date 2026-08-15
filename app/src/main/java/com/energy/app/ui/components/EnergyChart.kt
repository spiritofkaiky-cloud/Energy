package com.energy.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.energy.app.ui.theme.EnergyOrange
import com.energy.app.ui.theme.Motion

/**
 * Energy line chart (§24): smooth curve (catmull-rom), soft gradient fill,
 * minimal grid lines, animated entry, highlighted last point. No axes
 * clutter — the number in the header carries the meaning.
 */
@Composable
fun EnergyLineChart(
    values: List<Float>,
    modifier: Modifier = Modifier,
    lineColor: androidx.compose.ui.graphics.Color = EnergyOrange,
    heightDp: Int = 150
) {
    var progress by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(values) { progress = 1f }
    val sweep by animateFloatAsState(progress, Motion.Soft, label = "chartSweep")

    // Theme values captured OUTSIDE the draw lambda.
    val gridColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.05f)
    val dotCenterColor = MaterialTheme.colorScheme.surface

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(heightDp.dp)
    ) {
        if (values.isEmpty()) return@Canvas
        val minV = values.minOrNull()?.coerceAtMost(0f) ?: 0f
        val maxV = values.maxOrNull()?.coerceAtLeast(1f) ?: 1f
        val range = (maxV - minV).coerceAtLeast(1f)
        val padTop = 12.dp.toPx()
        val padBottom = 8.dp.toPx()
        val stepX = if (values.size > 1) size.width / (values.size - 1) else 0f

        // Grid lines (3 subtle horizontals)
        for (g in 1..3) {
            val y = padTop + (size.height - padTop - padBottom) * g / 3f
            drawLine(
                color = gridColor,
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 1.dp.toPx()
            )
        }

        val pts = values.mapIndexed { i, v ->
            Offset(
                stepX * i,
                padTop + (1 - (v - minV) / range) * (size.height - padTop - padBottom)
            )
        }

        // Smooth path (catmull-rom → bezier)
        val path = Path()
        val n = pts.size
        path.moveTo(pts[0].x, pts[0].y)
        for (i in 0 until n - 1) {
            val p0 = pts[(i - 1).coerceAtLeast(0)]
            val p1 = pts[i]
            val p2 = pts[i + 1]
            val p3 = pts[(i + 2).coerceAtMost(n - 1)]
            val c1x = p1.x + (p2.x - p0.x) / 6f
            val c1y = p1.y + (p2.y - p0.y) / 6f
            val c2x = p2.x - (p3.x - p1.x) / 6f
            val c2y = p2.y - (p3.y - p1.y) / 6f
            path.cubicTo(c1x, c1y, c2x, c2y, p2.x, p2.y)
        }

        // Animated entry: clip to sweep fraction along x.
        val clipPath = Path().apply {
            moveTo(pts[0].x, pts[0].y)
            for (i in 0 until n - 1) {
                val p1 = pts[i]
                val p2 = pts[i + 1]
                if (p2.x > size.width * sweep) break
                lineTo(p2.x, p2.y)
            }
        }

        // Gradient fill under the curve.
        val fillPath = Path().apply {
            addPath(path)
            lineTo(size.width * sweep, size.height)
            lineTo(0f, size.height)
            close()
        }
        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(lineColor.copy(alpha = 0.22f), lineColor.copy(alpha = 0f)),
                startY = padTop,
                endY = size.height
            )
        )

        // Stroke (full path, but only when sweep complete it stays subtle).
        drawPath(
            path = clipPath,
            color = lineColor,
            style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
        )

        // Highlight last point.
        drawCircle(
            color = lineColor,
            radius = 4.dp.toPx(),
            center = pts.last()
        )
        drawCircle(
            color = dotCenterColor,
            radius = 1.8.dp.toPx(),
            center = pts.last()
        )
    }
}

/**
 * Energy bar chart: rounded bars, quiet grid, animated growth.
 */
@Composable
fun EnergyBarChart(
    values: List<Float>,
    modifier: Modifier = Modifier,
    barColor: androidx.compose.ui.graphics.Color = EnergyOrange,
    heightDp: Int = 140
) {
    var progress by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(values) { progress = 1f }
    val sweep by animateFloatAsState(progress, Motion.Soft, label = "barSweep")

    // Theme value captured outside the draw lambda.
    val emptyBarColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f)

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(heightDp.dp)
    ) {
        if (values.isEmpty()) return@Canvas
        val maxV = values.maxOrNull()?.coerceAtLeast(0.01f) ?: 1f
        val gap = size.width / values.size
        val barW = gap * 0.58f
        val baseline = size.height - 6.dp.toPx()
        values.forEachIndexed { i, v ->
            val h = (v / maxV) * (size.height - 16.dp.toPx()) * sweep
            val x = gap * i + (gap - barW) / 2
            drawRoundRect(
                color = if (v > 0) barColor.copy(alpha = 0.9f) else emptyBarColor,
                topLeft = Offset(x, baseline - h.coerceAtLeast(if (v > 0) 3.dp.toPx() else 2.dp.toPx())),
                size = Size(barW, h.coerceAtLeast(if (v > 0) 3.dp.toPx() else 2.dp.toPx())),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(3.dp.toPx())
            )
        }
    }
}
