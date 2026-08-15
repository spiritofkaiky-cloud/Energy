package com.energy.app.ui.screens.profile

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.energy.app.data.settings.NotificationPrefs
import com.energy.app.ui.theme.Space

/**
 * NOTIFICATIONS + QUIET HOURS (§3–5). Category-level controls; the daily
 * reminder lives here too. Quiet hours suppress non-critical notifications
 * without ever interfering with an active workout.
 */
@Composable
fun NotificationSettingsScreen(
    onBack: () -> Unit,
    viewModel: ProfileViewModel = viewModel()
) {
    val prefs by viewModel.preferences.collectAsState()
    val alarm by viewModel.alarm.collectAsState()
    val n = prefs.notifications

    SettingsScaffold(title = "Notifications", onBack = onBack) {
        SettingsSection(label = "Reminder", explain = "One daily nudge to move. That's enough.")
        ToggleRow(
            title = "Daily workout reminder",
            sub = if (alarm.enabled) "Every day at ${String.format("%02d:%02d", alarm.hour, alarm.minute)}"
            else "Off",
            checked = alarm.enabled,
            onToggle = { viewModel.setAlarmEnabled(it) }
        )
        if (alarm.enabled) {
            TextButton(onClick = {
                val total = alarm.hour * 60 + alarm.minute + 30
                viewModel.setAlarmTime((total / 60) % 24, total % 60)
            }) {
                Text("Shift time by 30 min", color = MaterialTheme.colorScheme.primary)
            }
        }

        SettingsSection(label = "Categories", explain = "Energy only sends what you ask for.")
        CatRow("Workout completed", n.workoutComplete) {
            viewModel.setNotifications(n.copy(workoutComplete = it))
        }
        CatRow("Goal progress & completion", n.goalProgress && n.goalComplete) {
            viewModel.setNotifications(n.copy(goalProgress = it, goalComplete = it))
        }
        CatRow("Streaks", n.streak) {
            viewModel.setNotifications(n.copy(streak = it))
        }
        CatRow("Achievements & records", n.achievements) {
            viewModel.setNotifications(n.copy(achievements = it))
        }
        CatRow("Recovery & rest days", n.recovery) {
            viewModel.setNotifications(n.copy(recovery = it))
        }
        CatRow("Sync issues", n.syncIssues) {
            viewModel.setNotifications(n.copy(syncIssues = it))
        }

        SettingsSection(label = "Quiet hours", explain = "Suppresses non-critical notifications during this window. Active workouts are never silenced.")
        ToggleRow(
            title = "Enable quiet hours",
            sub = "Reminders and achievement sounds stay silent",
            checked = prefs.quietHoursEnabled,
            onToggle = { viewModel.setQuietHours(it, prefs.quietStart, prefs.quietEnd) }
        )
        if (prefs.quietHoursEnabled) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Start", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                TextButton(onClick = {
                    val s = (prefs.quietStart + 100) % 2400
                    viewModel.setQuietHours(true, s, prefs.quietEnd)
                }) { Text(String.format("%02d:%02d", prefs.quietStart / 100, prefs.quietStart % 100)) }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("End", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                TextButton(onClick = {
                    val e = (prefs.quietEnd + 100) % 2400
                    viewModel.setQuietHours(true, prefs.quietStart, e)
                }) { Text(String.format("%02d:%02d", prefs.quietEnd / 100, prefs.quietEnd % 100)) }
            }
        }
    }
}

@Composable
private fun CatRow(title: String, checked: Boolean, onToggle: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Space.XS),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        androidx.compose.material3.Switch(checked = checked, onCheckedChange = onToggle)
    }
    Spacer(Modifier.height(2.dp))
}
