package com.mariesta.menzies.washui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.mariesta.menzies.washui.theme.WashTheme

@Composable
fun BrushStrokeLoader(modifier: Modifier = Modifier) {
    val colors = WashTheme.colors
    val transition = rememberInfiniteTransition(label = "brush-stroke")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Restart),
        label = "progress",
    )
    Canvas(modifier = modifier.size(48.dp)) {
        drawArc(
            color = colors.primary.copy(alpha = 0.25f),
            startAngle = 0f,
            sweepAngle = 360f,
            useCenter = false,
            style = Stroke(width = 4f, cap = StrokeCap.Round),
            topLeft = Offset(8f, 8f),
            size = androidx.compose.ui.geometry.Size(size.width - 16f, size.height - 16f),
        )
        drawArc(
            color = colors.primary,
            startAngle = -90f,
            sweepAngle = 300f * progress,
            useCenter = false,
            style = Stroke(width = 4f, cap = StrokeCap.Round),
            topLeft = Offset(8f, 8f),
            size = androidx.compose.ui.geometry.Size(size.width - 16f, size.height - 16f),
        )
    }
}

@Composable
fun BrushTipLoader(modifier: Modifier = Modifier) {
    val colors = WashTheme.colors
    val transition = rememberInfiniteTransition(label = "brush-tip")
    val alpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(620), RepeatMode.Reverse),
        label = "alpha",
    )
    Canvas(modifier = modifier.size(32.dp)) {
        drawCircle(colors.primary.copy(alpha = alpha), radius = size.minDimension * 0.18f)
        drawCircle(colors.primary.copy(alpha = alpha * 0.45f), radius = size.minDimension * 0.32f, style = Stroke(2f))
    }
}
