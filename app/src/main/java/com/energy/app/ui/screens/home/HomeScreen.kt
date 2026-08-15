package com.energy.app.ui.screens.home

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.energy.app.data.stats.Achievements
import com.energy.app.ui.components.ActivityRing
import com.energy.app.ui.components.EnergyButton
import com.energy.app.ui.components.HairlineCard
import com.energy.app.ui.components.MapWidget
import com.energy.app.ui.components.ScoreGauge
import com.energy.app.ui.components.SkeletonBox
import com.energy.app.ui.theme.EnergyHairline
import com.energy.app.ui.theme.RingExercise
import com.energy.app.ui.theme.RingMove
import com.energy.app.ui.theme.RingStand
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Oura-style dashboard: Energy Score hero gauge, activity rings with glow,
 * streak chip, stat pills, and the day-movement map. APP_SPEC §5.4.
 */
@Composable
fun HomeScreen(
    onOpenFullMap: () -> Unit = {},
    viewModel: HomeViewModel = viewModel()
) {
    val context = LocalContext.current
    val points by viewModel.points.collectAsState()
    val tracking by viewModel.tracking.collectAsState()
    val health by viewModel.dailyHealth.collectAsState()
    val healthData = health
    val score by viewModel.score.collectAsState()
    val streak by viewModel.streak.collectAsState()

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
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(12.dp))

        // Greeting — muted, Oura-style
        Text(
            text = "Good morning, Runner 👋",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = SimpleDateFormat("EEEE, MMM d", Locale.US).format(Date()),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(28.dp))

        // Hero: Energy Score
        ScoreGauge(score = score.value, size = 230)
        Spacer(Modifier.height(10.dp))
        Text(
            text = score.message,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(20.dp))

        // Streak chip + achievements
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
                        text = if (streak == 1) "day streak 🔥" else "day streak 🔥",
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

        Spacer(Modifier.height(16.dp))

        // Activity rings with soft glow
        HairlineCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                RingWithLabel(progress = 0.62f, color = RingMove, label = "Move")
                RingWithLabel(progress = 0.44f, color = RingExercise, label = "Exercise")
                RingWithLabel(progress = 0.71f, color = RingStand, label = "Stand")
            }
        }

        Spacer(Modifier.height(16.dp))

        // Health pills (Oura-style chips)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            PillChip(
                modifier = Modifier.weight(1f),
                label = "Steps",
                value = if (healthData != null) String.format("%,d", healthData.steps) else "—"
            )
            PillChip(
                modifier = Modifier.weight(1f),
                label = "Heart rate",
                value = healthData?.avgHeartRateBpm?.let { "$it" } ?: "—"
            )
        }

        Spacer(Modifier.height(20.dp))

        // Day movement map
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
                    Text(
                        text = if (tracking) "● Tracking · ${points.size} pts" else "● Paused",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
                if (points.isNotEmpty()) {
                    TextButton(
                        onClick = { viewModel.stopTracking() },
                        modifier = Modifier.align(Alignment.BottomEnd)
                    ) {
                        Text("Pause")
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Tap the map to open full screen",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Column(Modifier.padding(vertical = 8.dp)) {
                Text(
                    text = "See your day drawn on a map — like Strava.",
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

        Spacer(Modifier.height(20.dp))

        // Health Connect block (M2)
        if (healthData != null && viewModel.healthAvailable) {
            HairlineCard {
                Text(
                    text = "Health Connect",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Steps ${healthData.steps} · Avg HR ${healthData.avgHeartRateBpm ?: "—"} bpm",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            HairlineCard {
                Text(
                    text = "M2 · Health Connect",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = if (viewModel.healthAvailable)
                        "Permissions pending — grant access in the Health Connect app."
                    else
                        "Not installed on this device.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(Modifier.height(24.dp))
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
private fun PillChip(modifier: Modifier = Modifier, label: String, value: String) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, EnergyHairline, RoundedCornerShape(50))
            .padding(horizontal = 18.dp, vertical = 14.dp)
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
        }
    }
}

private fun hasLocationPermission(context: Context): Boolean =
    ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED
