package com.energy.app.ui.screens.progress

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.energy.app.data.stats.Achievements
import com.energy.app.data.workout.WorkoutMath
import com.energy.app.ui.components.HairlineCard
import com.energy.app.ui.theme.EnergyHairline
import com.energy.app.ui.theme.EnergyMint
import com.energy.app.ui.theme.EnergyOrange

/**
 * Progress — "Am I improving?" (APP_SPEC §7). Every chart answers a
 * question: training volume per day, score trend, consistency.
 */
@Composable
fun ProgressScreen(viewModel: ProgressViewModel = viewModel()) {
    val data by viewModel.data.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
    ) {
        Spacer(Modifier.height(12.dp))
        Text(
            text = "Progress",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "The last 14 days, from your saved workouts.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(20.dp))

        // ── 14-day summary ────────────────────────────────────────────────
        HairlineCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                SummaryStat(
                    value = data.workouts14.toString(),
                    label = "workouts",
                    sub = "14 days"
                )
                SummaryStat(
                    value = String.format("%.1f", data.km14),
                    label = "km",
                    sub = "14 days"
                )
                SummaryStat(
                    value = "${data.activeDays14}",
                    label = "active days",
                    sub = "of 14"
                )
            }
            val avgPace = data.avgPace14
            if (avgPace != null) {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "Avg run pace (14d): ${WorkoutMath.formatPace(avgPace * 60)}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // ── Daily distance bars ───────────────────────────────────────────
        HairlineCard {
            Text(
                text = "Distance per day",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Am I training more?",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(16.dp))
            BarChart(
                values = data.days.map { it.km.toFloat() },
                labels = data.days.map { it.label },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
            )
            if (data.days.none { it.km > 0 }) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "No workouts in the last 14 days — your first workout starts here.",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // ── Energy Score trend ────────────────────────────────────────────
        if (data.scoreTrend.size >= 2) {
            HairlineCard {
                Text(
                    text = "Energy Score trend",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Daily estimate from your activity.",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(16.dp))
                LineChart(
                    values = data.scoreTrend.map { it.second.toFloat() },
                    labels = data.scoreTrend.map { it.first },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                )
            }
            Spacer(Modifier.height(16.dp))
        }

        // ── Consistency ───────────────────────────────────────────────────
        HairlineCard {
            Text(
                text = "Consistency",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                val pct = (data.activeDays14 * 100 / 14).coerceIn(0, 100)
                Text(
                    text = "$pct%",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Light,
                    color = EnergyOrange
                )
                Spacer(Modifier.width(14.dp))
                Text(
                    text = "of the last 14 days had activity. " +
                        if (pct >= 70) "Strong consistency — keep it up."
                        else if (pct >= 40) "Decent rhythm. Aim for 4+ active days a week."
                        else "A little movement most days beats one big day.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (data.bestMonthLabel != null) {
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "🏆 Best month: ${data.bestMonthLabel} · %.1f km".format(data.bestMonthKm),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // ── Personal records ──────────────────────────────────────────────
        HairlineCard {
            Text(
                text = "Personal records",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(10.dp))
            if (data.records.isEmpty()) {
                Text(
                    text = "Records unlock as you build history — fastest 1 km, 5 km, longest workout and more.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                data.records.forEach { r ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(r.label, style = MaterialTheme.typography.bodyMedium)
                        Text(
                            r.valueText,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = EnergyOrange
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // ── Lifetime ──────────────────────────────────────────────────────
        HairlineCard {
            Text(
                text = "All time",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                SummaryStat("${data.lifetime.workoutCount}", "workouts")
                SummaryStat(String.format("%.1f", data.lifetime.totalKm), "km")
                SummaryStat("${data.lifetime.totalMinutes / 60}h", "moving")
            }
            if (data.lifetime.bestPaceSecondsPerKm != null) {
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "Best run pace: ${WorkoutMath.formatPace(data.lifetime.bestPaceSecondsPerKm)}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // ── Streak badges ─────────────────────────────────────────────────
        HairlineCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Achievements.forEach { a ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = a.emoji, style = MaterialTheme.typography.headlineMedium)
                        Text(
                            text = a.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun SummaryStat(value: String, label: String, sub: String? = null) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        sub?.let {
            Text(
                it,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** Animated grouped bar chart — pure Canvas, no chart library. */
@Composable
private fun BarChart(
    values: List<Float>,
    labels: List<String>,
    modifier: Modifier = Modifier
) {
    val maxV = values.maxOrNull()?.coerceAtLeast(0.01f) ?: 1f
    val progress by animateFloatAsState(targetValue = 1f, label = "barProgress")
    Canvas(modifier = modifier) {
        if (values.isEmpty()) return@Canvas
        val barWidth = size.width / values.size * 0.62f
        val gap = size.width / values.size
        values.forEachIndexed { i, v ->
            val h = (v / maxV * (size.height - 22.dp.toPx())).coerceAtLeast(if (v > 0) 4.dp.toPx() else 2.dp.toPx())
            val x = gap * i + (gap - barWidth) / 2
            val y = size.height - 14.dp.toPx() - h
            drawRoundRect(
                color = if (v > 0) EnergyOrange.copy(alpha = 0.9f) else Color.White.copy(alpha = 0.10f),
                topLeft = Offset(x, y),
                size = Size(barWidth, h * progress),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx())
            )
        }
    }
}

/** Score trend line with dots — pure Canvas. */
@Composable
private fun LineChart(
    values: List<Float>,
    labels: List<String>,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        if (values.size < 2) return@Canvas
        val minV = values.minOrNull()?.coerceAtMost(0f) ?: 0f
        val maxV = values.maxOrNull()?.coerceAtLeast(1f) ?: 1f
        val range = (maxV - minV).coerceAtLeast(1f)
        val stepX = size.width / (values.size - 1)
        val pts = values.mapIndexed { i, v ->
            Offset(
                stepX * i,
                size.height - 10.dp.toPx() - (v - minV) / range * (size.height - 20.dp.toPx())
            )
        }
        val path = Path().apply {
            moveTo(pts[0].x, pts[0].y)
            pts.drop(1).forEach { lineTo(it.x, it.y) }
        }
        drawPath(
            path,
            color = EnergyOrange,
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
        )
        pts.forEach { p ->
            drawCircle(EnergyOrange, radius = 4.dp.toPx(), center = p)
            drawCircle(Color.White.copy(alpha = 0.9f), radius = 1.8.dp.toPx(), center = p)
        }
    }
}
