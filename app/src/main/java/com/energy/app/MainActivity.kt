package com.energy.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.energy.app.data.settings.ThemeMode
import com.energy.app.ui.EnergyApp
import com.energy.app.ui.theme.EnergyTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installSplashScreen()
        enableEdgeToEdge()
        val container = (application as EnergyApplication).container
        setContent {
            val themeMode by container.settingsRepository.themeMode
                .collectAsState(initial = ThemeMode.SYSTEM)
            val prefs by container.settingsRepository.preferences
                .collectAsState(initial = com.energy.app.data.settings.UserPreferences())
            EnergyTheme(themeMode = themeMode, accent = prefs.accent) {
                EnergyApp()
            }
        }
    }
}
