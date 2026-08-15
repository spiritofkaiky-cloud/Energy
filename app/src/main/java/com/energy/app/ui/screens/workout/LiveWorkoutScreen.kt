package com.energy.app.ui.screens.workout

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.energy.app.data.workout.SaveStatus
import com.energy.app.data.workout.WorkoutMath
import com.energy.app.data.workout.WorkoutState
import com.energy.app.data.workout.WorkoutType
import com.energy.app.ui.components.MapWidget
import com.energy.app.ui.theme.EnergyCoral
import com.energy.app.ui.theme.EnergyMint
import com.energy.app.ui.theme.EnergyOrange
import kotlinx.coroutines.delay

/**
 * Live workout — full-screen map, big timer, live stats, GPS status,
 * accidental-touch protection on Finish, crash-draft restore, and a
 * finish flow that only claims "saved" once the workout is on disk.
 * APP_SPEC §9.
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
    val saveStatus by viewModel.saveStatus.collectAsState()
    val saved by viewModel.savedWorkout.collectAsState()
    val restored by viewModel.restored.collectAsState()
    val newRecords by viewModel.newRecords.collectAsState()
    val insights by viewModel.insights.collectAsState()
    val lastFix by viewModel.lastFixMillis.collectAsState()

    // ── countdown (only for fresh sessions) ───────────────────────────────
    var countdown by remember { mutableIntStateOf(if (state == WorkoutState.IDLE) 3 else 0) }
    var finished by remember { mutableStateOf(false) }
    var confirmFinish by remember { mutableStateOf(false) }

    LaunchedEffect(countdown) {
        if (countdown > 0) {
            buzz(context, light = true)
            delay(800)
            countdown--
        } else if (countdown == 0 && state == WorkoutState.IDLE && !finished) {
            viewModel.startWorkout(type, context)
            buzz(context, light = false)
        }
    }

    // Auto-cancel the confirm-finish window.
    LaunchedEffect(confirmFinish) {
        if (confirmFinish) {
            delay(4_000)
            confirmFinish = false
        }
    }

    // GPS silence hint: no accepted fix for 30 s while recording.
    val gpsSilent = state == WorkoutState.RECORDING &&
        lastFix > 0 && System.currentTimeMillis() - lastFix > 30_000

    Box(Modifier.fillMaxSize()) {
        MapWidget(
            points = rememberPoints(points),
            speeds = rememberSpeeds(points),
            modifier = Modifier.fillMaxSize(),
            interactive = true
        )

        // ── Top bar ───────────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.45f))
                .statusBarsPadding()
                .padding(vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${type.emoji} ${type.label}",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )
                if (restored) {
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "· restored",
                        style = MaterialTheme.typography.labelMedium,
                        color = EnergyOrange
                    )
                }
            }
            Text(
                text = WorkoutMath.formatDuration(elapsed),
                style = MaterialTheme.typography.displayLarge,
                color = Color.White,
                fontWeight = FontWeight.ExtraBold
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = WorkoutMath.formatSpeed(viewModel.currentSpeedKmh),
                    style = MaterialTheme.typography.titleLarge,
                    color = EnergyOrange,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "· max ${WorkoutMath.formatSpeed(maxSpeed)}",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }
        }

        // ── GPS status chip ───────────────────────────────────────────────
        AnimatedVisibility(
            visible = gpsSilent,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Card(
                shape = RoundedCornerShape(50),
                colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.75f)),
                modifier = Modifier.padding(horizontal = 24.dp)
            ) {
                Text(
                    text = "Location isn't updating. Check location access or move somewhere with a clearer GPS signal.",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp)
                )
            }
        }

        // ── Bottom control sheet ──────────────────────────────────────────
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.55f))
                .navigationBarsPadding()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                LiveStat("DISTANCE", WorkoutMath.formatDistance(distance))
                LiveStat("PACE", WorkoutMath.formatPace(WorkoutMath.paceSecondsPerKm(distance, elapsed)))
                LiveStat("SPEED", WorkoutMath.formatSpeed(viewModel.currentSpeedKmh))
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
                    onClick = {
                        buzz(context, light = true)
                        viewModel.togglePause(context)
                    }
                )
                ControlButton(
                    label = if (confirmFinish) "Tap again to finish" else "Finish",
                    color = if (confirmFinish) EnergyCoral else Color.White.copy(alpha = 0.28f),
                    onClick = {
                        buzz(context, light = true)
                        if (confirmFinish) {
                            confirmFinish = false
                            finished = true
                            viewModel.stopWorkout(context)
                        } else {
                            confirmFinish = true
                        }
                    }
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = when {
                    state == WorkoutState.PAUSED -> "Paused — resume to keep tracking"
                    restored && state == WorkoutState.PAUSED ->
                        "Restored from a previous session — resume or finish it"
                    else -> "Live · taps on the map zoom & pan"
                },
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.75f)
            )
            if (restored) {
                TextButton(onClick = {
                    viewModel.discardDraft()
                    onExit()
                }) {
                    Text("Discard this draft", color = Color.White.copy(alpha = 0.6f))
                }
            }
        }
    }

    // ── Countdown overlay ─────────────────────────────────────────────────
    if (countdown > 0) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.82f)),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "$countdown",
                    style = MaterialTheme.typography.displayLarge.copy(fontSize = 140.sp),
                    fontWeight = FontWeight.ExtraBold,
                    color = EnergyOrange
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Get ready…",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White
                )
            }
        }
    }

    // ── Saving overlay ────────────────────────────────────────────────────
    AnimatedVisibility(
        visible = finished && saveStatus == SaveStatus.SAVING,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.75f)),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = EnergyOrange)
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "Saving your workout…",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Stored on this device first, then synced when possible.",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }
        }
    }

    // ── Save failed overlay (draft kept) ──────────────────────────────────
    AnimatedVisibility(
        visible = finished && saveStatus == SaveStatus.FAILED,
        enter = fadeIn() + scaleIn(initialScale = 0.9f),
        exit = fadeOut()
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.8f)),
            contentAlignment = Alignment.Center
        ) {
            Card(
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(28.dp)
            ) {
                Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "⚠️ Couldn't save",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Your workout is kept as a draft on this device. " +
                            "It will be restored automatically the next time you open Energy.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(20.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        TextButton(onClick = { viewModel.retrySave() }) {
                            Text("Retry save")
                        }
                        TextButton(onClick = { viewModel.discardDraft(); onExit() }) {
                            Text("Discard")
                        }
                    }
                }
            }
        }
    }

    // ── Summary overlay ───────────────────────────────────────────────────
    AnimatedVisibility(
        visible = finished && saveStatus == SaveStatus.SAVED && saved != null,
        enter = fadeIn() + scaleIn(initialScale = 0.9f),
        exit = fadeOut()
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.7f)),
            contentAlignment = Alignment.Center
        ) {
            saved?.let { w ->
                Card(
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    Column(Modifier.padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${w.type.emoji} Workout saved",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Stored on this device" +
                                if (w.syncState == com.energy.app.data.workout.SyncState.SYNCED) " · synced to cloud ✓"
                                else " · cloud sync pending (works offline)",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            StatColumn(value = WorkoutMath.formatDistance(w.distanceMeters), label = "Distance")
                            StatColumn(value = WorkoutMath.formatDuration(w.durationMillis), label = "Time")
                            StatColumn(
                                value = WorkoutMath.formatPace(
                                    WorkoutMath.paceSecondsPerKm(w.distanceMeters, w.durationMillis)
                                ),
                                label = "Pace"
                            )
                            StatColumn(value = "${w.calories}", label = "kcal")
                        }
                        if (w.elevationGainMeters > 0) {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = "Elevation gain ${String.format("%.0f", w.elevationGainMeters)} m · " +
                                    "max ${WorkoutMath.formatSpeed(w.maxSpeedKmh)}",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (newRecords.isNotEmpty()) {
                            Spacer(Modifier.height(16.dp))
                            Card(
                                shape = RoundedCornerShape(18.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = EnergyOrange.copy(alpha = 0.14f)
                                )
                            ) {
                                Column(Modifier.padding(16.dp)) {
                                    Text(
                                        text = "🏆 New personal record!",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(Modifier.height(6.dp))
                                    newRecords.forEach { r ->
                                        Text(
                                            text = "${r.label}: ${r.valueText}",
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                }
                            }
                        }

                        if (insights.isNotEmpty()) {
                            Spacer(Modifier.height(12.dp))
                            Column(Modifier.fillMaxWidth()) {
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
                        TextButton(onClick = onExit) {
                            Text("Done", style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
            }
        }
    }
}

/** Cached mapping so the route list isn't rebuilt on every recomposition. */
@Composable
private fun rememberPoints(points: List<com.energy.app.data.workout.WorkoutPoint>): List<com.energy.app.data.location.DayPoint> =
    remember(points) {
        points.map { com.energy.app.data.location.DayPoint(it.lat, it.lng, it.timeMillis) }
    }

@Composable
private fun rememberSpeeds(points: List<com.energy.app.data.workout.WorkoutPoint>): List<Float> =
    remember(points) { points.map { it.speedKmh.toFloat() } }

private fun buzz(context: Context, light: Boolean) {
    runCatching {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager)
                .defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(
                VibrationEffect.createOneShot(if (light) 30L else 120L, if (light) 40 else 120)
            )
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(if (light) 30L else 120L)
        }
    }
}

@Composable
private fun LiveStat(label: String, value: String) {
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
