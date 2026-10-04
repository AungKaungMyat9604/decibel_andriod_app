package com.decibel.ui.screens

import android.content.res.Configuration
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.decibel.data.StandbyWaveStyle
import com.decibel.player.PlayableItem
import com.decibel.player.PlaybackWaveCapture
import com.decibel.ui.components.HoldSeekIconButton
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.sin

private val AmoledBlack = Color.Black
private const val ExitTipHideMs = 2_400L
private const val ChromeHideMs = 5_000L

/**
 * Full-screen AMOLED standby. Wave accents follow [MaterialTheme] primary so
 * Standby matches the app palette; background stays pure black for OLED.
 * Landscape: wave column | track + controls column.
 * Transport buttons auto-hide when idle; tap to show. Long-press to exit.
 */
@Composable
fun StandbyScreen(
    track: PlayableItem,
    isPlaying: Boolean,
    positionMs: Long,
    durationMs: Long,
    waveStyle: StandbyWaveStyle,
    cycleWavePerTrack: Boolean = false,
    waveCapture: PlaybackWaveCapture,
    sleepRemainingLabel: String? = null,
    onDismiss: () -> Unit,
    onTogglePlay: () -> Unit,
    onSeek: (Long) -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
) {
    val landscape =
        LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val scheme = MaterialTheme.colorScheme
    val accent = scheme.primary
    val accentDim = scheme.primary.copy(alpha = 0.35f)
    val titleColor = Color.White.copy(alpha = 0.92f)
    val mutedColor = scheme.onSurfaceVariant.copy(alpha = 0.75f).let {
        // Keep readable on pure black even in light theme.
        if (it.red + it.green + it.blue < 1.2f) Color(0xFFB0B0B0) else it
    }
    val controlColor = Color.White.copy(alpha = 0.78f)
    var showExitTip by remember { mutableStateOf(false) }
    var exitTipEpoch by remember { mutableLongStateOf(0L) }
    var chromeVisible by remember { mutableStateOf(true) }
    var chromeEpoch by remember { mutableLongStateOf(0L) }

    val styles = StandbyWaveStyle.entries
    var cycleIndex by remember(waveStyle) {
        mutableIntStateOf(styles.indexOf(waveStyle).coerceAtLeast(0))
    }
    var lastTrackId by remember { mutableStateOf(track.id) }
    LaunchedEffect(track.id, cycleWavePerTrack, waveStyle) {
        if (!cycleWavePerTrack) {
            cycleIndex = styles.indexOf(waveStyle).coerceAtLeast(0)
            lastTrackId = track.id
            return@LaunchedEffect
        }
        if (track.id != lastTrackId) {
            cycleIndex = (cycleIndex + 1) % styles.size
            lastTrackId = track.id
        }
    }
    val activeWaveStyle = if (cycleWavePerTrack) {
        styles[cycleIndex % styles.size]
    } else {
        waveStyle
    }

    fun bumpChrome() {
        chromeVisible = true
        chromeEpoch++
    }

    var frame by remember { mutableLongStateOf(0L) }
    // Cap redraw: ~30 fps while playing, ~12 fps when paused — vsync 60/120 was wasteful.
    LaunchedEffect(isPlaying) {
        val intervalMs = if (isPlaying) 33L else 83L
        while (true) {
            frame = System.nanoTime() / 1_000_000L
            delay(intervalMs)
        }
    }
    LaunchedEffect(showExitTip, exitTipEpoch) {
        if (!showExitTip) return@LaunchedEffect
        delay(ExitTipHideMs)
        showExitTip = false
    }
    LaunchedEffect(chromeVisible, chromeEpoch) {
        if (!chromeVisible) return@LaunchedEffect
        delay(ChromeHideMs)
        chromeVisible = false
        showExitTip = false
    }
    val frameTick = frame
    val bandsBuf = remember { FloatArray(PlaybackWaveCapture.BAND_COUNT) }
    val pausedBandsBuf = remember { FloatArray(PlaybackWaveCapture.BAND_COUNT) }
    waveCapture.copyBandsInto(bandsBuf)
    val energy = waveCapture.snapshotEnergy()
    val liveEnergy = if (isPlaying) energy.coerceIn(0f, 1f) else energy * 0.2f
    val liveBands = if (isPlaying) {
        bandsBuf
    } else {
        for (i in bandsBuf.indices) {
            pausedBandsBuf[i] = bandsBuf[i] * 0.15f
        }
        pausedBandsBuf
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AmoledBlack)
            .pointerInput(chromeVisible) {
                detectTapGestures(
                    onLongPress = { onDismiss() },
                    onTap = {
                        if (!chromeVisible) {
                            bumpChrome()
                        } else {
                            bumpChrome()
                            showExitTip = true
                            exitTipEpoch++
                        }
                    },
                )
            },
    ) {
        if (landscape) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StandbyWaveCanvas(
                    modifier = Modifier
                        .weight(1.15f)
                        .fillMaxHeight()
                        .padding(end = 12.dp),
                    waveStyle = activeWaveStyle,
                    liveBands = liveBands,
                    liveEnergy = liveEnergy,
                    frameTick = frameTick,
                    accent = accent,
                    accentDim = accentDim,
                    centerFrac = 0.5f,
                )
                StandbyMetaColumn(
                    modifier = Modifier
                        .weight(0.85f)
                        .fillMaxHeight()
                        .padding(start = 8.dp),
                    track = track,
                    isPlaying = isPlaying,
                    positionMs = positionMs,
                    durationMs = durationMs,
                    sleepRemainingLabel = sleepRemainingLabel,
                    chromeVisible = chromeVisible,
                    onChromeInteract = { bumpChrome() },
                    onTogglePlay = onTogglePlay,
                    onSeek = onSeek,
                    onPrevious = onPrevious,
                    onNext = onNext,
                    centered = true,
                    titleColor = titleColor,
                    mutedColor = mutedColor,
                    controlColor = controlColor,
                    accent = accent,
                )
            }
        } else {
            StandbyWaveCanvas(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 48.dp),
                waveStyle = activeWaveStyle,
                liveBands = liveBands,
                liveEnergy = liveEnergy,
                frameTick = frameTick,
                accent = accent,
                accentDim = accentDim,
                centerFrac = 0.42f,
            )
            StandbyMetaColumn(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 28.dp, vertical = 28.dp),
                track = track,
                isPlaying = isPlaying,
                positionMs = positionMs,
                durationMs = durationMs,
                sleepRemainingLabel = sleepRemainingLabel,
                chromeVisible = chromeVisible,
                onChromeInteract = { bumpChrome() },
                onTogglePlay = onTogglePlay,
                onSeek = onSeek,
                onPrevious = onPrevious,
                onNext = onNext,
                centered = false,
                titleColor = titleColor,
                mutedColor = mutedColor,
                controlColor = controlColor,
                accent = accent,
            )
        }

        if (showExitTip && chromeVisible) {
            Text(
                text = "Long-press to exit standby",
                color = Color.White.copy(alpha = 0.95f),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 20.dp, start = 24.dp, end = 24.dp)
                    .background(Color.Black.copy(alpha = 0.72f), RoundedCornerShape(20.dp))
                    .padding(horizontal = 14.dp, vertical = 8.dp),
            )
        }
    }
}

@Composable
private fun StandbyWaveCanvas(
    modifier: Modifier,
    waveStyle: StandbyWaveStyle,
    liveBands: FloatArray,
    liveEnergy: Float,
    frameTick: Long,
    accent: Color,
    accentDim: Color,
    centerFrac: Float,
) {
    Canvas(modifier = modifier) {
        @Suppress("UNUSED_EXPRESSION")
        frameTick
        when (waveStyle) {
            StandbyWaveStyle.Bars -> drawBars(liveBands, accent, centerFrac)
            StandbyWaveStyle.DenseBars -> drawDenseBars(liveBands, accent, centerFrac)
            StandbyWaveStyle.PeakBars -> drawPeakBars(liveBands, accent, centerFrac)
            StandbyWaveStyle.FloorBars -> drawFloorBars(liveBands, accent, accentDim, centerFrac)
            StandbyWaveStyle.LedBars -> drawLedBars(liveBands, accent, centerFrac)
            StandbyWaveStyle.SoftWave -> drawSoftWave(liveBands, liveEnergy, accent, accentDim, centerFrac)
            StandbyWaveStyle.Rings -> drawRings(liveEnergy, liveBands, accent, centerFrac)
            StandbyWaveStyle.Orbit -> drawOrbit(liveEnergy, liveBands, accent, centerFrac)
            StandbyWaveStyle.Pulse -> drawBreathingPulse(liveEnergy, liveBands, accent, centerFrac)
            StandbyWaveStyle.MirroredBars -> drawMirroredBars(liveBands, accent, centerFrac)
            StandbyWaveStyle.Spectrum -> drawSpectrum(liveBands, liveEnergy, accent, accentDim, centerFrac)
            StandbyWaveStyle.SpectrumArea -> drawSpectrumArea(liveBands, liveEnergy, accent, accentDim, centerFrac)
            StandbyWaveStyle.CenterSpectrum -> drawCenterSpectrum(liveBands, liveEnergy, accent, accentDim, centerFrac)
            StandbyWaveStyle.SplitSpectrum -> drawSplitSpectrum(liveBands, liveEnergy, accent, accentDim, centerFrac)
            StandbyWaveStyle.NeonBars -> drawNeonBars(liveBands, accent, centerFrac)
            StandbyWaveStyle.Spiral -> drawSpiral(liveEnergy, liveBands, frameTick, accent, centerFrac)
            StandbyWaveStyle.Lattice -> drawLattice(liveBands, liveEnergy, accent, centerFrac)
            StandbyWaveStyle.Ribbon -> drawRibbon(liveBands, liveEnergy, frameTick, accent, centerFrac)
            StandbyWaveStyle.Starburst -> drawStarburst(liveEnergy, liveBands, frameTick, accent, centerFrac)
        }
    }
}

@Composable
private fun StandbyMetaColumn(
    modifier: Modifier,
    track: PlayableItem,
    isPlaying: Boolean,
    positionMs: Long,
    durationMs: Long,
    sleepRemainingLabel: String?,
    chromeVisible: Boolean,
    onChromeInteract: () -> Unit,
    onTogglePlay: () -> Unit,
    onSeek: (Long) -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    centered: Boolean,
    titleColor: Color,
    mutedColor: Color,
    controlColor: Color,
    accent: Color,
) {
    var holdSeekSpeed by remember { mutableStateOf<Int?>(null) }
    var holdSeekForward by remember { mutableStateOf(true) }
    val duration = durationMs.coerceAtLeast(1L)
    val position = positionMs.coerceIn(0L, duration)

    fun interact(block: () -> Unit) {
        onChromeInteract()
        block()
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = if (centered) {
            Arrangement.Center
        } else {
            Arrangement.spacedBy(6.dp)
        },
    ) {
        Text(
            text = track.title,
            color = titleColor,
            fontSize = if (centered) 20.sp else 18.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = if (centered) 4 else 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            text = track.uploader,
            color = mutedColor,
            fontSize = 13.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp),
        )
        if (!sleepRemainingLabel.isNullOrBlank()) {
            Text(
                text = "Sleep $sleepRemainingLabel",
                color = accent,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                maxLines = 1,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = if (centered) 16.dp else 8.dp)
                .height(2.dp)
                .background(mutedColor.copy(alpha = 0.28f), RoundedCornerShape(1.dp)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth((position.toFloat() / duration.toFloat()).coerceIn(0f, 1f))
                    .height(2.dp)
                    .background(accent.copy(alpha = 0.85f), RoundedCornerShape(1.dp)),
            )
        }

        // Reserve control-row height so hide/show never shifts title/progress.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = if (centered) 16.dp else 8.dp)
                .height(44.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (chromeVisible) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    HoldSeekIconButton(
                        forward = false,
                        positionMs = positionMs,
                        durationMs = durationMs,
                        enabled = true,
                        imageVector = Icons.Default.SkipPrevious,
                        contentDescription = "Previous",
                        tint = controlColor,
                        onClick = { interact(onPrevious) },
                        onSeek = { ms ->
                            onChromeInteract()
                            onSeek(ms)
                        },
                        onHoldSpeedChange = { speed ->
                            if (speed != null) onChromeInteract()
                            holdSeekForward = false
                            holdSeekSpeed = speed
                        },
                        modifier = Modifier.size(40.dp),
                        iconModifier = Modifier.size(22.dp),
                    )
                    IconButton(
                        onClick = { interact(onTogglePlay) },
                        modifier = Modifier.size(44.dp),
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = accent.copy(alpha = 0.95f),
                            modifier = Modifier.size(26.dp),
                        )
                    }
                    HoldSeekIconButton(
                        forward = true,
                        positionMs = positionMs,
                        durationMs = durationMs,
                        enabled = true,
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Next",
                        tint = controlColor,
                        onClick = { interact(onNext) },
                        onSeek = { ms ->
                            onChromeInteract()
                            onSeek(ms)
                        },
                        onHoldSpeedChange = { speed ->
                            if (speed != null) onChromeInteract()
                            holdSeekForward = true
                            holdSeekSpeed = speed
                        },
                        modifier = Modifier.size(40.dp),
                        iconModifier = Modifier.size(22.dp),
                    )
                }

                val speed = holdSeekSpeed
                if (speed != null) {
                    Text(
                        text = "x$speed",
                        color = accent,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .offset(x = if (holdSeekForward) 68.dp else (-68).dp),
                    )
                }
            }
        }
    }
}

private fun DrawScope.drawBars(bands: FloatArray, accent: Color, centerFrac: Float) {
    val barCount = bands.size.coerceAtLeast(1)
    val gap = size.width * 0.01f
    val barWidth = (size.width - gap * (barCount - 1)) / barCount
    val midY = size.height * centerFrac
    val maxH = size.height * 0.42f
    for (i in 0 until barCount) {
        val level = bands[i].coerceIn(0f, 1f)
        val h = max(maxH * (0.06f + 0.94f * level), 4.dp.toPx())
        val x = i * (barWidth + gap)
        drawRoundRect(
            color = accent.copy(alpha = 0.55f + 0.4f * level),
            topLeft = Offset(x, midY - h / 2f),
            size = Size(barWidth, h),
            cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f),
        )
    }
}

private fun sampleBand(bands: FloatArray, t: Float): Float {
    if (bands.isEmpty()) return 0f
    if (bands.size == 1) return bands[0]
    val scaled = t.coerceIn(0f, 1f) * (bands.size - 1)
    val i = floor(scaled).toInt().coerceIn(0, bands.lastIndex - 1)
    val frac = scaled - i
    return bands[i] * (1f - frac) + bands[i + 1] * frac
}

private fun DrawScope.drawDenseBars(bands: FloatArray, accent: Color, centerFrac: Float) {
    val barCount = (bands.size * 3).coerceAtLeast(24)
    val gap = size.width * 0.004f
    val barWidth = (size.width - gap * (barCount - 1)) / barCount
    val midY = size.height * centerFrac
    val maxH = size.height * 0.44f
    for (i in 0 until barCount) {
        val level = sampleBand(bands, i / (barCount - 1f)).coerceIn(0f, 1f)
        val h = max(maxH * (0.04f + 0.96f * level), 3.dp.toPx())
        val x = i * (barWidth + gap)
        drawRoundRect(
            color = accent.copy(alpha = 0.4f + 0.55f * level),
            topLeft = Offset(x, midY - h / 2f),
            size = Size(barWidth, h),
            cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f),
        )
    }
}

private fun DrawScope.drawPeakBars(bands: FloatArray, accent: Color, centerFrac: Float) {
    val barCount = bands.size.coerceAtLeast(1)
    val gap = size.width * 0.012f
    val barWidth = (size.width - gap * (barCount - 1)) / barCount
    val midY = size.height * centerFrac
    val maxH = size.height * 0.4f
    val capH = 3.dp.toPx()
    for (i in 0 until barCount) {
        val level = bands[i].coerceIn(0f, 1f)
        val h = max(maxH * (0.05f + 0.95f * level), 4.dp.toPx())
        val x = i * (barWidth + gap)
        val top = midY - h / 2f
        drawRoundRect(
            color = accent.copy(alpha = 0.35f + 0.45f * level),
            topLeft = Offset(x, top),
            size = Size(barWidth, h),
            cornerRadius = CornerRadius(barWidth / 4f, barWidth / 4f),
        )
        drawRoundRect(
            color = accent.copy(alpha = 0.85f + 0.15f * level),
            topLeft = Offset(x, top - capH * 1.6f),
            size = Size(barWidth, capH),
            cornerRadius = CornerRadius(capH / 2f, capH / 2f),
        )
    }
}

private fun DrawScope.drawFloorBars(
    bands: FloatArray,
    accent: Color,
    accentDim: Color,
    centerFrac: Float,
) {
    val barCount = bands.size.coerceAtLeast(1)
    val gap = size.width * 0.01f
    val barWidth = (size.width - gap * (barCount - 1)) / barCount
    val maxH = size.height * 0.5f
    val baseY = size.height * (centerFrac + 0.28f).coerceAtMost(0.92f)
    for (i in 0 until barCount) {
        val level = bands[i].coerceIn(0f, 1f)
        val h = max(maxH * (0.04f + 0.96f * level), 3.dp.toPx())
        val x = i * (barWidth + gap)
        drawRoundRect(
            brush = Brush.verticalGradient(
                colors = listOf(accent.copy(alpha = 0.85f), accentDim.copy(alpha = 0.2f)),
                startY = baseY - h,
                endY = baseY,
            ),
            topLeft = Offset(x, baseY - h),
            size = Size(barWidth, h),
            cornerRadius = CornerRadius(barWidth / 3f, barWidth / 3f),
        )
    }
}

private fun DrawScope.drawLedBars(bands: FloatArray, accent: Color, centerFrac: Float) {
    val barCount = bands.size.coerceAtLeast(1)
    val gap = size.width * 0.014f
    val barWidth = (size.width - gap * (barCount - 1)) / barCount
    val midY = size.height * centerFrac
    val maxH = size.height * 0.4f
    val segments = 12
    val segGap = 2.dp.toPx()
    val segH = (maxH - segGap * (segments - 1)) / segments
    for (i in 0 until barCount) {
        val level = bands[i].coerceIn(0f, 1f)
        val lit = (level * segments).toInt().coerceIn(0, segments)
        val x = i * (barWidth + gap)
        for (s in 0 until segments) {
            val fromBottom = s
            val y = midY + maxH / 2f - (fromBottom + 1) * (segH + segGap) + segGap
            val on = fromBottom < lit
            drawRoundRect(
                color = accent.copy(
                    alpha = if (on) {
                        0.35f + 0.55f * (fromBottom / segments.toFloat())
                    } else {
                        0.08f
                    },
                ),
                topLeft = Offset(x, y),
                size = Size(barWidth, segH),
                cornerRadius = CornerRadius(1.5.dp.toPx(), 1.5.dp.toPx()),
            )
        }
    }
}

private fun DrawScope.drawNeonBars(bands: FloatArray, accent: Color, centerFrac: Float) {
    val barCount = bands.size.coerceAtLeast(1)
    val gap = size.width * 0.016f
    val barWidth = (size.width - gap * (barCount - 1)) / barCount
    val midY = size.height * centerFrac
    val maxH = size.height * 0.42f
    for (i in 0 until barCount) {
        val level = bands[i].coerceIn(0f, 1f)
        val h = max(maxH * (0.06f + 0.94f * level), 4.dp.toPx())
        val x = i * (barWidth + gap)
        val glow = barWidth * 1.8f
        drawRoundRect(
            color = accent.copy(alpha = 0.12f + 0.18f * level),
            topLeft = Offset(x - (glow - barWidth) / 2f, midY - h / 2f),
            size = Size(glow, h),
            cornerRadius = CornerRadius(glow / 2f, glow / 2f),
        )
        drawRoundRect(
            color = accent.copy(alpha = 0.7f + 0.3f * level),
            topLeft = Offset(x, midY - h / 2f),
            size = Size(barWidth * 0.45f, h),
            cornerRadius = CornerRadius(barWidth / 4f, barWidth / 4f),
        )
    }
}

private fun DrawScope.drawSoftWave(bands: FloatArray, energy: Float, accent: Color, accentDim: Color, centerFrac: Float) {
    val midY = size.height * centerFrac
    val amp = size.height * (0.06f + 0.22f * energy)
    val path = Path()
    val steps = max(bands.size * 2, 48)
    for (i in 0..steps) {
        val x = size.width * i / steps
        val bandIdx = (i * (bands.size - 1) / steps).coerceIn(0, bands.lastIndex)
        val nextIdx = (bandIdx + 1).coerceAtMost(bands.lastIndex)
        val frac = (i.toFloat() * (bands.size - 1) / steps) - bandIdx
        val level = bands[bandIdx] * (1f - frac) + bands[nextIdx] * frac
        val y = midY - level * amp * 2.2f +
            sin(i * 0.35f + energy * PI.toFloat()).toFloat() * amp * 0.15f
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    drawPath(
        path,
        color = accent.copy(alpha = 0.9f),
        style = Stroke(width = 3.5.dp.toPx(), cap = StrokeCap.Round),
    )
    // Mirrored dim wave
    val mirror = Path()
    for (i in 0..steps) {
        val x = size.width * i / steps
        val bandIdx = (i * (bands.size - 1) / steps).coerceIn(0, bands.lastIndex)
        val level = bands[bandIdx]
        val y = midY + level * amp * 1.6f
        if (i == 0) mirror.moveTo(x, y) else mirror.lineTo(x, y)
    }
    drawPath(
        mirror,
        color = accentDim,
        style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round),
    )
}

private fun DrawScope.drawRings(energy: Float, bands: FloatArray, accent: Color, centerFrac: Float) {
    val cx = size.width / 2f
    val cy = size.height * centerFrac
    val maxR = minOf(size.width, size.height) * 0.38f
    val count = 5
    for (i in 0 until count) {
        val band = bands.getOrElse(i * bands.size / count) { energy }
        val r = maxR * (0.2f + band * 0.8f) * (0.55f + energy * 0.45f)
        drawCircle(
            color = accent.copy(alpha = (0.15f + band * 0.55f).coerceIn(0.1f, 0.75f)),
            radius = r,
            center = Offset(cx, cy),
            style = Stroke(width = (1.5f + band * 2f).dp.toPx()),
        )
    }
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(accent.copy(alpha = 0.2f + energy * 0.35f), Color.Transparent),
            center = Offset(cx, cy),
            radius = maxR * (0.2f + energy * 0.25f),
        ),
        radius = maxR * (0.2f + energy * 0.25f),
        center = Offset(cx, cy),
    )
}

private fun DrawScope.drawOrbit(energy: Float, bands: FloatArray, accent: Color, centerFrac: Float) {
    val cx = size.width / 2f
    val cy = size.height * centerFrac
    val base = minOf(size.width, size.height) * 0.14f
    val orbits = 3
    for (o in 0 until orbits) {
        val radius = base * (1f + o * 0.85f) * (0.7f + energy * 0.5f)
        drawCircle(
            color = accent.copy(alpha = 0.12f),
            radius = radius,
            center = Offset(cx, cy),
            style = Stroke(width = 1.2.dp.toPx()),
        )
        val dots = 5 + o * 2
        for (d in 0 until dots) {
            val band = bands.getOrElse((o * dots + d) % bands.size) { energy }
            val angle = (d * (2f * PI.toFloat() / dots)) + band * PI.toFloat() * 0.35f +
                energy * (if (o % 2 == 0) 1f else -1f)
            val x = cx + cos(angle) * radius
            val y = cy + sin(angle) * radius
            val r = (2.5f + band * 5f).dp.toPx()
            drawCircle(
                color = accent.copy(alpha = 0.45f + band * 0.5f),
                radius = r,
                center = Offset(x, y),
            )
        }
    }
}

private fun DrawScope.drawBreathingPulse(energy: Float, bands: FloatArray, accent: Color, centerFrac: Float) {
    val cx = size.width / 2f
    val cy = size.height * centerFrac
    val maxR = minOf(size.width, size.height) * 0.34f
    val breathe = 0.45f + energy * 0.55f
    val bass = bands.getOrElse(0) { energy }
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                accent.copy(alpha = 0.2f + bass * 0.45f),
                accent.copy(alpha = 0.06f),
                Color.Transparent,
            ),
            center = Offset(cx, cy),
            radius = maxR * breathe,
        ),
        radius = maxR * breathe,
        center = Offset(cx, cy),
    )
    drawCircle(
        color = accent.copy(alpha = 0.35f + energy * 0.35f),
        radius = maxR * (0.28f + bass * 0.35f),
        center = Offset(cx, cy),
        style = Stroke(width = (1.5f + energy * 2f).dp.toPx()),
    )
    val lineW = size.width * (0.25f + energy * 0.4f)
    drawLine(
        color = accent.copy(alpha = 0.25f + energy * 0.4f),
        start = Offset(cx - lineW / 2f, cy),
        end = Offset(cx + lineW / 2f, cy),
        strokeWidth = (1.2f + bass * 2.5f).dp.toPx(),
        cap = StrokeCap.Round,
    )
}

private fun DrawScope.drawMirroredBars(bands: FloatArray, accent: Color, centerFrac: Float) {
    val barCount = bands.size.coerceAtLeast(1)
    val gap = size.width * 0.012f
    val barWidth = (size.width - gap * (barCount - 1)) / barCount
    val midY = size.height * centerFrac
    val maxH = size.height * 0.36f
    for (i in 0 until barCount) {
        val level = bands[i].coerceIn(0f, 1f)
        val h = max(maxH * (0.05f + 0.95f * level), 3.dp.toPx())
        val x = i * (barWidth + gap)
        val alpha = 0.4f + 0.55f * level
        drawRoundRect(
            color = accent.copy(alpha = alpha),
            topLeft = Offset(x, midY - h),
            size = Size(barWidth, h),
            cornerRadius = CornerRadius(barWidth / 2.5f, barWidth / 2.5f),
        )
        drawRoundRect(
            color = accent.copy(alpha = alpha * 0.45f),
            topLeft = Offset(x, midY + 4.dp.toPx()),
            size = Size(barWidth, h * 0.72f),
            cornerRadius = CornerRadius(barWidth / 2.5f, barWidth / 2.5f),
        )
    }
}

private fun DrawScope.drawSpectrum(bands: FloatArray, energy: Float, accent: Color, accentDim: Color, centerFrac: Float) {
    val barCount = bands.size.coerceAtLeast(1)
    val gap = size.width * 0.008f
    val barWidth = (size.width - gap * (barCount - 1)) / barCount
    val maxH = size.height * 0.52f
    val baseY = size.height * centerFrac + maxH * 0.5f
    for (i in 0 until barCount) {
        val level = bands[i].coerceIn(0f, 1f)
        val h = max(maxH * (0.04f + 0.96f * level), 2.dp.toPx())
        val x = i * (barWidth + gap)
        val tip = accent.copy(alpha = 0.35f + 0.55f * level)
        drawRoundRect(
            brush = Brush.verticalGradient(
                colors = listOf(tip, accentDim.copy(alpha = 0.15f + energy * 0.2f)),
                startY = baseY - h,
                endY = baseY,
            ),
            topLeft = Offset(x, baseY - h),
            size = Size(barWidth, h),
            cornerRadius = CornerRadius(barWidth / 3f, barWidth / 3f),
        )
    }
}

private fun DrawScope.drawSpectrumArea(
    bands: FloatArray,
    energy: Float,
    accent: Color,
    accentDim: Color,
    centerFrac: Float,
) {
    val baseY = size.height * (centerFrac + 0.22f).coerceAtMost(0.88f)
    val amp = size.height * (0.18f + 0.28f * energy)
    val steps = max(bands.size * 4, 64)
    val fill = Path()
    fill.moveTo(0f, baseY)
    for (i in 0..steps) {
        val t = i / steps.toFloat()
        val x = size.width * t
        val level = sampleBand(bands, t)
        val y = baseY - level * amp
        fill.lineTo(x, y)
    }
    fill.lineTo(size.width, baseY)
    fill.close()
    drawPath(
        fill,
        brush = Brush.verticalGradient(
            colors = listOf(
                accent.copy(alpha = 0.55f + energy * 0.25f),
                accentDim.copy(alpha = 0.08f),
            ),
            startY = baseY - amp,
            endY = baseY,
        ),
    )
    val stroke = Path()
    for (i in 0..steps) {
        val t = i / steps.toFloat()
        val x = size.width * t
        val level = sampleBand(bands, t)
        val y = baseY - level * amp
        if (i == 0) stroke.moveTo(x, y) else stroke.lineTo(x, y)
    }
    drawPath(
        stroke,
        color = accent.copy(alpha = 0.9f),
        style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round),
    )
}

private fun DrawScope.drawCenterSpectrum(
    bands: FloatArray,
    energy: Float,
    accent: Color,
    accentDim: Color,
    centerFrac: Float,
) {
    val barCount = (bands.size * 2).coerceAtLeast(16)
    val gap = size.width * 0.006f
    val barWidth = (size.width - gap * (barCount - 1)) / barCount
    val midY = size.height * centerFrac
    val maxH = size.height * 0.38f
    for (i in 0 until barCount) {
        val level = sampleBand(bands, i / (barCount - 1f)).coerceIn(0f, 1f)
        val h = max(maxH * (0.04f + 0.96f * level), 3.dp.toPx())
        val x = i * (barWidth + gap)
        drawRoundRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    accent.copy(alpha = 0.75f),
                    accentDim.copy(alpha = 0.2f + energy * 0.15f),
                    accent.copy(alpha = 0.75f),
                ),
                startY = midY - h / 2f,
                endY = midY + h / 2f,
            ),
            topLeft = Offset(x, midY - h / 2f),
            size = Size(barWidth, h),
            cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f),
        )
    }
}

private fun DrawScope.drawSplitSpectrum(
    bands: FloatArray,
    energy: Float,
    accent: Color,
    accentDim: Color,
    centerFrac: Float,
) {
    val midY = size.height * centerFrac
    val amp = size.height * (0.14f + 0.22f * energy)
    val steps = max(bands.size * 3, 56)
    fun buildHalf(up: Boolean): Path {
        val path = Path()
        path.moveTo(0f, midY)
        for (i in 0..steps) {
            val t = i / steps.toFloat()
            val x = size.width * t
            val level = sampleBand(bands, t)
            val y = midY + (if (up) -1f else 1f) * level * amp
            path.lineTo(x, y)
        }
        path.lineTo(size.width, midY)
        path.close()
        return path
    }
    drawPath(
        buildHalf(up = true),
        brush = Brush.verticalGradient(
            colors = listOf(accent.copy(alpha = 0.55f), Color.Transparent),
            startY = midY - amp,
            endY = midY,
        ),
    )
    drawPath(
        buildHalf(up = false),
        brush = Brush.verticalGradient(
            colors = listOf(Color.Transparent, accentDim.copy(alpha = 0.45f)),
            startY = midY,
            endY = midY + amp,
        ),
    )
}

private fun DrawScope.drawSpiral(energy: Float, bands: FloatArray, frameMs: Long, accent: Color, centerFrac: Float) {
    val cx = size.width / 2f
    val cy = size.height * centerFrac
    val maxR = minOf(size.width, size.height) * 0.4f
    val spin = (frameMs % 12000L) / 12000f * 2f * PI.toFloat()
    val arms = 3
    val pointsPerArm = 28
    for (arm in 0 until arms) {
        val armOffset = arm * (2f * PI.toFloat() / arms)
        for (p in 0 until pointsPerArm) {
            val t = p / (pointsPerArm - 1f)
            val band = bands.getOrElse((p * bands.size / pointsPerArm) % bands.size) { energy }
            val r = maxR * t * (0.55f + energy * 0.45f) * (0.85f + band * 0.3f)
            val angle = armOffset + spin * (if (arm % 2 == 0) 1f else -1f) + t * 3.2f * PI.toFloat()
            val x = cx + cos(angle) * r
            val y = cy + sin(angle) * r
            drawCircle(
                color = accent.copy(alpha = (0.15f + band * 0.7f) * (0.4f + t * 0.6f)),
                radius = (1.8f + band * 4.5f * (1f - t * 0.35f)).dp.toPx(),
                center = Offset(x, y),
            )
        }
    }
    drawCircle(
        color = accent.copy(alpha = 0.12f + energy * 0.25f),
        radius = maxR * 0.08f * (0.7f + energy),
        center = Offset(cx, cy),
    )
}

private fun DrawScope.drawLattice(bands: FloatArray, energy: Float, accent: Color, centerFrac: Float) {
    val cols = 9
    val rows = 7
    val padX = size.width * 0.06f
    val usableW = size.width - padX * 2
    val usableH = size.height * 0.62f
    val originY = size.height * centerFrac - usableH * 0.5f
    for (row in 0 until rows) {
        for (col in 0 until cols) {
            val bandIdx = ((col + row) * bands.size / (cols + rows)).coerceIn(0, bands.lastIndex)
            val level = bands[bandIdx].coerceIn(0f, 1f)
            val x = padX + usableW * col / (cols - 1).coerceAtLeast(1)
            val y = originY + usableH * row / (rows - 1).coerceAtLeast(1)
            val pulse = 0.55f + level * 0.45f + energy * 0.15f
            val r = (2f + level * 7f + energy * 2f).dp.toPx() * pulse.coerceAtMost(1.35f)
            drawCircle(
                color = accent.copy(alpha = 0.12f + level * 0.65f),
                radius = r,
                center = Offset(x, y),
            )
            if (level > 0.35f) {
                drawCircle(
                    color = accent.copy(alpha = 0.08f + level * 0.2f),
                    radius = r * 2.2f,
                    center = Offset(x, y),
                    style = Stroke(width = 1.dp.toPx()),
                )
            }
        }
    }
}

private fun DrawScope.drawRibbon(bands: FloatArray, energy: Float, frameMs: Long, accent: Color, centerFrac: Float) {
    val midY = size.height * centerFrac
    val layers = 3
    val steps = max(bands.size * 3, 64)
    val phase = (frameMs % 8000L) / 8000f * 2f * PI.toFloat()
    for (layer in 0 until layers) {
        val path = Path()
        val amp = size.height * (0.05f + 0.14f * energy) * (1f - layer * 0.18f)
        val layerPhase = phase + layer * 0.9f
        val alpha = (0.75f - layer * 0.22f).coerceIn(0.25f, 0.85f)
        for (i in 0..steps) {
            val x = size.width * i / steps
            val bandIdx = (i * (bands.size - 1) / steps).coerceIn(0, bands.lastIndex)
            val nextIdx = (bandIdx + 1).coerceAtMost(bands.lastIndex)
            val frac = (i.toFloat() * (bands.size - 1) / steps) - bandIdx
            val level = bands[bandIdx] * (1f - frac) + bands[nextIdx] * frac
            val y = midY +
                (if (layer % 2 == 0) -1f else 1f) * level * amp * 2.4f +
                sin(i * 0.22f + layerPhase).toFloat() * amp * 0.35f +
                layer * 10.dp.toPx()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(
            path,
            color = accent.copy(alpha = alpha),
            style = Stroke(width = (2.2f + energy * 1.5f - layer * 0.4f).dp.toPx(), cap = StrokeCap.Round),
        )
    }
}

private fun DrawScope.drawStarburst(energy: Float, bands: FloatArray, frameMs: Long, accent: Color, centerFrac: Float) {
    val cx = size.width / 2f
    val cy = size.height * centerFrac
    val maxR = minOf(size.width, size.height) * 0.42f
    val rays = bands.size.coerceAtLeast(12)
    val spin = (frameMs % 20000L) / 20000f * 2f * PI.toFloat()
    for (i in 0 until rays) {
        val band = bands.getOrElse(i % bands.size) { energy }
        val angle = spin + i * (2f * PI.toFloat() / rays)
        val len = maxR * (0.2f + band * 0.8f) * (0.65f + energy * 0.45f)
        val x2 = cx + cos(angle) * len
        val y2 = cy + sin(angle) * len
        drawLine(
            color = accent.copy(alpha = 0.2f + band * 0.65f),
            start = Offset(cx, cy),
            end = Offset(x2, y2),
            strokeWidth = (1.2f + band * 2.8f).dp.toPx(),
            cap = StrokeCap.Round,
        )
        drawCircle(
            color = accent.copy(alpha = 0.35f + band * 0.5f),
            radius = (1.5f + band * 3.5f).dp.toPx(),
            center = Offset(x2, y2),
        )
    }
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(accent.copy(alpha = 0.18f + energy * 0.3f), Color.Transparent),
            center = Offset(cx, cy),
            radius = maxR * 0.22f,
        ),
        radius = maxR * 0.22f,
        center = Offset(cx, cy),
    )
}
