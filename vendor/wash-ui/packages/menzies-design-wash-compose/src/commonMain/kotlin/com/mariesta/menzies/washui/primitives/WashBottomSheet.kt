package com.mariesta.menzies.washui.primitives

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.mariesta.menzies.washui.theme.WashTheme

/**
 * Compact bottom-anchored sheet using a dialog host (no Material ModalBottomSheet).
 * Good enough for gallery demos of sheet-style filters.
 */
@Composable
fun WashBottomSheet(
    open: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    if (!open) return

    val colors = WashTheme.colors
    val shape = RoundedCornerShape(topStart = colors.radiusBox, topEnd = colors.radiusBox)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .clip(shape)
                .background(colors.base_100, shape)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            content = content,
        )
    }
}
