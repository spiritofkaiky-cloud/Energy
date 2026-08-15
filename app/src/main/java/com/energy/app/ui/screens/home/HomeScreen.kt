package com.energy.app.ui.screens.home

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.energy.app.data.location.DayPoint
import com.energy.app.data.stats.Achievements
import com.energy.app.data.stats.ScoreFactor
import com.energy.app.data.workout.WorkoutMath
import com.energy.app.data.workout.WorkoutType
import com.energy.app.ui.components.ActivityRing
import com.energy.app.ui.components.EnergyButton
import com.energy.app.ui.components.HairlineCard
import com.energy.app.ui.components.MapWidget
import com.energy.app.ui.components.ScoreGauge
import com.energy.app.ui.theme.EnergyHairline
import com.energy.app.ui.theme.EnergyOrange
import com.energy.app.ui.theme.RingExercise
import com.energy.app.ui.theme.RingMove
import com.energy.app.ui.theme.RingStand
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * The strongest screen in the app (APP_SPEC §6): answers
 * "How am I doing today?" with the Energy Score hero, real activity rings,
 * today's stats, a movement map, and an explainable daily recommendation.
 * Everything shown is computed from real signals — nothing hardcoded.
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

    var showScoreDetails by remember { mutableStateOf(false) }
    var showRingDetails by remember { mutableStateOf(false) }

    var hasPermission by remember {
        mutableStateOf(hasLocationPermission(context))
    }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        hasPermission = result.values.any { it }
        if (hasPermission) viewModel.startTracking()
    }

    LaunchedEffect(hasPermission) {
        if (hasPermission) viewModel.startTracking()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(12.dp))

        // ── Header: greeting + date ───────────────────────────────────────
        Text(
            text = "${greetingForHour(Calendar.getInstance().get(Calendar.HOUR_OF_DAY))}, $userName",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = SimpleDateFormat("EEEE, MMM d", Locale.US).format(Date()),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(24.dp))

        // ── Active workout banner ─────────────────────────────────────────
        AnimatedVisibility(visible = activeWorkout) {
            Surface(
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onResumeWorkout(workoutType)
                },
                shape = MaterialTheme.shapes.medium,
                color = EnergyOrange.copy(alpha = 0.16f),
                border = androidx.compose.foundation.BorderStroke(1.dp, EnergyOrange.copy(alpha = 0.5f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = workoutType.emoji,
                        style = MaterialTheme.typography.titleLarge
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = if (workoutState == com.energy.app.data.workout.WorkoutState.PAUSED)
                                "Workout paused — tap to resume" else "Workout in progress",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Your session survives restarts — pick up where you left off.",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // ── Hero: Energy Score ────────────────────────────────────────────
        Surface(
            onClick = {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                showScoreDetails = !showScoreDetails
            },
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surface,
            border = androidx.compose.foundation.BorderStroke(1.dp, EnergyHairline),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                Modifier.padding(vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                ScoreGauge(score = score.value, size = 210)
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = score.category,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    score.trendVs7Day?.let { trend ->
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = if (trend >= 0) "▲ +$trend" else "▼ ${-trend}",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (trend >= 0) com.energy.app.ui.theme.EnergyMint
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "vs 7-day avg",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Estimate from your activity — not a medical measurement.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.alpha(0.8f)
                )
                AnimatedVisibility(
                    visible = showScoreDetails,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column(Modifier.padding(horizontal = 24.dp, vertical = 4.dp)) {
                        Spacer(Modifier.height(12.dp))
                        score.factors.forEach { f ->
                            FactorRow(f)
                            Spacer(Modifier.height(8.dp))
                        }
                        Text(
                            text = "Tap the score to collapse.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // ── Daily recommendation ──────────────────────────────────────────
        HairlineCard {
            Row(verticalAlignment = Alignment.Top) {
                Text(text = "💡", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Today's take",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = score.recommendation.text,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = score.recommendation.basis,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // ── Activity rings (real data) ────────────────────────────────────
        Surface(
            onClick = {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                showRingDetails = !showRingDetails
            },
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surface,
            border = androidx.compose.foundation.BorderStroke(1.dp, EnergyHairline),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(vertical = 20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RingWithLabel(progress = rings.move, color = RingMove, label = "Move")
                    RingWithLabel(progress = rings.exercise, color = RingExercise, label = "Exercise")
                    RingWithLabel(progress = rings.stand, color = RingStand, label = "Stand")
                }
                AnimatedVisibility(
                    visible = showRingDetails,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column(Modifier.padding(horizontal = 24.dp)) {
                        Spacer(Modifier.height(12.dp))
                        RingDetailLine(color = RingMove, text = "Move · ${rings.moveDetail}")
                        RingDetailLine(color = RingExercise, text = "Exercise · ${rings.exerciseDetail}")
                        RingDetailLine(color = RingStand, text = "Stand · ${rings.standDetail}")
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Rings are estimates from your workouts, steps and movement.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Tap a ring for details",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // ── Today's stats ─────────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            PillChip(
                modifier = Modifier.weight(1f),
                label = "Steps",
                value = String.format("%,d", today.steps),
                sub = if (healthData == null) "no source" else "of ${prefs.stepGoal}"
            )
            PillChip(
                modifier = Modifier.weight(1f),
                label = "Distance",
                value = WorkoutMath.formatDistance(today.distanceKm * 1000),
                sub = "workouts + movement"
            )
        }
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            PillChip(
                modifier = Modifier.weight(1f),
                label = "Active kcal",
                value = "${today.activeCalories}",
                sub = "est. from activity"
            )
            PillChip(
                modifier = Modifier.weight(1f),
                label = "Avg HR",
                value = today.heartRateBpm?.let { "$it" } ?: "—",
                sub = today.heartRateBpm?.let { "bpm · Health Connect" } ?: "no source yet"
            )
        }

        Spacer(Modifier.height(20.dp))

        // ── Today's movement map ──────────────────────────────────────────
        Text(
            text = "Today's movement",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(10.dp))
        if (hasPermission) {
            Box {
                MapWidget(
                    points = points,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .clip(MaterialTheme.shapes.large)
                        .border(1.dp, EnergyHairline, MaterialTheme.shapes.large)
                        .clickable { onOpenFullMap() }
                )
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(10.dp),
                    shape = RoundedCornerShape(50),
                    color = Color.Black.copy(alpha = 0.55f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (tracking) "●" else "○",
                            color = if (tracking) EnergyOrange else Color.White.copy(alpha = 0.7f),
                            style = MaterialTheme.typography.labelMedium
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = if (tracking) "Tracking" else "Paused",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            MovementSummaryLine(points = points, today = today)
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Tap the map to open full screen · day tracking pauses in the background to save battery",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Surface(
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surface,
                border = androidx.compose.foundation.BorderStroke(1.dp, EnergyHairline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(20.dp)) {
                    Text(
                        text = "🗺️",
                        style = MaterialTheme.typography.displaySmall
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Start moving and today's route will appear here.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(12.dp))
                    EnergyButton(
                        text = "Enable day tracking",
                        onClick = {
                            launcher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        // ── Streak ────────────────────────────────────────────────────────
        HairlineCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "$streak",
                        style = MaterialTheme.typography.headlineLarge
                    )
                    Text(
                        text = "day streak 🔥",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Achievements.forEach { a ->
                    val unlocked = streak >= a.days
                    Text(
                        text = a.emoji,
                        style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier.alpha(if (unlocked) 1f else 0.25f)
                    )
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        // ── Health Connect status ─────────────────────────────────────────
        if (healthData != null && viewModel.healthAvailable) {
            HairlineCard {
                Text(
                    text = "Health Connect · connected",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Steps, heart rate and more sync from your device's health store.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            HairlineCard {
                Text(
                    text = "Health Connect",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = if (viewModel.healthAvailable)
                        "Permissions pending — grant access in the Health Connect app to see steps and heart rate."
                    else
                        "Not installed on this device. Steps and heart rate stay empty until a health source is connected.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

private fun greetingForHour(hour: Int): String = when (hour) {
    in 5..11 -> "Good morning"
    in 12..17 -> "Good afternoon"
    else -> "Good evening"
}

@Composable
private fun FactorRow(f: ScoreFactor) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(f.label, style = MaterialTheme.typography.bodyMedium)
            Text(
                f.detail,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text = "${f.points} pts",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = if (f.points >= 0) MaterialTheme.colorScheme.primary
            else com.energy.app.ui.theme.EnergyCoral
        )
    }
}

@Composable
private fun RingDetailLine(color: Color, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 3.dp)) {
        Box(
            Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(Modifier.width(10.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun RingWithLabel(progress: Float, color: Color, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        ActivityRing(progress = progress, color = color, sizeDp = 86, glow = true)
        Spacer(Modifier.height(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun PillChip(modifier: Modifier = Modifier, label: String, value: String, sub: String? = null) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, EnergyHairline, RoundedCornerShape(20.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            sub?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.alpha(0.8f)
                )
            }
        }
    }
}

@Composable
private fun MovementSummaryLine(points: List<DayPoint>, today: TodayStats) {
    val km = StatsPathLength.km(points)
    val dominant = today.workoutMinutes
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "${WorkoutMath.formatDistance(km * 1000)} of movement today",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (dominant > 0) {
            Text(
                text = "🏃 $dominant min workout time",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** Small pure helper so Home can compute map-card distance without the VM. */
private object StatsPathLength {
    fun km(points: List<DayPoint>): Double {
        if (points.size < 2) return 0.0
        var km = 0.0
        for (i in 1 until points.size) {
            km += com.energy.app.data.stats.StatsRepository.haversineKm(
                points[i - 1].lat, points[i - 1].lng, points[i].lat, points[i].lng
            )
        }
        return km
    }
}

private fun hasLocationPermission(context: Context): Boolean =
    ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED
