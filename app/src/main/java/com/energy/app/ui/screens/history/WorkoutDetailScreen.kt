package com.energy.app.ui.screens.history

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.energy.app.data.workout.WorkoutMath
import com.energy.app.data.workout.WorkoutType
import com.energy.app.ui.components.HairlineCard
import com.energy.app.ui.components.MapWidget
import com.energy.app.ui.theme.EnergyOrange
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Workout detail (APP_SPEC §11) — full route map, stat grid, splits,
 * personal records, data-derived insights, elevation, delete.
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
            TextButton(onClick = onBack, modifier = Modifier.align(Alignment.TopStart).padding(8.dp)) {
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
        // ── Map ───────────────────────────────────────────────────────────
        MapWidget(
            points = remember(w.points) {
                w.points.map { com.energy.app.data.location.DayPoint(it.lat, it.lng, it.timeMillis) }
            },
            speeds = remember(w.points) { w.points.map { it.speedKmh.toFloat() } },
            modifier = Modifier.fillMaxSize(),
            interactive = true
        )
        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(8.dp)
        ) {
            TextButton(
                onClick = onBack,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.5f))
            ) {
                Text("← Back", color = androidx.compose.ui.graphics.Color.White)
            }
        }

        // ── Details sheet ─────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(
                    MaterialTheme.colorScheme.surface.copy(alpha = 0.97f),
                    RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
                )
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${w.type.emoji} ${w.type.label}",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = { confirmDelete = true }) {
                    Text("Delete", color = com.energy.app.ui.theme.EnergyCoral)
                }
            }
            Text(
                text = SimpleDateFormat("EEEE, MMM d, yyyy · HH:mm", Locale.US).format(Date(w.startMillis)),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                DetailStat(value = WorkoutMath.formatDistance(w.distanceMeters), label = "Distance")
                DetailStat(value = WorkoutMath.formatDuration(w.durationMillis), label = "Time")
                DetailStat(
                    value = WorkoutMath.formatPace(
                        WorkoutMath.paceSecondsPerKm(w.distanceMeters, w.durationMillis)
                    ),
                    label = "Avg pace"
                )
                DetailStat(value = "${w.calories}", label = "kcal")
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = "Max ${WorkoutMath.formatSpeed(w.maxSpeedKmh)} · avg ${WorkoutMath.formatSpeed(w.avgSpeedKmh)}" +
                    (if (w.elevationGainMeters > 0)
                        " · elevation +${String.format("%.0f", w.elevationGainMeters)} m" else "") +
                    " · ${w.points.size} GPS points",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // ── Records held by this workout ──────────────────────────────
            if (records.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                HairlineCard {
                    Text(
                        text = "🏆 Records from this workout",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(8.dp))
                    records.forEach { r ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
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

            // ── Splits ────────────────────────────────────────────────────
            if (splits.isNotEmpty()) {
                Spacer(Modifier.height(14.dp))
                HairlineCard {
                    Text(
                        text = "Splits",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(8.dp))
                    splits.forEachIndexed { i, s ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "km ${i + 1}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                WorkoutMath.formatPace(s),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            // ── Insights ──────────────────────────────────────────────────
            if (insights.isNotEmpty()) {
                Spacer(Modifier.height(14.dp))
                HairlineCard {
                    Text(
                        text = "Insights",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(8.dp))
                    insights.forEach { i ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Text(i.emoji, style = MaterialTheme.typography.bodyLarge)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                i.text,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
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
                }) { Text("Delete", color = com.energy.app.ui.theme.EnergyCoral) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("Keep") }
            }
        )
    }
}

@Composable
private fun DetailStat(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(text = label, style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
