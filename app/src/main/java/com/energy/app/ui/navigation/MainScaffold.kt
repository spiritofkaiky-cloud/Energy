package com.energy.app.ui.navigation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.energy.app.data.workout.WorkoutType
import com.energy.app.ui.components.AuroraBackground
import com.energy.app.ui.screens.history.HistoryScreen
import com.energy.app.ui.screens.home.HomeScreen
import com.energy.app.ui.screens.profile.ProfileScreen
import com.energy.app.ui.screens.workout.WorkoutScreen

private data class TabItem(val label: String, val icon: ImageVector)

private val tabs = listOf(
    TabItem("Home", Icons.Filled.Home),
    TabItem("Workout", Icons.Filled.PlayArrow),
    TabItem("History", Icons.Filled.DateRange),
    TabItem("Profile", Icons.Filled.Person)
)

/**
 * Main shell: living aurora background + 4 tabs. APP_SPEC §5.4 bottom navigation.
 */
@Composable
fun MainScaffold(
    onSignedOut: () -> Unit,
    onStartWorkout: (WorkoutType) -> Unit,
    onOpenFullMap: () -> Unit,
    onWorkoutClick: (String) -> Unit,
    onOpenContact: () -> Unit
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        AuroraBackground()
        Scaffold(
            containerColor = Color.Transparent,
            bottomBar = {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                    tabs.forEachIndexed { index, tab ->
                        NavigationBarItem(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label, style = MaterialTheme.typography.labelMedium) },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            }
        ) { padding ->
            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = {
                    (fadeIn(tween(250)) + slideInHorizontally(tween(300)) { it / 10 })
                        .togetherWith(fadeOut(tween(200)) + slideOutHorizontally(tween(300)) { -it / 10 })
                },
                modifier = Modifier.padding(padding),
                label = "tabContent"
            ) { tab ->
                when (tab) {
                    0 -> HomeScreen(onOpenFullMap = onOpenFullMap)
                    1 -> WorkoutScreen(onStart = onStartWorkout)
                    2 -> HistoryScreen(onWorkoutClick = onWorkoutClick)
                    else -> ProfileScreen(onSignOut = onSignedOut, onOpenContact = onOpenContact)
                }
            }
        }
    }
}
