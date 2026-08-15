package com.energy.app.ui.screens.profile

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.energy.app.data.cloud.CloudStatus
import com.energy.app.data.settings.ThemeMode
import com.energy.app.data.settings.Units
import com.energy.app.data.workout.WorkoutMath
import com.energy.app.ui.components.EnergyButton
import com.energy.app.ui.components.Metric
import com.energy.app.ui.components.SectionHeader
import com.energy.app.ui.theme.EnergyCoral
import com.energy.app.ui.theme.EnergyOrange
import com.energy.app.ui.theme.MetaLabel
import com.energy.app.ui.theme.Space

/**
 * PROFILE (§25) — a personal control center: identity + lifetime numbers up
 * top, grouped settings below. Sections with hairline dividers, not a form.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onSignOut: () -> Unit,
    onOpenContact: () -> Unit = {},
    viewModel: ProfileViewModel = viewModel()
) {
    val context = LocalContext.current
    val signedOut by viewModel.signedOut.collectAsState()
    val themeMode by viewModel.themeMode.collectAsState()
    val alarm by viewModel.alarm.collectAsState()
    val user by viewModel.user.collectAsState()
    val userData = user
    val lifetime by viewModel.lifetime.collectAsState()
    val cloud by viewModel.cloudState.collectAsState()
    val pendingSync by viewModel.pendingSyncCount.collectAsState()
    val streak by viewModel.streak.collectAsState()
    val prefs by viewModel.preferences.collectAsState()

    var showTimePicker by remember { mutableStateOf(false) }

    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    LaunchedEffect(signedOut) {
        if (signedOut) onSignOut()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Space.XL)
    ) {
        Spacer(Modifier.height(Space.LG))

        // ── Identity ──────────────────────────────────────────────────────
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(EnergyOrange, EnergyCoral))),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = (userData?.name?.firstOrNull()?.uppercaseChar() ?: 'E').toString(),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            Spacer(Modifier.width(Space.MD))
            Column {
                Text(
                    text = userData?.name ?: "Runner",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = when {
                        userData?.isGuest == true -> "Guest · data stays on this device"
                        userData?.email != null -> userData.email.orEmpty()
                        else -> "Energy athlete"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "🔥 $streak day streak · ${activityLevel(lifetime)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = EnergyOrange,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }

        Spacer(Modifier.height(Space.XL))

        // ── Lifetime ──────────────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Metric("${lifetime.workoutCount}", "Workouts", valueStyle = MaterialTheme.typography.titleLarge)
            Metric("%.1f".format(lifetime.totalKm), "km total", valueStyle = MaterialTheme.typography.titleLarge)
            Metric(
                lifetime.bestPaceSecondsPerKm?.let {
                    WorkoutMath.formatPace(it).replace(" /km", "")
                } ?: "—",
                "Best pace",
                valueStyle = MaterialTheme.typography.titleLarge
            )
        }

        Spacer(Modifier.height(Space.XXL))

        // ── GOALS ─────────────────────────────────────────────────────────
        SectionHeader(label = "Goals")
        Spacer(Modifier.height(Space.XS))
        StepperRow("Daily step goal", String.format("%,d", prefs.stepGoal),
            onMinus = { viewModel.setStepGoal(prefs.stepGoal - 1_000) },
            onPlus = { viewModel.setStepGoal(prefs.stepGoal + 1_000) })
        StepperRow("Daily calorie goal", "${prefs.calorieGoal} kcal",
            onMinus = { viewModel.setCalorieGoal(prefs.calorieGoal - 50) },
            onPlus = { viewModel.setCalorieGoal(prefs.calorieGoal + 50) })
        StepperRow("Weight", "${prefs.weightKg} kg",
            onMinus = { viewModel.setWeightKg(prefs.weightKg - 1) },
            onPlus = { viewModel.setWeightKg(prefs.weightKg + 1) })

        Spacer(Modifier.height(Space.XXL))

        // ── WORKOUT ───────────────────────────────────────────────────────
        SectionHeader(label = "Workout")
        Spacer(Modifier.height(Space.XS))
        ToggleRow(
            title = "Auto-pause workouts",
            sub = "Pause when you stop moving",
            checked = prefs.autoPause,
            onToggle = { viewModel.setAutoPause(it) }
        )
        ToggleRow(
            title = "Battery saver",
            sub = "Wider GPS intervals while idle",
            checked = prefs.batterySaver,
            onToggle = { viewModel.setBatterySaver(it) }
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = Space.SM),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Units", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            FilterChip(selected = prefs.units == Units.METRIC,
                onClick = { viewModel.setUnits(Units.METRIC) }, label = { Text("km") })
            Spacer(Modifier.width(Space.XS))
            FilterChip(selected = prefs.units == Units.IMPERIAL,
                onClick = { viewModel.setUnits(Units.IMPERIAL) }, label = { Text("mi") })
        }

        Spacer(Modifier.height(Space.XXL))

        // ── APPEARANCE ────────────────────────────────────────────────────
        SectionHeader(label = "Appearance")
        Spacer(Modifier.height(Space.XS))
        Row(horizontalArrangement = Arrangement.spacedBy(Space.XS)) {
            ThemeMode.entries.forEach { mode ->
                FilterChip(
                    selected = themeMode == mode,
                    onClick = { viewModel.setThemeMode(mode) },
                    label = { Text(mode.name.lowercase().replaceFirstChar { it.uppercase() }) }
                )
            }
        }

        Spacer(Modifier.height(Space.XXL))

        // ── REMINDER ──────────────────────────────────────────────────────
        SectionHeader(label = "Exercise reminder")
        Spacer(Modifier.height(Space.XS))
        ToggleRow(
            title = "Daily workout reminder",
            sub = if (alarm.enabled)
                "Every day at ${String.format("%02d:%02d", alarm.hour, alarm.minute)}"
            else "Nudge me to move at the same time every day",
            checked = alarm.enabled,
            onToggle = { enabled ->
                if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
                viewModel.setAlarmEnabled(enabled)
            }
        )
        if (alarm.enabled) {
            TextButton(onClick = { showTimePicker = true }) {
                Text("Change time", color = MaterialTheme.colorScheme.primary)
            }
        }

        // ── CLOUD ─────────────────────────────────────────────────────────
        if (cloud.status != CloudStatus.NOT_CONFIGURED) {
            Spacer(Modifier.height(Space.XXL))
            SectionHeader(label = "Cloud sync")
            Spacer(Modifier.height(Space.XS))
            Text(
                text = when (cloud.status) {
                    CloudStatus.SIGNED_IN, CloudStatus.SYNCED ->
                        "Signed in as ${cloud.userEmail ?: "your account"}"
                    CloudStatus.SIGNED_OUT -> "Not signed in — workouts stay on this device"
                    else -> "Syncing…"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (pendingSync > 0) {
                TextButton(onClick = { viewModel.retryPendingSync() }) {
                    Text("Retry $pendingSync pending sync", color = MaterialTheme.colorScheme.primary)
                }
            }
        }

        Spacer(Modifier.height(Space.XXL))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Spacer(Modifier.height(Space.MD))
        TextButton(onClick = onOpenContact) { Text("Help & contact") }
        EnergyButton(
            text = "Sign out",
            onClick = { viewModel.signOut() },
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = "Energy 0.5.1 · local-first · MapLibre + OpenFreeMap",
            style = MetaLabel,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .padding(vertical = Space.MD)
                .align(Alignment.CenterHorizontally)
        )
        Spacer(Modifier.height(Space.LG))
    }

    if (showTimePicker) {
        val pickerState = rememberTimePickerState(
            initialHour = alarm.hour,
            initialMinute = alarm.minute,
            is24Hour = false
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.setAlarmTime(pickerState.hour, pickerState.minute)
                    showTimePicker = false
                }) { Text("Set") }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("Cancel") }
            },
            text = { TimePicker(state = pickerState) }
        )
    }
}

@Composable
private fun StepperRow(label: String, value: String, onMinus: () -> Unit, onPlus: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Space.SM),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        TextButton(onClick = onMinus) { Text("−") }
        Text(
            value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium
        )
        TextButton(onClick = onPlus) { Text("+") }
    }
}

@Composable
private fun ToggleRow(title: String, sub: String, checked: Boolean, onToggle: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Space.SM),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium)
            Text(
                sub,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(checked = checked, onCheckedChange = onToggle)
    }
}

private fun activityLevel(lifetime: com.energy.app.data.stats.LifetimeStats): String = when {
    lifetime.workoutCount >= 100 -> "Seasoned athlete"
    lifetime.workoutCount >= 30 -> "Consistent mover"
    lifetime.workoutCount >= 10 -> "Building the habit"
    lifetime.workoutCount >= 1 -> "Getting started"
    else -> "First workout ahead"
}
