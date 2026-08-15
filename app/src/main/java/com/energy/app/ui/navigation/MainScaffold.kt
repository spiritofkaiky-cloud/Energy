package com.energy.app.ui.navigation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
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
import androidx.compose.ui.graphics.vector.ImageVector
import com.energy.app.ui.screens.history.HistoryScreen
import com.energy.app.ui.screens.home.HomeScreen
import com.energy.app.ui.screens.profile.ProfileScreen
import com.energy.app.ui.screens.workout.WorkoutScreen

private data class Tab(val label: String, val icon: ImageVector)

private val tabs = listOf(
    Tab("Home", Icons.Filled.Home),
    Tab("Workout", Icons.Filled.PlayArrow),
    Tab("History", Icons.Filled.DateRange),
    Tab("Profile", Icons.Filled.Person)
)

/**
 * Main tab shell: animated pill indicator + smooth slide/fade between
 * tabs. APP_SPEC §5.4 bottom navigation.
 */
@Composable
fun MainScaffold(onSignedOut: () -> Unit) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
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
                0 -> HomeScreen()
                1 -> WorkoutScreen()
                2 -> HistoryScreen()
                else -> ProfileScreen(onSignOut = onSignedOut)
            }
        }
    }
}
