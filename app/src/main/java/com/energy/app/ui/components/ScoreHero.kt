package com.energy.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.energy.app.data.stats.EnergyScore
import com.energy.app.data.stats.ScoreFactor
import com.energy.app.ui.theme.EnergyMint
import com.energy.app.ui.theme.LocalAccent
import com.energy.app.ui.theme.EnergyOrange
import com.energy.app.ui.theme.EnergyOrangeSoft
import com.energy.app.ui.theme.MetaLabel
import com.energy.app.ui.theme.Motion
import com.energy.app.ui.theme.Space

/**
 * The Energy Score hero (§8) — the product's visual signature.
 *
 * Large light numeral over a quiet arc track with a soft gradient glow;
 * category line under the number; trend chip; tap to unfold the explainable
 * factor breakdown. Calm, not flashy — Oura-level restraint.
 */
@Composable
fun ScoreHero(
    score: EnergyScore,
    modifier: Modifier = Modifier,
    sizeDp: Int = 232
) {
    var expanded by remember { mutableStateOf(false) }
    var target by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(score.value) { target = score.value / 100f }
    val sweep by animateFloatAsState(target, Motion.Lively, label = "scoreSweep")

    // Theme values captured OUTSIDE the draw lambda.
    val trackColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.07f)
    val accent = LocalAccent.current

    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { expanded = !expanded }
                )
                .padding(horizontal = Space.XXL),
            contentAlignment = Alignment.Center
        ) {
            Canvas(Modifier.height(sizeDp.dp).width(sizeDp.dp)) {
                val stroke = 10.dp.toPx()
                val inset = stroke / 2 + 8.dp.toPx()
                val arcSize = Size(this.size.width - inset * 2, this.size.height - inset * 2)
                val topLeft = Offset(inset, inset)

                // Track
                drawArc(
                    color = trackColor,
                    startAngle = 140f,
                    sweepAngle = 260f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(stroke, cap = StrokeCap.Round)
                )
                // Value arc with subtle gradient
                if (sweep > 0.005f) {
                    drawArc(
                        brush = Brush.sweepGradient(
                            listOf(accent, accent.copy(alpha = 0.75f), accent),
                            center = Offset(this.size.width / 2, this.size.height / 2)
                        ),
                        startAngle = 140f,
                        sweepAngle = 260f * sweep,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(stroke, cap = StrokeCap.Round)
                    )
                    // Soft glow companion
                    drawArc(
                        color = accent.copy(alpha = 0.16f),
                        startAngle = 140f,
                        sweepAngle = 260f * sweep,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(stroke + 14.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                AnimatedNumber(
                    value = score.value,
                    style = MaterialTheme.typography.displayLarge,
                    fontWeight = FontWeight.Light
                )
                Text(
                    text = "ENERGY SCORE",
                    style = MetaLabel,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(Modifier.height(Space.SM))

        // Category + trend — the "feeling" line.
        Text(
            text = feelingLine(score),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(Space.XS))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = score.category,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = accent
            )
            score.trendVs7Day?.let { trend ->
                Spacer(Modifier.width(Space.SM))
                Text(
                    text = if (trend >= 0) "↑ $trend" else "↓ ${-trend}",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (trend >= 0) EnergyMint
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = " from yesterday",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Explainable factors (unfold on tap).
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Space.XXL, vertical = Space.MD)
            ) {
                score.factors.forEach { f ->
                    FactorRow(f)
                    Spacer(Modifier.height(Space.XS))
                }
                Text(
                    text = "Estimated from your activity — not a medical measurement.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.alpha(0.8f)
                )
            }
        }
    }
}

@Composable
private fun FactorRow(f: ScoreFactor) {
    val accent = LocalAccent.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(f.label, style = MaterialTheme.typography.bodyMedium)
            Text(
                f.detail,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        // Tiny proportional bar instead of bare points text.
        val frac = (f.points.toFloat() / f.maxPoints).coerceIn(-1f, 1f)
        val barTrack = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f)
        Canvas(Modifier.width(72.dp).height(4.dp)) {
            drawRoundRect(
                color = barTrack,
                size = Size(this.size.width, this.size.height),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx())
            )
            if (frac != 0f) {
                drawRoundRect(
                    color = if (f.points >= 0) accent else EnergyMint,
                    size = Size(this.size.width * kotlin.math.abs(frac), this.size.height),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx())
                )
            }
        }
        Spacer(Modifier.width(Space.SM))
        Text(
            text = if (f.points >= 0) "+${f.points}" else "${f.points}",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

private fun feelingLine(score: EnergyScore): String = when {
    score.value >= 85 -> "Feeling strong today"
    score.value >= 70 -> "A solid day in motion"
    score.value >= 60 -> "Warming up — keep it steady"
    score.value >= 30 -> "A gentle day. Move a little if you can"
    else -> "Take it easy. Recovery counts too"
}
