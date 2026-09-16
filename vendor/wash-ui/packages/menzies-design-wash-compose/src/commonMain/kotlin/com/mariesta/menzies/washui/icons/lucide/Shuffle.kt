// Lightweight Lucide-style shuffle glyph for Decibel transport controls.
package com.mariesta.menzies.washui.icons.lucide

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.mariesta.menzies.washui.icons.LucideIcons

private var _shuffle: ImageVector? = null

public val LucideIcons.Shuffle: ImageVector
    get() {
        _shuffle?.let { return it }
        return ImageVector.Builder(
            name = "Lucide.Shuffle",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(
                fill = SolidColor(Color.Transparent),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(2f, 18f)
                horizontalLineTo(3.757f)
                curveTo(5.7f, 18f, 7.5f, 17f, 8.6f, 15.3f)
                lineTo(14f, 6.3f)
                curveTo(15.1f, 4.5f, 17f, 3.5f, 19f, 3.5f)
                horizontalLineTo(22f)
            }
            path(
                fill = SolidColor(Color.Transparent),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(2f, 6f)
                horizontalLineTo(3.757f)
                curveTo(5.7f, 6f, 7.5f, 7f, 8.6f, 8.7f)
                lineTo(14f, 17.7f)
                curveTo(15.1f, 19.5f, 17f, 20.5f, 19f, 20.5f)
                horizontalLineTo(22f)
            }
            path(
                fill = SolidColor(Color.Transparent),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(18f, 2f)
                lineTo(22f, 6f)
                lineTo(18f, 10f)
            }
            path(
                fill = SolidColor(Color.Transparent),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(18f, 14f)
                lineTo(22f, 18f)
                lineTo(18f, 22f)
            }
        }.build().also { _shuffle = it }
    }
