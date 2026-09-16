package com.mariesta.menzies.washui.primitives

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mariesta.menzies.washui.theme.WashTheme

@Composable
fun WashDivider(
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    thickness: Dp = 1.dp,
) {
    val colors = WashTheme.colors
    val line = if (color == Color.Unspecified) colors.ink_border.copy(alpha = 0.6f) else color
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(thickness)
            .background(line),
    )
}
