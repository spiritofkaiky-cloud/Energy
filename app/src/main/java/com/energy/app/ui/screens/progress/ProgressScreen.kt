package com.energy.app.ui.screens.progress

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.energy.app.data.workout.WorkoutMath
import com.energy.app.ui.components.EnergyBarChart
import com.energy.app.ui.components.EnergyLineChart
import com.energy.app.ui.components.EmptyState
import com.energy.app.ui.components.Metric
import com.energy.app.ui.components.SectionHeader
import com.energy.app.ui.components.SegmentedControl
import com.energy.app.ui.theme.EnergyMint
import com.energy.app.ui.theme.EnergyOrange
import com.energy.app.ui.theme.MetaLabel
import com.energy.app.ui.theme.Space

/**
 * PROGRESS (§23) — a calm analytics experience. One major chart at a time
 * behind WEEK / MONTH / YEAR; the headline number above it answers the
 * question, the chart shows the shape.
 */
@Composable
fun ProgressScreen(viewModel: ProgressViewModel = viewModel()) {
    val data by viewModel.data.collectAsState()
    var period by remember { mutableIntStateOf(0) } // 0 week, 1 month, 2 year

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Space.XL)
    ) {
        Spacer(Modifier.height(Space.MD))
        Text(
            text = "PROGRESS",
            style = MetaLabel,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = when (period) {
                0 -> "This week"
                1 -> "This month"
                else -> "This year"
            },
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(Modifier.height(Space.MD))
        SegmentedControl(
            options = listOf("Week", "Month", "Year"),
            selectedIndex = period,
            onSelect = { period = it }
        )

        Spacer(Modifier.height(Space.XL))

        // ── One major chart ───────────────────────────────────────────────
        when (period) {
            0 -> {
                val km = data.days14.takeLast(7).sumOf { it.km }
                val prevKm = data.days14.take(7).sumOf { it.km }
                ChartHeadline(
                    value = "%.1f".format(km),
                    unit = "km",
                    delta = if (prevKm > 0) (km - prevKm) / prevKm * 100 else null,
                    caption = "vs the previous week"
                )
                EnergyBarChart(
                    values = data.days14.takeLast(7).map { it.km.toFloat() },
                    heightDp = 150,
                    modifier = Modifier.padding(top = Space.MD)
                )
                DayLabels(data.days14.takeLast(7).map { it.label })
            }
            1 -> {
                val km30 = data.days30.sumOf { it.km }
                ChartHeadline(
                    value = "%.1f".format(km30),
                    unit = "km",
                    delta = null,
                    caption = "${data.workouts14} workouts in the last 14 days"
                )
                EnergyLineChart(
                    values = data.days30.map { it.km.toFloat() },
                    heightDp = 160,
                    modifier = Modifier.padding(top = Space.MD)
                )
            }
            else -> {
                val kmYear = data.months12.sumOf { it.km }
                ChartHeadline(
                    value = "%.1f".format(kmYear),
                    unit = "km",
                    delta = null,
                    caption = "last 12 months"
                )
                EnergyBarChart(
                    values = data.months12.map { it.km.toFloat() },
                    heightDp = 150,
                    modifier = Modifier.padding(top = Space.MD)
                )
                DayLabels(data.months12.map { it.label })
            }
        }

        Spacer(Modifier.height(Space.XXL))

        // ── Consistency ───────────────────────────────────────────────────
        SectionHeader(label = "Consistency")
        Spacer(Modifier.height(Space.SM))
        Row(verticalAlignment = Alignment.CenterVertically) {
            val pct = (data.activeDays14 * 100 / 14).coerceIn(0, 100)
            Text(
                text = "$pct%",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Light,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.width(Space.MD))
            Column {
                Text(
                    text = "of the last 14 days had activity",
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = if (pct >= 70) "Strong rhythm. Keep it."
                    else if (pct >= 40) "Decent. Aim for 4+ active days a week."
                    else "Small, regular days beat rare big ones.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (data.bestMonthLabel != null) {
            Text(
                text = "Best month · ${data.bestMonthLabel} · %.1f km".format(data.bestMonthKm),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = Space.XS)
            )
        }
        val avgPace = data.avgPace14
        if (avgPace != null) {
            Text(
                text = "Avg run pace (14d) · ${WorkoutMath.formatPace(avgPace * 60)}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = Space.XS)
            )
        }

        Spacer(Modifier.height(Space.XXL))

        // ── Personal records ──────────────────────────────────────────────
        SectionHeader(label = "Personal records")
        Spacer(Modifier.height(Space.SM))
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
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(r.label, style = MaterialTheme.typography.bodyMedium)
                    Text(
                        r.valueText,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        Spacer(Modifier.height(Space.XXL))

        // ── All time ──────────────────────────────────────────────────────
        SectionHeader(label = "All time")
        Spacer(Modifier.height(Space.SM))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Metric("${data.lifetime.workoutCount}", "Workouts", valueStyle = MaterialTheme.typography.titleLarge)
            Metric("%.1f".format(data.lifetime.totalKm), "km", valueStyle = MaterialTheme.typography.titleLarge)
            Metric("${data.lifetime.totalMinutes / 60}h", "Moving", valueStyle = MaterialTheme.typography.titleLarge)
        }

        if (data.lifetime.workoutCount == 0) {
            Spacer(Modifier.height(Space.XL))
            EmptyState(
                glyph = "📈",
                title = "Your progress starts with one workout",
                body = "Distance, pace and consistency trends will appear here."
            )
        }

        Spacer(Modifier.height(Space.XXL))
    }
}

@Composable
private fun ChartHeadline(value: String, unit: String, delta: Double?, caption: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = value,
            style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.Light
        )
        Spacer(Modifier.width(Space.XS))
        Text(
            text = unit,
            style = MetaLabel,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 14.dp)
        )
        if (delta != null) {
            Spacer(Modifier.width(Space.MD))
            Text(
                text = if (delta >= 0) "↑ +%.0f%%".format(delta) else "↓ %.0f%%".format(delta),
                style = MaterialTheme.typography.titleSmall,
                color = if (delta >= 0) EnergyMint else EnergyOrange
            )
        }
    }
    Text(
        text = caption,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun DayLabels(labels: List<String>) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = Space.XS),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        labels.forEachIndexed { i, l ->
            if (i % 2 == 0 || labels.size <= 10) {
                Text(
                    text = l.take(3),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else Spacer(Modifier.width(1.dp))
        }
    }
}
