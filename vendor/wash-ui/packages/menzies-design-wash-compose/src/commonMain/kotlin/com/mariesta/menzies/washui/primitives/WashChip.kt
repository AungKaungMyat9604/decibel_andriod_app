package com.mariesta.menzies.washui.primitives

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mariesta.menzies.washui.effects.rememberWashRipple
import com.mariesta.menzies.washui.theme.WashTheme

/**
 * Compact selectable chip (Foundation). Replaces Material FilterChip hosts.
 */
@Composable
fun WashChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = WashTheme.colors
    val shape = RoundedCornerShape(999.dp)
    val interaction = remember { MutableInteractionSource() }
    val indication = if (enabled) rememberWashRipple(colors.primary.copy(alpha = 0.2f)) else null
    val container = when {
        !enabled -> colors.base_200.copy(alpha = 0.5f)
        selected -> colors.primary.copy(alpha = 0.18f)
        else -> colors.base_200
    }
    val border = when {
        selected -> colors.primary
        else -> colors.ink_border
    }
    val content = when {
        !enabled -> colors.base_content.copy(alpha = 0.5f)
        selected -> colors.primary
        else -> colors.base_content
    }

    WashText(
        text = label,
        color = content,
        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
        modifier = modifier
            .clip(shape)
            .background(container, shape)
            .border(1.dp, border, shape)
            .clickable(
                enabled = enabled,
                role = Role.Checkbox,
                interactionSource = interaction,
                indication = indication,
                onClick = onClick,
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
    )
}
