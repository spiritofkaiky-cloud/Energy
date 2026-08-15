package com.energy.app.ui.screens.workout

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.energy.app.data.location.DayPoint
import com.energy.app.data.workout.SavedWorkout
import com.energy.app.data.workout.WorkoutState
import com.energy.app.data.workout.WorkoutType
import com.energy.app.ui.components.MapWidget
import com.energy.app.ui.theme.EnergyCoral
import com.energy.app.ui.theme.EnergyOrange

/**
 * Live workout — full-screen interactive map, big timer, pause/resume,
 * finish summary. APP_SPEC §5.5.
 */
@Composable
fun LiveWorkoutScreen(
    typeName: String,
    onExit: () -> Unit,
    viewModel: WorkoutViewModel = viewModel()
) {
    val context = LocalContext.current
    val type = runCatching { WorkoutType.valueOf(typeName) }.getOrDefault(WorkoutType.RUN)
    val state by viewModel.state.collectAsState()
    val points by viewModel.points.collectAsState()
    val distance by viewModel.distanceMeters.collectAsState()
    val elapsed by viewModel.elapsedMillis.collectAsState()
    val maxSpeed by viewModel.maxSpeedKmh.collectAsState()

    var summary by remember { mutableStateOf<SavedWorkout?>(null) }

    LaunchedEffect(Unit) {
        viewModel.startWorkout(type, context)
    }

    Box(Modifier.fillMaxSize()) {
        // Full-screen real map — route draws itself live.
        // (Position marker layer exists in MapWidget but is disabled here:
        // its halo circle leaks ~3 GB of native memory on the emulator's
        // software renderer. Enable on real devices where GL is proper.)
        MapWidget(
            points = points.map { DayPoint(it.lat, it.lng, it.timeMillis) },
            currentPosition = null,
            modifier = Modifier.fillMaxSize(),
            interactive = true
        )

        // Top bar: type + big timer
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.45f))
                .padding(vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "${type.emoji} ${type.label}",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = formatTime(elapsed),
                style = MaterialTheme.typography.displayLarge,
                color = Color.White,
                fontWeight = FontWeight.ExtraBold
            )
            // Live speed readout — the "speed tracker" (updates every tick)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = String.format("%.1f km/h", viewModel.currentSpeedKmh),
                    style = MaterialTheme.typography.titleLarge,
                    color = EnergyOrange,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "· max ${String.format("%.1f", maxSpeed)}",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }
        }

        // Bottom control sheet
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.55f))
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                LiveStat("DISTANCE", formatDistance(distance))
                LiveStat("PACE", formatPace(elapsed, distance))
                LiveStat("SPEED", String.format("%.1f", viewModel.currentSpeedKmh) + " km/h")
            }
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ControlButton(
                    label = if (state == WorkoutState.PAUSED) "Resume" else "Pause",
                    color = EnergyOrange,
                    onClick = { viewModel.togglePause(context) }
                )
                ControlButton(
                    label = "Finish",
                    color = EnergyCoral,
                    onClick = { summary = viewModel.stopWorkout(context) }
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = if (state == WorkoutState.PAUSED) "Paused — resume to keep tracking"
                else "Live · taps on the map zoom & pan",
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.75f)
            )
        }
    }

    // Summary overlay
    AnimatedVisibility(
        visible = summary != null,
        enter = fadeIn() + scaleIn(initialScale = 0.9f),
        exit = fadeOut()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.7f)),
            contentAlignment = Alignment.Center
        ) {
            summary?.let { w ->
                Card(
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(28.dp)
                ) {
                    Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${w.type.emoji} Workout saved!",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Syncing to cloud comes with M5.",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(20.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            StatColumn(value = formatDistance(w.distanceMeters), label = "Distance")
                            StatColumn(value = formatTime(w.durationMillis), label = "Time")
                            StatColumn(value = formatPace(w.durationMillis, w.distanceMeters), label = "Pace")
                            StatColumn(value = w.calories.toString(), label = "kcal")
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "Max speed ${String.format("%.1f", maxSpeed)} km/h",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(20.dp))
                        TextButton(onClick = onExit) {
                            Text("Done", style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LiveStat(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = Color.White.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun StatColumn(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun RowScope.ControlButton(label: String, color: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .weight(1f)
            .height(56.dp)
            .clip(CircleShape)
            .background(color)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
            fontWeight = FontWeight.SemiBold
        )
    }
}

private fun formatTime(ms: Long): String {
    val h = ms / 3_600_000
    val m = (ms % 3_600_000) / 60_000
    val s = (ms % 60_000) / 1_000
    return if (h > 0) String.format("%d:%02d:%02d", h, m, s)
    else String.format("%02d:%02d", m, s)
}

private fun formatDistance(meters: Double): String =
    if (meters >= 1000) String.format("%.2f km", meters / 1000) else String.format("%.0f m", meters)

private fun formatPace(ms: Long, meters: Double): String {
    if (meters < 20) return "—"
    val minPerKm = (ms / 60_000.0) / (meters / 1000.0)
    val m = minPerKm.toInt()
    val s = ((minPerKm - m) * 60).toInt()
    return String.format("%d:%02d /km", m, s)
}
