package com.mariesta.menzies.washui.charts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.mariesta.menzies.washui.theme.WashTheme
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

@Composable
fun AreaChart(
    points: List<ChartPoint>,
    modifier: Modifier = Modifier,
    maxPoints: Int = 120,
    fillAlpha: Float = 0.35f,
) {
    val theme = rememberWashChartTheme()
    val sampled = downsampleSeries(points, maxPoints)
    val scales = chartScales(sampled) ?: return
    Canvas(modifier = modifier.fillMaxWidth().height(220.dp)) {
        drawChartGrid(theme)
        val xScale = scales.xScale(size.width)
        val yScale = scales.yScale(size.height)
        val baselineY = yScale(0f.coerceIn(scales.minY, scales.maxY))
        val areaPath = Path().apply {
            moveTo(xScale(sampled.first().x), baselineY)
            sampled.forEach { point ->
                lineTo(xScale(point.x), yScale(point.y))
            }
            lineTo(xScale(sampled.last().x), baselineY)
            close()
        }
        drawPath(areaPath, color = colorAt(theme, 0).copy(alpha = fillAlpha))
        drawPath(linePath(sampled, xScale, yScale), color = colorAt(theme, 0), style = Stroke(width = 2.5f))
    }
}

@Composable
fun ColumnChart(
    values: List<Float>,
    modifier: Modifier = Modifier,
) {
    val theme = rememberWashChartTheme()
    if (values.isEmpty()) return
    val (minValue, maxValue) = signedValueScales(values)
    Canvas(modifier = modifier.fillMaxWidth().height(220.dp)) {
        drawChartGrid(theme)
        val barWidth = size.width / (values.size * 1.5f)
        val range = (maxValue - minValue).takeIf { it != 0f } ?: 1f
        val zeroY = if (minValue >= 0f) {
            size.height
        } else {
            size.height - ((0f - minValue) / range) * size.height
        }
        values.forEachIndexed { index, value ->
            val left = index * barWidth * 1.5f + barWidth * 0.25f
            val height = (kotlin.math.abs(value) / range) * size.height
            val top = if (value >= 0f) zeroY - height else zeroY
            drawRect(
                color = colorAt(theme, index),
                topLeft = Offset(left, top),
                size = Size(barWidth, height),
            )
        }
    }
}

@Composable
fun DonutChart(
    slices: List<ChartSlice>,
    modifier: Modifier = Modifier,
    innerRadiusRatio: Float = 0.55f,
) {
    val theme = rememberWashChartTheme()
    val holeColor = WashTheme.colors.base_100
    val values = slices.map { it.value }
    val total = values.sum().takeIf { it > 0f } ?: return
    Canvas(modifier = modifier.fillMaxWidth().height(220.dp)) {
        var start = -90f
        val radius = size.minDimension / 2.2f
        val center = Offset(size.width / 2f, size.height / 2f)
        values.forEachIndexed { index, value ->
            val sweep = (value / total) * 360f
            drawArc(
                color = colorAt(theme, index),
                startAngle = start,
                sweepAngle = sweep,
                useCenter = true,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2, radius * 2),
            )
            start += sweep
        }
        drawCircle(
            color = holeColor,
            radius = radius * innerRadiusRatio,
            center = center,
        )
    }
}

@Composable
fun ScatterChart(
    series: List<ScatterSeries>,
    modifier: Modifier = Modifier,
    pointRadius: Float = 5f,
) {
    val theme = rememberWashChartTheme()
    val allPoints = series.flatMap { it.points }
    val scales = chartScales(allPoints) ?: return
    Canvas(modifier = modifier.fillMaxWidth().height(220.dp)) {
        drawChartGrid(theme)
        val xScale = scales.xScale(size.width)
        val yScale = scales.yScale(size.height)
        series.forEachIndexed { seriesIndex, scatterSeries ->
            val color = colorAt(theme, seriesIndex)
            scatterSeries.points.forEach { point ->
                drawCircle(
                    color = color,
                    radius = pointRadius,
                    center = Offset(xScale(point.x), yScale(point.y)),
                )
            }
        }
    }
}

@Composable
fun RadarChart(
    categories: List<String>,
    series: List<RadarSeries>,
    modifier: Modifier = Modifier,
    maxValue: Float? = null,
    fillAlpha: Float = 0.2f,
) {
    val theme = rememberWashChartTheme()
    if (categories.isEmpty() || series.isEmpty()) return
    val axisCount = categories.size
    val computedMax = maxValue ?: series.flatMap { it.values }.maxOrNull()?.coerceAtLeast(1f) ?: 1f
    Canvas(modifier = modifier.fillMaxWidth().height(260.dp)) {
        drawChartGrid(theme, horizontalLines = 3)
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = min(size.width, size.height) * 0.34f
        val ringCount = 4
        repeat(ringCount) { ring ->
            val ringRadius = radius * ((ring + 1) / ringCount.toFloat())
            val ringPath = radarPolygonPath(center, ringRadius, axisCount, 0f)
            drawPath(ringPath, color = theme.grid, style = Stroke(width = 1f))
        }
        for (axis in 0 until axisCount) {
            val angle = radarAngle(axis, axisCount)
            val end = Offset(
                center.x + cos(angle).toFloat() * radius,
                center.y + sin(angle).toFloat() * radius,
            )
            drawLine(theme.grid, center, end, strokeWidth = 1f)
        }
        series.forEachIndexed { seriesIndex, radarSeries ->
            val values = radarSeries.values.take(axisCount)
            if (values.isEmpty()) return@forEachIndexed
            val color = colorAt(theme, seriesIndex)
            val polygon = Path()
            values.forEachIndexed { axis, value ->
                val ratio = (value / computedMax).coerceIn(0f, 1f)
                val angle = radarAngle(axis, axisCount)
                val point = Offset(
                    center.x + cos(angle).toFloat() * radius * ratio,
                    center.y + sin(angle).toFloat() * radius * ratio,
                )
                if (axis == 0) polygon.moveTo(point.x, point.y) else polygon.lineTo(point.x, point.y)
            }
            polygon.close()
            drawPath(polygon, color = color.copy(alpha = fillAlpha))
            drawPath(polygon, color = color, style = Stroke(width = 2f))
        }
    }
}

private fun radarAngle(axis: Int, axisCount: Int): Double =
    -PI / 2 + (2 * PI * axis / axisCount)

private fun radarPolygonPath(center: Offset, radius: Float, axisCount: Int, rotation: Float): Path {
    val path = Path()
    repeat(axisCount) { axis ->
        val angle = radarAngle(axis, axisCount) + rotation
        val point = Offset(
            center.x + cos(angle).toFloat() * radius,
            center.y + sin(angle).toFloat() * radius,
        )
        if (axis == 0) path.moveTo(point.x, point.y) else path.lineTo(point.x, point.y)
    }
    path.close()
    return path
}
