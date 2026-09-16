package com.mariesta.menzies.washui.primitives

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mariesta.menzies.washui.theme.WashTheme

/**
 * Wash navbar chrome: paper background, ink border, brand + actions.
 * Mirrors web recipe `navbar border-b border-ink-border/80 bg-base-100/80`.
 */
@Composable
fun WashTopBar(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    navigationIcon: @Composable (() -> Unit)? = null,
    /** Optional brand mark shown beside the title (e.g. shared launcher logo). */
    brand: @Composable (() -> Unit)? = null,
    actions: @Composable (RowScope.() -> Unit)? = null,
) {
    val colors = WashTheme.colors

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .background(colors.base_100.copy(alpha = 0.92f))
                .padding(horizontal = 4.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            if (navigationIcon != null) {
                Box(contentAlignment = Alignment.Center) {
                    navigationIcon()
                }
            }
            if (brand != null) {
                Box(
                    modifier = Modifier.padding(start = 4.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    brand()
                }
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp),
            ) {
                WashText(
                    text = title,
                    color = colors.base_content,
                    fontWeight = FontWeight.SemiBold,
                )
                if (!subtitle.isNullOrBlank()) {
                    WashText(
                        text = subtitle,
                        color = colors.primary,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
            if (actions != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    content = actions,
                )
            }
        }
        WashDivider()
    }
}
