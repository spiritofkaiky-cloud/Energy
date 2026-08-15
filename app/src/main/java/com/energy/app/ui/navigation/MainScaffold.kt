package com.energy.app.ui.navigation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.energy.app.EnergyApplication
import com.energy.app.data.workout.WorkoutType
import com.energy.app.ui.components.AuroraBackground
import com.energy.app.ui.screens.activity.ActivityScreen
import com.energy.app.ui.screens.home.HomeScreen
import com.energy.app.ui.screens.profile.ProfileScreen
import com.energy.app.ui.screens.progress.ProgressScreen
import com.energy.app.ui.theme.EnergyIcons
import com.energy.app.ui.theme.MetaLabel
import com.energy.app.ui.theme.Motion

private data class Tab(val label: String, val icon: ImageVector)

private val tabs = listOf(
    Tab("Today", Icons.Filled.Home),
    Tab("Activity", Icons.Filled.PlayArrow),
    Tab("Progress", EnergyIcons.TrendingUp),
    Tab("Profile", Icons.Filled.Person)
)

/**
 * Main shell (§15) — quiet bottom navigation: hairline-top bar, icon +
 * uppercase label, accent dot on the active tab, spring icon lift, haptic
 * tick on switch. No giant pill container.
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
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val haptics = LocalHapticFeedback.current

    // Battery: only track passive movement while the app is actually visible.
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            val tracker = (context.applicationContext as EnergyApplication).container.locationTracker
            when (event) {
                Lifecycle.Event.ON_START -> tracker.start()
                Lifecycle.Event.ON_STOP -> tracker.stop()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        AuroraBackground()
        Scaffold(
            containerColor = Color.Transparent,
            bottomBar = { EnergyNavBar(selectedTab) { i ->
                if (i != selectedTab) {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    selectedTab = i
                }
            } }
        ) { padding ->
            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = {
                    (fadeIn(tween(Motion.Medium)) + slideInVertically(tween(Motion.Medium)) { it / 24 })
                        .togetherWith(fadeOut(tween(Motion.Fast)))
                },
                modifier = Modifier.padding(padding),
                label = "tabContent"
            ) { tab ->
                when (tab) {
                    0 -> HomeScreen(
                        onOpenFullMap = onOpenFullMap,
                        onResumeWorkout = onStartWorkout
                    )
                    1 -> ActivityScreen(
                        onStart = onStartWorkout,
                        onWorkoutClick = onWorkoutClick
                    )
                    2 -> ProgressScreen()
                    else -> ProfileScreen(onSignOut = onSignedOut, onOpenContact = onOpenContact)
                }
            }
        }
    }
}

@Composable
private fun EnergyNavBar(selected: Int, onSelect: (Int) -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.97f))
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(MaterialTheme.colorScheme.outlineVariant)
        )
        Row(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            tabs.forEachIndexed { i, tab ->
                val active = i == selected
                val alpha by animateFloatAsState(
                    targetValue = if (active) 1f else 0.55f,
                    animationSpec = tween(Motion.Fast),
                    label = "tabAlpha"
                )
                val lift by animateDpAsState(
                    targetValue = if (active) 2.dp else 0.dp,
                    animationSpec = tween(Motion.Fast),
                    label = "tabLift"
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { onSelect(i) }
                        )
                        .padding(vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = tab.icon,
                        contentDescription = tab.label,
                        tint = if (active) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .size(21.dp)
                            .padding(bottom = lift)
                    )
                    Text(
                        text = tab.label.uppercase(),
                        style = MetaLabel,
                        color = if (active) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .padding(top = 3.dp)
                            .alpha(alpha)
                    )
                }
            }
        }
    }
}
