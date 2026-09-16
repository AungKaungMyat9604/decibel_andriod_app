package com.decibel.ui.screens

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.mariesta.menzies.washui.icons.LucideIcons
import com.mariesta.menzies.washui.icons.WashIcon
import com.mariesta.menzies.washui.icons.lucide.ArrowLeft
import com.mariesta.menzies.washui.icons.lucide.ArrowRight
import com.mariesta.menzies.washui.icons.lucide.Circle
import com.mariesta.menzies.washui.icons.lucide.Music4
import com.mariesta.menzies.washui.icons.lucide.Repeat
import com.mariesta.menzies.washui.icons.lucide.Shuffle
import com.mariesta.menzies.washui.icons.lucide.Square
import com.mariesta.menzies.washui.primitives.WashIconButton
import com.mariesta.menzies.washui.primitives.WashPanel
import com.decibel.ui.coilImageModel
import com.mariesta.menzies.washui.primitives.WashSlider
import com.mariesta.menzies.washui.primitives.WashText
import com.mariesta.menzies.washui.theme.WashTheme
import com.decibel.player.PlaybackState
import com.decibel.player.RepeatMode
import com.decibel.youtube.formatDuration

@Composable
fun NowPlayingScreen(
    playback: PlaybackState,
    player: ExoPlayer,
    onTogglePlay: () -> Unit,
    onSeek: (Long) -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
) {
    val colors = WashTheme.colors
    val current = playback.current
    val landscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    if (current == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                WashText(text = "Nothing playing", fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
                WashText(
                    text = "Play online from Browse or offline from Library.",
                    color = colors.ink_muted,
                    fontSize = 13.sp,
                )
            }
        }
        return
    }

    var hasVideo by remember(current.id) { mutableStateOf(false) }
    var showOverlay by remember { mutableStateOf(true) }
    DisposableEffect(player, current.id) {
        fun refresh() {
            val size = player.videoSize
            hasVideo = size.width > 0 && size.height > 0
        }
        val listener = object : Player.Listener {
            override fun onVideoSizeChanged(videoSize: VideoSize) {
                hasVideo = videoSize.width > 0 && videoSize.height > 0
            }

            override fun onPlaybackStateChanged(playbackState: Int) = refresh()

            override fun onRenderedFirstFrame() {
                hasVideo = true
            }
        }
        player.addListener(listener)
        refresh()
        onDispose { player.removeListener(listener) }
    }

    val immersive = landscape && hasVideo

    if (immersive) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.base_300)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) { showOverlay = !showOverlay },
        ) {
            PlayerSurface(player = player, hasVideo = true, modifier = Modifier.fillMaxSize())
            if (playback.resolving) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(colors.base_100.copy(alpha = 0.45f)),
                    contentAlignment = Alignment.Center,
                ) {
                    WashText(text = "Resolving stream…", fontWeight = FontWeight.Medium)
                }
            }
            if (showOverlay) {
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .background(colors.base_100.copy(alpha = 0.82f))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    WashText(text = current.title, fontWeight = FontWeight.SemiBold, maxLines = 1)
                    TransportBlock(
                        playback = playback,
                        onTogglePlay = onTogglePlay,
                        onSeek = onSeek,
                        onPrevious = onPrevious,
                        onNext = onNext,
                        onToggleShuffle = onToggleShuffle,
                        onCycleRepeat = onCycleRepeat,
                    )
                }
            }
        }
        return
    }

    if (landscape && !hasVideo) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(
                modifier = Modifier
                    .weight(0.45f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(colors.radiusBox))
                    .background(colors.base_300),
            ) {
                MediaStage(
                    playback = playback,
                    player = player,
                    hasVideo = false,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            Column(
                modifier = Modifier
                    .weight(0.55f)
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                MetaAndTransport(
                    playback = playback,
                    hasVideo = false,
                    onTogglePlay = onTogglePlay,
                    onSeek = onSeek,
                    onPrevious = onPrevious,
                    onNext = onNext,
                    onToggleShuffle = onToggleShuffle,
                    onCycleRepeat = onCycleRepeat,
                )
            }
        }
        return
    }

    // Portrait
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(if (hasVideo) 16f / 9f else 1f)
                .clip(RoundedCornerShape(colors.radiusBox))
                .background(colors.base_300),
        ) {
            MediaStage(
                playback = playback,
                player = player,
                hasVideo = hasVideo,
                modifier = Modifier.fillMaxSize(),
            )
        }
        MetaAndTransport(
            playback = playback,
            hasVideo = hasVideo,
            onTogglePlay = onTogglePlay,
            onSeek = onSeek,
            onPrevious = onPrevious,
            onNext = onNext,
            onToggleShuffle = onToggleShuffle,
            onCycleRepeat = onCycleRepeat,
        )
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun MediaStage(
    playback: PlaybackState,
    player: ExoPlayer,
    hasVideo: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = WashTheme.colors
    val current = playback.current ?: return
    Box(modifier = modifier) {
        if (!hasVideo) {
            val thumbModel = remember(current.thumbnailUrl) { coilImageModel(current.thumbnailUrl) }
            if (thumbModel != null) {
                AsyncImage(
                    model = thumbModel,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(colors.base_300),
                    contentAlignment = Alignment.Center,
                ) {
                    WashIcon(
                        imageVector = LucideIcons.Music4,
                        contentDescription = null,
                        tint = colors.ink_muted,
                        size = 48.dp,
                    )
                }
            }
        }
        PlayerSurface(player = player, hasVideo = hasVideo, modifier = Modifier.fillMaxSize())
        if (playback.resolving) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(colors.base_100.copy(alpha = 0.55f)),
                contentAlignment = Alignment.Center,
            ) {
                WashText(text = "Resolving stream…", fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
private fun PlayerSurface(
    player: ExoPlayer,
    hasVideo: Boolean,
    modifier: Modifier = Modifier,
) {
    AndroidView(
        factory = { context ->
            PlayerView(context).apply {
                useController = false
                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                setShutterBackgroundColor(android.graphics.Color.TRANSPARENT)
                setBackgroundColor(android.graphics.Color.TRANSPARENT)
                this.player = player
            }
        },
        update = { view ->
            if (view.player !== player) view.player = player
            view.alpha = if (hasVideo) 1f else 0f
        },
        modifier = modifier,
    )
}

@Composable
private fun MetaAndTransport(
    playback: PlaybackState,
    hasVideo: Boolean,
    onTogglePlay: () -> Unit,
    onSeek: (Long) -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
) {
    val colors = WashTheme.colors
    val current = playback.current ?: return
    WashPanel(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            WashText(text = current.title, fontWeight = FontWeight.SemiBold, fontSize = 20.sp)
            WashText(
                text = buildString {
                    append(current.uploader)
                    append(" · ")
                    append(if (current.isLocal) "Offline" else "Online")
                    if (hasVideo) append(" · Video")
                },
                color = colors.ink_muted,
                fontSize = 13.sp,
            )
            if (!playback.error.isNullOrBlank()) {
                WashText(text = playback.error, color = colors.error, fontSize = 13.sp)
            }
            TransportBlock(
                playback = playback,
                onTogglePlay = onTogglePlay,
                onSeek = onSeek,
                onPrevious = onPrevious,
                onNext = onNext,
                onToggleShuffle = onToggleShuffle,
                onCycleRepeat = onCycleRepeat,
            )
        }
    }
}

@Composable
private fun TransportBlock(
    playback: PlaybackState,
    onTogglePlay: () -> Unit,
    onSeek: (Long) -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
) {
    val colors = WashTheme.colors
    val duration = playback.durationMs.coerceAtLeast(1L)
    val position = playback.positionMs.coerceIn(0L, duration)
    WashSlider(
        value = position.toFloat() / duration.toFloat(),
        onValueChange = { fraction -> onSeek((fraction * duration).toLong()) },
        modifier = Modifier.fillMaxWidth(),
        enabled = !playback.resolving,
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        WashText(text = (position / 1000).formatDuration(), color = colors.ink_muted, fontSize = 12.sp)
        WashText(text = (duration / 1000).formatDuration(), color = colors.ink_muted, fontSize = 12.sp)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        WashIconButton(
            onClick = onToggleShuffle,
            imageVector = LucideIcons.Shuffle,
            contentDescription = "Shuffle",
            tint = if (playback.shuffle) colors.primary else colors.ink_muted,
            iconSize = 22.dp,
            buttonSize = 44.dp,
        )
        WashIconButton(
            onClick = onPrevious,
            imageVector = LucideIcons.ArrowLeft,
            contentDescription = "Previous",
            tint = colors.primary,
            iconSize = 26.dp,
            buttonSize = 48.dp,
        )
        WashIconButton(
            onClick = onTogglePlay,
            imageVector = if (playback.isPlaying) LucideIcons.Square else LucideIcons.Circle,
            contentDescription = if (playback.isPlaying) "Pause" else "Play",
            tint = colors.primary,
            iconSize = 32.dp,
            buttonSize = 64.dp,
            enabled = !playback.resolving,
        )
        WashIconButton(
            onClick = onNext,
            imageVector = LucideIcons.ArrowRight,
            contentDescription = "Next",
            tint = colors.primary,
            iconSize = 26.dp,
            buttonSize = 48.dp,
        )
        Box {
            WashIconButton(
                onClick = onCycleRepeat,
                imageVector = LucideIcons.Repeat,
                contentDescription = "Repeat",
                tint = if (playback.repeatMode != RepeatMode.Off) colors.primary else colors.ink_muted,
                iconSize = 22.dp,
                buttonSize = 44.dp,
            )
            if (playback.repeatMode == RepeatMode.One) {
                WashText(
                    text = "1",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.primary,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp),
                )
            }
        }
    }
}
