package com.mariesta.menzies.washui.primitives

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.mariesta.menzies.washui.theme.WashTheme

data class WashSelectOption(
    val value: String,
    val label: String,
    val disabled: Boolean = false,
)

@Composable
fun WashSelect(
    value: String,
    onValueChange: (String) -> Unit,
    options: List<WashSelectOption>,
    modifier: Modifier = Modifier,
    label: String? = null,
    hint: String? = null,
    enabled: Boolean = true,
) {
    val colors = WashTheme.colors
    var expanded by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(colors.radiusField)
    val selectedLabel = options.firstOrNull { it.value == value }?.label ?: value

    WashFormField(
        label = label,
        hint = hint,
        error = null,
        requiredMark = false,
        modifier = modifier,
    ) {
        Box {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(shape)
                    .background(if (enabled) colors.base_200 else colors.base_200.copy(alpha = 0.5f), shape)
                    .border(BorderStroke(1.dp, colors.ink_border), shape)
                    .clickable(enabled = enabled) { expanded = true }
                    .padding(horizontal = 12.dp, vertical = 12.dp),
            ) {
                WashText(
                    text = selectedLabel,
                    color = if (enabled) colors.base_content else colors.base_content.copy(alpha = 0.5f),
                )
            }
            if (expanded) {
                Popup(
                    onDismissRequest = { expanded = false },
                    properties = PopupProperties(focusable = true),
                ) {
                    Column(
                        modifier = Modifier
                            .padding(top = 4.dp)
                            .widthIn(min = 180.dp)
                            .fillMaxWidth()
                            .shadow(6.dp, shape)
                            .clip(shape)
                            .background(colors.base_100, shape)
                            .border(BorderStroke(1.dp, colors.ink_border), shape)
                            .padding(vertical = 6.dp),
                    ) {
                        options.forEach { option ->
                            WashText(
                                text = option.label,
                                color = if (option.disabled) {
                                    colors.base_content.copy(alpha = 0.4f)
                                } else {
                                    colors.base_content
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(enabled = !option.disabled) {
                                        onValueChange(option.value)
                                        expanded = false
                                    }
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}
