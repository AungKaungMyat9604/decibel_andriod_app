package com.mariesta.menzies.washui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import com.mariesta.menzies.washui.primitives.WashText
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mariesta.menzies.washui.effects.rememberWashRipple
import com.mariesta.menzies.washui.theme.WashTheme

@Composable
fun WashAccordion(
    modifier: Modifier = Modifier,
    name: String = "wash-accordion",
    content: @Composable () -> Unit,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        content()
    }
}

@Composable
fun WashAccordionItem(
    title: String,
    modifier: Modifier = Modifier,
    defaultOpen: Boolean = false,
    name: String = "wash-accordion",
    content: @Composable () -> Unit,
) {
    val colors = WashTheme.colors
    var expanded by remember { mutableStateOf(defaultOpen) }
    val shape = RoundedCornerShape(colors.radiusField)
    val interaction = remember { MutableInteractionSource() }
    val indication = rememberWashRipple(colors.primary.copy(alpha = 0.2f))

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.base_100, shape)
            .border(1.dp, colors.ink_border, shape),
    ) {
        WashText(
            text = if (expanded) "▼ $title" else "▶ $title",
            color = colors.base_content,
            fontWeight = FontWeight.Medium,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = interaction,
                    indication = indication,
                    onClick = { expanded = !expanded },
                )
                .padding(horizontal = 16.dp, vertical = 12.dp),
        )
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(),
            exit = shrinkVertically(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                content()
            }
        }
    }
}
