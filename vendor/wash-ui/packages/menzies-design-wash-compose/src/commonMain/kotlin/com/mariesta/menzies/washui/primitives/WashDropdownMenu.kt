package com.mariesta.menzies.washui.primitives

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.mariesta.menzies.washui.theme.WashTheme

/**
 * Anchored action menu. Mirrors web recipe `menu rounded-box border border-ink-border bg-base-100`.
 */
@Composable
fun WashDropdownMenu(
    open: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    items: List<String>,
    onItemClick: (String) -> Unit = {},
) {
    if (!open) return

    val colors = WashTheme.colors
    val shape = RoundedCornerShape(colors.radiusBox)

    Popup(
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true),
    ) {
        Column(
            modifier = modifier
                .padding(top = 4.dp)
                .widthIn(min = 180.dp)
                .shadow(6.dp, shape)
                .clip(shape)
                .background(colors.base_100, shape)
                .border(1.dp, colors.ink_border, shape)
                .padding(vertical = 6.dp),
        ) {
            items.forEach { label ->
                WashText(
                    text = label,
                    color = colors.base_content,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onItemClick(label)
                            onDismiss()
                        }
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                )
            }
        }
    }
}
