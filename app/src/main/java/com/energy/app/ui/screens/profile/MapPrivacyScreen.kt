package com.energy.app.ui.screens.profile

import androidx.compose.foundation.layout.Spacer
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
import com.energy.app.data.settings.RoutePrivacy
import com.energy.app.ui.theme.Space

/**
 * MAPS + LOCATION PRIVACY (§9–10, §22). Real controls only: route color,
 * speed coloring, and route privacy for exports. Location permission state
 * is shown honestly.
 */
@Composable
fun MapPrivacyScreen(
    onBack: () -> Unit,
    viewModel: ProfileViewModel = viewModel()
) {
    val prefs by viewModel.preferences.collectAsState()

    SettingsScaffold(title = "Maps & Privacy", onBack = onBack) {
        SettingsSection(label = "Route style", explain = "How your route looks on the map.")
        ToggleRow(
            title = "Accent-colored route",
            sub = "Draw routes in the Energy accent",
            checked = prefs.routeColorAccent,
            onToggle = { viewModel.setRouteColorAccent(it) }
        )
        ToggleRow(
            title = "Speed-colored route",
            sub = "Faster segments glow brighter",
            checked = prefs.speedColorRoute,
            onToggle = { viewModel.setSpeedColorRoute(it) }
        )

        SettingsSection(
            label = "Location privacy",
            explain = "Energy uses your location to record routes and calculate distance. Location never leaves your device."
        )
        ChipRow(
            options = RoutePrivacy.entries,
            labelOf = { it.label },
            selected = prefs.routePrivacy,
            onSelect = { viewModel.setRoutePrivacy(it) }
        )
        Text(
            text = prefs.routePrivacy.sub,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp)
        )
        Spacer(Modifier.height(Space.MD))
        NoticeRow(
            "Route privacy affects exported files (GPX/JSON). Routes are always " +
                "stored locally on this device unless you choose cloud sync."
        )
        Spacer(Modifier.height(Space.MD))
        NoticeRow(
            "Tip: pick Approximate if your workouts start and end at home — the " +
                "first and last 150 m are trimmed from exports."
        )
    }
}
