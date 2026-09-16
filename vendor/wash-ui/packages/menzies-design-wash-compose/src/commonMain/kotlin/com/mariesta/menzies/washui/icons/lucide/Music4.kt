// Lucide Music4 — matches Menzies Design / lucide-react music-4.
package com.mariesta.menzies.washui.icons.lucide

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.mariesta.menzies.washui.icons.LucideIcons

private var _music4: ImageVector? = null

public val LucideIcons.Music4: ImageVector
    get() {
        _music4?.let { return it }
        return ImageVector.Builder(
            name = "Lucide.Music4",
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
                moveTo(9f, 18f)
                lineTo(9f, 5f)
                lineTo(21f, 3f)
                lineTo(21f, 16f)
            }
            path(
                fill = SolidColor(Color.Transparent),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(9f, 9f)
                lineTo(21f, 7f)
            }
            path(
                fill = SolidColor(Color.Transparent),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(3f, 18f)
                arcTo(horizontalEllipseRadius = 3f, verticalEllipseRadius = 3f, theta = 0f, isMoreThanHalf = true, isPositiveArc = false, x1 = 9f, y1 = 18f)
                arcTo(horizontalEllipseRadius = 3f, verticalEllipseRadius = 3f, theta = 0f, isMoreThanHalf = true, isPositiveArc = false, x1 = 3f, y1 = 18f)
            }
            path(
                fill = SolidColor(Color.Transparent),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(15f, 16f)
                arcTo(horizontalEllipseRadius = 3f, verticalEllipseRadius = 3f, theta = 0f, isMoreThanHalf = true, isPositiveArc = false, x1 = 21f, y1 = 16f)
                arcTo(horizontalEllipseRadius = 3f, verticalEllipseRadius = 3f, theta = 0f, isMoreThanHalf = true, isPositiveArc = false, x1 = 15f, y1 = 16f)
            }
        }.build().also { _music4 = it }
    }
