package com.energy.app.ui.screens.profile

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.energy.app.EnergyApplication
import com.energy.app.data.settings.RoutePrivacy
import com.energy.app.data.workout.ExportManager
import com.energy.app.ui.theme.EnergyMint
import com.energy.app.ui.theme.Space
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * HEALTH + DATA (§11–14). Real statuses: Health Connect availability,
 * data sources, local storage used, per-workout export (JSON/CSV/GPX via
 * share sheet) and destructive actions behind explicit confirmations.
 */
@Composable
fun HealthDataScreen(
    onBack: () -> Unit,
    viewModel: ProfileViewModel = viewModel()
) {
    val context = LocalContext.current
    val container = (context.applicationContext as EnergyApplication).container
    val prefs by viewModel.preferences.collectAsState()
    val lifetime by viewModel.lifetime.collectAsState()
    val healthAvailable = container.healthRepository.available
    val scope = rememberCoroutineScope()

    var exportMenu by remember { mutableStateOf<com.energy.app.data.workout.SavedWorkout?>(null) }
    var confirmDeleteAll by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf(false) }

    val workouts = remember { mutableStateOf<List<com.energy.app.data.workout.SavedWorkout>>(emptyList()) }
    LaunchedEffect(Unit) {
        workouts.value = container.workoutRepository.workouts.first()
    }

    val usedBytes = remember(workouts.value.size) {
        com.energy.app.data.settings.SettingsRepository.workoutsDirSizeBytes(context)
    }

    SettingsScaffold(title = "Health & Data", onBack = onBack) {
        SettingsSection(
            label = "Health Connect",
            explain = "Energy reads steps, heart rate and workouts from Health Connect — with your permission."
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = if (healthAvailable) "●" else "○",
                color = if (healthAvailable) EnergyMint else MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.width(Space.XS))
            Text(
                text = if (healthAvailable) "Available on this device"
                else "Not available — health data can't be read here",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        NoticeRow(
            "Data sources: Health Connect (steps, heart rate), phone GPS " +
                "(routes, distance), Energy itself (workouts). Future sources: " +
                "wearables and heart-rate straps."
        )

        SettingsSection(
            label = "Export",
            explain = "Take your workouts with you. Formats: JSON (full data), CSV (splits), GPX (route)."
        )
        if (workouts.value.isEmpty()) {
            NoticeRow("No workouts to export yet.")
        } else {
            workouts.value.take(8).forEach { w ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${w.type.emoji} ${w.type.label} · " +
                            com.energy.app.data.workout.WorkoutMath.formatDistance(w.distanceMeters),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = { exportMenu = w }) { Text("Export") }
                }
            }
        }

        SettingsSection(label = "Storage", explain = "Workout routes stored on this device.")
        Text(
            text = "%.1f MB".format(usedBytes / 1_048_576.0),
            style = MaterialTheme.typography.titleLarge
        )

        SettingsSection(label = "Danger zone")
        TextButton(onClick = { confirmDeleteAll = true }) {
            Text(
                "Delete all workouts",
                color = MaterialTheme.colorScheme.error
            )
        }
    }

    // ── Export share sheet ────────────────────────────────────────────────
    exportMenu?.let { w ->
        AlertDialog(
            onDismissRequest = { exportMenu = null },
            title = { Text("Export ${w.type.label}") },
            text = {
                Column {
                    Text("Route privacy: ${prefs.routePrivacy.label}")
                    Spacer(Modifier.height(Space.XS))
                    listOf("json" to "JSON — full data", "csv" to "CSV — km splits", "gpx" to "GPX — route")
                        .forEach { (fmt, label) ->
                            TextButton(
                                onClick = {
                                    share(context, w, prefs.routePrivacy, fmt)
                                    exportMenu = null
                                }
                            ) { Text(label, modifier = Modifier.fillMaxWidth()) }
                        }
                }
            },
            confirmButton = {
                TextButton(onClick = { exportMenu = null }) { Text("Cancel") }
            }
        )
    }

    if (confirmDeleteAll) {
        AlertDialog(
            onDismissRequest = { confirmDeleteAll = false },
            title = { Text("Delete all ${lifetime.workoutCount} workouts?") },
            text = { Text("Every route and stat is removed from this device. This cannot be undone. Exported files elsewhere are not affected.") },
            confirmButton = {
                TextButton(onClick = {
                    deleting = true
                    scope.launch {
                        container.eraseAllLocalData()
                        deleting = false
                        confirmDeleteAll = false
                        viewModel.refresh()
                    }
                }) {
                    Text(
                        if (deleting) "Deleting…" else "Delete everything",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDeleteAll = false }) { Text("Keep my data") }
            }
        )
    }
}

private fun share(context: android.content.Context, w: com.energy.app.data.workout.SavedWorkout, privacy: RoutePrivacy, format: String) {
    runCatching {
        val file = ExportManager(context.cacheDir).exportWorkout(w, privacy, format)
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = when (format) {
                "gpx" -> "application/gpx+xml"
                "csv" -> "text/csv"
                else -> "application/json"
            }
            putExtra(android.content.Intent.EXTRA_STREAM, uri)
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(android.content.Intent.createChooser(intent, "Export workout"))
    }
}
