package com.mariesta.menzies.washui.primitives

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mariesta.menzies.washui.effects.rememberWashRipple
import com.mariesta.menzies.washui.theme.WashTheme

enum class WashButtonVariant {
    Default,
    Primary,
    Secondary,
    Accent,
    Neutral,
    Info,
    Success,
    Warning,
    Error,
    Ghost,
    Link,
    Outline,
}

enum class WashButtonSize {
    Xs,
    Sm,
    Md,
    Lg,
    Xl,
}

@Composable
fun WashButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: WashButtonVariant = WashButtonVariant.Default,
    size: WashButtonSize = WashButtonSize.Md,
    enabled: Boolean = true,
    loading: Boolean = false,
    ripple: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colors = WashTheme.colors
    val (container, contentColor, border) = buttonColors(variant, colors)
    val padding = when (size) {
        WashButtonSize.Xs -> PaddingValues(horizontal = 10.dp, vertical = 4.dp)
        WashButtonSize.Sm -> PaddingValues(horizontal = 12.dp, vertical = 6.dp)
        WashButtonSize.Md -> PaddingValues(horizontal = 16.dp, vertical = 8.dp)
        WashButtonSize.Lg -> PaddingValues(horizontal = 20.dp, vertical = 10.dp)
        WashButtonSize.Xl -> PaddingValues(horizontal = 24.dp, vertical = 12.dp)
    }
    val shape = RoundedCornerShape(colors.radiusField)
    val interaction = remember { MutableInteractionSource() }
    val indication = if (ripple && enabled && !loading) rememberWashRipple(colors.primary.copy(alpha = 0.25f)) else null

    Box(
        modifier = modifier
            .defaultMinSize(minHeight = 40.dp)
            .clip(shape)
            .background(if (enabled) container else container.copy(alpha = 0.5f), shape)
            .then(
                if (border != null) Modifier.border(BorderStroke(1.dp, border), shape) else Modifier,
            )
            .clickable(
                enabled = enabled && !loading,
                role = Role.Button,
                interactionSource = interaction,
                indication = indication,
                onClick = onClick,
            )
            .padding(padding),
        contentAlignment = Alignment.Center,
    ) {
        if (loading) {
            WashCircularProgress(
                modifier = Modifier
                    .padding(end = 8.dp)
                    .size(16.dp),
                color = contentColor,
                strokeWidth = 2.dp,
            )
        }
        content()
    }
}

@Composable
fun WashButton(
    onClick: () -> Unit,
    text: String,
    modifier: Modifier = Modifier,
    variant: WashButtonVariant = WashButtonVariant.Primary,
    size: WashButtonSize = WashButtonSize.Md,
    enabled: Boolean = true,
    loading: Boolean = false,
) {
    WashButton(
        onClick = onClick,
        modifier = modifier,
        variant = variant,
        size = size,
        enabled = enabled,
        loading = loading,
    ) {
        WashText(text = text, color = buttonContentColor(variant, WashTheme.colors), fontWeight = FontWeight.Medium)
    }
}

private data class ButtonPalette(val container: Color, val content: Color, val border: Color?)

private fun buttonColors(variant: WashButtonVariant, colors: com.mariesta.menzies.washui.theme.WashColorScheme): ButtonPalette =
    when (variant) {
        WashButtonVariant.Primary -> ButtonPalette(colors.primary, colors.primary_content, null)
        WashButtonVariant.Secondary -> ButtonPalette(colors.secondary, colors.secondary_content, null)
        WashButtonVariant.Accent -> ButtonPalette(colors.accent, colors.accent_content, null)
        WashButtonVariant.Neutral -> ButtonPalette(colors.neutral, colors.neutral_content, null)
        WashButtonVariant.Info -> ButtonPalette(colors.info, colors.info_content, null)
        WashButtonVariant.Success -> ButtonPalette(colors.success, colors.success_content, null)
        WashButtonVariant.Warning -> ButtonPalette(colors.warning, colors.warning_content, null)
        WashButtonVariant.Error -> ButtonPalette(colors.error, colors.error_content, null)
        WashButtonVariant.Ghost -> ButtonPalette(Color.Transparent, colors.base_content, null)
        WashButtonVariant.Link -> ButtonPalette(Color.Transparent, colors.primary, null)
        WashButtonVariant.Outline -> ButtonPalette(Color.Transparent, colors.primary, colors.primary)
        WashButtonVariant.Default -> ButtonPalette(colors.base_200, colors.base_content, colors.ink_border)
    }

private fun buttonContentColor(variant: WashButtonVariant, colors: com.mariesta.menzies.washui.theme.WashColorScheme): Color =
    buttonColors(variant, colors).content
