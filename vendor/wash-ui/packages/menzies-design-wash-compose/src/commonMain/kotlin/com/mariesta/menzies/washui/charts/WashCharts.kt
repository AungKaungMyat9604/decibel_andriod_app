package com.mariesta.menzies.washui.charts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun LineChart(
    points: List<ChartPoint>,
    modifier: Modifier = Modifier,
    maxPoints: Int = 120,
) {
    val theme = rememberWashChartTheme()
    val sampled = downsampleSeries(points, maxPoints)
    if (sampled.isEmpty()) return
    val minX = sampled.minOf { it.x }
    val maxX = sampled.maxOf { it.x }
    val minY = sampled.minOf { it.y }
    val maxY = sampled.maxOf { it.y }
    Canvas(modifier = modifier.fillMaxWidth().height(220.dp)) {
        drawChartGrid(theme)
        val xScale = { v: Float ->
            if (maxX == minX) 0f else ((v - minX) / (maxX - minX)) * size.width
        }
        val yScale = { v: Float ->
            val range = (maxY - minY).takeIf { it != 0f } ?: 1f
            size.height - ((v - minY) / range) * size.height
        }
        drawLineSeries(sampled, colorAt(theme, 0), xScale, yScale)
    }
}

@Composable
fun BarChart(
    values: List<Float>,
    modifier: Modifier = Modifier,
) {
    val theme = rememberWashChartTheme()
    if (values.isEmpty()) return
    val (minValue, maxValue) = signedValueScales(values)
    Canvas(modifier = modifier.fillMaxWidth().height(220.dp)) {
        drawChartGrid(theme)
        val barHeight = size.height / (values.size * 1.5f)
        val range = (maxValue - minValue).takeIf { it != 0f } ?: 1f
        val zeroX = if (minValue >= 0f) {
            0f
        } else {
            ((0f - minValue) / range) * size.width
        }
        values.forEachIndexed { index, value ->
            val top = index * barHeight * 1.5f + barHeight * 0.25f
            val barWidth = (kotlin.math.abs(value) / range) * size.width
            val left = if (value >= 0f) zeroX else zeroX - barWidth
            drawRect(
                color = colorAt(theme, index),
                topLeft = androidx.compose.ui.geometry.Offset(left, top),
                size = androidx.compose.ui.geometry.Size(barWidth, barHeight),
            )
        }
    }
}

@Composable
fun PieChart(
    values: List<Float>,
    modifier: Modifier = Modifier,
) {
    val theme = rememberWashChartTheme()
    val total = values.sum().takeIf { it > 0f } ?: return
    Canvas(modifier = modifier.fillMaxWidth().height(220.dp)) {
        var start = -90f
        val radius = size.minDimension / 2.2f
        val center = androidx.compose.ui.geometry.Offset(size.width / 2f, size.height / 2f)
        values.forEachIndexed { index, value ->
            val sweep = (value / total) * 360f
            drawArc(
                color = colorAt(theme, index),
                startAngle = start,
                sweepAngle = sweep,
                useCenter = true,
                topLeft = androidx.compose.ui.geometry.Offset(center.x - radius, center.y - radius),
                size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
            )
            start += sweep
        }
    }
}
