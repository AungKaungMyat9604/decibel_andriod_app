package com.mariesta.menzies.washui.charts

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.mariesta.menzies.washui.theme.WashColorScheme
import com.mariesta.menzies.washui.theme.WashTheme

data class WashChartTheme(
    val foreground: Color,
    val grid: Color,
    val axis: Color,
    val series: List<Color>,
    val annotation: Color,
)

@Composable
fun rememberWashChartTheme(): WashChartTheme {
    val colors = WashTheme.colors
    return remember(colors) {
        WashChartTheme(
            foreground = colors.base_content,
            grid = colors.ink_border.copy(alpha = 0.35f),
            axis = colors.ink_muted,
            series = listOf(
                colors.primary,
                colors.secondary,
                colors.accent,
                colors.info,
                colors.success,
                colors.warning,
                colors.error,
            ),
            annotation = colors.neutral,
        )
    }
}

data class ChartPoint(val x: Float, val y: Float)

data class ChartSlice(val label: String, val value: Float)

data class ScatterSeries(val name: String, val points: List<ChartPoint>)

data class RadarSeries(val name: String, val values: List<Float>)

fun indexedChartPoints(values: List<Float>): List<ChartPoint> =
    values.mapIndexed { index, value -> ChartPoint(index.toFloat(), value) }

data class ChartScales(
    val minX: Float,
    val maxX: Float,
    val minY: Float,
    val maxY: Float,
)

fun chartScales(points: List<ChartPoint>): ChartScales? {
    if (points.isEmpty()) return null
    return ChartScales(
        minX = points.minOf { it.x },
        maxX = points.maxOf { it.x },
        minY = points.minOf { it.y },
        maxY = points.maxOf { it.y },
    )
}

fun ChartScales.xScale(width: Float): (Float) -> Float = { value ->
    if (maxX == minX) 0f else ((value - minX) / (maxX - minX)) * width
}

fun ChartScales.yScale(height: Float): (Float) -> Float = { value ->
    val range = (maxY - minY).takeIf { it != 0f } ?: 1f
    height - ((value - minY) / range) * height
}

fun signedValueScales(values: List<Float>): Pair<Float, Float> {
    if (values.isEmpty()) return 0f to 1f
    val min = values.minOrNull() ?: 0f
    val max = values.maxOrNull() ?: 1f
    return when {
        min >= 0f -> 0f to max.coerceAtLeast(1f)
        max <= 0f -> min to 0f
        else -> min to max
    }
}

fun downsampleSeries(points: List<ChartPoint>, maxPoints: Int): List<ChartPoint> {
    if (points.size <= maxPoints) return points
    val step = points.size.toFloat() / maxPoints
    return buildList {
        var i = 0f
        while (size < maxPoints) {
            add(points[i.toInt().coerceIn(0, points.lastIndex)])
            i += step
        }
    }
}

fun DrawScope.drawChartGrid(theme: WashChartTheme, horizontalLines: Int = 4) {
    val step = size.height / horizontalLines
    repeat(horizontalLines + 1) { index ->
        val y = index * step
        drawLine(theme.grid, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
    }
}

fun linePath(points: List<ChartPoint>, xScale: (Float) -> Float, yScale: (Float) -> Float): Path {
    val path = Path()
    points.forEachIndexed { index, point ->
        val x = xScale(point.x)
        val y = yScale(point.y)
        if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    return path
}

fun DrawScope.drawLineSeries(
    points: List<ChartPoint>,
    color: Color,
    xScale: (Float) -> Float,
    yScale: (Float) -> Float,
    strokeWidth: Float = 2.5f,
) {
    if (points.size < 2) return
    drawPath(linePath(points, xScale, yScale), color = color, style = Stroke(width = strokeWidth))
}

fun colorAt(theme: WashChartTheme, index: Int): Color = theme.series[index % theme.series.size]

fun WashColorScheme.toChartTheme(): WashChartTheme = WashChartTheme(
    foreground = base_content,
    grid = ink_border.copy(alpha = 0.35f),
    axis = ink_muted,
    series = listOf(primary, secondary, accent, info, success, warning, error),
    annotation = neutral,
)
