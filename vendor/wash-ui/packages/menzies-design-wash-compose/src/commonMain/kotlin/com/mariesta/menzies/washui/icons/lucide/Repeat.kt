// Lightweight Lucide-style repeat glyph for Decibel transport controls.
package com.mariesta.menzies.washui.icons.lucide

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.mariesta.menzies.washui.icons.LucideIcons

private var _repeat: ImageVector? = null

public val LucideIcons.Repeat: ImageVector
    get() {
        _repeat?.let { return it }
        return ImageVector.Builder(
            name = "Lucide.Repeat",
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
                moveTo(17f, 2f)
                lineTo(21f, 6f)
                lineTo(17f, 10f)
            }
            path(
                fill = SolidColor(Color.Transparent),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(3f, 11f)
                verticalLineTo(10f)
                curveTo(3f, 7.8f, 4.8f, 6f, 7f, 6f)
                horizontalLineTo(21f)
            }
            path(
                fill = SolidColor(Color.Transparent),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(7f, 22f)
                lineTo(3f, 18f)
                lineTo(7f, 14f)
            }
            path(
                fill = SolidColor(Color.Transparent),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(21f, 13f)
                verticalLineTo(14f)
                curveTo(21f, 16.2f, 19.2f, 18f, 17f, 18f)
                horizontalLineTo(3f)
            }
        }.build().also { _repeat = it }
    }
