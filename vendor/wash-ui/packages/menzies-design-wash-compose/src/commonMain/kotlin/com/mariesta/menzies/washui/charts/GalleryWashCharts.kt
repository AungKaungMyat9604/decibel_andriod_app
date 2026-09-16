package com.mariesta.menzies.washui.charts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

data class RangeAreaBand(
    val low: List<ChartPoint>,
    val high: List<ChartPoint>,
)

data class SlopeDatum(
    val label: String,
    val start: Float,
    val end: Float,
)

data class TimelineBar(
    val label: String,
    val start: Float,
    val end: Float,
    val lane: Int = 0,
)

data class HeatmapCell(
    val row: Int,
    val col: Int,
    val value: Float,
)

data class TreemapNode(
    val label: String,
    val value: Float,
)

data class SunburstRing(
    val values: List<Float>,
)

data class BubblePoint(
    val x: Float,
    val y: Float,
    val size: Float,
)

data class FunnelStage(
    val label: String,
    val value: Float,
)

data class BoxPlotDatum(
    val min: Float,
    val q1: Float,
    val median: Float,
    val q3: Float,
    val max: Float,
)

data class CandlestickDatum(
    val open: Float,
    val high: Float,
    val low: Float,
    val close: Float,
)

data class ViolinDatum(
    val densities: List<Float>,
)

@Composable
fun RangeAreaChart(
    band: RangeAreaBand,
    modifier: Modifier = Modifier,
    fillAlpha: Float = 0.28f,
) {
    val theme = rememberWashChartTheme()
    val points = band.low + band.high
    val scales = chartScales(points) ?: return
    Canvas(modifier = modifier.fillMaxWidth().height(220.dp)) {
        drawChartGrid(theme)
        val xScale = scales.xScale(size.width)
        val yScale = scales.yScale(size.height)
        if (band.low.isEmpty() || band.high.isEmpty()) return@Canvas
        val area = Path().apply {
            band.high.forEachIndexed { index, point ->
                val x = xScale(point.x)
                val y = yScale(point.y)
                if (index == 0) moveTo(x, y) else lineTo(x, y)
            }
            for (index in band.low.indices.reversed()) {
                val point = band.low[index]
                lineTo(xScale(point.x), yScale(point.y))
            }
            close()
        }
        drawPath(area, color = colorAt(theme, 0).copy(alpha = fillAlpha))
        drawLineSeries(band.high, colorAt(theme, 0), xScale, yScale)
        drawLineSeries(band.low, colorAt(theme, 1), xScale, yScale)
    }
}

@Composable
fun SlopeChart(
    data: List<SlopeDatum>,
    modifier: Modifier = Modifier,
) {
    val theme = rememberWashChartTheme()
    if (data.isEmpty()) return
    val values = data.flatMap { listOf(it.start, it.end) }
    val (minValue, maxValue) = signedValueScales(values)
    Canvas(modifier = modifier.fillMaxWidth().height(220.dp)) {
        drawChartGrid(theme)
        val range = (maxValue - minValue).takeIf { it != 0f } ?: 1f
        val leftX = size.width * 0.18f
        val rightX = size.width * 0.82f
        data.forEachIndexed { index, datum ->
            val y1 = size.height - ((datum.start - minValue) / range) * size.height
            val y2 = size.height - ((datum.end - minValue) / range) * size.height
            val color = colorAt(theme, index)
            drawLine(color, Offset(leftX, y1), Offset(rightX, y2), strokeWidth = 2.5f)
            drawCircle(color, radius = 5f, center = Offset(leftX, y1))
            drawCircle(color, radius = 5f, center = Offset(rightX, y2))
        }
    }
}

@Composable
fun MixedChart(
    columns: List<Float>,
    line: List<ChartPoint>,
    modifier: Modifier = Modifier,
) {
    val theme = rememberWashChartTheme()
    if (columns.isEmpty() && line.isEmpty()) return
    val allY = columns + line.map { it.y }
    val (minValue, maxValue) = signedValueScales(allY)
    Canvas(modifier = modifier.fillMaxWidth().height(220.dp)) {
        drawChartGrid(theme)
        val range = (maxValue - minValue).takeIf { it != 0f } ?: 1f
        val zeroY = if (minValue >= 0f) {
            size.height
        } else {
            size.height - ((0f - minValue) / range) * size.height
        }
        if (columns.isNotEmpty()) {
            val barWidth = size.width / (columns.size * 1.5f)
            columns.forEachIndexed { index, value ->
                val left = index * barWidth * 1.5f + barWidth * 0.25f
                val height = (abs(value) / range) * size.height
                val top = if (value >= 0f) zeroY - height else zeroY
                drawRect(
                    color = colorAt(theme, 0).copy(alpha = 0.55f),
                    topLeft = Offset(left, top),
                    size = Size(barWidth, height),
                )
            }
        }
        if (line.isNotEmpty()) {
            val scales = chartScales(line) ?: return@Canvas
            val xScale = scales.xScale(size.width)
            val yScale = { v: Float -> size.height - ((v - minValue) / range) * size.height }
            drawLineSeries(line, colorAt(theme, 1), xScale, yScale, strokeWidth = 3f)
        }
    }
}

@Composable
fun TimelineChart(
    bars: List<TimelineBar>,
    modifier: Modifier = Modifier,
) {
    val theme = rememberWashChartTheme()
    if (bars.isEmpty()) return
    val minStart = bars.minOf { it.start }
    val maxEnd = bars.maxOf { it.end }
    val lanes = (bars.maxOf { it.lane } + 1).coerceAtLeast(1)
    Canvas(modifier = modifier.fillMaxWidth().height(220.dp)) {
        drawChartGrid(theme)
        val range = (maxEnd - minStart).takeIf { it != 0f } ?: 1f
        val laneHeight = size.height / (lanes * 1.4f)
        bars.forEachIndexed { index, bar ->
            val left = ((bar.start - minStart) / range) * size.width
            val width = ((bar.end - bar.start) / range) * size.width
            val top = bar.lane * laneHeight * 1.4f + laneHeight * 0.2f
            drawRect(
                color = colorAt(theme, index),
                topLeft = Offset(left, top),
                size = Size(width.coerceAtLeast(4f), laneHeight),
            )
        }
    }
}

@Composable
fun RadialBarChart(
    values: List<Float>,
    modifier: Modifier = Modifier,
    maxValue: Float? = null,
) {
    val theme = rememberWashChartTheme()
    if (values.isEmpty()) return
    val ceiling = maxValue ?: values.maxOrNull()?.coerceAtLeast(1f) ?: 1f
    Canvas(modifier = modifier.fillMaxWidth().height(240.dp)) {
        drawChartGrid(theme, horizontalLines = 3)
        val center = Offset(size.width / 2f, size.height / 2f)
        val outer = min(size.width, size.height) * 0.42f
        val ringGap = outer / (values.size + 1f)
        values.forEachIndexed { index, value ->
            val radius = outer - index * ringGap
            val sweep = (value / ceiling).coerceIn(0f, 1f) * 270f
            drawArc(
                color = theme.grid,
                startAngle = 135f,
                sweepAngle = 270f,
                useCenter = false,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2, radius * 2),
                style = Stroke(width = ringGap * 0.55f, cap = StrokeCap.Round),
            )
            drawArc(
                color = colorAt(theme, index),
                startAngle = 135f,
                sweepAngle = sweep,
                useCenter = false,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2, radius * 2),
                style = Stroke(width = ringGap * 0.55f, cap = StrokeCap.Round),
            )
        }
    }
}

@Composable
fun PolarAreaChart(
    values: List<Float>,
    modifier: Modifier = Modifier,
) {
    val theme = rememberWashChartTheme()
    if (values.isEmpty()) return
    val max = values.maxOrNull()?.coerceAtLeast(1f) ?: 1f
    Canvas(modifier = modifier.fillMaxWidth().height(240.dp)) {
        drawChartGrid(theme, horizontalLines = 3)
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = min(size.width, size.height) * 0.38f
        val sweep = 360f / values.size
        values.forEachIndexed { index, value ->
            val r = radius * (value / max).coerceIn(0.08f, 1f)
            drawArc(
                color = colorAt(theme, index).copy(alpha = 0.75f),
                startAngle = -90f + index * sweep,
                sweepAngle = sweep,
                useCenter = true,
                topLeft = Offset(center.x - r, center.y - r),
                size = Size(r * 2, r * 2),
            )
        }
    }
}

@Composable
fun GaugeChart(
    value: Float,
    modifier: Modifier = Modifier,
    maxValue: Float = 100f,
) {
    val theme = rememberWashChartTheme()
    val ratio = (value / maxValue).coerceIn(0f, 1f)
    Canvas(modifier = modifier.fillMaxWidth().height(200.dp)) {
        val center = Offset(size.width / 2f, size.height * 0.78f)
        val radius = min(size.width, size.height) * 0.55f
        drawArc(
            color = theme.grid,
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(center.x - radius, center.y - radius),
            size = Size(radius * 2, radius * 2),
            style = Stroke(width = 18f, cap = StrokeCap.Round),
        )
        drawArc(
            color = colorAt(theme, 0),
            startAngle = 180f,
            sweepAngle = 180f * ratio,
            useCenter = false,
            topLeft = Offset(center.x - radius, center.y - radius),
            size = Size(radius * 2, radius * 2),
            style = Stroke(width = 18f, cap = StrokeCap.Round),
        )
        val needleAngle = PI + PI * ratio
        val needle = Offset(
            center.x + cos(needleAngle).toFloat() * radius * 0.78f,
            center.y + sin(needleAngle).toFloat() * radius * 0.78f,
        )
        drawLine(colorAt(theme, 1), center, needle, strokeWidth = 3.5f, cap = StrokeCap.Round)
        drawCircle(colorAt(theme, 1), radius = 6f, center = center)
    }
}

@Composable
fun SparklineChart(
    points: List<ChartPoint>,
    modifier: Modifier = Modifier,
) {
    val theme = rememberWashChartTheme()
    val sampled = downsampleSeries(points, 80)
    val scales = chartScales(sampled) ?: return
    Canvas(modifier = modifier.fillMaxWidth().height(64.dp)) {
        val xScale = scales.xScale(size.width)
        val yScale = scales.yScale(size.height)
        drawLineSeries(sampled, colorAt(theme, 0), xScale, yScale, strokeWidth = 2f)
        sampled.lastOrNull()?.let { last ->
            drawCircle(colorAt(theme, 0), radius = 3.5f, center = Offset(xScale(last.x), yScale(last.y)))
        }
    }
}

@Composable
fun HeatmapChart(
    cells: List<HeatmapCell>,
    rows: Int,
    cols: Int,
    modifier: Modifier = Modifier,
) {
    val theme = rememberWashChartTheme()
    if (cells.isEmpty() || rows <= 0 || cols <= 0) return
    val max = cells.maxOf { it.value }.coerceAtLeast(1f)
    Canvas(modifier = modifier.fillMaxWidth().height(220.dp)) {
        val cellW = size.width / cols
        val cellH = size.height / rows
        cells.forEach { cell ->
            val alpha = (cell.value / max).coerceIn(0.12f, 1f)
            drawRect(
                color = colorAt(theme, 0).copy(alpha = alpha),
                topLeft = Offset(cell.col * cellW, cell.row * cellH),
                size = Size(cellW - 2f, cellH - 2f),
            )
        }
    }
}

@Composable
fun TreemapChart(
    nodes: List<TreemapNode>,
    modifier: Modifier = Modifier,
) {
    val theme = rememberWashChartTheme()
    if (nodes.isEmpty()) return
    val total = nodes.sumOf { it.value.toDouble() }.toFloat().takeIf { it > 0f } ?: return
    Canvas(modifier = modifier.fillMaxWidth().height(220.dp)) {
        var x = 0f
        nodes.forEachIndexed { index, node ->
            val width = (node.value / total) * size.width
            drawRect(
                color = colorAt(theme, index).copy(alpha = 0.82f),
                topLeft = Offset(x + 1f, 1f),
                size = Size(width - 2f, size.height - 2f),
            )
            x += width
        }
    }
}

@Composable
fun SunburstChart(
    rings: List<SunburstRing>,
    modifier: Modifier = Modifier,
) {
    val theme = rememberWashChartTheme()
    if (rings.isEmpty()) return
    Canvas(modifier = modifier.fillMaxWidth().height(240.dp)) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val outer = min(size.width, size.height) * 0.42f
        val ringWidth = outer / (rings.size + 0.5f)
        rings.forEachIndexed { ringIndex, ring ->
            val total = ring.values.sum().takeIf { it > 0f } ?: return@forEachIndexed
            var start = -90f
            val radius = ringWidth * (ringIndex + 1.2f)
            ring.values.forEachIndexed { sliceIndex, value ->
                val sweep = (value / total) * 360f
                drawArc(
                    color = colorAt(theme, sliceIndex + ringIndex).copy(alpha = 0.85f),
                    startAngle = start,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = Size(radius * 2, radius * 2),
                    style = Stroke(width = ringWidth * 0.9f),
                )
                start += sweep
            }
        }
    }
}

@Composable
fun BubbleChart(
    points: List<BubblePoint>,
    modifier: Modifier = Modifier,
) {
    val theme = rememberWashChartTheme()
    if (points.isEmpty()) return
    val chartPoints = points.map { ChartPoint(it.x, it.y) }
    val scales = chartScales(chartPoints) ?: return
    val maxSize = points.maxOf { it.size }.coerceAtLeast(1f)
    Canvas(modifier = modifier.fillMaxWidth().height(220.dp)) {
        drawChartGrid(theme)
        val xScale = scales.xScale(size.width)
        val yScale = scales.yScale(size.height)
        points.forEachIndexed { index, point ->
            val radius = 6f + (point.size / maxSize) * 22f
            drawCircle(
                color = colorAt(theme, index).copy(alpha = 0.55f),
                radius = radius,
                center = Offset(xScale(point.x), yScale(point.y)),
            )
        }
    }
}

@Composable
fun FunnelChart(
    stages: List<FunnelStage>,
    modifier: Modifier = Modifier,
) {
    val theme = rememberWashChartTheme()
    if (stages.isEmpty()) return
    val max = stages.maxOf { it.value }.coerceAtLeast(1f)
    Canvas(modifier = modifier.fillMaxWidth().height(240.dp)) {
        val rowH = size.height / stages.size
        stages.forEachIndexed { index, stage ->
            val width = (stage.value / max) * size.width * 0.92f
            val left = (size.width - width) / 2f
            val top = index * rowH + 4f
            drawRect(
                color = colorAt(theme, index),
                topLeft = Offset(left, top),
                size = Size(width, rowH - 8f),
            )
        }
    }
}

@Composable
fun BoxPlotChart(
    series: List<BoxPlotDatum>,
    modifier: Modifier = Modifier,
) {
    val theme = rememberWashChartTheme()
    if (series.isEmpty()) return
    val values = series.flatMap { listOf(it.min, it.q1, it.median, it.q3, it.max) }
    val (minValue, maxValue) = signedValueScales(values)
    Canvas(modifier = modifier.fillMaxWidth().height(220.dp)) {
        drawChartGrid(theme)
        val range = (maxValue - minValue).takeIf { it != 0f } ?: 1f
        val slot = size.width / series.size
        series.forEachIndexed { index, datum ->
            val cx = index * slot + slot / 2f
            val y = { v: Float -> size.height - ((v - minValue) / range) * size.height }
            val color = colorAt(theme, index)
            drawLine(color, Offset(cx, y(datum.min)), Offset(cx, y(datum.max)), strokeWidth = 2f)
            drawRect(
                color = color.copy(alpha = 0.35f),
                topLeft = Offset(cx - slot * 0.18f, y(datum.q3)),
                size = Size(slot * 0.36f, y(datum.q1) - y(datum.q3)),
            )
            drawLine(
                color,
                Offset(cx - slot * 0.18f, y(datum.median)),
                Offset(cx + slot * 0.18f, y(datum.median)),
                strokeWidth = 3f,
            )
        }
    }
}

@Composable
fun ViolinChart(
    series: List<ViolinDatum>,
    modifier: Modifier = Modifier,
) {
    val theme = rememberWashChartTheme()
    if (series.isEmpty()) return
    Canvas(modifier = modifier.fillMaxWidth().height(220.dp)) {
        drawChartGrid(theme)
        val slot = size.width / series.size
        series.forEachIndexed { index, datum ->
            val densities = datum.densities
            if (densities.isEmpty()) return@forEachIndexed
            val maxD = densities.maxOrNull()?.coerceAtLeast(0.01f) ?: 0.01f
            val cx = index * slot + slot / 2f
            val path = Path()
            densities.forEachIndexed { step, density ->
                val y = size.height * (step / (densities.lastIndex.coerceAtLeast(1).toFloat()))
                val half = (density / maxD) * slot * 0.35f
                if (step == 0) path.moveTo(cx - half, y) else path.lineTo(cx - half, y)
            }
            for (step in densities.indices.reversed()) {
                val y = size.height * (step / (densities.lastIndex.coerceAtLeast(1).toFloat()))
                val half = (densities[step] / maxD) * slot * 0.35f
                path.lineTo(cx + half, y)
            }
            path.close()
            drawPath(path, color = colorAt(theme, index).copy(alpha = 0.45f))
            drawPath(path, color = colorAt(theme, index), style = Stroke(width = 1.5f))
        }
    }
}

@Composable
fun BeeswarmChart(
    values: List<Float>,
    modifier: Modifier = Modifier,
) {
    val theme = rememberWashChartTheme()
    if (values.isEmpty()) return
    val (minValue, maxValue) = signedValueScales(values)
    Canvas(modifier = modifier.fillMaxWidth().height(200.dp)) {
        drawChartGrid(theme, horizontalLines = 3)
        val range = (maxValue - minValue).takeIf { it != 0f } ?: 1f
        val midY = size.height / 2f
        values.forEachIndexed { index, value ->
            val x = ((value - minValue) / range) * size.width
            val jitter = ((index % 7) - 3) * 8f
            drawCircle(
                color = colorAt(theme, index),
                radius = 5.5f,
                center = Offset(x, midY + jitter),
            )
        }
    }
}

@Composable
fun WaffleChart(
    filled: Int,
    total: Int = 100,
    columns: Int = 10,
    modifier: Modifier = Modifier,
) {
    val theme = rememberWashChartTheme()
    val cells = total.coerceAtLeast(1)
    val cols = columns.coerceAtLeast(1)
    val rows = (cells + cols - 1) / cols
    Canvas(modifier = modifier.fillMaxWidth().height(220.dp)) {
        val cellW = size.width / cols
        val cellH = size.height / rows
        repeat(cells) { index ->
            val row = index / cols
            val col = index % cols
            val color = if (index < filled) colorAt(theme, 0) else theme.grid
            drawRect(
                color = color,
                topLeft = Offset(col * cellW + 2f, row * cellH + 2f),
                size = Size(cellW - 4f, cellH - 4f),
            )
        }
    }
}

@Composable
fun CandlestickChart(
    candles: List<CandlestickDatum>,
    modifier: Modifier = Modifier,
) {
    val theme = rememberWashChartTheme()
    if (candles.isEmpty()) return
    val values = candles.flatMap { listOf(it.open, it.high, it.low, it.close) }
    val (minValue, maxValue) = signedValueScales(values)
    Canvas(modifier = modifier.fillMaxWidth().height(220.dp)) {
        drawChartGrid(theme)
        val range = (maxValue - minValue).takeIf { it != 0f } ?: 1f
        val slot = size.width / candles.size
        candles.forEachIndexed { index, candle ->
            val cx = index * slot + slot / 2f
            val y = { v: Float -> size.height - ((v - minValue) / range) * size.height }
            val up = candle.close >= candle.open
            val color = if (up) colorAt(theme, 4) else colorAt(theme, 6)
            drawLine(color, Offset(cx, y(candle.high)), Offset(cx, y(candle.low)), strokeWidth = 2f)
            val top = y(maxOf(candle.open, candle.close))
            val bottom = y(minOf(candle.open, candle.close))
            drawRect(
                color = color,
                topLeft = Offset(cx - slot * 0.22f, top),
                size = Size(slot * 0.44f, (bottom - top).coerceAtLeast(2f)),
            )
        }
    }
}

@Composable
fun HistogramChart(
    bins: List<Float>,
    modifier: Modifier = Modifier,
) {
    ColumnChart(values = bins, modifier = modifier)
}

@Composable
fun CustomSeriesChart(
    series: List<ScatterSeries>,
    modifier: Modifier = Modifier,
) {
    val theme = rememberWashChartTheme()
    val all = series.flatMap { it.points }
    val scales = chartScales(all) ?: return
    Canvas(modifier = modifier.fillMaxWidth().height(220.dp)) {
        drawChartGrid(theme)
        val xScale = scales.xScale(size.width)
        val yScale = scales.yScale(size.height)
        series.forEachIndexed { index, item ->
            drawLineSeries(item.points, colorAt(theme, index), xScale, yScale, strokeWidth = 2.5f)
            item.points.forEach { point ->
                drawCircle(
                    color = colorAt(theme, index),
                    radius = 3.5f,
                    center = Offset(xScale(point.x), yScale(point.y)),
                )
            }
        }
    }
}

@Composable
fun InteractiveLineChart(
    points: List<ChartPoint>,
    modifier: Modifier = Modifier,
    onSelect: (ChartPoint?) -> Unit = {},
) {
    val theme = rememberWashChartTheme()
    val sampled = downsampleSeries(points, 120)
    val scales = chartScales(sampled) ?: return
    var selected by remember { mutableStateOf<ChartPoint?>(null) }
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(220.dp)
            .pointerInput(sampled) {
                detectTapGestures { offset ->
                    val nearest = sampled.minByOrNull { point ->
                        val x = scales.xScale(size.width.toFloat())(point.x)
                        val y = scales.yScale(size.height.toFloat())(point.y)
                        val dx = x - offset.x
                        val dy = y - offset.y
                        sqrt(dx * dx + dy * dy)
                    }
                    selected = nearest
                    onSelect(nearest)
                }
            },
    ) {
        drawChartGrid(theme)
        val xScale = scales.xScale(size.width)
        val yScale = scales.yScale(size.height)
        drawLineSeries(sampled, colorAt(theme, 0), xScale, yScale)
        selected?.let { point ->
            drawCircle(
                color = colorAt(theme, 1),
                radius = 8f,
                center = Offset(xScale(point.x), yScale(point.y)),
            )
        }
    }
}

@Composable
fun NarrativeChart(
    points: List<ChartPoint>,
    markers: List<Int>,
    modifier: Modifier = Modifier,
) {
    val theme = rememberWashChartTheme()
    val sampled = downsampleSeries(points, 120)
    val scales = chartScales(sampled) ?: return
    Canvas(modifier = modifier.fillMaxWidth().height(220.dp)) {
        drawChartGrid(theme)
        val xScale = scales.xScale(size.width)
        val yScale = scales.yScale(size.height)
        drawLineSeries(sampled, colorAt(theme, 0), xScale, yScale)
        markers.forEach { index ->
            val point = sampled.getOrNull(index) ?: return@forEach
            val center = Offset(xScale(point.x), yScale(point.y))
            drawCircle(color = colorAt(theme, 2), radius = 7f, center = center)
            drawLine(
                color = colorAt(theme, 2).copy(alpha = 0.5f),
                start = Offset(center.x, 0f),
                end = Offset(center.x, size.height),
                strokeWidth = 1.5f,
            )
        }
    }
}

@Composable
fun UnitChart(
    filled: Int,
    total: Int = 20,
    columns: Int = 10,
    modifier: Modifier = Modifier,
) {
    WaffleChart(filled = filled, total = total, columns = columns, modifier = modifier)
}
