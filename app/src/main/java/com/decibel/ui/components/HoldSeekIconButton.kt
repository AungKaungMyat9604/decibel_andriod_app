package com.decibel.ui.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.coroutines.coroutineContext

/** Tap = skip track. Long-press = scrub with gradually increasing seek speed. */
@Composable
fun HoldSeekIconButton(
    forward: Boolean,
    positionMs: Long,
    durationMs: Long,
    enabled: Boolean,
    imageVector: ImageVector,
    contentDescription: String,
    tint: Color,
    onClick: () -> Unit,
    onSeek: (Long) -> Unit,
    onHoldSpeedChange: (Int?) -> Unit,
    modifier: Modifier = Modifier,
    iconModifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    var didHoldSeek by remember { mutableStateOf(false) }

    LaunchedEffect(pressed, enabled) {
        if (!pressed || !enabled) {
            if (didHoldSeek) onHoldSpeedChange(null)
            return@LaunchedEffect
        }
        didHoldSeek = false
        val startPos = positionMs
        val duration = durationMs.coerceAtLeast(1L)
        delay(HoldSeekStartDelayMs)
        if (!coroutineContext.isActive) return@LaunchedEffect

        didHoldSeek = true
        var pos = startPos
        var multiplier = 1f
        val direction = if (forward) 1 else -1

        try {
            while (coroutineContext.isActive) {
                val stepMs = (HoldSeekBaseStepMs * multiplier).toLong().coerceAtLeast(HoldSeekBaseStepMs)
                pos = (pos + direction * stepMs).coerceIn(0L, duration)
                onSeek(pos)
                onHoldSpeedChange(multiplier.toInt().coerceAtLeast(1))
                delay(HoldSeekTickMs)
                multiplier = (multiplier * HoldSeekAccel).coerceAtMost(HoldSeekMaxMultiplier)
            }
        } finally {
            onHoldSpeedChange(null)
        }
    }

    IconButton(
        onClick = {
            if (didHoldSeek) {
                didHoldSeek = false
            } else {
                onClick()
            }
        },
        enabled = enabled,
        interactionSource = interactionSource,
        modifier = modifier,
    ) {
        Icon(
            imageVector = imageVector,
            contentDescription = contentDescription,
            tint = tint,
            modifier = iconModifier,
        )
    }
}

private const val HoldSeekStartDelayMs = 320L
private const val HoldSeekTickMs = 50L
private const val HoldSeekBaseStepMs = 280L
private const val HoldSeekAccel = 1.12f
private const val HoldSeekMaxMultiplier = 24f
