package com.mariesta.menzies.washui.icons

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Marker object for Lucide ImageVectors (lucide-react 1.28.0 paths). */
public object LucideIcons

/** Marker object for Simple Icons brand ImageVectors. */
public object BrandIcons

/**
 * Renders a Wash icon via Foundation [Image] + vector painter (no Material Icon host).
 * Brand marks keep baked-in Simple Icons colors when [tint] is [Color.Unspecified];
 * Lucide strokes tint when a color is supplied.
 */
@Composable
public fun WashIcon(
    imageVector: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = Color.Unspecified,
    size: Dp = 24.dp,
) {
    Image(
        painter = rememberVectorPainter(imageVector),
        contentDescription = contentDescription,
        modifier = modifier.size(size),
        colorFilter = if (tint != Color.Unspecified) ColorFilter.tint(tint) else null,
    )
}
