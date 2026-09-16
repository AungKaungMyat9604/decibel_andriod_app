package com.mariesta.menzies.washui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.mariesta.menzies.washui.effects.pageWash

/**
 * Page atmosphere: soft pigment radial washes over base-100 (web `page-wash`).
 * Wrap app shells so content sits on the same paper language as demo-web.
 *
 * Grain is off by default for smooth full-screen performance. Enable only for
 * decorative previews; the wash brushes are cached via [pageWash].
 */
@Composable
fun WashBackground(
    modifier: Modifier = Modifier,
    grain: Boolean = false,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier.pageWash(grain = grain),
        content = content,
    )
}
