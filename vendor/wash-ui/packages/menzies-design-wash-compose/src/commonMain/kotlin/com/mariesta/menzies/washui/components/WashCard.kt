package com.mariesta.menzies.washui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import com.mariesta.menzies.washui.primitives.WashText
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mariesta.menzies.washui.effects.washPanel
import com.mariesta.menzies.washui.theme.WashTheme

@Composable
fun WashCard(
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    bordered: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = WashTheme.colors
    val shape = RoundedCornerShape(colors.radiusBox)
    val padding = if (compact) 12.dp else 16.dp

    Column(
        modifier = modifier
            .fillMaxWidth()
            .washPanel()
            .then(
                if (bordered) Modifier.border(1.dp, colors.ink_border, shape) else Modifier,
            )
            .padding(padding),
        content = content,
    )
}

@Composable
fun WashCardBody(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier = modifier.fillMaxWidth(), content = content)
}

enum class WashCardTitleTone {
    Primary,
    Secondary,
    Error,
}

@Composable
fun WashCardTitle(
    text: String,
    modifier: Modifier = Modifier,
    tone: WashCardTitleTone = WashCardTitleTone.Primary,
) {
    val colors = WashTheme.colors
    val titleColor = when (tone) {
        WashCardTitleTone.Primary -> colors.primary
        WashCardTitleTone.Secondary -> colors.secondary
        WashCardTitleTone.Error -> colors.error
    }
    WashText(
        text = text,
        color = titleColor,
        fontWeight = FontWeight.Bold,
        modifier = modifier.padding(bottom = 8.dp),
    )
}
