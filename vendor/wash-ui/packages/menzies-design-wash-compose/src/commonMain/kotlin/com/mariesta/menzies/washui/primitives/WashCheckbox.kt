package com.mariesta.menzies.washui.primitives

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.mariesta.menzies.washui.effects.rememberWashRipple
import com.mariesta.menzies.washui.theme.WashTheme

@Composable
fun WashCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    enabled: Boolean = true,
) {
    val colors = WashTheme.colors
    val interaction = remember { MutableInteractionSource() }
    val indication = if (enabled) rememberWashRipple(colors.primary.copy(alpha = 0.2f)) else null
    val boxSize = 20.dp
    val shape = RoundedCornerShape(4.dp)
    val borderColor = when {
        !enabled -> colors.ink_border.copy(alpha = 0.4f)
        checked -> colors.primary
        else -> colors.ink_border
    }
    val fillColor = when {
        !enabled && checked -> colors.primary.copy(alpha = 0.4f)
        checked -> colors.primary
        else -> colors.base_100
    }
    val markColor = if (enabled) colors.primary_content else colors.primary_content.copy(alpha = 0.6f)

    val control: @Composable () -> Unit = {
        Box(
            modifier = modifier
                .size(boxSize)
                .clip(shape)
                .background(fillColor, shape)
                .border(1.5.dp, borderColor, shape)
                .clickable(
                    enabled = enabled,
                    role = Role.Checkbox,
                    interactionSource = interaction,
                    indication = indication,
                    onClick = { onCheckedChange(!checked) },
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (checked) {
                Canvas(modifier = Modifier.size(12.dp)) {
                    val stroke = Stroke(
                        width = 2.dp.toPx(),
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round,
                    )
                    val path = Path().apply {
                        moveTo(size.width * 0.15f, size.height * 0.55f)
                        lineTo(size.width * 0.4f, size.height * 0.8f)
                        lineTo(size.width * 0.85f, size.height * 0.25f)
                    }
                    drawPath(path = path, color = markColor, style = stroke)
                }
            }
        }
    }

    if (label.isNullOrBlank()) {
        control()
        return
    }

    Row(
        modifier = Modifier.clickable(
            enabled = enabled,
            role = Role.Checkbox,
            interactionSource = interaction,
            indication = indication,
            onClick = { onCheckedChange(!checked) },
        ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        control()
        WashText(
            text = label,
            color = if (enabled) colors.base_content else colors.base_content.copy(alpha = 0.6f),
        )
    }
}
