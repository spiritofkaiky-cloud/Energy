package com.energy.app.ui.screens.profile

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.energy.app.data.settings.FitnessLevel
import com.energy.app.data.settings.GpsMode
import com.energy.app.data.settings.UserPreferences
import com.energy.app.data.workout.WorkoutType
import com.energy.app.ui.theme.Space

/**
 * WORKOUT SETTINGS (§17–18): behavior controls + fitness profile.
 * Every setting is wired to real behavior (auto-pause, GPS intervals,
 * countdown, keep-awake, finish confirmation).
 */
@Composable
fun WorkoutSettingsScreen(
    onBack: () -> Unit,
    viewModel: ProfileViewModel = viewModel()
) {
    val prefs by viewModel.preferences.collectAsState()

    SettingsScaffold(title = "Workout", onBack = onBack) {
        SettingsSection(
            label = "Behavior",
            explain = "How workouts start, run and end."
        )
        ToggleRow(
            title = "Auto-pause",
            sub = "Automatically pauses your workout when you stop moving.",
            checked = prefs.autoPause,
            onToggle = { viewModel.setAutoPause(it) }
        )
        ToggleRow(
            title = "Keep screen awake",
            sub = "The display stays on while you're recording.",
            checked = prefs.keepScreenAwake,
            onToggle = { viewModel.setKeepScreenAwake(it) }
        )
        ToggleRow(
            title = "Confirm before finishing",
            sub = "Ask for a second tap before ending a workout.",
            checked = prefs.confirmFinish,
            onToggle = { viewModel.setConfirmFinish(it) }
        )
        SettingsSection(label = "Start countdown", explain = "Seconds before tracking begins.")
        ChipRow(
            options = listOf(0, 3, 5, 10),
            labelOf = { if (it == 0) "None" else "$it s" },
            selected = prefs.countdownSeconds,
            onSelect = { viewModel.setCountdownSeconds(it) }
        )
        SettingsSection(label = "GPS accuracy", explain = "Affects idle tracking; active workouts always record precisely.")
        ChipRow(
            options = GpsMode.entries,
            labelOf = { it.label },
            selected = prefs.gpsMode,
            onSelect = { viewModel.setGpsMode(it) }
        )
        Text(
            text = prefs.gpsMode.sub,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp)
        )

        SettingsSection(label = "Fitness profile", explain = "Used to personalize recommendations. Not a medical assessment.")
        ChipRow(
            options = FitnessLevel.entries,
            labelOf = { it.label },
            selected = prefs.fitnessLevel,
            onSelect = { viewModel.setFitnessLevel(it) }
        )
        Spacer(Modifier.height(Space.SM))
        Text(
            text = "Preferred activity",
            style = MaterialTheme.typography.bodyMedium
        )
        ChipRow(
            options = WorkoutType.entries,
            labelOf = { "${it.emoji} ${it.label}" },
            selected = prefs.preferredActivity,
            onSelect = { viewModel.setPreferredActivity(it) }
        )
        Spacer(Modifier.height(Space.SM))
        Text(
            text = "Default workout type",
            style = MaterialTheme.typography.bodyMedium
        )
        ChipRow(
            options = WorkoutType.entries,
            labelOf = { "${it.emoji} ${it.label}" },
            selected = prefs.defaultWorkoutType,
            onSelect = { viewModel.setDefaultWorkoutType(it) }
        )
    }
}
