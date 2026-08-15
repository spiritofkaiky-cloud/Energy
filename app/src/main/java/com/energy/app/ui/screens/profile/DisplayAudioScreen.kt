package com.energy.app.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.energy.app.data.settings.AnnounceInterval
import com.energy.app.data.settings.MetricPreset
import com.energy.app.ui.theme.Space

/**
 * WORKOUT DISPLAY + AUDIO & COACHING (§8, §2).
 * Metric presets control the live screen; audio cues are real TTS
 * announcements during workouts (off by default — never nag).
 */
@Composable
fun DisplayAudioScreen(
    onBack: () -> Unit,
    viewModel: ProfileViewModel = viewModel()
) {
    val prefs by viewModel.preferences.collectAsState()

    SettingsScaffold(title = "Display & Audio", onBack = onBack) {
        SettingsSection(
            label = "Live metrics",
            explain = "What's shown while you train."
        )
        MetricPreset.entries.forEach { p ->
            PresetRow(
                preset = p,
                selected = prefs.metricPreset == p,
                onSelect = { viewModel.setMetricPreset(p) }
            )
        }

        SettingsSection(
            label = "Audio & coaching",
            explain = "Spoken announcements during workouts. Off by default — Energy never talks over your music uninvited."
        )
        ToggleRow(
            title = "Audio cues",
            sub = "Announce distance and time milestones.",
            checked = prefs.audioCues,
            onToggle = { viewModel.setAudioCues(it) }
        )
        ChipRow(
            options = AnnounceInterval.entries,
            labelOf = { it.label },
            selected = prefs.announceInterval,
            onSelect = { viewModel.setAnnounceInterval(it) }
        )
    }
}

@Composable
private fun PresetRow(preset: MetricPreset, selected: Boolean, onSelect: () -> Unit) {
    val (primary, secondaries) = when (preset) {
        MetricPreset.MINIMAL -> "Distance" to "Time · Pace"
        MetricPreset.RUNNER -> "Pace" to "Distance · Heart rate · Time"
        MetricPreset.PERFORMANCE -> "Current pace" to "Average pace · Heart rate · Lap"
        MetricPreset.FULL -> "All" to "Pace · speed · HR · distance · time"
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Space.SM)
            .then(
                Modifier.background(
                    if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
                    else androidx.compose.ui.graphics.Color.Transparent,
                    androidx.compose.foundation.shape.RoundedCornerShape(Space.SM)
                )
            )
            .padding(Space.SM)
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = preset.name.lowercase().replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = secondaries,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = if (selected) "●" else "○",
            color = if (selected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .androidxClickable(onSelect)
        )
    }
    Spacer(Modifier.height(1.dp))
}

@Composable
private fun Modifier.androidxClickable(onClick: () -> Unit): Modifier =
    this.then(
        clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = onClick
        )
    )
