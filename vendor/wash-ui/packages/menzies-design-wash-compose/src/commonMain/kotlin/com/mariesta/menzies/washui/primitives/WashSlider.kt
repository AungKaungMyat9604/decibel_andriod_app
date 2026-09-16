package com.mariesta.menzies.washui.primitives

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.mariesta.menzies.washui.theme.WashTheme

/**
 * Foundation range control (no Material Slider).
 */
@Composable
fun WashSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    enabled: Boolean = true,
    activeColor: Color = Color.Unspecified,
    inactiveColor: Color = Color.Unspecified,
    thumbColor: Color = Color.Unspecified,
) {
    val colors = WashTheme.colors
    val active = if (activeColor != Color.Unspecified) activeColor else colors.primary
    val inactive = if (inactiveColor != Color.Unspecified) inactiveColor else colors.base_300
    val thumb = if (thumbColor != Color.Unspecified) thumbColor else colors.primary
    val coerced = value.coerceIn(valueRange.start, valueRange.endInclusive)
    val fraction = if (valueRange.endInclusive == valueRange.start) {
        0f
    } else {
        (coerced - valueRange.start) / (valueRange.endInclusive - valueRange.start)
    }

    fun valueFromX(x: Float, width: Float): Float {
        val f = (x / width).coerceIn(0f, 1f)
        return valueRange.start + f * (valueRange.endInclusive - valueRange.start)
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(32.dp)
            .pointerInput(enabled, valueRange) {
                if (!enabled) return@pointerInput
                detectTapGestures { offset ->
                    onValueChange(valueFromX(offset.x, size.width.toFloat()))
                }
            }
            .pointerInput(enabled, valueRange) {
                if (!enabled) return@pointerInput
                detectDragGestures { change, _ ->
                    change.consume()
                    onValueChange(valueFromX(change.position.x, size.width.toFloat()))
                }
            },
    ) {
        val trackH = 4.dp.toPx()
        val cy = size.height / 2f
        val trackTop = cy - trackH / 2f
        val radius = CornerRadius(trackH, trackH)
        drawRoundRect(
            color = if (enabled) inactive else inactive.copy(alpha = 0.5f),
            topLeft = Offset(0f, trackTop),
            size = Size(size.width, trackH),
            cornerRadius = radius,
        )
        drawRoundRect(
            color = if (enabled) active else active.copy(alpha = 0.5f),
            topLeft = Offset(0f, trackTop),
            size = Size(size.width * fraction, trackH),
            cornerRadius = radius,
        )
        val thumbR = 8.dp.toPx()
        val tx = (size.width * fraction).coerceIn(thumbR, size.width - thumbR)
        drawCircle(
            color = if (enabled) thumb else thumb.copy(alpha = 0.5f),
            radius = thumbR,
            center = Offset(tx, cy),
        )
    }
}
