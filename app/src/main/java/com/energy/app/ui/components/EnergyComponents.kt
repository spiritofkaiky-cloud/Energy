package com.energy.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.energy.app.ui.theme.MetaLabel
import com.energy.app.ui.theme.Motion
import com.energy.app.ui.theme.Space

/**
 * Section header: tiny uppercase label + optional trailing action (§44).
 * The quiet spine of the section-based layout.
 */
@Composable
fun SectionHeader(
    label: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label.uppercase(),
            style = MetaLabel,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (actionLabel != null && onAction != null) {
            Box(Modifier.weight(1f))
            Text(
                text = actionLabel,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .clickable(onClick = onAction)
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            )
        }
    }
}

/**
 * Metric — the typographic unit of the app: the number IS the content,
 * the label is a whisper underneath (§5).
 */
@Composable
fun Metric(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    valueColor: Color = MaterialTheme.colorScheme.onBackground,
    valueStyle: TextStyle = MaterialTheme.typography.displaySmall,
    sub: String? = null
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.Start) {
        Text(
            text = value,
            style = valueStyle,
            color = valueColor,
            fontWeight = FontWeight.Light,
            maxLines = 1
        )
        Text(
            text = label.uppercase(),
            style = MetaLabel,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
        if (sub != null) {
            Text(
                text = sub,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.alpha(0.8f),
                maxLines = 1
            )
        }
    }
}

/**
 * Segmented control — restrained, hairline-track, sliding indicator.
 */
@Composable
fun SegmentedControl(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        options.forEachIndexed { i, option ->
            val selected = i == selectedIndex
            val bg by animateColorAsState(
                targetValue = if (selected) MaterialTheme.colorScheme.surface
                else Color.Transparent,
                animationSpec = tween(Motion.Medium),
                label = "segBg"
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(50))
                    .background(bg)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { onSelect(i) }
                    )
                    .padding(vertical = 9.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = option.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (selected) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Empty state — intentional, never "nothing here" (§28).
 */
@Composable
fun EmptyState(
    glyph: String,
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = Space.XXL),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = glyph, style = MaterialTheme.typography.displayMedium)
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(top = Space.MD)
        )
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .padding(top = Space.XS)
                .padding(horizontal = Space.XXL)
                .alpha(0.9f)
        )
        if (actionLabel != null && onAction != null) {
            EnergyButton(
                text = actionLabel,
                onClick = onAction,
                modifier = Modifier.padding(top = Space.LG)
            )
        }
    }
}

/**
 * Compact stat strip — numbers in a row with hairline separators (§12).
 */
@Composable
fun StatStrip(
    stats: List<Pair<String, String>>, // (value, label)
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        stats.forEachIndexed { i, (value, label) ->
            if (i > 0) {
                Box(
                    Modifier
                        .width(1.dp)
                        .height(34.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant)
                )
            }
            Metric(
                value = value,
                label = label,
                valueStyle = MaterialTheme.typography.titleLarge,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = Space.SM)
            )
        }
    }
}

/**
 * Press-scale helper for physical-feeling controls (§37).
 */
@Composable
fun pressScaleModifier(
    pressedScale: Float = 0.97f,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() }
): Modifier {
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        animationSpec = tween(Motion.Instant),
        label = "pressScale"
    )
    return Modifier.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}
