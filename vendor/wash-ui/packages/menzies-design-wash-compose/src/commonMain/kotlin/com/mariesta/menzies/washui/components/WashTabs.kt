package com.mariesta.menzies.washui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.mariesta.menzies.washui.effects.rememberWashRipple
import com.mariesta.menzies.washui.theme.WashTheme

private data class WashTabsContext(
    val value: String,
    val setValue: (String) -> Unit,
    val baseId: String,
)

private val LocalWashTabs = staticCompositionLocalOf<WashTabsContext?> { null }

@Composable
fun WashTabs(
    defaultValue: String,
    modifier: Modifier = Modifier,
    boxed: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colors = WashTheme.colors
    var value by remember { mutableStateOf(defaultValue) }
    val baseId = remember { "wash-tabs-${kotlin.random.Random.nextInt()}" }
    val ctx = WashTabsContext(value = value, setValue = { value = it }, baseId = baseId)
    val shape = RoundedCornerShape(colors.radiusField)

    CompositionLocalProvider(LocalWashTabs provides ctx) {
        Column(modifier = modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (boxed) {
                            Modifier
                                .clip(shape)
                                .background(colors.base_200)
                                .border(1.dp, colors.ink_border, shape)
                                .padding(4.dp)
                        } else {
                            Modifier
                        },
                    ),
            ) {
                content()
            }
        }
    }
}

@Composable
fun WashTab(
    value: String,
    modifier: Modifier = Modifier,
    panel: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val ctx = LocalWashTabs.current ?: error("WashTab must be used within WashTabs")
    val colors = WashTheme.colors
    val selected = ctx.value == value
    val shape = RoundedCornerShape(colors.radiusField)
    val interaction = remember { MutableInteractionSource() }
    val indication = rememberWashRipple(colors.primary.copy(alpha = 0.2f))

    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .clip(shape)
                .background(if (selected) colors.primary else colors.base_100, shape)
                .clickable(
                    interactionSource = interaction,
                    indication = indication,
                    onClick = { ctx.setValue(value) },
                )
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .semantics {
                    role = Role.Tab
                    this.selected = selected
                },
        ) {
            content()
        }
        if (panel != null && selected) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            ) {
                panel()
            }
        }
    }
}
