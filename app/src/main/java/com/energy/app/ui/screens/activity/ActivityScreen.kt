package com.energy.app.ui.screens.activity

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.energy.app.data.workout.SavedWorkout
import com.energy.app.data.workout.SyncState
import com.energy.app.data.workout.WorkoutMath
import com.energy.app.data.workout.WorkoutState
import com.energy.app.data.workout.WorkoutType
import com.energy.app.ui.components.EmptyState
import com.energy.app.ui.components.EnergyButton
import com.energy.app.ui.components.MapWidget
import com.energy.app.ui.components.SectionHeader
import com.energy.app.ui.components.SkeletonBox
import com.energy.app.ui.screens.history.HistoryViewModel
import com.energy.app.ui.screens.workout.WorkoutViewModel
import com.energy.app.ui.theme.CycleColor
import com.energy.app.ui.theme.HikeColor
import com.energy.app.ui.theme.MetaLabel
import com.energy.app.ui.theme.Motion
import com.energy.app.ui.theme.Radius
import com.energy.app.ui.theme.RunColor
import com.energy.app.ui.theme.Space
import com.energy.app.ui.theme.WalkColor
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * ACTIVITY (§16 + §22) — one screen, two roles:
 *  1. Starting a workout — an intentional act, not a settings page.
 *  2. The activity timeline — a chronological stream, not card-stacking.
 */
@Composable
fun ActivityScreen(
    onStart: (WorkoutType) -> Unit,
    onWorkoutClick: (String) -> Unit,
    workoutViewModel: WorkoutViewModel = viewModel(),
    historyViewModel: HistoryViewModel = viewModel()
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val sessionState by workoutViewModel.state.collectAsState()
    val sessionType by workoutViewModel.type.collectAsState()
    val workouts by historyViewModel.workouts.collectAsState(initial = null)

    var selected by remember { mutableStateOf(WorkoutType.RUN) }
    var goal by remember { mutableStateOf("Open") }
    var filter by remember { mutableStateOf<WorkoutType?>(null) }

    val hasLocation = hasLocationPermission(context)
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = Space.XL, end = Space.XL, top = Space.MD, bottom = Space.XXL
        ),
        verticalArrangement = Arrangement.spacedBy(Space.LG)
    ) {
        // ── Workout entry ─────────────────────────────────────────────────
        item {
            Column {
                Text(
                    text = "Ready to move?",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Routes and pace work fully offline. Data stays on your device.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item {
            WorkoutTypeSelector(
                selected = selected,
                onSelect = { selected = it }
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Space.XS)
            ) {
                listOf("Open", "3 km", "5 km", "30 min").forEach { g ->
                    FilterChip(
                        selected = goal == g,
                        onClick = { goal = g },
                        label = { Text(g) }
                    )
                }
            }
        }

        if (sessionState != WorkoutState.IDLE) {
            item {
                Surface(
                    onClick = { onStart(sessionType) },
                    shape = RoundedCornerShape(Radius.MD),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    border = BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Space.MD),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(sessionType.emoji, style = MaterialTheme.typography.titleLarge)
                        Spacer(Modifier.width(Space.SM))
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = "A ${sessionType.label.lowercase()} is in progress",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Return to it",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text("→", color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }

        item {
            EnergyButton(
                text = if (goal == "Open") "${selected.emoji}  Start ${selected.label}"
                else "${selected.emoji}  Start · $goal",
                onClick = { onStart(selected) },
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (hasLocation) "●" else "○",
                    color = if (hasLocation) MaterialTheme.colorScheme.secondary
                    else MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.labelMedium
                )
                Spacer(Modifier.width(Space.XS))
                Text(
                    text = if (hasLocation) "GPS ready"
                    else "Location access needed for routes",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                if (!hasLocation) {
                    Text(
                        text = "Enable",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .clickable {
                                permissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                    )
                                )
                            }
                            .padding(horizontal = Space.SM, vertical = Space.XS)
                    )
                }
            }
        }

        // ── Activity timeline ─────────────────────────────────────────────
        item {
            Spacer(Modifier.height(Space.XS))
            SectionHeader(label = "Recent activity")
        }

        val list = workouts
        when {
            list == null -> {
                items(3) {
                    SkeletonBox(
                        modifier = Modifier.fillMaxWidth(),
                        height = 84.dp,
                        corner = Radius.MD
                    )
                }
            }
            list.isEmpty() -> {
                item {
                    EmptyState(
                        glyph = "🏃",
                        title = "Your first workout is waiting",
                        body = "Start a walk, run, cycle or hike and your activity will appear here.",
                        actionLabel = "Start a workout",
                        onAction = { onStart(selected) }
                    )
                }
            }
            else -> {
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(Space.XS)) {
                        FilterChip(
                            selected = filter == null,
                            onClick = { filter = null },
                            label = { Text("All") }
                        )
                        WorkoutType.entries.forEach { t ->
                            FilterChip(
                                selected = filter == t,
                                onClick = { filter = if (filter == t) null else t },
                                label = { Text(t.label) }
                            )
                        }
                    }
                }
                val filtered = if (filter == null) list else list.filter { it.type == filter }
                filtered.groupBy { dateHeader(it.startMillis) }.forEach { (header, group) ->
                    item(key = "h-$header") {
                        Text(
                            text = header.uppercase(),
                            style = MetaLabel,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = Space.SM)
                        )
                    }
                    items(group, key = { it.id }) { w ->
                        TimelineRow(workout = w, onClick = { onWorkoutClick(w.id) })
                    }
                }
            }
        }
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
        else -> SimpleDateFormat("EEE, MMM d", Locale.US).format(Date(startMillis))
    }
}

/** The selector: four large visual tiles, one strong restrained active state. */
@Composable
private fun WorkoutTypeSelector(
    selected: WorkoutType,
    onSelect: (WorkoutType) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Space.SM)
    ) {
        WorkoutType.entries.forEach { type ->
            val active = type == selected
            val container by animateColorAsState(
                targetValue = if (active) typeColor(type).copy(alpha = 0.16f)
                else MaterialTheme.colorScheme.surface,
                animationSpec = tween(Motion.Medium),
                label = "typeBg"
            )
            val scale by animateFloatAsState(
                targetValue = if (active) 1.02f else 1f,
                animationSpec = tween(Motion.Fast),
                label = "typeScale"
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(Radius.MD))
                    .background(container)
                    .border(
                        width = if (active) 1.5.dp else 1.dp,
                        color = if (active) typeColor(type).copy(alpha = 0.7f)
                        else MaterialTheme.colorScheme.outlineVariant,
                        shape = RoundedCornerShape(Radius.MD)
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { onSelect(type) }
                    )
                    .padding(vertical = Space.MD),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = type.emoji,
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier
                )
                Text(
                    text = type.label.uppercase(),
                    style = MetaLabel,
                    color = if (active) typeColor(type)
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = Space.XS)
                )
            }
        }
    }
}

/** Timeline row — a stream entry, not a card stack (§22). */
@Composable
private fun TimelineRow(workout: SavedWorkout, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.MD))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(vertical = Space.SM),
        verticalAlignment = Alignment.CenterVertically
    ) {
        MapWidget(
            points = workout.points.map {
                com.energy.app.data.location.DayPoint(it.lat, it.lng, it.timeMillis)
            },
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(Radius.SM)),
            interactive = false
        )
        Spacer(Modifier.width(Space.MD))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = workout.type.label,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                if (workout.syncState == SyncState.PENDING) {
                    Text(
                        text = " · ☁",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Text(
                text = SimpleDateFormat("HH:mm", Locale.US).format(Date(workout.startMillis)),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = WorkoutMath.formatDistance(workout.distanceMeters),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.width(Space.SM))
        Text(
            text = WorkoutMath.formatDuration(workout.durationMillis),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(52.dp),
            textAlign = TextAlign.End
        )
    }
}

private fun typeColor(type: WorkoutType): Color = when (type) {
    WorkoutType.RUN -> RunColor
    WorkoutType.WALK -> WalkColor
    WorkoutType.CYCLE -> CycleColor
    WorkoutType.HIKE -> HikeColor
}

private fun hasLocationPermission(context: Context): Boolean =
    ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED
