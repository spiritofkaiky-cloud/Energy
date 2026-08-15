package com.energy.app.ui.screens.splash

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.energy.app.ui.components.GradientBackground
import com.energy.app.ui.theme.EnergyCoral
import com.energy.app.ui.theme.EnergyOrange
import kotlinx.coroutines.delay

/**
 * Animated splash: pulsing bolt logo, staggered title entrance, then
 * hand-off to Sign-In. APP_SPEC §5.1.
 */
@Composable
fun SplashScreen(onFinished: () -> Unit) {
    var entered by remember { mutableStateOf(false) }

    val logoScale by animateFloatAsState(
        targetValue = if (entered) 1f else 0.6f,
        animationSpec = tween(600),
        label = "splashLogoScale"
    )
    val titleAlpha by animateFloatAsState(
        targetValue = if (entered) 1f else 0f,
        animationSpec = tween(800, delayMillis = 200),
        label = "splashTitleAlpha"
    )
    val pulse by rememberInfiniteTransition(label = "splashPulse").animateFloat(
        initialValue = 1f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(tween(1000, easing = LinearEasing), RepeatMode.Reverse),
        label = "splashPulseScale"
    )

    LaunchedEffect(Unit) {
        entered = true
        delay(2000)
        onFinished()
    }

    GradientBackground(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .scale(logoScale * pulse)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(EnergyOrange, EnergyCoral))),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "⚡",
                    style = MaterialTheme.typography.displayLarge,
                    color = Color.White
                )
            }
            Spacer(Modifier.height(28.dp))
            Text(
                text = "Energy",
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                modifier = Modifier.alpha(titleAlpha)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Move. Track. Energize.",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.alpha(titleAlpha)
            )
        }
    }
}
