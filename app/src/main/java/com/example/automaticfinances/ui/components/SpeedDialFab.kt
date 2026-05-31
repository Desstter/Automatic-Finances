package com.example.automaticfinances.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import com.example.automaticfinances.ui.theme.MotionTokens
import com.example.automaticfinances.ui.theme.Spacing

/**
 * A single mini-action surfaced by [SpeedDialFab].
 */
data class SpeedDialAction(
    val label: String,
    val icon: ImageVector,
    val onClick: () -> Unit,
)

/**
 * Expandable "speed dial" FAB. Tapping the main button reveals labelled mini-FABs
 * that stack above it. Used on Home to offer the two quick-add paths — voice and
 * manual — without burying the flagship voice flow in a menu.
 *
 * The caller owns [expanded] so it can also draw a dismiss scrim over screen
 * content (see [SpeedDialScrim]). Selecting any action collapses the dial first.
 */
@Composable
fun SpeedDialFab(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    actions: List<SpeedDialAction>,
    modifier: Modifier = Modifier,
) {
    // Main icon rotates 0° -> 45° so the "+" reads as a close affordance when open.
    val rotation by animateFloatAsState(
        targetValue = if (expanded) 45f else 0f,
        animationSpec = MotionTokens.expressiveSpatialDefault(),
        label = "fabRotation",
    )

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        // Reverse so the first action sits closest to the main FAB.
        actions.asReversed().forEach { action ->
            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn(MotionTokens.expressiveEffectsDefault()) +
                    scaleIn(
                        animationSpec = MotionTokens.expressiveSpatialDefault(),
                        transformOrigin = TransformOrigin(1f, 1f),
                    ) +
                    slideInVertically(MotionTokens.expressiveSpatialDefault()) { it / 2 },
                exit = fadeOut(MotionTokens.expressiveEffectsDefault()) +
                    scaleOut(
                        animationSpec = MotionTokens.expressiveEffectsDefault(),
                        transformOrigin = TransformOrigin(1f, 1f),
                    ) +
                    slideOutVertically(MotionTokens.expressiveEffectsDefault()) { it / 2 },
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                ) {
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                    ) {
                        Text(
                            text = action.label,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(
                                horizontal = Spacing.md,
                                vertical = Spacing.sm,
                            ),
                        )
                    }
                    SmallFloatingActionButton(
                        onClick = {
                            onExpandedChange(false)
                            action.onClick()
                        },
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    ) {
                        Icon(action.icon, contentDescription = action.label)
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { onExpandedChange(!expanded) },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = if (expanded) "Cerrar" else "Registrar",
                modifier = Modifier.rotate(rotation),
            )
        }
    }
}

/**
 * Full-bleed dismiss scrim for [SpeedDialFab]. Place it inside the screen's content
 * (it fades in over everything below the FAB) so a tap anywhere collapses the dial.
 */
@Composable
fun SpeedDialScrim(
    visible: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(MotionTokens.expressiveEffectsDefault()),
        exit = fadeOut(MotionTokens.expressiveEffectsDefault()),
        modifier = modifier,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.32f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss,
                ),
        )
    }
}
