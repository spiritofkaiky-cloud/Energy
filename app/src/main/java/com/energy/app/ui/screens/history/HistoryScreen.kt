package com.energy.app.ui.screens.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.energy.app.data.workout.SavedWorkout
import com.energy.app.ui.components.MapWidget
import com.energy.app.ui.components.SkeletonBox
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Workout history — saved workouts with mini route maps (APP_SPEC §5.7).
 * Cloud sync arrives with M5; data is local-first.
 */
@Composable
fun HistoryScreen(
    onWorkoutClick: (String) -> Unit,
    viewModel: HistoryViewModel = viewModel()
) {
    val workouts by viewModel.workouts.collectAsState(initial = null)

    val list = workouts
    if (list == null) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SkeletonBox(modifier = Modifier.fillMaxWidth(), height = 96.dp, corner = 20.dp)
            SkeletonBox(modifier = Modifier.fillMaxWidth(), height = 96.dp, corner = 20.dp)
            SkeletonBox(modifier = Modifier.fillMaxWidth(), height = 96.dp, corner = 20.dp)
        }
        return
    }

    if (list.isEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(60.dp))
            Text(text = "🗺️", style = MaterialTheme.typography.displayLarge)
            Spacer(Modifier.height(16.dp))
            Text(
                text = "No workouts yet",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Start one from the Workout tab and your routes will appear here.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "Workout history",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "${list.size} saved · tap for route detail",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        items(list, key = { it.id }) { workout ->
            WorkoutCard(workout = workout, onClick = { onWorkoutClick(workout.id) })
        }
        item { Spacer(Modifier.height(12.dp)) }
    }
}

@Composable
private fun WorkoutCard(workout: SavedWorkout, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MapWidget(
                points = workout.points.map { com.energy.app.data.location.DayPoint(it.lat, it.lng, it.timeMillis) },
                modifier = Modifier
                    .size(width = 96.dp, height = 96.dp),
                interactive = false
            )
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = "${workout.type.emoji} ${workout.type.label}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = SimpleDateFormat("EEE, MMM d · HH:mm", Locale.US).format(Date(workout.startMillis)),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                Row {
                    MiniStat(workout.distanceMetersText())
                    Spacer(Modifier.width(14.dp))
                    MiniStat(minutesText(workout.durationMillis))
                    Spacer(Modifier.width(14.dp))
                    MiniStat(paceText(workout))
                }
            }
        }
    }
}

@Composable
private fun MiniStat(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold
    )
}

private fun SavedWorkout.distanceMetersText(): String =
    if (distanceMeters >= 1000) String.format("%.2f km", distanceMeters / 1000)
    else String.format("%.0f m", distanceMeters)

private fun minutesText(ms: Long): String {
    val m = ms / 60_000
    return if (m >= 60) "${m / 60}h ${m % 60}m" else "${m}m"
}

private fun paceText(w: SavedWorkout): String {
    if (w.distanceMeters < 20) return "—"
    val minPerKm = (w.durationMillis / 60_000.0) / (w.distanceMeters / 1000.0)
    return String.format("%d:%02d/km", minPerKm.toInt(), ((minPerKm - minPerKm.toInt()) * 60).toInt())
}
