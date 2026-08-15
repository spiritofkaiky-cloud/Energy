package com.energy.app.ui

import androidx.compose.runtime.Composable
import androidx.navigation.compose.rememberNavController
import com.energy.app.ui.navigation.EnergyNavHost

/** Compose root — single activity, single nav graph (APP_SPEC §7). */
@Composable
fun EnergyApp() {
    val navController = rememberNavController()
    EnergyNavHost(navController = navController)
}
