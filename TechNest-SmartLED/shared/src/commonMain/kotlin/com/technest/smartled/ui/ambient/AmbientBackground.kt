package com.technest.smartled.ui.ambient

import androidx.compose.animation.core.withInfiniteAnimationFrameMillis
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.technest.smartled.ui.theme.DynamicAccent
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Ambient background that responds to the LED state.
 *
 * Draws a subtle radial gradient that:
 * - Uses the [DynamicAccent] ambient color at very low opacity (~3%).
 * - Slowly shifts the gradient center for organic movement.
 * - Transitions smoothly when the accent changes.
 * - Is intentionally calm — never distracting.
 */
@Composable
fun AmbientBackground(
    accent: DynamicAccent,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    // Track animation time for slow gradient movement
    var elapsedMs by remember { mutableFloatStateOf(0f) }
    androidx.compose.runtime.LaunchedEffect(accent.isActive) {
        while (true) {
            withInfiniteAnimationFrameMillis { frameTimeMs ->
                elapsedMs = frameTimeMs.toFloat()
            }
        }
    }

    val ambientColor = accent.ambient
    val glowColor = accent.glow

    // Use a stable position offset that drifts slowly
    val driftX = remember(accent.sourceColor) { Random.nextFloat() * 0.3f + 0.35f }
    val driftY = remember(accent.sourceColor) { Random.nextFloat() * 0.3f + 0.35f }

    // Primary gradient center moves slowly in a figure-8 pattern
    val centerX = { width: Float ->
        width * (driftX + 0.15f * sin(elapsedMs * 0.0001f))
    }
    val centerY = { height: Float ->
        height * (driftY + 0.15f * cos(elapsedMs * 0.00015f))
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .drawBehind {
                drawAmbientGradient(
                    ambientColor = ambientColor,
                    glowColor = glowColor,
                    centerX = centerX(size.width),
                    centerY = centerY(size.height),
                    maxDimension = maxOf(size.width, size.height),
                )
            },
    ) {
        content()
    }
}

/**
 * Draw a subtle ambient gradient behind the content.
 *
 * Uses two overlapping radial gradients:
 * 1. A tight glow at the center (~6% alpha)
 * 2. A wide ambient spread (~3% alpha)
 */
private fun DrawScope.drawAmbientGradient(
    ambientColor: Color,
    glowColor: Color,
    centerX: Float,
    centerY: Float,
    maxDimension: Float,
) {
    // Tight glow
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                glowColor,
                glowColor.copy(alpha = 0f),
            ),
            center = Offset(centerX, centerY),
            radius = maxDimension * 0.3f,
        ),
        radius = maxDimension * 0.3f,
        center = Offset(centerX, centerY),
    )

    // Wide ambient spread
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                ambientColor,
                ambientColor.copy(alpha = 0f),
            ),
            center = Offset(centerX * 0.8f, centerY * 0.7f),
            radius = maxDimension * 0.7f,
        ),
        radius = maxDimension * 0.7f,
        center = Offset(centerX * 0.8f, centerY * 0.7f),
    )

    // Secondary ambient source for multi-line color richness
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                ambientColor.copy(alpha = ambientColor.alpha * 0.5f),
                Color.Transparent,
            ),
            center = Offset(maxDimension - centerX, maxDimension - centerY),
            radius = maxDimension * 0.5f,
        ),
        radius = maxDimension * 0.5f,
        center = Offset(maxDimension - centerX, maxDimension - centerY),
    )
}
