package com.energy.app.ui.screens.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.energy.app.data.workout.SavedWorkout
import com.energy.app.data.workout.SyncState
import com.energy.app.data.workout.WorkoutMath
import com.energy.app.data.workout.WorkoutType
import com.energy.app.ui.components.MapWidget
import com.energy.app.ui.components.SkeletonBox
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * History — a rich activity timeline (APP_SPEC §20): date-grouped rows,
 * type filters, sync state badges, route thumbnails.
 */
@Composable
fun HistoryScreen(
    onWorkoutClick: (String) -> Unit,
    viewModel: HistoryViewModel = viewModel()
) {
    val workouts by viewModel.workouts.collectAsState(initial = null)
    var filter by remember { mutableStateOf<WorkoutType?>(null) }

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
                text = "Your first workout starts here",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Start one from the Workout tab and your routes will appear here — " +
                    "with splits, pace and personal records.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    val filtered = if (filter == null) list else list.filter { it.type == filter }
    val grouped = filtered.groupBy { dateHeader(it.startMillis) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "Activity",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "${list.size} workouts · everything is stored on this device",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = filter == null,
                    onClick = { filter = null },
                    label = { Text("All") }
                )
                WorkoutType.entries.forEach { t ->
                    FilterChip(
                        selected = filter == t,
                        onClick = { filter = if (filter == t) null else t },
                        label = { Text("${t.emoji} ${t.label}") }
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
        }

        grouped.forEach { (header, group) ->
            item(key = "header-$header") {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = header,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(4.dp))
            }
            group.forEach { workout ->
                item(key = workout.id) {
                    WorkoutCard(workout = workout, onClick = { onWorkoutClick(workout.id) })
                }
            }
        }

        item { Spacer(Modifier.height(12.dp)) }
    }
}

private fun dateHeader(startMillis: Long): String {
    val dayFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    val today = dayFmt.format(Date())
    val cal = Calendar.getInstance()
    cal.add(Calendar.DAY_OF_YEAR, -1)
    val yesterday = dayFmt.format(cal.time)
    return when (dayFmt.format(Date(startMillis))) {
        today -> "Today"
        yesterday -> "Yesterday"
        else -> SimpleDateFormat("EEEE, MMM d", Locale.US).format(Date(startMillis))
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
                points = workout.points.map {
                    com.energy.app.data.location.DayPoint(it.lat, it.lng, it.timeMillis)
                },
                modifier = Modifier
                    .size(width = 96.dp, height = 96.dp),
                interactive = false
            )
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${workout.type.emoji} ${workout.type.label}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )
                    if (workout.syncState == SyncState.PENDING) {
                        Text(
                            text = "☁",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else if (workout.syncState == SyncState.FAILED) {
                        Text(
                            text = "⚠",
                            style = MaterialTheme.typography.labelLarge,
                            color = com.energy.app.ui.theme.EnergyCoral
                        )
                    }
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    text = SimpleDateFormat("EEE, MMM d · HH:mm", Locale.US).format(Date(workout.startMillis)),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                Row {
                    MiniStat(WorkoutMath.formatDistance(workout.distanceMeters))
                    Spacer(Modifier.width(14.dp))
                    MiniStat(WorkoutMath.formatDuration(workout.durationMillis))
                    Spacer(Modifier.width(14.dp))
                    MiniStat(
                        WorkoutMath.formatPace(
                            WorkoutMath.paceSecondsPerKm(
                                workout.distanceMeters, workout.durationMillis
                            )
                        )
                    )
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
