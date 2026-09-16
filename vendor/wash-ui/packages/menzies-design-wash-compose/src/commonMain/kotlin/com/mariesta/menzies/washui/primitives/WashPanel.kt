package com.mariesta.menzies.washui.primitives

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mariesta.menzies.washui.effects.washPanel
import com.mariesta.menzies.washui.theme.WashTheme

/**
 * Paper panel surface. Default content padding is 16.dp (web `wash-panel`).
 * Pass [flush] = true for edge-to-edge headers or dense layouts (web `wash-panel-flush`).
 */
@Composable
fun WashPanel(
    modifier: Modifier = Modifier,
    flush: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = WashTheme.colors
    Column(
        modifier = modifier
            .fillMaxWidth()
            .washPanel()
            .border(1.dp, colors.ink_border, RoundedCornerShape(colors.radiusBox))
            .then(if (flush) Modifier else Modifier.padding(16.dp)),
        content = content,
    )
}
