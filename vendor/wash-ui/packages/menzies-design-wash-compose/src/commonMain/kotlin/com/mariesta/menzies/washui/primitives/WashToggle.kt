package com.mariesta.menzies.washui.primitives

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.mariesta.menzies.washui.effects.rememberWashRipple
import com.mariesta.menzies.washui.theme.WashTheme

@Composable
fun WashToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    enabled: Boolean = true,
) {
    val colors = WashTheme.colors
    val interaction = remember { MutableInteractionSource() }
    val indication = if (enabled) rememberWashRipple(colors.primary.copy(alpha = 0.2f)) else null
    val trackWidth = 48.dp
    val trackHeight = 28.dp
    val thumbSize = 22.dp
    val thumbOffset by animateDpAsState(
        targetValue = if (checked) trackWidth - thumbSize - 3.dp else 3.dp,
        animationSpec = tween(durationMillis = 160),
        label = "wash-toggle-thumb",
    )
    val trackColor = when {
        !enabled && checked -> colors.primary.copy(alpha = 0.4f)
        !enabled -> colors.base_300.copy(alpha = 0.4f)
        checked -> colors.primary
        else -> colors.base_300
    }
    val thumbColor = when {
        !enabled && checked -> colors.primary_content.copy(alpha = 0.6f)
        !enabled -> colors.base_content.copy(alpha = 0.4f)
        checked -> colors.primary_content
        else -> colors.base_content
    }

    val control: @Composable () -> Unit = {
        Box(
            modifier = modifier
                .width(trackWidth)
                .height(trackHeight)
                .clip(RoundedCornerShape(999.dp))
                .background(trackColor)
                .clickable(
                    enabled = enabled,
                    role = Role.Switch,
                    interactionSource = interaction,
                    indication = indication,
                    onClick = { onCheckedChange(!checked) },
                ),
        ) {
            Box(
                modifier = Modifier
                    .padding(vertical = 3.dp)
                    .offset(x = thumbOffset)
                    .size(thumbSize)
                    .clip(CircleShape)
                    .background(thumbColor),
            )
        }
    }

    if (label.isNullOrBlank()) {
        control()
        return
    }

    Row(
        modifier = Modifier.clickable(
            enabled = enabled,
            role = Role.Switch,
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
