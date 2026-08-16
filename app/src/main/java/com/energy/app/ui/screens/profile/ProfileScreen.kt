package com.energy.app.ui.screens.profile

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.energy.app.data.cloud.CloudStatus
import com.energy.app.data.settings.Accent
import com.energy.app.data.settings.GpsMode
import com.energy.app.data.settings.Haptics
import com.energy.app.data.settings.RoutePrivacy
import com.energy.app.data.settings.ThemeMode
import com.energy.app.data.settings.Units
import com.energy.app.data.workout.WorkoutMath
import com.energy.app.ui.components.Metric
import com.energy.app.ui.components.SectionHeader
import com.energy.app.ui.theme.EnergyCoral
import com.energy.app.ui.theme.EnergyOrange
import com.energy.app.ui.theme.MetaLabel
import com.energy.app.ui.theme.Space

/**
 * PROFILE (§39) — the control center. Identity + lifetime stats, quick
 * inline controls (theme/accent/haptics/units), then grouped navigation
 * into deep settings screens. Defaults keep it usable with zero config.
 */
@Composable
fun ProfileScreen(
    onSignOut: () -> Unit,
    onOpenContact: () -> Unit = {},
    onOpenWorkoutSettings: () -> Unit = {},
    onOpenDisplay: () -> Unit = {},
    onOpenAudio: () -> Unit = {},
    onOpenNotifications: () -> Unit = {},
    onOpenMaps: () -> Unit = {},
    onOpenHealth: () -> Unit = {},
    onOpenPrivacy: () -> Unit = {},
    onOpenData: () -> Unit = {},
    onOpenAbout: () -> Unit = {},
    viewModel: ProfileViewModel = viewModel()
) {
    val signedOut by viewModel.signedOut.collectAsState()
    val themeMode by viewModel.themeMode.collectAsState()
    val user by viewModel.user.collectAsState()
    val userData = user
    val lifetime by viewModel.lifetime.collectAsState()
    val cloud by viewModel.cloudState.collectAsState()
    val pendingSync by viewModel.pendingSyncCount.collectAsState()
    val streak by viewModel.streak.collectAsState()
    val prefs by viewModel.preferences.collectAsState()

    LaunchedEffect(signedOut) { if (signedOut) onSignOut() }

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
                    .background(
                        Brush.linearGradient(
                            listOf(
                                if (prefs.accent == Accent.ORANGE) EnergyOrange else EnergyCoral,
                                EnergyCoral
                            )
                        )
                    ),
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
                    text = "🔥 $streak day streak · ${activityLevel(lifetime)} · ${prefs.fitnessLevel.label}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
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

        // ── QUICK CONTROLS (inline — no need to open screens) ─────────────
        Spacer(Modifier.height(Space.XXL))
        SettingsSection(label = "Appearance")
        ChipRow(
            options = ThemeMode.entries,
            labelOf = { it.name.lowercase().replaceFirstChar { c -> c.uppercase() } },
            selected = themeMode,
            onSelect = { viewModel.setThemeMode(it) }
        )
        Spacer(Modifier.height(Space.SM))
        ChipRow(
            options = Accent.entries,
            labelOf = { if (it == Accent.ORANGE) "Orange" else "Coral" },
            selected = prefs.accent,
            onSelect = { viewModel.setAccent(it) }
        )
        Spacer(Modifier.height(Space.SM))
        ChipRow(
            options = Haptics.entries,
            labelOf = { it.name.lowercase().replaceFirstChar { c -> c.uppercase() } },
            selected = prefs.haptics,
            onSelect = { viewModel.setHaptics(it) }
        )
        Spacer(Modifier.height(Space.SM))
        ChipRow(
            options = Units.entries,
            labelOf = { if (it == Units.METRIC) "Metric · km" else "Imperial · mi" },
            selected = prefs.units,
            onSelect = { viewModel.setUnits(it) }
        )
        Spacer(Modifier.height(Space.SM))
        ToggleRow(
            title = "Visual effects",
            sub = "Aurora background and entry animations",
            checked = prefs.visualEffects,
            onToggle = { viewModel.setVisualEffects(it) }
        )

        // ── PERSONAL ──────────────────────────────────────────────────────
        SettingsSection(label = "Goals", explain = "Your daily targets — used by rings, score and insights.")
        StepperRow("Step goal", "${prefs.stepGoal / 1000}K",
            onMinus = { viewModel.setStepGoal(prefs.stepGoal - 1_000) },
            onPlus = { viewModel.setStepGoal(prefs.stepGoal + 1_000) })
        StepperRow("Calorie goal", "${prefs.calorieGoal} kcal",
            onMinus = { viewModel.setCalorieGoal(prefs.calorieGoal - 50) },
            onPlus = { viewModel.setCalorieGoal(prefs.calorieGoal + 50) })
        StepperRow("Weight", "${prefs.weightKg} kg",
            onMinus = { viewModel.setWeightKg(prefs.weightKg - 1) },
            onPlus = { viewModel.setWeightKg(prefs.weightKg + 1) })
        StepperRow("Height", "${prefs.heightCm} cm",
            onMinus = { viewModel.setHeightCm(prefs.heightCm - 1) },
            onPlus = { viewModel.setHeightCm(prefs.heightCm + 1) })

        SettingsSection(label = "Personal")
        NavRow("Fitness profile", prefs.fitnessLevel.label,
            onClick = onOpenWorkoutSettings)

        // ── WORKOUT ───────────────────────────────────────────────────────
        SettingsSection(label = "Workout")
        NavRow("Workout settings", summaryOfWorkout(prefs), onClick = onOpenWorkoutSettings)
        NavRow("Workout display", prefs.metricPreset.name.lowercase().replaceFirstChar { it.uppercase() },
            onClick = onOpenDisplay)
        NavRow("Audio & coaching", if (prefs.audioCues) prefs.announceInterval.label else "Off",
            onClick = onOpenAudio)
        NavRow("Maps & GPS", prefs.gpsMode.label, onClick = onOpenMaps)

        // ── HEALTH ────────────────────────────────────────────────────────
        SettingsSection(label = "Health")
        NavRow("Health Connect", cloudHealthPlaceholder(), onClick = onOpenHealth)
        NavRow("Privacy & location", prefs.routePrivacy.label, onClick = onOpenPrivacy)

        // ── NOTIFICATIONS ─────────────────────────────────────────────────
        SettingsSection(label = "Notifications")
        NavRow("Notifications & reminders", quietSummary(prefs), onClick = onOpenNotifications)

        // ── DATA ──────────────────────────────────────────────────────────
        SettingsSection(label = "Data & sync")
        NavRow(
            "Data, export & storage",
            when {
                cloud.status == CloudStatus.SIGNED_IN || cloud.status == CloudStatus.SYNCED -> "Cloud on"
                pendingSync > 0 -> "$pendingSync waiting to sync"
                else -> "Local · works offline"
            },
            onClick = onOpenData
        )

        // ── ACCOUNT ───────────────────────────────────────────────────────
        SettingsSection(label = "Account")
        NavRow("Help & support", "FAQ · GPS · Health Connect", onClick = onOpenContact)
        NavRow("About Energy", "v0.6.0", onClick = onOpenAbout)

        Spacer(Modifier.height(Space.XL))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Spacer(Modifier.height(Space.MD))

        Text(
            text = "Sign out",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier
                .fillMaxWidth()
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.error.copy(alpha = 0.08f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { viewModel.signOut() }
                )
                .padding(vertical = Space.SM),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Text(
            text = "Energy 0.6.3 · local-first · MapLibre + OpenFreeMap",
            style = MetaLabel,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .padding(vertical = Space.MD)
                .align(Alignment.CenterHorizontally)
        )
        Spacer(Modifier.height(Space.LG))
    }
}

private fun cloudHealthPlaceholder(): String = "Steps · HR · sleep"

private fun summaryOfWorkout(p: com.energy.app.data.settings.UserPreferences): String =
    listOfNotNull(
        if (p.autoPause) "auto-pause" else null,
        p.gpsMode.label.lowercase(),
        "countdown ${p.countdownSeconds}s"
    ).joinToString(" · ")

private fun quietSummary(p: com.energy.app.data.settings.UserPreferences): String =
    if (p.quietHoursEnabled)
        "Quiet ${String.format("%02d:%02d", p.quietStart / 100, p.quietStart % 100)}–" +
            String.format("%02d:%02d", p.quietEnd / 100, p.quietEnd % 100)
    else "Reminder · goals · streaks"

@Composable
private fun StepperRow(label: String, value: String, onMinus: () -> Unit, onPlus: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
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

private fun activityLevel(l: com.energy.app.data.stats.LifetimeStats): String = when {
    l.workoutCount >= 100 -> "Seasoned athlete"
    l.workoutCount >= 30 -> "Consistent mover"
    l.workoutCount >= 10 -> "Building the habit"
    l.workoutCount >= 1 -> "Getting started"
    else -> "First workout ahead"
}
