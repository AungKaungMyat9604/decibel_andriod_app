package com.mariesta.menzies.washui.effects

import androidx.compose.foundation.Indication
import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.invalidateDraw
import com.mariesta.menzies.washui.theme.WashTheme
import kotlinx.coroutines.launch

@Composable
expect fun rememberReducedMotion(): Boolean

/**
 * Press highlight without Material [ripple]. Foundation-only [IndicationNodeFactory].
 */
@Composable
fun rememberWashRipple(color: Color = Color.Unspecified): Indication {
    val reduced = rememberReducedMotion()
    val pressColor = when {
        reduced -> Color.Transparent
        color != Color.Unspecified -> color
        else -> WashTheme.colors.primary.copy(alpha = 0.22f)
    }
    return remember(pressColor) { WashPressIndication(pressColor) }
}

fun Modifier.washRipple(enabled: Boolean = true): Modifier = composed {
    if (!enabled) this else this
}

private class WashPressIndication(
    private val color: Color,
) : IndicationNodeFactory {
    override fun create(interactionSource: InteractionSource): DelegatableNode =
        WashPressIndicationNode(interactionSource, color)

    override fun hashCode(): Int = color.hashCode()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is WashPressIndication) return false
        return color == other.color
    }
}

private class WashPressIndicationNode(
    private val interactionSource: InteractionSource,
    private val color: Color,
) : Modifier.Node(), DrawModifierNode {
    private var pressed = false

    override fun onAttach() {
        coroutineScope.launch {
            interactionSource.interactions.collect { interaction ->
                when (interaction) {
                    is PressInteraction.Press -> {
                        pressed = true
                        invalidateDraw()
                    }
                    is PressInteraction.Release,
                    is PressInteraction.Cancel,
                    -> {
                        pressed = false
                        invalidateDraw()
                    }
                }
            }
        }
    }

    override fun ContentDrawScope.draw() {
        drawContent()
        if (pressed && color.alpha > 0.001f) {
            drawRect(color = color)
        }
    }
}
