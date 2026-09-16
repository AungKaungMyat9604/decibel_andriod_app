package com.mariesta.menzies.washui.primitives

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.mariesta.menzies.washui.effects.rememberWashRipple
import com.mariesta.menzies.washui.theme.WashTheme

@Composable
fun WashRadio(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    enabled: Boolean = true,
) {
    val colors = WashTheme.colors
    val interaction = remember { MutableInteractionSource() }
    val indication = if (enabled) rememberWashRipple(colors.primary.copy(alpha = 0.2f)) else null
    val ring = when {
        !enabled -> colors.ink_border.copy(alpha = 0.4f)
        selected -> colors.primary
        else -> colors.ink_border
    }
    val fill = when {
        !enabled -> colors.primary.copy(alpha = 0.35f)
        else -> colors.primary
    }

    val control: @Composable () -> Unit = {
        Box(
            modifier = modifier
                .size(20.dp)
                .clip(CircleShape)
                .border(2.dp, ring, CircleShape)
                .clickable(
                    enabled = enabled,
                    role = Role.RadioButton,
                    interactionSource = interaction,
                    indication = indication,
                    onClick = onClick,
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(fill),
                )
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
            role = Role.RadioButton,
            interactionSource = interaction,
            indication = indication,
            onClick = onClick,
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
