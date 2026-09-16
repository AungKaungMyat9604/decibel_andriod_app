package com.mariesta.menzies.washui.primitives

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mariesta.menzies.washui.effects.rememberWashRipple
import com.mariesta.menzies.washui.icons.WashIcon
import com.mariesta.menzies.washui.theme.WashTheme

/**
 * Icon-only control with Wash ripple. Matches ghost square btn patterns from the web recipes.
 */
@Composable
fun WashIconButton(
    onClick: () -> Unit,
    imageVector: ImageVector,
    contentDescription: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    tint: Color = Color.Unspecified,
    iconSize: Dp = 22.dp,
    buttonSize: Dp = 40.dp,
) {
    val colors = WashTheme.colors
    val resolvedTint = if (tint == Color.Unspecified) colors.base_content else tint
    val interaction = remember { MutableInteractionSource() }
    val indication = if (enabled) rememberWashRipple(colors.primary.copy(alpha = 0.25f)) else null

    Box(
        modifier = modifier
            .defaultMinSize(minWidth = buttonSize, minHeight = buttonSize)
            .size(buttonSize)
            .clip(RoundedCornerShape(colors.radiusField))
            .clickable(
                enabled = enabled,
                role = Role.Button,
                interactionSource = interaction,
                indication = indication,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        WashIcon(
            imageVector = imageVector,
            contentDescription = contentDescription,
            tint = if (enabled) resolvedTint else resolvedTint.copy(alpha = 0.4f),
            size = iconSize,
        )
    }
}
