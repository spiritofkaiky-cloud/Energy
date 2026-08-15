package com.energy.app.ui.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.energy.app.data.workout.WorkoutType
import com.energy.app.ui.screens.auth.SignInScreen
import com.energy.app.ui.screens.contact.ContactScreen
import com.energy.app.ui.screens.history.HistoryScreen
import com.energy.app.ui.screens.history.WorkoutDetailScreen
import com.energy.app.ui.screens.map.FullMapScreen
import com.energy.app.ui.screens.splash.SplashScreen
import com.energy.app.ui.screens.workout.LiveWorkoutScreen

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
                },
                onStartWorkout = { type ->
                    navController.navigate(EnergyDestinations.workoutLive(type.name))
                },
                onOpenFullMap = {
                    navController.navigate(EnergyDestinations.MAP_FULL)
                },
                onWorkoutClick = { id ->
                    navController.navigate(EnergyDestinations.historyDetail(id))
                },
                onOpenContact = {
                    navController.navigate(EnergyDestinations.CONTACT)
                }
            )
        }
        composable(
            route = EnergyDestinations.WORKOUT_LIVE,
            arguments = listOf(navArgument("type") { type = NavType.StringType })
        ) { entry ->
            LiveWorkoutScreen(
                typeName = entry.arguments?.getString("type") ?: WorkoutType.RUN.name,
                onExit = { navController.popBackStack() }
            )
        }
        composable(EnergyDestinations.MAP_FULL) {
            FullMapScreen(onClose = { navController.popBackStack() })
        }
        composable(EnergyDestinations.CONTACT) {
            ContactScreen(onBack = { navController.popBackStack() })
        }
        composable(
            route = EnergyDestinations.HISTORY_DETAIL,
            arguments = listOf(navArgument("id") { type = NavType.StringType })
        ) { entry ->
            WorkoutDetailScreen(
                workoutId = entry.arguments?.getString("id").orEmpty(),
                onBack = { navController.popBackStack() }
            )
        }
    }
}
