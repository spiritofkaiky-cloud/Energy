package com.energy.app.ui.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.energy.app.ui.screens.auth.SignInScreen
import com.energy.app.ui.screens.splash.SplashScreen

/**
 * Top-level navigation: Splash → Sign-In → Main.
 * Smooth transitions per APP_SPEC §6 (250–350 ms fades + slides).
 */
@Composable
fun EnergyNavHost(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = EnergyDestinations.SPLASH,
        enterTransition = { fadeIn(tween(300)) + slideInHorizontally(tween(300)) { it / 12 } },
        exitTransition = { fadeOut(tween(250)) },
        popEnterTransition = { fadeIn(tween(250)) },
        popExitTransition = { fadeOut(tween(250)) + slideOutHorizontally(tween(250)) { it / 12 } }
    ) {
        composable(EnergyDestinations.SPLASH) {
            SplashScreen(
                onFinished = {
                    navController.navigate(EnergyDestinations.SIGN_IN) {
                        popUpTo(EnergyDestinations.SPLASH) { inclusive = true }
                    }
                }
            )
        }
        composable(EnergyDestinations.SIGN_IN) {
            SignInScreen(
                onSignedIn = {
                    navController.navigate(EnergyDestinations.MAIN) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
        composable(EnergyDestinations.MAIN) {
            MainScaffold(
                onSignedOut = {
                    navController.navigate(EnergyDestinations.SIGN_IN) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
    }
}
