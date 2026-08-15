package com.energy.app.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.energy.app.ui.theme.LocalDarkTheme
import kotlin.math.cos
import kotlin.math.sin

/**
 * Living background: slow-drifting aurora blobs, Instagram-style — the
 * background breathes and recolors between dark and light themes.
 */
@Composable
fun AuroraBackground(modifier: Modifier = Modifier) {
    val dark = LocalDarkTheme.current
    val t = rememberInfiniteTransition(label = "aurora")
    val p1 by t.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(18_000), RepeatMode.Restart),
        label = "aurora1"
    )
    val p2 by t.animateFloat(
        initialValue = 120f, targetValue = 480f,
        animationSpec = infiniteRepeatable(tween(24_000), RepeatMode.Restart),
        label = "aurora2"
    )
    val p3 by t.animateFloat(
        initialValue = 240f, targetValue = 600f,
        animationSpec = infiniteRepeatable(tween(30_000), RepeatMode.Restart),
        label = "aurora3"
    )

    val accent = if (dark) Color(0xFFFF7A1A) else Color(0xFFFF9A5A)
    val accent2 = if (dark) Color(0xFFFF5F6D) else Color(0xFFFF8B96)
    val accent3 = if (dark) Color(0xFF4CD9A5) else Color(0xFF7FE3C2)
    val strength = if (dark) 0.16f else 0.10f

    Canvas(modifier = modifier.fillMaxSize()) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val maxR = size.width.coerceAtLeast(size.height) * 0.9f
        fun blob(phase: Float, color: Color, radius: Float, eccentricity: Float) {
            val ang = Math.toRadians(phase.toDouble())
            val center = Offset(
                cx + (cos(ang) * size.width * 0.28f).toFloat(),
                cy + (sin(ang) * size.height * 0.22f * eccentricity).toFloat()
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(color.copy(alpha = strength), color.copy(alpha = 0f)),
                    center = center,
                    radius = radius
                ),
                radius = radius,
                center = center
            )
        }
        blob(p1, accent, maxR * 0.75f, 1.15f)
        blob(p2, accent2, maxR * 0.65f, 0.9f)
        blob(p3, accent3, maxR * 0.55f, 1.3f)
    }
}
