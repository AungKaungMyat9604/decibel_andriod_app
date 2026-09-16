package com.mariesta.menzies.washui.primitives

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Minimal Wash app shell: optional top bar stacked over scrollable content.
 * Replaces Material3 Scaffold for gallery chrome without Material layout slots.
 *
 * Does not paint a fill. Wrap with [com.mariesta.menzies.washui.components.WashBackground]
 * (or [com.mariesta.menzies.washui.effects.pageWash] on [modifier]) for page atmosphere.
 *
 * For web `WashShell` / `wash-shell-main` parity, pass
 * `contentPadding = PaddingValues(horizontal = 16.dp, vertical = 24.dp)`.
 *
 * Applies [safeDrawingPadding] so top chrome clears the status bar, display
 * cutout / punch-hole camera, and gesture / navigation bars when the host
 * activity draws edge-to-edge.
 */
@Composable
fun WashScaffold(
    modifier: Modifier = Modifier,
    topBar: @Composable (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    content: @Composable (PaddingValues) -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .safeDrawingPadding(),
    ) {
        topBar?.invoke()
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxSize()
                .padding(contentPadding),
        ) {
            content(contentPadding)
        }
    }
}
