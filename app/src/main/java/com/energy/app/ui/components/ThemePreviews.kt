package com.energy.app.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.energy.app.data.stats.EnergyScore
import com.energy.app.data.stats.Recommendation
import com.energy.app.data.stats.ScoreFactor
import com.energy.app.ui.theme.EnergyTheme

/**
 * Dual-theme previews (§21): the two most-reused components, rendered in
 * both light and dark so theme regressions are visible in Android Studio
 * without launching the app.
 */
private val sampleScore = EnergyScore(
    value = 82,
    category = "Strong",
    trendVs7Day = 6,
    factors = listOf(
        ScoreFactor("Activity", 8, 10, "2 workouts this week"),
        ScoreFactor("Consistency", 6, 10, "4 active days"),
        ScoreFactor("Recovery", 4, 10, "Light recent load")
    ),
    recommendation = Recommendation(
        "Your score is high today because your recent activity and consistency are strong.",
        "Based on the last 7 days"
    )
)

@Preview(name = "ScoreHero — dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Preview(name = "ScoreHero — light", uiMode = Configuration.UI_MODE_NIGHT_NO, showBackground = true)
@Composable
private fun ScoreHeroPreview() {
    EnergyTheme(themeMode = com.energy.app.data.settings.ThemeMode.SYSTEM) {
        ScoreHero(score = sampleScore, modifier = Modifier.padding(16.dp))
    }
}

@Preview(name = "EnergyButton — dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Preview(name = "EnergyButton — light", uiMode = Configuration.UI_MODE_NIGHT_NO, showBackground = true)
@Composable
private fun EnergyButtonPreview() {
    EnergyTheme(themeMode = com.energy.app.data.settings.ThemeMode.SYSTEM) {
        Column(Modifier.padding(16.dp)) {
            EnergyButton(text = "Start Run", onClick = {})
            EnergyButton(
                text = "One moment…",
                onClick = {},
                loading = true,
                modifier = Modifier.padding(top = 12.dp)
            )
        }
    }
}
