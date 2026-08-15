package com.energy.app.ui.screens.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.energy.app.data.location.DayPoint
import com.energy.app.ui.components.MapWidget
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Workout detail — full route map + stats (APP_SPEC §5.6).
 */
@Composable
fun WorkoutDetailScreen(
    workoutId: String,
    onBack: () -> Unit,
    viewModel: WorkoutDetailViewModel = viewModel()
) {
    val workout by viewModel.workout.collectAsState()

    LaunchedEffect(workoutId) { viewModel.load(workoutId) }

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

    Box(Modifier.fillMaxSize()) {
        MapWidget(
            points = w.points.map { DayPoint(it.lat, it.lng, it.timeMillis) },
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
                modifier = Modifier.clip(CircleShape)
            ) {
                Text("← Back")
            }
        }
        Card(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(Modifier.padding(20.dp)) {
                Text(
                    text = "${w.type.emoji} ${w.type.label}",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))
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
                    DetailStat(value = w.distanceMetersText(), label = "Distance")
                    DetailStat(value = minutesText(w.durationMillis), label = "Time")
                    DetailStat(value = paceText(w), label = "Pace")
                    DetailStat(value = w.calories.toString(), label = "kcal")
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Avg speed ${String.format("%.1f", w.avgSpeedKmh)} km/h · ${w.points.size} GPS points",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
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

private fun SavedWorkout_.distanceMetersText(): String =
    if (distanceMeters >= 1000) String.format("%.2f km", distanceMeters / 1000)
    else String.format("%.0f m", distanceMeters)

private fun minutesText(ms: Long): String {
    val m = ms / 60_000
    return if (m >= 60) "${m / 60}h ${m % 60}m" else "${m}m"
}

private fun paceText(w: SavedWorkout_): String {
    if (w.distanceMeters < 20) return "—"
    val minPerKm = (w.durationMillis / 60_000.0) / (w.distanceMeters / 1000.0)
    return String.format("%d:%02d/km", minPerKm.toInt(), ((minPerKm - minPerKm.toInt()) * 60).toInt())
}

// Local typealias to keep imports tidy in this file
private typealias SavedWorkout_ = com.energy.app.data.workout.SavedWorkout
