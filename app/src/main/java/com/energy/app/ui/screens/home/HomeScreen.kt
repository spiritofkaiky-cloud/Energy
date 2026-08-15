package com.energy.app.ui.screens.home

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.energy.app.data.location.DayPoint
import com.energy.app.data.workout.WorkoutMath
import com.energy.app.data.workout.WorkoutState
import com.energy.app.data.workout.WorkoutType
import com.energy.app.ui.components.EnergyButton
import com.energy.app.ui.components.EnergyRing
import com.energy.app.ui.components.EnergyRingLegend
import com.energy.app.ui.components.MapWidget
import com.energy.app.ui.components.Metric
import com.energy.app.ui.components.ScoreHero
import com.energy.app.ui.components.SectionHeader
import com.energy.app.ui.components.StatStrip
import com.energy.app.ui.theme.EnergyOrange
import com.energy.app.ui.theme.MetaLabel
import com.energy.app.ui.theme.Radius
import com.energy.app.ui.theme.RingExercise
import com.energy.app.ui.theme.RingMove
import com.energy.app.ui.theme.RingStand
import com.energy.app.ui.theme.Space
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * TODAY (§7) — a personal daily health briefing, not a widget dashboard.
 *
 * Composition (top → bottom):
 *   context line → greeting → Score Hero → daily insight → Energy Ring →
 *   stat strip → movement map (edge-to-edge) → streak → health status.
 * Sections, not cards. Orange appears only as accent.
 */
@Composable
fun HomeScreen(
    onOpenFullMap: () -> Unit = {},
    onResumeWorkout: (WorkoutType) -> Unit = {},
    viewModel: HomeViewModel = viewModel()
) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val points by viewModel.points.collectAsState()
    val tracking by viewModel.tracking.collectAsState()
    val health by viewModel.dailyHealth.collectAsState()
    val healthData = health
    val score by viewModel.score.collectAsState()
    val streak by viewModel.streak.collectAsState()
    val rings by viewModel.rings.collectAsState()
    val today by viewModel.today.collectAsState()
    val prefs by viewModel.prefs.collectAsState()
    val activeWorkout by viewModel.activeWorkout.collectAsState()
    val workoutState by viewModel.workoutState.collectAsState()
    val workoutType by viewModel.workoutType.collectAsState()
    val userName by viewModel.userName.collectAsState()

    var hasPermission by remember { mutableStateOf(hasLocationPermission(context)) }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        hasPermission = result.values.any { it }
        if (hasPermission) viewModel.startTracking()
    }

    LaunchedEffect(hasPermission) {
        if (hasPermission) viewModel.startTracking()
    }

    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val routeKm = dayPathKm(points)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Space.XL)
    ) {
        Spacer(Modifier.height(Space.MD))

        // ── Context line (level 4 — whisper) ──────────────────────────────
        Text(
            text = "TODAY · " +
                SimpleDateFormat("EEEE, MMM d", Locale.US).format(Date()).uppercase(),
            style = MetaLabel,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // ── Greeting (level 1–2) ──────────────────────────────────────────
        Spacer(Modifier.height(Space.XS))
        Text(
            text = "${greetingForHour(hour)}, $userName",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.fillMaxWidth()
        )

        // ── Active workout banner ─────────────────────────────────────────
        AnimatedVisibility(visible = activeWorkout) {
            Surface(
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onResumeWorkout(workoutType)
                },
                shape = RoundedCornerShape(Radius.MD),
                color = EnergyOrange.copy(alpha = 0.12f),
                border = BorderStroke(1.dp, EnergyOrange.copy(alpha = 0.4f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Space.MD)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = Space.MD, vertical = Space.SM),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(workoutType.emoji, style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.width(Space.SM))
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = if (workoutState == WorkoutState.PAUSED)
                                "Workout paused — tap to resume" else "Workout in progress",
                            style = MaterialTheme.typography.titleSmall,
                            color = EnergyOrange
                        )
                        Text(
                            text = "Your session survives restarts.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text("→", color = EnergyOrange)
                }
            }
        }

        // ── Score hero (the centerpiece) ──────────────────────────────────
        Spacer(Modifier.height(Space.XL))
        ScoreHero(score = score, modifier = Modifier.fillMaxWidth())

        Spacer(Modifier.height(Space.XXL))

        // ── Daily insight (level 2 — personal guidance) ───────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            Text(
                text = "💡",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.width(Space.SM))
            Column {
                Text(
                    text = score.recommendation.text,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = score.recommendation.basis,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.alpha(0.85f)
                )
            }
        }

        Spacer(Modifier.height(Space.XXL))

        // ── Energy Ring + legend ──────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            EnergyRing(
                move = rings.move,
                exercise = rings.exercise,
                stand = rings.stand,
                sizeDp = 148
            )
            Spacer(Modifier.width(Space.XL))
            Column(Modifier.weight(1f)) {
                Text(
                    text = "TODAY'S BALANCE",
                    style = MetaLabel,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(Space.MD))
                RingLegendRow(RingMove, "Move", rings.moveDetail)
                Spacer(Modifier.height(Space.SM))
                RingLegendRow(RingExercise, "Exercise", rings.exerciseDetail)
                Spacer(Modifier.height(Space.SM))
                RingLegendRow(RingStand, "Stand", rings.standDetail)
                Spacer(Modifier.height(Space.MD))
                Text(
                    text = "Estimates from your activity — not medical measurements.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.alpha(0.8f)
                )
            }
        }

        Spacer(Modifier.height(Space.XXL))

        // ── Stat strip (level 2 numbers) ──────────────────────────────────
        StatStrip(
            stats = listOf(
                compact(today.steps.toDouble()) to "steps",
                "%.1f".format(today.distanceKm) to "km",
                "${today.workoutMinutes}" to "min",
                "${today.activeCalories}" to "kcal"
            )
        )
        Text(
            text = "Steps from Health Connect when connected · calories estimated",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .padding(top = Space.XS)
                .alpha(0.8f)
        )

        Spacer(Modifier.height(Space.XXL))

        // ── Today's movement (edge-to-edge map object) ────────────────────
        SectionHeader(
            label = "Today's movement",
            actionLabel = if (points.isNotEmpty()) "View full route →" else null,
            onAction = if (points.isNotEmpty()) ({ onOpenFullMap() }) else null
        )
        Spacer(Modifier.height(Space.SM))

        if (hasPermission) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(232.dp)
                    .clip(RoundedCornerShape(Radius.XL))
            ) {
                MapWidget(
                    points = points,
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { onOpenFullMap() }
                        ),
                    interactive = false
                )
                // Floating metrics — the map is the object, numbers float on it.
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(Space.MD),
                    shape = RoundedCornerShape(Radius.MD),
                    color = Color.Black.copy(alpha = 0.55f)
                ) {
                    Column(Modifier.padding(horizontal = Space.MD, vertical = Space.SM)) {
                        Text(
                            text = if (routeKm > 0) WorkoutMath.formatDistance(routeKm * 1000) else "—",
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White,
                            fontWeight = FontWeight.Light
                        )
                        Text(
                            text = "MOVED TODAY",
                            style = MetaLabel,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(Space.MD),
                    shape = RoundedCornerShape(50),
                    color = Color.Black.copy(alpha = 0.55f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = Space.SM, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (tracking) "●" else "○",
                            color = if (tracking) EnergyOrange else Color.White.copy(alpha = 0.7f),
                            style = MaterialTheme.typography.labelSmall
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = if (tracking) "Tracking" else "Paused",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White
                        )
                    }
                }
            }
            if (points.isEmpty()) {
                Text(
                    text = "Start moving and today's route will draw itself here.",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = Space.SM)
                )
            }
        } else {
            Surface(
                shape = RoundedCornerShape(Radius.XL),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(Space.LG), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "🗺️", style = MaterialTheme.typography.displayMedium)
                    Text(
                        text = "Your day, drawn on a map",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Location powers routes and the movement map. It never leaves your device.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = Space.XS)
                    )
                    EnergyButton(
                        text = "Enable location",
                        onClick = {
                            launcher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        },
                        modifier = Modifier.padding(top = Space.MD)
                    )
                }
            }
        }

        Spacer(Modifier.height(Space.XXL))

        // ── Streak + health status (quiet footer rows) ────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$streak",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Light,
                color = EnergyOrange
            )
            Spacer(Modifier.width(Space.SM))
            Column {
                Text(
                    text = "day streak",
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = "Move daily to keep it alive",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.weight(1f))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (healthData != null && viewModel.healthAvailable) "♥ connected"
                    else if (viewModel.healthAvailable) "♥ permissions pending"
                    else "♥ no health source",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (healthData != null && viewModel.healthAvailable)
                        MaterialTheme.colorScheme.secondary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(Modifier.height(Space.XXL))
    }
}

@Composable
private fun RingLegendRow(color: Color, label: String, detail: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .width(8.dp)
                .height(8.dp)
                .clip(RoundedCornerShape(50))
                .background(color)
        )
        Spacer(Modifier.width(Space.SM))
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.width(72.dp)
        )
        Text(
            text = detail,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun compact(n: Double): String = when {
    n >= 10_000 -> "%.1fK".format(n / 1_000)
    n >= 1_000 -> "%.1fK".format(n / 1_000)
    else -> n.toInt().toString()
}

private fun greetingForHour(hour: Int): String = when (hour) {
    in 5..11 -> "Good morning"
    in 12..17 -> "Good afternoon"
    else -> "Good evening"
}

private fun dayPathKm(points: List<DayPoint>): Double {
    if (points.size < 2) return 0.0
    var km = 0.0
    for (i in 1 until points.size) {
        km += com.energy.app.data.stats.StatsRepository.haversineKm(
            points[i - 1].lat, points[i - 1].lng, points[i].lat, points[i].lng
        )
    }
    return km
}

private fun hasLocationPermission(context: Context): Boolean =
    ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED
