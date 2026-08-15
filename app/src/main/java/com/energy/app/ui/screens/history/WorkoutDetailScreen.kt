package com.energy.app.ui.screens.history

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.energy.app.data.workout.WorkoutMath
import com.energy.app.data.workout.WorkoutType
import com.energy.app.ui.components.MapWidget
import com.energy.app.ui.components.Metric
import com.energy.app.ui.components.SectionHeader
import com.energy.app.ui.theme.EnergyOrange
import com.energy.app.ui.theme.MetaLabel
import com.energy.app.ui.theme.Radius
import com.energy.app.ui.theme.Space
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Workout detail (§21) — storytelling in sections:
 * hero numbers → Performance → Effort → Highlights, over the route map.
 * Not a grid of numbers inside cards.
 */
@Composable
fun WorkoutDetailScreen(
    workoutId: String,
    onBack: () -> Unit,
    viewModel: WorkoutDetailViewModel = viewModel()
) {
    val workout by viewModel.workout.collectAsState()
    val records by viewModel.records.collectAsState()
    val insights by viewModel.insights.collectAsState()
    val deleted by viewModel.deleted.collectAsState()
    var confirmDelete by remember { mutableStateOf(false) }

    LaunchedEffect(workoutId) { viewModel.load(workoutId) }
    LaunchedEffect(deleted) { if (deleted) onBack() }

    val w = workout
    if (w == null) {
        Box(Modifier.fillMaxSize()) {
            TextButton(onClick = onBack, modifier = Modifier.align(Alignment.TopStart).padding(Space.XS)) {
                Text("← Back")
            }
            Text(
                "Loading…",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.align(Alignment.Center)
            )
        }
        return
    }

    val splits = remember(w.points) {
        if (w.type == WorkoutType.RUN || w.type == WorkoutType.WALK) {
            WorkoutMath.splits(WorkoutMath.cumulativeDistanceTime(w.points))
        } else emptyList()
    }

    Box(Modifier.fillMaxSize()) {
        // ── Map (the route is the stage) ──────────────────────────────────
        MapWidget(
            points = remember(w.points) {
                w.points.map { com.energy.app.data.location.DayPoint(it.lat, it.lng, it.timeMillis) }
            },
            speeds = remember(w.points) { w.points.map { it.speedKmh.toFloat() } },
            modifier = Modifier.fillMaxSize(),
            interactive = true
        )
        TextButton(
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(Space.XS)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.5f))
        ) {
            Text("← Back", color = Color.White)
        }

        // ── Story sheet ───────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(
                    MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
                    RoundedCornerShape(topStart = Radius.XL, topEnd = Radius.XL)
                )
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Space.XL, vertical = Space.LG)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "${w.type.emoji}  ${w.type.label}",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = SimpleDateFormat("EEEE, MMM d · HH:mm", Locale.US)
                            .format(Date(w.startMillis)),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                TextButton(onClick = { confirmDelete = true }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            }

            Spacer(Modifier.height(Space.MD))

            // ── Hero numbers ──────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Metric(WorkoutMath.formatDistance(w.distanceMeters), "Distance", valueStyle = MaterialTheme.typography.displaySmall)
                Metric(WorkoutMath.formatDuration(w.durationMillis), "Time", valueStyle = MaterialTheme.typography.displaySmall)
                Metric(
                    WorkoutMath.formatPace(
                        WorkoutMath.paceSecondsPerKm(w.distanceMeters, w.durationMillis)
                    ).replace(" /km", ""),
                    "Pace",
                    valueStyle = MaterialTheme.typography.displaySmall
                )
            }

            Spacer(Modifier.height(Space.LG))

            // ── Performance ───────────────────────────────────────────────
            SectionHeader(label = "Performance")
            Spacer(Modifier.height(Space.XS))
            KeyValueRow("Average speed", WorkoutMath.formatSpeed(w.avgSpeedKmh))
            KeyValueRow("Best speed", WorkoutMath.formatSpeed(w.maxSpeedKmh))
            if (splits.isNotEmpty()) {
                KeyValueRow("Splits", "${splits.size} km recorded")
                splits.forEachIndexed { i, s ->
                    KeyValueRow("  km ${i + 1}", WorkoutMath.formatPace(s).replace(" /km", ""))
                }
            }

            Spacer(Modifier.height(Space.LG))

            // ── Effort ────────────────────────────────────────────────────
            SectionHeader(label = "Effort")
            Spacer(Modifier.height(Space.XS))
            KeyValueRow("Calories", "${w.calories} kcal")
            if (w.elevationGainMeters > 0) {
                KeyValueRow("Elevation gain", "+${String.format("%.0f", w.elevationGainMeters)} m")
            }
            KeyValueRow("GPS points", "${w.points.size}")

            // ── Highlights ────────────────────────────────────────────────
            if (records.isNotEmpty() || insights.isNotEmpty()) {
                Spacer(Modifier.height(Space.LG))
                SectionHeader(label = "Highlights")
                Spacer(Modifier.height(Space.XS))
                records.forEach { r ->
                    KeyValueRow(r.label, r.valueText, highlight = true)
                }
                insights.forEach { i ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(i.emoji, style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.width(Space.XS))
                        Text(
                            i.text,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            Spacer(Modifier.height(Space.XL))
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete this workout?") },
            text = {
                Text("The route and stats will be removed from this device. This can't be undone.")
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    viewModel.delete(workoutId)
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("Keep") }
            }
        )
    }
}

@Composable
private fun KeyValueRow(key: String, value: String, highlight: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = key,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = if (highlight) MaterialTheme.typography.titleMedium
            else MaterialTheme.typography.bodyMedium,
            fontWeight = if (highlight) FontWeight.SemiBold else FontWeight.Medium,
            color = if (highlight) EnergyOrange else MaterialTheme.colorScheme.onSurface
        )
    }
}
