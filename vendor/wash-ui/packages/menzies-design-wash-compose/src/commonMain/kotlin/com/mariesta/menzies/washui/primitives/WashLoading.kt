package com.mariesta.menzies.washui.primitives

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mariesta.menzies.washui.theme.WashTheme

enum class WashLoadingSize {
    Sm,
    Md,
    Lg,
}

enum class WashLoadingVariant {
    Spinner,
    Dots,
    Ring,
    Ball,
    Bars,
    Infinity,
}

@Composable
fun WashLoading(
    modifier: Modifier = Modifier,
    label: String = "Loading",
    size: WashLoadingSize = WashLoadingSize.Md,
    variant: WashLoadingVariant = WashLoadingVariant.Spinner,
) {
    val colors = WashTheme.colors
    val indicatorSize = when (size) {
        WashLoadingSize.Sm -> 20.dp
        WashLoadingSize.Md -> 28.dp
        WashLoadingSize.Lg -> 40.dp
    }
    val stroke = when (size) {
        WashLoadingSize.Sm -> 2.dp
        WashLoadingSize.Md -> 3.dp
        WashLoadingSize.Lg -> 4.dp
    }

    Box(
        modifier = modifier.semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        when (variant) {
            WashLoadingVariant.Spinner,
            WashLoadingVariant.Ring,
            -> WashCircularProgress(
                modifier = Modifier.size(indicatorSize),
                color = colors.primary,
                strokeWidth = stroke,
            )
            WashLoadingVariant.Dots -> LoadingDots(indicatorSize, colors.primary)
            WashLoadingVariant.Ball -> WashCircularProgress(
                modifier = Modifier.size(indicatorSize / 2),
                color = colors.primary,
                strokeWidth = stroke,
            )
            WashLoadingVariant.Bars -> WashLinearProgress(
                modifier = Modifier.size(width = indicatorSize * 2, height = stroke),
                color = colors.primary,
                trackColor = colors.base_300,
            )
            WashLoadingVariant.Infinity -> WashCircularProgress(
                modifier = Modifier.size(indicatorSize),
                color = colors.secondary,
                strokeWidth = stroke,
            )
        }
    }
}

@Composable
fun WashLoading(
    label: String,
    modifier: Modifier = Modifier,
    size: WashLoadingSize = WashLoadingSize.Md,
    variant: WashLoadingVariant = WashLoadingVariant.Spinner,
    showLabel: Boolean = false,
) {
    val colors = WashTheme.colors
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        WashLoading(label = label, size = size, variant = variant)
        if (showLabel) {
            WashText(text = label, color = colors.base_content)
        }
    }
}

@Composable
internal fun WashCircularProgress(
    modifier: Modifier = Modifier,
    color: androidx.compose.ui.graphics.Color,
    strokeWidth: Dp,
) {
    val transition = rememberInfiniteTransition(label = "wash-spin")
    val angle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "wash-spin-angle",
    )
    Canvas(modifier = modifier) {
        val stroke = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
        val inset = stroke.width / 2f
        rotate(angle) {
            drawArc(
                color = color,
                startAngle = 0f,
                sweepAngle = 270f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = Size(size.width - stroke.width, size.height - stroke.width),
                style = stroke,
            )
        }
    }
}

@Composable
internal fun WashLinearProgress(
    modifier: Modifier = Modifier,
    color: androidx.compose.ui.graphics.Color,
    trackColor: androidx.compose.ui.graphics.Color,
) {
    val transition = rememberInfiniteTransition(label = "wash-bars")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "wash-bars-progress",
    )
    Canvas(modifier = modifier) {
        drawRect(trackColor)
        val barWidth = size.width * 0.35f
        val x = (size.width + barWidth) * progress - barWidth
        drawRect(
            color = color,
            topLeft = Offset(x, 0f),
            size = Size(barWidth, size.height),
        )
    }
}

@Composable
private fun LoadingDots(size: Dp, color: androidx.compose.ui.graphics.Color) {
    val transition = rememberInfiniteTransition(label = "wash-dots")
    val pulse by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "wash-dots-pulse",
    )
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        repeat(3) { index ->
            val alpha = ((pulse + index * 0.2f) % 1.2f).coerceIn(0.35f, 1f)
            Box(
                modifier = Modifier
                    .size(size / 4)
                    .clip(CircleShape)
                    .background(color.copy(alpha = alpha)),
            )
        }
    }
}
