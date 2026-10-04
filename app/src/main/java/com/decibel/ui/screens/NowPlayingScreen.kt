package com.decibel.ui.screens

import android.app.Activity
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.decibel.player.PlayableItem
import com.decibel.player.PlaybackState
import com.decibel.player.RepeatMode
import com.decibel.player.SleepTimerOption
import com.decibel.player.SleepTimerState
import com.decibel.player.formatSleepRemaining
import com.decibel.ui.coilImageModel
import com.decibel.ui.components.HoldSeekIconButton
import com.decibel.ui.components.SleepTimerPickerDialog
import com.decibel.youtube.formatDuration
import kotlinx.coroutines.delay

@Composable
fun NowPlayingScreen(
    playback: PlaybackState,
    player: ExoPlayer,
    sleepTimer: SleepTimerState = SleepTimerState(),
    chromeVisible: Boolean = true,
    onToggleChrome: () -> Unit = {},
    onChromeInteract: () -> Unit = {},
    onFullscreenLandscapeLock: (Boolean) -> Unit = {},
    onTogglePlay: () -> Unit,
    onSeek: (Long) -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onPlayQueueItem: (PlayableItem) -> Unit = {},
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    onEnterStandby: () -> Unit = {},
    onSetSleepTimer: (SleepTimerOption) -> Unit = {},
) {
    val baseColors = MaterialTheme.colorScheme
    val current = playback.current
    val landscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val activity = LocalContext.current as? Activity

    // Unlock rotation when leaving Playing so other tabs aren't stuck.
    DisposableEffect(activity) {
        onDispose {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            onFullscreenLandscapeLock(false)
        }
    }

    if (current == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "Nothing playing",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 18.sp,
                    color = baseColors.onSurface,
                )
                Text(
                    text = "Play online from Browse or offline from Library.",
                    color = baseColors.onSurfaceVariant,
                    fontSize = 13.sp,
                )
            }
        }
        return
    }

    // Landscape Playing uses a pure black stage; force readable on-colors even in light theme.
    if (landscape) {
        MaterialTheme(
            colorScheme = baseColors.copy(
                background = Color.Black,
                surface = Color.Black,
                surfaceVariant = Color(0xFF141414),
                onSurface = Color.White,
                onSurfaceVariant = Color(0xFFB0B0B0),
            ),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black),
            ) {
                NowPlayingBody(
                    playback = playback,
                    player = player,
                    landscape = true,
                    chromeVisible = chromeVisible,
                    sleepTimer = sleepTimer,
                    onToggleChrome = onToggleChrome,
                    onChromeInteract = onChromeInteract,
                    onFullscreenLandscapeLock = onFullscreenLandscapeLock,
                    onTogglePlay = onTogglePlay,
                    onSeek = onSeek,
                    onPrevious = onPrevious,
                    onNext = onNext,
                    onPlayQueueItem = onPlayQueueItem,
                    onToggleShuffle = onToggleShuffle,
                    onCycleRepeat = onCycleRepeat,
                    onEnterStandby = onEnterStandby,
                    onSetSleepTimer = onSetSleepTimer,
                )
            }
        }
    } else {
        NowPlayingBody(
            playback = playback,
            player = player,
            landscape = false,
            chromeVisible = true,
            sleepTimer = sleepTimer,
            onToggleChrome = {},
            onChromeInteract = {},
            onFullscreenLandscapeLock = onFullscreenLandscapeLock,
            onTogglePlay = onTogglePlay,
            onSeek = onSeek,
            onPrevious = onPrevious,
            onNext = onNext,
            onPlayQueueItem = onPlayQueueItem,
            onToggleShuffle = onToggleShuffle,
            onCycleRepeat = onCycleRepeat,
            onEnterStandby = onEnterStandby,
            onSetSleepTimer = onSetSleepTimer,
        )
    }
}

@Composable
private fun NowPlayingBody(
    playback: PlaybackState,
    player: ExoPlayer,
    landscape: Boolean,
    chromeVisible: Boolean,
    sleepTimer: SleepTimerState,
    onToggleChrome: () -> Unit,
    onChromeInteract: () -> Unit,
    onFullscreenLandscapeLock: (Boolean) -> Unit,
    onTogglePlay: () -> Unit,
    onSeek: (Long) -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onPlayQueueItem: (PlayableItem) -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    onEnterStandby: () -> Unit,
    onSetSleepTimer: (SleepTimerOption) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val current = playback.current ?: return

    var hasVideo by remember(current.id) { mutableStateOf(false) }
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

    // Landscape always uses the same immersive stage (video or artwork) so next/prev
    // never swaps layout trees when hasVideo briefly resets.
    val immersive = landscape

    if (immersive) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) { onToggleChrome() },
        ) {
            // Full-bleed stage — letterbox/pillarbox centers the video in the display.
            MediaStage(
                playback = playback,
                player = player,
                hasVideo = hasVideo,
                modifier = Modifier
                    .fillMaxSize()
                    .align(Alignment.Center),
            )
            if (playback.resolving) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.45f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "Resolving stream…",
                        fontWeight = FontWeight.Medium,
                        color = colors.onSurface,
                    )
                }
            }
            if (chromeVisible) {
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.82f))
                        .windowInsetsPadding(
                            WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom),
                        )
                        .padding(16.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) { /* absorb taps so controls don't toggle chrome */ },
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = current.title,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = colors.onSurface,
                    )
                    TransportBlock(
                        playback = playback,
                        landscape = true,
                        showSleepButton = true,
                        sleepTimer = sleepTimer,
                        onChromeInteract = onChromeInteract,
                        onFullscreenLandscapeLock = onFullscreenLandscapeLock,
                        onTogglePlay = onTogglePlay,
                        onSeek = onSeek,
                        onPrevious = onPrevious,
                        onNext = onNext,
                        onToggleShuffle = onToggleShuffle,
                        onCycleRepeat = onCycleRepeat,
                        onEnterStandby = onEnterStandby,
                        onSetSleepTimer = onSetSleepTimer,
                    )
                }
            }
        }
        return
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Fixed 16:9 stage for both audio and video so next/prev never reflows the page.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.Black),
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
            landscape = false,
            showSleepButton = false,
            sleepTimer = sleepTimer,
            onChromeInteract = {},
            onFullscreenLandscapeLock = onFullscreenLandscapeLock,
            onTogglePlay = onTogglePlay,
            onSeek = onSeek,
            onPrevious = onPrevious,
            onNext = onNext,
            onToggleShuffle = onToggleShuffle,
            onCycleRepeat = onCycleRepeat,
            onEnterStandby = onEnterStandby,
            onSetSleepTimer = onSetSleepTimer,
        )
        UpcomingTrackCard(
            items = playback.upcomingTracks(limit = 5),
            onPlayItem = onPlayQueueItem,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        )
    }
}

@Composable
private fun UpcomingTrackCard(
    items: List<PlayableItem>,
    onPlayItem: (PlayableItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 12.dp, bottom = 4.dp),
        ) {
            Text(
                text = "Up next",
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                color = colors.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
            if (items.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                            contentDescription = null,
                            tint = colors.onSurfaceVariant.copy(alpha = 0.55f),
                            modifier = Modifier.size(36.dp),
                        )
                        Text(
                            text = "No upcoming tracks",
                            color = colors.onSurfaceVariant,
                            fontSize = 14.sp,
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                ) {
                    items(items, key = { it.id }) { item ->
                        UpcomingQueueRow(
                            item = item,
                            onPlay = { onPlayItem(item) },
                        )
                        HorizontalDivider(
                            color = colors.outline.copy(alpha = 0.12f),
                            thickness = 0.5.dp,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun UpcomingQueueRow(
    item: PlayableItem,
    onPlay: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onPlay)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LibraryThumb(
            thumbnailUrl = item.thumbnailUrl,
            modifier = Modifier.size(40.dp),
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = item.title,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontSize = 14.sp,
                color = colors.onSurface,
            )
            Text(
                text = buildString {
                    append(item.uploader)
                    append(" · ")
                    append(item.durationSeconds.formatDuration())
                    append(" · ")
                    append(if (item.isLocal) "Offline" else "Online")
                },
                color = colors.onSurfaceVariant,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** Up to [limit] following items in the active play order. */
private fun PlaybackState.upcomingTracks(limit: Int = 5): List<PlayableItem> {
    val current = current ?: return emptyList()
    val order = queue
    if (order.isEmpty() || limit <= 0) return emptyList()
    val index = order.indexOfFirst { it.id == current.id }
    if (index < 0) return emptyList()

    return when (repeatMode) {
        RepeatMode.All -> {
            if (order.size == 1) return emptyList()
            buildList {
                var i = 1
                while (size < limit && i < order.size) {
                    add(order[(index + i) % order.size])
                    i++
                }
            }
        }
        RepeatMode.One, RepeatMode.Off -> order.drop(index + 1).take(limit)
    }
}

@Composable
private fun MediaStage(
    playback: PlaybackState,
    player: ExoPlayer,
    hasVideo: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
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
                        .background(colors.surfaceVariant),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = colors.onSurfaceVariant,
                        modifier = Modifier.padding(4.dp),
                    )
                }
            }
        }
        PlayerSurface(player = player, hasVideo = hasVideo, modifier = Modifier.fillMaxSize())
        if (playback.resolving) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(colors.surface.copy(alpha = 0.55f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Resolving stream…",
                    fontWeight = FontWeight.Medium,
                    color = colors.onSurface,
                )
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
    // Black behind FIT letterbox / pillarbox when the video doesn't fill the view.
    Box(
        modifier = modifier.background(Color.Black),
    ) {
        AndroidView(
            factory = { context ->
                // Inflate with texture_view — SurfaceView ignores NavHost fade alpha and
                // appears to "stick" on screen when leaving the Playing tab.
                (android.view.LayoutInflater.from(context)
                    .inflate(com.decibel.R.layout.player_view_texture, null, false) as PlayerView)
                    .apply {
                        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                        setBackgroundColor(android.graphics.Color.BLACK)
                        setShutterBackgroundColor(android.graphics.Color.BLACK)
                        this.player = player
                    }
            },
            update = { view ->
                if (view.player !== player) view.player = player
                view.setBackgroundColor(android.graphics.Color.BLACK)
                view.setShutterBackgroundColor(android.graphics.Color.BLACK)
                view.alpha = if (hasVideo) 1f else 0f
            },
            onRelease = { view ->
                // Detach without stopping playback (audio continues via ExoPlayer + mini-player).
                view.player = null
            },
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
private fun MetaAndTransport(
    playback: PlaybackState,
    hasVideo: Boolean,
    landscape: Boolean,
    showSleepButton: Boolean,
    sleepTimer: SleepTimerState,
    onChromeInteract: () -> Unit,
    onFullscreenLandscapeLock: (Boolean) -> Unit,
    onTogglePlay: () -> Unit,
    onSeek: (Long) -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    onEnterStandby: () -> Unit,
    onSetSleepTimer: (SleepTimerOption) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val current = playback.current ?: return
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = current.title,
                fontWeight = FontWeight.SemiBold,
                fontSize = 20.sp,
                color = colors.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = buildString {
                    append(current.uploader)
                    append(" · ")
                    append(if (current.isLocal) "Offline" else "Online")
                    if (hasVideo) append(" · Video")
                },
                color = colors.onSurfaceVariant,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            // Fixed slot so error appearing/disappearing doesn't shove controls down.
            Box(modifier = Modifier.fillMaxWidth().height(16.dp)) {
                if (!playback.error.isNullOrBlank()) {
                    Text(
                        text = playback.error,
                        color = colors.error,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            TransportBlock(
                playback = playback,
                landscape = landscape,
                showSleepButton = showSleepButton,
                sleepTimer = sleepTimer,
                onChromeInteract = onChromeInteract,
                onFullscreenLandscapeLock = onFullscreenLandscapeLock,
                onTogglePlay = onTogglePlay,
                onSeek = onSeek,
                onPrevious = onPrevious,
                onNext = onNext,
                onToggleShuffle = onToggleShuffle,
                onCycleRepeat = onCycleRepeat,
                onEnterStandby = onEnterStandby,
                onSetSleepTimer = onSetSleepTimer,
            )
        }
    }
}

@Composable
private fun TransportBlock(
    playback: PlaybackState,
    landscape: Boolean,
    showSleepButton: Boolean,
    sleepTimer: SleepTimerState,
    onChromeInteract: () -> Unit,
    onFullscreenLandscapeLock: (Boolean) -> Unit,
    onTogglePlay: () -> Unit,
    onSeek: (Long) -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    onEnterStandby: () -> Unit,
    onSetSleepTimer: (SleepTimerOption) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val activity = LocalContext.current as? Activity
    var showSleepDialog by remember { mutableStateOf(false) }
    var holdSeekLabel by remember { mutableStateOf<String?>(null) }
    val duration = playback.durationMs.coerceAtLeast(1L)
    val position = playback.positionMs.coerceIn(0L, duration)

    // Keep chrome visible while the sleep picker is open.
    LaunchedEffect(showSleepDialog) {
        if (!showSleepDialog) return@LaunchedEffect
        while (true) {
            onChromeInteract()
            delay(2_000)
        }
    }

    fun interact(block: () -> Unit) {
        onChromeInteract()
        block()
    }

    Slider(
        value = position.toFloat() / duration.toFloat(),
        onValueChange = { fraction ->
            onChromeInteract()
            onSeek((fraction * duration).toLong())
        },
        modifier = Modifier.fillMaxWidth(),
        enabled = !playback.resolving,
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = (position / 1000).formatDuration(),
            color = colors.onSurfaceVariant,
            fontSize = 12.sp,
        )
        if (holdSeekLabel != null) {
            Text(
                text = holdSeekLabel!!,
                color = colors.primary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Text(
            text = (duration / 1000).formatDuration(),
            color = colors.onSurfaceVariant,
            fontSize = 12.sp,
        )
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = { interact(onToggleShuffle) }) {
            Icon(
                Icons.Default.Shuffle,
                contentDescription = "Shuffle",
                tint = if (playback.shuffle) colors.primary else colors.onSurfaceVariant,
            )
        }
        HoldSeekIconButton(
            forward = false,
            positionMs = playback.positionMs,
            durationMs = playback.durationMs,
            enabled = !playback.resolving,
            imageVector = Icons.Default.SkipPrevious,
            contentDescription = "Previous",
            tint = colors.primary,
            onClick = { interact(onPrevious) },
            onSeek = { ms ->
                onChromeInteract()
                onSeek(ms)
            },
            onHoldSpeedChange = { speed ->
                holdSeekLabel = if (speed != null) "« ${speed}x" else null
            },
        )
        IconButton(
            onClick = { interact(onTogglePlay) },
            enabled = !playback.resolving,
        ) {
            Icon(
                imageVector = if (playback.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (playback.isPlaying) "Pause" else "Play",
                tint = colors.primary,
            )
        }
        HoldSeekIconButton(
            forward = true,
            positionMs = playback.positionMs,
            durationMs = playback.durationMs,
            enabled = !playback.resolving,
            imageVector = Icons.Default.SkipNext,
            contentDescription = "Next",
            tint = colors.primary,
            onClick = { interact(onNext) },
            onSeek = { ms ->
                onChromeInteract()
                onSeek(ms)
            },
            onHoldSpeedChange = { speed ->
                holdSeekLabel = if (speed != null) "» ${speed}x" else null
            },
        )
        IconButton(onClick = { interact(onCycleRepeat) }) {
            Icon(
                imageVector = when (playback.repeatMode) {
                    RepeatMode.One -> Icons.Default.RepeatOne
                    else -> Icons.Default.Repeat
                },
                contentDescription = "Repeat",
                tint = if (playback.repeatMode != RepeatMode.Off) colors.primary else colors.onSurfaceVariant,
            )
        }
        if (showSleepButton) {
            IconButton(onClick = {
                onChromeInteract()
                showSleepDialog = true
            }) {
                Icon(
                    imageVector = Icons.Default.Timer,
                    contentDescription = if (sleepTimer.isActive) {
                        "Sleep timer ${formatSleepRemaining(sleepTimer.remainingMs)}"
                    } else {
                        "Sleep timer"
                    },
                    tint = if (sleepTimer.isActive) colors.primary else colors.onSurfaceVariant,
                )
            }
        }
        IconButton(
            onClick = {
                interact {
                    if (landscape) {
                        onFullscreenLandscapeLock(false)
                        activity?.requestedOrientation =
                            ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
                    } else {
                        onFullscreenLandscapeLock(true)
                        activity?.requestedOrientation =
                            ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                    }
                }
            },
        ) {
            Icon(
                imageVector = if (landscape) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                contentDescription = if (landscape) "Exit landscape" else "Landscape",
                tint = colors.onSurfaceVariant,
            )
        }
        IconButton(onClick = { interact(onEnterStandby) }) {
            Icon(
                imageVector = Icons.Default.DarkMode,
                contentDescription = "Standby",
                tint = colors.onSurfaceVariant,
            )
        }
    }

    if (showSleepDialog) {
        SleepTimerPickerDialog(
            sleepTimer = sleepTimer,
            onDismiss = { showSleepDialog = false },
            onSelect = { option ->
                onSetSleepTimer(option)
                showSleepDialog = false
                onChromeInteract()
            },
        )
    }
}
