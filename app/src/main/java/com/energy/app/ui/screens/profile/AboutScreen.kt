package com.energy.app.ui.screens.profile

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.energy.app.ui.theme.Space

/**
 * ABOUT (§29): version, stack acknowledgements, privacy stance.
 */
@Composable
fun AboutScreen(onBack: () -> Unit) {
    SettingsScaffold(title = "About", onBack = onBack) {
        SettingsSection(label = "Energy")
        Text(
            text = "Version 0.6.0",
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(Modifier.height(Space.XS))
        Text(
            text = "A local-first fitness companion. Your workouts, routes and " +
                "health data live on this device by default. Cloud sync is opt-in.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        SettingsSection(label = "Built with")
        listOf(
            "Kotlin + Jetpack Compose" to "UI",
            "MapLibre + OpenFreeMap" to "Maps",
            "Android Health Connect" to "Health data",
            "Google Play Services Location" to "GPS",
            "Supabase (optional)" to "Cloud sync"
        ).forEach { (name, role) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp)
            ) {
                Text(name, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                Text(
                    role,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        SettingsSection(label = "Privacy")
        NoticeRow(
            "Energy Score is an estimate from your activity — not a medical " +
                "measurement. Location data never leaves your device without " +
                "your choice."
        )
    }
}
