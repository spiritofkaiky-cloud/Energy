package com.energy.app.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.energy.app.ui.theme.Motion
import com.energy.app.ui.theme.Radius

/**
 * Primary action (§37): strong typography, generous touch area, spring
 * press, clear loading and disabled states. Restrained — not a cartoon
 * button.
 */
@Composable
fun EnergyButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    loading: Boolean = false,
    enabled: Boolean = true
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        animationSpec = tween(Motion.Instant),
        label = "buttonScale"
    )

    Button(
        onClick = onClick,
        modifier = modifier
            .height(54.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        enabled = enabled && !loading,
        shape = RoundedCornerShape(Radius.MD),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = Color.White,
            disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
        ),
        interactionSource = interactionSource
    ) {
        AnimatedContent(
            targetState = loading,
            transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(200)) },
            label = "buttonContent"
        ) { isLoading ->
            if (isLoading) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                    Spacer(Modifier.width(10.dp))
                    Text("One moment…", style = MaterialTheme.typography.labelLarge)
                }
            } else {
                Text(
                    text = text,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.3.sp
                )
            }
        }
    }
}

/** Quiet secondary action — hairline surface, no shouting. */
@Composable
fun EnergySecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        animationSpec = tween(Motion.Instant),
        label = "secondaryScale"
    )

    OutlinedButton(
        onClick = onClick,
        modifier = modifier
            .height(54.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        enabled = enabled,
        shape = RoundedCornerShape(Radius.MD),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        interactionSource = interactionSource
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.3.sp
        )
    }
}
