package com.mariesta.menzies.washui.primitives

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.mariesta.menzies.washui.theme.WashTheme

enum class WashTooltipSide {
    Top,
    Bottom,
    Left,
    Right,
}

enum class WashTooltipTone {
    Primary,
    Secondary,
    Accent,
    Info,
    Success,
    Warning,
    Error,
    Neutral,
}

@Composable
fun WashTooltip(
    tip: String,
    modifier: Modifier = Modifier,
    side: WashTooltipSide = WashTooltipSide.Top,
    tone: WashTooltipTone? = null,
    prefer: WashTooltipSide? = null,
    content: @Composable () -> Unit,
) {
    val colors = WashTheme.colors
    val resolvedTone = tone ?: WashTooltipTone.Primary
    val containerColor = tooltipContainer(resolvedTone, colors)
    val contentColor = tooltipContent(resolvedTone, colors)
    val placement = prefer ?: side
    var show by remember { mutableStateOf(false) }
    var anchorSize by remember { mutableStateOf(IntSize.Zero) }
    val density = LocalDensity.current

    Box(
        modifier = modifier
            .onGloballyPositioned { coords ->
                anchorSize = coords.size
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onLongPress = { show = true },
                    onPress = {
                        tryAwaitRelease()
                        show = false
                    },
                )
            },
    ) {
        content()
        if (show && tip.isNotBlank()) {
            val offset = with(density) {
                when (placement) {
                    WashTooltipSide.Top -> IntOffset(0, -anchorSize.height - 8.dp.roundToPx())
                    WashTooltipSide.Bottom -> IntOffset(0, anchorSize.height + 4.dp.roundToPx())
                    WashTooltipSide.Left -> IntOffset(-anchorSize.width - 8.dp.roundToPx(), 0)
                    WashTooltipSide.Right -> IntOffset(anchorSize.width + 4.dp.roundToPx(), 0)
                }
            }
            Popup(
                offset = offset,
                onDismissRequest = { show = false },
                properties = PopupProperties(focusable = false),
            ) {
                WashText(
                    text = tip,
                    color = contentColor,
                    modifier = Modifier
                        .shadow(4.dp, RoundedCornerShape(6.dp))
                        .background(containerColor, RoundedCornerShape(6.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                )
            }
        }
    }
}

private fun tooltipContainer(
    tone: WashTooltipTone,
    colors: com.mariesta.menzies.washui.theme.WashColorScheme,
): androidx.compose.ui.graphics.Color =
    when (tone) {
        WashTooltipTone.Primary -> colors.primary
        WashTooltipTone.Secondary -> colors.secondary
        WashTooltipTone.Accent -> colors.accent
        WashTooltipTone.Info -> colors.info
        WashTooltipTone.Success -> colors.success
        WashTooltipTone.Warning -> colors.warning
        WashTooltipTone.Error -> colors.error
        WashTooltipTone.Neutral -> colors.neutral
    }

private fun tooltipContent(
    tone: WashTooltipTone,
    colors: com.mariesta.menzies.washui.theme.WashColorScheme,
): androidx.compose.ui.graphics.Color =
    when (tone) {
        WashTooltipTone.Primary -> colors.primary_content
        WashTooltipTone.Secondary -> colors.secondary_content
        WashTooltipTone.Accent -> colors.accent_content
        WashTooltipTone.Info -> colors.info_content
        WashTooltipTone.Success -> colors.success_content
        WashTooltipTone.Warning -> colors.warning_content
        WashTooltipTone.Error -> colors.error_content
        WashTooltipTone.Neutral -> colors.neutral_content
    }
