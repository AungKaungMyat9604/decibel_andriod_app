package com.decibel.player

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import com.decibel.service.PlaybackService
import com.decibel.ui.artworkUri
import com.decibel.youtube.YoutubeExtractor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

data class PlaybackState(
    val current: PlayableItem? = null,
    val queue: List<PlayableItem> = emptyList(),
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val resolving: Boolean = false,
    val error: String? = null,
    val shuffle: Boolean = false,
    val repeatMode: RepeatMode = RepeatMode.Off,
)

class PlaybackController(
    context: Context,
    private val eqController: EqController,
    private val waveCapture: PlaybackWaveCapture,
) {
    private val appContext = context.applicationContext
    private val prefs = PlaybackPrefs(appContext)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val mainHandler = Handler(Looper.getMainLooper())
    private var positionJob: Job? = null
    private var resolveJob: Job? = null
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var mediaController: MediaController? = null
    private val connecting = AtomicBoolean(false)

    private val bluetoothAudioMonitor = BluetoothAudioMonitor(appContext) { bluetooth ->
        waveCapture.setBluetoothOutput(bluetooth)
    }

    /** Original order before shuffle. */
    private var sourceQueue: List<PlayableItem> = emptyList()
    /** Effective play order (shuffled or not). */
    private var playOrder: List<PlayableItem> = emptyList()

    init {
        bluetoothAudioMonitor.start()
    }
    @OptIn(androidx.media3.common.util.UnstableApi::class)
    val player: ExoPlayer = ExoPlayer.Builder(
        appContext,
        object : androidx.media3.exoplayer.DefaultRenderersFactory(appContext) {
            override fun buildAudioSink(
                context: Context,
                enableFloatOutput: Boolean,
                enableAudioTrackPlaybackParams: Boolean,
            ): androidx.media3.exoplayer.audio.AudioSink {
                // Prefer 16-bit in the processor chain so Tee PCM is consistent across OEMs;
                // PlaybackWaveCapture still accepts float if the sink upgrades later.
                return androidx.media3.exoplayer.audio.DefaultAudioSink.Builder(context)
                    .setEnableFloatOutput(false)
                    .setEnableAudioTrackPlaybackParams(enableAudioTrackPlaybackParams)
                    .setAudioProcessors(arrayOf(waveCapture.audioProcessor))
                    .build()
            }
        },
    ).build().also { exo ->
        exo.setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                .build(),
            /* handleAudioFocus = */ true,
        )
        exo.setHandleAudioBecomingNoisy(true)
        exo.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _state.value = _state.value.copy(isPlaying = isPlaying)
                if (isPlaying) {
                    ensureSessionConnected()
                    startPlaybackService()
                    startPositionUpdates()
                    // Notifications / Dolby focus changes often leave Equalizer stale
                    // without changing session id — force a fresh bind once audio is back.
                    mainHandler.removeCallbacks(eqRebindRunnable)
                    mainHandler.postDelayed(eqRebindRunnable, 150L)
                } else {
                    stopPositionUpdates()
                    mainHandler.removeCallbacks(eqRebindRunnable)
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                refreshTiming()
                if (playbackState == Player.STATE_ENDED) {
                    onTrackEnded()
                }
                if (playbackState == Player.STATE_READY && player.playWhenReady) {
                    mainHandler.removeCallbacks(eqRebindRunnable)
                    mainHandler.postDelayed(eqRebindRunnable, 150L)
                }
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                refreshTiming()
            }

            override fun onAudioSessionIdChanged(audioSessionId: Int) {
                eqController.attach(audioSessionId, force = true)
            }
        })
        eqController.attach(exo.audioSessionId, force = true)
    }

    private val eqRebindRunnable = Runnable {
        eqController.attach(player.audioSessionId, force = true)
    }

    private var lastPersistAtMs = 0L

    private val _state = MutableStateFlow(
        PlaybackState(
            shuffle = prefs.shuffleEnabled,
            repeatMode = prefs.repeatMode,
        ),
    )
    val state: StateFlow<PlaybackState> = _state.asStateFlow()

    init {
        ensureSessionConnected()
    }

    fun play(item: PlayableItem, queue: List<PlayableItem> = listOf(item)) {
        resolveJob?.cancel()
        sourceQueue = queue.ifEmpty { listOf(item) }
        playOrder = buildPlayOrder(sourceQueue, item, _state.value.shuffle)
        startItem(item, keepQueue = true, autoPlay = true, startPositionMs = 0L)
    }

    /**
     * Restore last session into the mini player without auto-playing.
     * Used after cold start when a previous local track was left mid-listen.
     */
    fun restorePaused(
        item: PlayableItem,
        queue: List<PlayableItem>,
        positionMs: Long,
    ) {
        resolveJob?.cancel()
        sourceQueue = queue.ifEmpty { listOf(item) }
        playOrder = buildPlayOrder(sourceQueue, item, _state.value.shuffle)
        startItem(item, keepQueue = true, autoPlay = false, startPositionMs = positionMs)
    }

    fun togglePlayPause() {
        ensureSessionConnected()
        val ctrl = mediaController
        if (ctrl != null) {
            if (ctrl.isPlaying) ctrl.pause() else ctrl.play()
        } else {
            if (player.isPlaying) player.pause() else player.play()
        }
    }

    fun pause() {
        ensureSessionConnected()
        mediaController?.pause() ?: player.pause()
    }

    fun play() {
        ensureSessionConnected()
        mediaController?.play() ?: player.play()
    }

    fun seekTo(positionMs: Long) {
        val pos = positionMs.coerceAtLeast(0L)
        mediaController?.seekTo(pos) ?: player.seekTo(pos)
        refreshTiming()
    }

    fun playNext() {
        advance(+1, fromUser = true)
    }

    fun playPrevious() {
        if (player.currentPosition > 3_000) {
            seekTo(0)
            return
        }
        advance(-1, fromUser = true)
    }

    /** Jump to an item already in the active play order without rebuilding the queue. */
    fun playQueueItem(item: PlayableItem) {
        val order = playOrder.ifEmpty { _state.value.queue }
        if (order.none { it.id == item.id }) return
        startItem(item, keepQueue = true, autoPlay = true, startPositionMs = 0L)
    }

    /** Stop playback and hide the mini player. */
    fun stopAndClear() {
        resolveJob?.cancel()
        stopPositionUpdates()
        mediaController?.pause()
        player.stop()
        player.clearMediaItems()
        sourceQueue = emptyList()
        playOrder = emptyList()
        prefs.clearLastSession()
        _state.value = PlaybackState(
            shuffle = prefs.shuffleEnabled,
            repeatMode = prefs.repeatMode,
        )
    }

    fun toggleShuffle() {
        val next = !_state.value.shuffle
        prefs.shuffleEnabled = next
        val current = _state.value.current
        playOrder = if (current != null) {
            buildPlayOrder(sourceQueue.ifEmpty { _state.value.queue }, current, next)
        } else {
            if (next) sourceQueue.shuffled() else sourceQueue
        }
        _state.value = _state.value.copy(
            shuffle = next,
            queue = playOrder,
        )
    }

    fun cycleRepeatMode() {
        val next = when (_state.value.repeatMode) {
            RepeatMode.Off -> RepeatMode.All
            RepeatMode.All -> RepeatMode.One
            RepeatMode.One -> RepeatMode.Off
        }
        prefs.repeatMode = next
        _state.value = _state.value.copy(repeatMode = next)
    }

    fun release() {
        resolveJob?.cancel()
        stopPositionUpdates()
        bluetoothAudioMonitor.stop()
        controllerFuture?.let { future ->
            MediaController.releaseFuture(future)
        }
        mediaController = null
        controllerFuture = null
        eqController.release()
        player.release()
    }

    private fun onTrackEnded() {
        when (_state.value.repeatMode) {
            RepeatMode.One -> {
                val current = _state.value.current ?: return
                startItem(current, keepQueue = true, autoPlay = true, startPositionMs = 0L)
            }
            RepeatMode.All, RepeatMode.Off -> advance(+1, fromUser = false)
        }
    }

    private fun advance(delta: Int, fromUser: Boolean) {
        val order = playOrder.ifEmpty { _state.value.queue }
        if (order.isEmpty()) return
        val current = _state.value.current ?: return
        val index = order.indexOfFirst { it.id == current.id }
        if (index < 0) return
        val nextIndex = index + delta
        when {
            nextIndex in order.indices ->
                startItem(order[nextIndex], keepQueue = true, autoPlay = true, startPositionMs = 0L)
            _state.value.repeatMode == RepeatMode.All || (fromUser && _state.value.repeatMode != RepeatMode.Off) -> {
                val wrap = if (delta > 0) 0 else order.lastIndex
                startItem(order[wrap], keepQueue = true, autoPlay = true, startPositionMs = 0L)
            }
            fromUser && nextIndex >= order.size && _state.value.repeatMode == RepeatMode.Off -> {
                // User next at end — wrap if shuffle/all feel expected; stay stopped for Off.
            }
            !fromUser && _state.value.repeatMode == RepeatMode.Off -> {
                // Natural end — stop.
                persistSession(force = true)
                _state.value = _state.value.copy(isPlaying = false)
            }
        }
    }

    private fun startItem(
        item: PlayableItem,
        keepQueue: Boolean,
        autoPlay: Boolean,
        startPositionMs: Long,
    ) {
        resolveJob?.cancel()
        _state.value = _state.value.copy(
            current = item,
            queue = if (keepQueue) playOrder.ifEmpty { listOf(item) } else listOf(item),
            resolving = true,
            error = null,
            positionMs = startPositionMs.coerceAtLeast(0L),
            isPlaying = false,
        )
        persistSession(force = true)
        ensureSessionConnected()
        resolveJob = scope.launch {
            runCatching {
                val uri = resolveUri(item)
                val mediaItem = buildMediaItem(item, uri)
                player.setMediaItem(mediaItem)
                player.prepare()
                if (startPositionMs > 0L) {
                    player.seekTo(startPositionMs)
                }
                player.playWhenReady = autoPlay
                if (autoPlay) {
                    ensureSessionConnected()
                    startPlaybackService()
                }
                _state.value = _state.value.copy(resolving = false)
                refreshTiming()
                persistSession(force = true)
            }.onFailure { err ->
                _state.value = _state.value.copy(
                    resolving = false,
                    error = err.message ?: "Could not start playback",
                    isPlaying = false,
                )
            }
        }
    }

    private fun buildPlayOrder(
        source: List<PlayableItem>,
        current: PlayableItem,
        shuffle: Boolean,
    ): List<PlayableItem> {
        if (!shuffle || source.size <= 1) return source
        val rest = source.filterNot { it.id == current.id }.shuffled()
        return listOf(current) + rest
    }

    private fun buildMediaItem(item: PlayableItem, uri: Uri): MediaItem =
        MediaItem.Builder()
            .setUri(uri)
            .setMediaId(item.id)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(item.title)
                    .setArtist(item.uploader)
                    .setAlbumTitle(if (item.isLocal) "Offline" else "Online")
                    .setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC)
                    .setIsBrowsable(false)
                    .setIsPlayable(true)
                    .setArtworkUri(artworkUri(item.thumbnailUrl))
                    .build(),
            )
            .build()

    private suspend fun resolveUri(item: PlayableItem): Uri {
        val local = item.localPath
        if (!local.isNullOrBlank()) {
            return if (local.startsWith("content:") || local.startsWith("file:")) {
                Uri.parse(local)
            } else {
                Uri.fromFile(File(local))
            }
        }
        val url = withContext(Dispatchers.IO) {
            YoutubeExtractor.resolvePlaybackUrl(item.webpageUrl)
        }
        return Uri.parse(url)
    }

    private fun ensureSessionConnected() {
        if (mediaController != null) return
        if (!connecting.compareAndSet(false, true) && controllerFuture != null) return
        val token = SessionToken(
            appContext,
            ComponentName(appContext, PlaybackService::class.java),
        )
        val future = MediaController.Builder(appContext, token).buildAsync()
        controllerFuture = future
        future.addListener(
            {
                runCatching { mediaController = future.get() }
                connecting.set(false)
            },
            MoreExecutors.directExecutor(),
        )
        mainHandler.post {
            if (mediaController == null && future.isDone) {
                runCatching { mediaController = future.get() }
            }
        }
    }

    /** Publish the session so lock screen / Bluetooth / notification treat Decibel as music. */
    private fun startPlaybackService() {
        runCatching {
            ContextCompat.startForegroundService(
                appContext,
                Intent(appContext, PlaybackService::class.java),
            )
        }
    }

    private fun refreshTiming() {
        _state.value = _state.value.copy(
            isPlaying = player.isPlaying,
            positionMs = player.currentPosition.coerceAtLeast(0L),
            durationMs = player.duration.coerceAtLeast(0L)
                .takeIf { player.duration > 0 } ?: _state.value.durationMs,
        )
        persistSession(force = false)
    }

    private fun persistSession(force: Boolean) {
        val current = _state.value.current ?: return
        // Online stream URLs expire — only restore offline library items.
        if (!current.isLocal) return
        val now = System.currentTimeMillis()
        if (!force && now - lastPersistAtMs < 2_000L) return
        lastPersistAtMs = now
        val queueIds = (sourceQueue.ifEmpty { _state.value.queue }.ifEmpty { listOf(current) })
            .map { it.id }
        prefs.saveLastSession(
            itemId = current.id,
            queueIds = queueIds,
            positionMs = _state.value.positionMs,
        )
    }

    private fun startPositionUpdates() {
        if (positionJob?.isActive == true) return
        positionJob = scope.launch {
            while (isActive) {
                refreshTiming()
                delay(400)
            }
        }
    }

    private fun stopPositionUpdates() {
        positionJob?.cancel()
        positionJob = null
        refreshTiming()
        persistSession(force = true)
    }
}
