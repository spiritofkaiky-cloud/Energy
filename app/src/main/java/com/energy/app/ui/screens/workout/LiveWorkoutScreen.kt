package com.energy.app.ui.screens.workout

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.graphics.graphicsLayer
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
import com.energy.app.ui.components.Metric
import com.energy.app.ui.theme.EnergyCoral
import com.energy.app.ui.theme.EnergyMint
import com.energy.app.ui.theme.EnergyOrange
import com.energy.app.ui.theme.MetaLabel
import com.energy.app.ui.theme.Motion
import com.energy.app.ui.theme.Radius
import com.energy.app.ui.theme.Space
import kotlinx.coroutines.delay

/**
 * LIVE (§18–19) — a professional sports instrument.
 *
 * Hierarchy: CURRENT PACE hero → distance/time → map → controls.
 * Pausing transforms the screen: big PAUSED state, frozen map, prominent
 * resume. Finish requires a deliberate second tap. Saving is honest —
 * "saved" appears only when the workout is on disk.
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

    LaunchedEffect(confirmFinish) {
        if (confirmFinish) {
            delay(4_000)
            confirmFinish = false
        }
    }

    val gpsSilent = state == WorkoutState.RECORDING &&
        lastFix > 0 && System.currentTimeMillis() - lastFix > 30_000

    Box(Modifier.fillMaxSize()) {
        MapWidget(
            points = rememberPoints(points),
            speeds = rememberSpeeds(points),
            modifier = Modifier.fillMaxSize(),
            interactive = true
        )

        // ── Hero: current pace (the instrument's face) ────────────────────
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.45f))
                .statusBarsPadding()
                .padding(vertical = Space.SM),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${type.emoji} ${type.label}",
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Medium
                )
                if (restored) {
                    Text(
                        text = " · RESTORED",
                        style = MetaLabel,
                        color = EnergyOrange,
                        modifier = Modifier.padding(start = Space.XS)
                    )
                }
            }
            Text(
                text = WorkoutMath.formatPace(
                    WorkoutMath.paceSecondsPerKm(distance, elapsed),
                    imperial = false
                ).replace(" /km", ""),
                style = MaterialTheme.typography.displayMedium,
                color = Color.White,
                fontWeight = FontWeight.Light,
                fontSize = 46.sp
            )
            Text(
                text = "CURRENT PACE",
                style = MetaLabel,
                color = Color.White.copy(alpha = 0.65f)
            )
            Spacer(Modifier.height(Space.XS))
            Row(horizontalArrangement = Arrangement.spacedBy(Space.LG)) {
                LabeledNumber(WorkoutMath.formatDistance(distance), "DISTANCE")
                LabeledNumber(WorkoutMath.formatDuration(elapsed), "TIME")
                LabeledNumber(WorkoutMath.formatSpeed(viewModel.currentSpeedKmh), "SPEED")
            }
        }

        // ── GPS silence chip ──────────────────────────────────────────────
        AnimatedVisibility(
            visible = gpsSilent,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Card(
                shape = RoundedCornerShape(50),
                colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.75f)),
                modifier = Modifier.padding(horizontal = Space.XL)
            ) {
                Text(
                    text = "Location isn't updating. Check location access or move somewhere with a clearer GPS signal.",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = Space.MD, vertical = Space.SM)
                )
            }
        }

        // ── PAUSED morph — the interface transforms, not a tiny label ─────
        AnimatedVisibility(
            visible = state == WorkoutState.PAUSED,
            enter = fadeIn(tween(Motion.Medium)) + scaleIn(initialScale = 1.04f),
            exit = fadeOut(tween(Motion.Fast)) + scaleOut(targetScale = 1.04f),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "PAUSED",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Light,
                    color = Color.White
                )
                Text(
                    text = "Resume when you're ready",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }
        }

        // ── Bottom controls ───────────────────────────────────────────────
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.6f))
                .navigationBarsPadding()
                .padding(Space.LG)
                .animateContentSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Space.MD),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ControlButton(
                    label = if (state == WorkoutState.PAUSED) "Resume" else "Pause",
                    color = EnergyOrange,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        buzz(context, light = true)
                        viewModel.togglePause(context)
                    }
                )
                ControlButton(
                    label = if (confirmFinish) "Tap again to finish" else "Finish",
                    color = if (confirmFinish) EnergyCoral else Color.White.copy(alpha = 0.26f),
                    modifier = Modifier.weight(1f),
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
            Spacer(Modifier.height(Space.XS))
            Text(
                text = when {
                    state == WorkoutState.PAUSED -> "Map frozen · resume to keep tracking"
                    restored -> "Restored from a previous session"
                    else -> "Tap the map to pan & zoom"
                },
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.65f)
            )
            if (restored) {
                TextButton(onClick = {
                    viewModel.discardDraft()
                    onExit()
                }) {
                    Text("Discard this draft", color = Color.White.copy(alpha = 0.55f))
                }
            }
        }
    }

    // ── Countdown overlay ─────────────────────────────────────────────────
    if (countdown > 0) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.85f)),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "$countdown",
                    style = MaterialTheme.typography.displayLarge.copy(fontSize = 128.sp),
                    fontWeight = FontWeight.ExtraBold,
                    color = EnergyOrange
                )
                Text(
                    text = "Get ready…",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White
                )
            }
        }
    }

    // ── Saving overlay ────────────────────────────────────────────────────
    AnimatedVisibility(visible = finished && saveStatus == SaveStatus.SAVING) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.78f)),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = EnergyOrange)
                Spacer(Modifier.height(Space.MD))
                Text(
                    text = "Saving your workout…",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White
                )
                Text(
                    text = "Stored on this device first.",
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
                shape = RoundedCornerShape(Radius.XL),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Space.XL)
            ) {
                Column(
                    Modifier.padding(Space.XL),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Couldn't save",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(Space.XS))
                    Text(
                        text = "Your workout is kept as a draft on this device. " +
                            "It will be restored the next time you open Energy.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(Space.LG))
                    Row(horizontalArrangement = Arrangement.spacedBy(Space.SM)) {
                        TextButton(onClick = { viewModel.retrySave() }) { Text("Retry") }
                        TextButton(onClick = { viewModel.discardDraft(); onExit() }) { Text("Discard") }
                    }
                }
            }
        }
    }

    // ── Summary overlay — celebration first, analysis second (§20–21) ─────
    AnimatedVisibility(
        visible = finished && saveStatus == SaveStatus.SAVED && saved != null,
        enter = fadeIn() + scaleIn(initialScale = 0.94f),
        exit = fadeOut()
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.75f)),
            contentAlignment = Alignment.Center
        ) {
            saved?.let { w ->
                Card(
                    shape = RoundedCornerShape(Radius.XL),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Space.XL)
                ) {
                    Column(
                        Modifier.padding(Space.XL),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "${w.type.emoji}  ${w.type.label} saved",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (newRecords.isNotEmpty()) {
                            Text(
                                text = "🏆 New personal record",
                                style = MaterialTheme.typography.titleMedium,
                                color = EnergyOrange,
                                modifier = Modifier.padding(top = Space.XS)
                            )
                            newRecords.forEach { r ->
                                Text(
                                    text = "${r.label}: ${r.valueText}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                        Spacer(Modifier.height(Space.LG))

                        // Hero numbers
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Metric(
                                value = WorkoutMath.formatDistance(w.distanceMeters),
                                label = "Distance",
                                valueStyle = MaterialTheme.typography.displaySmall
                            )
                            Metric(
                                value = WorkoutMath.formatDuration(w.durationMillis),
                                label = "Time",
                                valueStyle = MaterialTheme.typography.displaySmall
                            )
                            Metric(
                                value = WorkoutMath.formatPace(
                                    WorkoutMath.paceSecondsPerKm(w.distanceMeters, w.durationMillis)
                                ).replace(" /km", ""),
                                label = "Pace",
                                valueStyle = MaterialTheme.typography.displaySmall
                            )
                        }
                        Spacer(Modifier.height(Space.MD))
                        Text(
                            text = "${w.calories} kcal" +
                                (if (w.elevationGainMeters > 0)
                                    " · +${String.format("%.0f", w.elevationGainMeters)} m" else "") +
                                " · max ${WorkoutMath.formatSpeed(w.maxSpeedKmh)}",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        if (insights.isNotEmpty()) {
                            Spacer(Modifier.height(Space.MD))
                            insights.take(2).forEach { i ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 2.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Text(i.emoji, style = MaterialTheme.typography.bodyLarge)
                                    Spacer(Modifier.width(Space.XS))
                                    Text(
                                        i.text,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(Space.LG))
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
private fun LabeledNumber(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = label,
            style = MetaLabel,
            color = Color.White.copy(alpha = 0.55f)
        )
    }
}

@Composable
private fun ControlButton(
    label: String,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.96f else 1f,
        animationSpec = tween(Motion.Instant),
        label = "controlScale"
    )
    Box(
        modifier = modifier
            .height(58.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(CircleShape)
            .background(color)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
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

/** Cached mappings so route lists aren't rebuilt on every recomposition. */
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
