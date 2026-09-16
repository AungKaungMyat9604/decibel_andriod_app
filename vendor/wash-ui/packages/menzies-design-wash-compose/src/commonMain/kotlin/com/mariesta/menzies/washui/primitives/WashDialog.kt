package com.mariesta.menzies.washui.primitives

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.mariesta.menzies.washui.effects.washPanel
import com.mariesta.menzies.washui.theme.WashTheme

enum class WashDialogTone {
    Primary,
    Secondary,
    Error,
}

@Composable
fun WashDialog(
    open: Boolean,
    onClose: () -> Unit,
    title: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    tone: WashDialogTone = WashDialogTone.Primary,
    dismissOnClickOutside: Boolean = true,
    actions: @Composable (() -> Unit)? = null,
    content: @Composable (() -> Unit)? = null,
) {
    if (!open) return

    val colors = WashTheme.colors
    val titleColor = when (tone) {
        WashDialogTone.Primary -> colors.primary
        WashDialogTone.Secondary -> colors.secondary
        WashDialogTone.Error -> colors.error
    }
    val shape = RoundedCornerShape(colors.radiusBox)

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = dismissOnClickOutside,
        ),
    ) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .washPanel()
                .background(colors.base_100, shape)
                .border(1.dp, colors.ink_border, shape)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            WashText(
                text = title,
                color = titleColor,
                fontWeight = FontWeight.Bold,
            )
            if (!description.isNullOrBlank()) {
                WashText(
                    text = description,
                    color = colors.ink_muted,
                    modifier = Modifier.padding(vertical = 4.dp),
                )
            }
            content?.invoke()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.End,
            ) {
                if (actions != null) {
                    actions()
                } else {
                    WashButton(
                        onClick = onClose,
                        text = "Close",
                        variant = WashButtonVariant.Ghost,
                    )
                }
            }
        }
    }
}
