package com.decibel.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.decibel.data.AppearancePrefs
import com.decibel.data.DownloadJob
import com.decibel.data.DownloadProgressHub
import com.decibel.data.DownloadRepository
import com.decibel.data.LibraryPrefs
import com.decibel.data.LibraryScope
import com.decibel.data.LibrarySort
import com.decibel.data.LibraryStore
import com.decibel.data.MediaType
import com.decibel.data.SavedVideo
import com.decibel.data.StandbyAutoEnter
import com.decibel.data.StandbyWaveStyle
import com.decibel.data.ThemeMode
import com.decibel.data.ThemePreset
import com.decibel.player.EqController
import com.decibel.player.EqUiState
import com.decibel.player.PlayableItem
import com.decibel.player.PlaybackController
import com.decibel.player.PlaybackPrefs
import com.decibel.player.PlaybackState
import com.decibel.player.SleepTimer
import com.decibel.player.SleepTimerOption
import com.decibel.player.SleepTimerState
import com.decibel.service.DownloadForegroundService
import com.decibel.youtube.CatalogVideo
import com.decibel.youtube.MediaDownloadOption
import com.decibel.youtube.VideoLookup
import com.decibel.youtube.YoutubeCatalog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.Page
import kotlin.coroutines.cancellation.CancellationException

data class BrowseUiState(
    val query: String = "",
    val items: List<CatalogVideo> = emptyList(),
    val loading: Boolean = false,
    val loadingMore: Boolean = false,
    val error: String? = null,
    val isSearch: Boolean = false,
    val feedTitle: String = "Trending music",
)

data class PreviewUiState(
    val video: CatalogVideo? = null,
    val lookup: VideoLookup? = null,
    val loading: Boolean = false,
    val selectedOption: MediaDownloadOption? = null,
    val error: String? = null,
    val playingOnline: Boolean = false,
)

data class DuplicateDownloadPrompt(
    val lookup: VideoLookup,
    val option: MediaDownloadOption,
)

class MainViewModel(
    private val appContext: Context,
    private val repository: DownloadRepository,
    private val libraryStore: LibraryStore,
    private val playback: PlaybackController,
    private val appearancePrefs: AppearancePrefs,
    private val libraryPrefs: LibraryPrefs,
    private val eqController: EqController,
) : ViewModel() {
    private val _browse = MutableStateFlow(BrowseUiState(loading = true))
    val browse: StateFlow<BrowseUiState> = _browse.asStateFlow()

    private val _preview = MutableStateFlow(PreviewUiState())
    val preview: StateFlow<PreviewUiState> = _preview.asStateFlow()

    private var previewJob: Job? = null

    private val _duplicatePrompt = MutableStateFlow<DuplicateDownloadPrompt?>(null)
    val duplicatePrompt: StateFlow<DuplicateDownloadPrompt?> = _duplicatePrompt.asStateFlow()

    private var nextPage: Page? = null
    private var sourceUrl: String = ""
    private var searchQuery: String = ""

    private val _videoFolder = MutableStateFlow(libraryStore.displayFolder(MediaType.VIDEO))
    val videoFolder: StateFlow<String> = _videoFolder.asStateFlow()

    private val _musicFolder = MutableStateFlow(libraryStore.displayFolder(MediaType.AUDIO))
    val musicFolder: StateFlow<String> = _musicFolder.asStateFlow()

    private val _usingAppVideoFolder =
        MutableStateFlow(libraryStore.storageSettings().isUsingAppFolder(MediaType.VIDEO))
    val usingAppVideoFolder: StateFlow<Boolean> = _usingAppVideoFolder.asStateFlow()

    private val _usingAppMusicFolder =
        MutableStateFlow(libraryStore.storageSettings().isUsingAppFolder(MediaType.AUDIO))
    val usingAppMusicFolder: StateFlow<Boolean> = _usingAppMusicFolder.asStateFlow()

    /** @deprecated Prefer [videoFolder] / [musicFolder]. */
    private val _libraryFolder = MutableStateFlow(libraryStore.displayFolder())
    val libraryFolder: StateFlow<String> = _libraryFolder.asStateFlow()

    private val _usingAppFolder = MutableStateFlow(libraryStore.storageSettings().isUsingAppFolder())
    val usingAppFolder: StateFlow<Boolean> = _usingAppFolder.asStateFlow()

    val library: StateFlow<List<SavedVideo>> = libraryStore.videos
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val downloadJobs: StateFlow<List<DownloadJob>> = DownloadProgressHub.jobs
    val playbackState: StateFlow<PlaybackState> = playback.state

    val themeMode: StateFlow<ThemeMode> = appearancePrefs.themeMode
    val themePreset: StateFlow<ThemePreset> = appearancePrefs.themePreset
    val eqState: StateFlow<EqUiState> = eqController.state
    val libraryScope: StateFlow<LibraryScope> = libraryPrefs.scope
    val librarySort: StateFlow<LibrarySort> = libraryPrefs.sort

    private val _libraryFilter = MutableStateFlow("")
    val libraryFilter: StateFlow<String> = _libraryFilter.asStateFlow()

    private val sleepTimer = SleepTimer(viewModelScope) {
        playback.pause()
    }
    val sleepTimerState: StateFlow<SleepTimerState> = sleepTimer.state

    init {
        refreshFeed()
        restoreLastPlayback()
    }

    fun setLibraryScope(scope: LibraryScope) = libraryPrefs.setScope(scope)

    fun setLibrarySort(sort: LibrarySort) = libraryPrefs.setSort(sort)

    fun setLibraryFilter(query: String) {
        _libraryFilter.value = query
    }

    private fun restoreLastPlayback() {
        if (playback.state.value.current != null) return
        val session = PlaybackPrefs(appContext).loadLastSession() ?: return
        val storage = libraryStore.storageSettings()
        fun playable(id: String): PlayableItem? {
            val saved = libraryStore.getById(id) ?: return null
            if (!storage.exists(saved.filePath)) return null
            return PlayableItem.fromLocal(saved)
        }
        val item = playable(session.itemId) ?: run {
            PlaybackPrefs(appContext).clearLastSession()
            return
        }
        val queue = session.queueIds.mapNotNull { playable(it) }.ifEmpty { listOf(item) }
        playback.restorePaused(item, queue, session.positionMs)
    }

    fun setQuery(query: String) {
        _browse.value = _browse.value.copy(query = query)
    }

    fun refreshFeed() {
        viewModelScope.launch {
            _browse.value = _browse.value.copy(loading = true, error = null, query = "")
            runCatching {
                withContext(Dispatchers.IO) { YoutubeCatalog.loadTrending() }
            }.onSuccess { page ->
                nextPage = page.nextPage
                sourceUrl = page.sourceUrl
                searchQuery = ""
                _browse.value = BrowseUiState(
                    items = page.items,
                    loading = false,
                    isSearch = false,
                    feedTitle = "Trending music",
                )
            }.onFailure { err ->
                _browse.value = _browse.value.copy(
                    loading = false,
                    error = err.message ?: "Could not load feed",
                )
            }
        }
    }

    fun search() {
        val q = _browse.value.query.trim()
        if (q.isEmpty()) {
            refreshFeed()
            return
        }
        viewModelScope.launch {
            _browse.value = _browse.value.copy(loading = true, error = null)
            runCatching {
                withContext(Dispatchers.IO) { YoutubeCatalog.search(q) }
            }.onSuccess { page ->
                nextPage = page.nextPage
                sourceUrl = page.sourceUrl
                searchQuery = page.searchQuery
                _browse.value = _browse.value.copy(
                    items = page.items,
                    loading = false,
                    isSearch = true,
                    feedTitle = "Search · $q",
                )
            }.onFailure { err ->
                _browse.value = _browse.value.copy(
                    loading = false,
                    error = err.message ?: "Search failed",
                )
            }
        }
    }

    fun loadMore() {
        val page = nextPage ?: return
        if (_browse.value.loadingMore || _browse.value.loading) return
        viewModelScope.launch {
            _browse.value = _browse.value.copy(loadingMore = true)
            runCatching {
                withContext(Dispatchers.IO) {
                    if (_browse.value.isSearch) {
                        YoutubeCatalog.loadMoreSearch(searchQuery, page)
                    } else {
                        YoutubeCatalog.loadMoreKiosk(sourceUrl, page)
                    }
                }
            }.onSuccess { more ->
                nextPage = more.nextPage
                _browse.value = _browse.value.copy(
                    items = _browse.value.items + more.items,
                    loadingMore = false,
                )
            }.onFailure {
                _browse.value = _browse.value.copy(loadingMore = false)
            }
        }
    }

    fun openPreview(video: CatalogVideo) {
        previewJob?.cancel()
        _preview.value = PreviewUiState(video = video, loading = true)
        previewJob = viewModelScope.launch {
            try {
                val lookup = withContext(Dispatchers.IO) { repository.fetchInfo(video.webpageUrl) }
                if (!isPreviewOpenFor(video)) return@launch
                val defaultOption = lookup.downloadOptions.firstOrNull {
                    it.kind == com.decibel.youtube.DownloadKind.VIDEO_HQ
                } ?: lookup.downloadOptions.firstOrNull()
                _preview.value = PreviewUiState(
                    video = video,
                    lookup = lookup,
                    loading = false,
                    selectedOption = defaultOption,
                )
            } catch (e: CancellationException) {
                throw e
            } catch (err: Throwable) {
                if (!isPreviewOpenFor(video)) return@launch
                _preview.value = PreviewUiState(
                    video = video,
                    loading = false,
                    error = err.message ?: "Could not load video",
                )
            }
        }
    }

    fun closePreview() {
        previewJob?.cancel()
        previewJob = null
        _preview.value = PreviewUiState()
    }

    private fun isPreviewOpenFor(video: CatalogVideo): Boolean {
        val current = _preview.value.video ?: return false
        return current.id == video.id || current.webpageUrl == video.webpageUrl
    }

    fun selectDownloadOption(option: MediaDownloadOption) {
        _preview.value = _preview.value.copy(selectedOption = option)
    }

    fun playOnlineFromPreview() {
        val video = _preview.value.video ?: return
        val queue = _browse.value.items.map { PlayableItem.fromCatalog(it) }
        val item = PlayableItem.fromCatalog(video)
        _preview.value = _preview.value.copy(playingOnline = true)
        playback.play(item, queue)
        closePreview()
    }

    fun requestDownloadFromPreview() {
        val lookup = _preview.value.lookup ?: return
        val option = _preview.value.selectedOption ?: return
        if (lookup.downloadOptions.isEmpty()) {
            _preview.value = _preview.value.copy(error = "No download options available.")
            return
        }
        if (repository.isAlreadyDownloaded(lookup, option)) {
            _duplicatePrompt.value = DuplicateDownloadPrompt(lookup, option)
            return
        }
        enqueueDownload(lookup, option, overwrite = false)
        closePreview()
    }

    fun confirmDuplicateDownload() {
        val prompt = _duplicatePrompt.value ?: return
        _duplicatePrompt.value = null
        enqueueDownload(prompt.lookup, prompt.option, overwrite = true)
        closePreview()
    }

    fun cancelDuplicateDownload() {
        _duplicatePrompt.value = null
    }

    private fun enqueueDownload(
        lookup: VideoLookup,
        option: MediaDownloadOption,
        overwrite: Boolean,
    ) {
        DownloadForegroundService.startDownload(appContext, lookup, option, overwrite = overwrite)
    }

    fun convertToMp3(id: String) {
        val video = libraryStore.getById(id) ?: return
        if (video.format.equals("mp3", ignoreCase = true)) return
        DownloadForegroundService.startConvert(appContext, video)
    }

    fun clearDownloadStatus() {
        DownloadProgressHub.dismissTerminal()
    }

    fun markNotificationsSeen() {
        DownloadProgressHub.markAllSeen()
    }

    fun dismissNotification(jobId: String) {
        DownloadProgressHub.dismiss(jobId)
    }

    fun deleteVideo(id: String) {
        deleteVideos(listOf(id))
    }

    fun deleteVideos(ids: Collection<String>) {
        if (ids.isEmpty()) return
        viewModelScope.launch {
            val idSet = ids.toSet()
            val current = playback.state.value.current
            if (current?.id in idSet) {
                playback.player.stop()
            }
            libraryStore.deleteMany(idSet)
        }
    }

    fun toggleFavourite(id: String) {
        viewModelScope.launch {
            libraryStore.toggleFavourite(id)
        }
    }

    fun setFavourite(id: String, favourite: Boolean) {
        viewModelScope.launch {
            libraryStore.setFavourite(id, favourite)
        }
    }

    fun reorderLibrary(orderedVisibleIds: List<String>) {
        viewModelScope.launch {
            libraryStore.setOrderForSubset(orderedVisibleIds)
        }
    }

    fun setLibraryFolder(type: MediaType, uri: android.net.Uri) {
        libraryStore.storageSettings().takePersistablePermission(type, uri)
        refreshFolderState()
        viewModelScope.launch { libraryStore.refreshFromFolder() }
    }

    /** @deprecated Prefer [setLibraryFolder] with [MediaType]. */
    fun setLibraryFolder(uri: android.net.Uri) = setLibraryFolder(MediaType.VIDEO, uri)

    fun useAppLibraryFolder(type: MediaType) {
        libraryStore.storageSettings().clearCustomFolder(type)
        refreshFolderState()
        viewModelScope.launch { libraryStore.refreshFromFolder() }
    }

    fun useAppLibraryFolder() {
        libraryStore.storageSettings().clearAllCustomFolders()
        refreshFolderState()
        viewModelScope.launch { libraryStore.refreshFromFolder() }
    }

    fun refreshLibraryFolder() {
        viewModelScope.launch { libraryStore.refreshFromFolder() }
    }

    fun setThemeMode(mode: ThemeMode) = appearancePrefs.setThemeMode(mode)

    fun setThemePreset(preset: ThemePreset) = appearancePrefs.setThemePreset(preset)

    val standbyWaveStyle: StateFlow<StandbyWaveStyle> = appearancePrefs.standbyWaveStyle
    val standbyAutoEnter: StateFlow<StandbyAutoEnter> = appearancePrefs.standbyAutoEnter
    val standbyCycleWave: StateFlow<Boolean> = appearancePrefs.standbyCycleWave

    fun setStandbyWaveStyle(style: StandbyWaveStyle) = appearancePrefs.setStandbyWaveStyle(style)

    fun setStandbyAutoEnter(auto: StandbyAutoEnter) = appearancePrefs.setStandbyAutoEnter(auto)

    fun setStandbyCycleWave(enabled: Boolean) = appearancePrefs.setStandbyCycleWave(enabled)

    fun setEqEnabled(enabled: Boolean) = eqController.setEnabled(enabled)

    fun applyEqPreset(presetId: String) = eqController.applyPreset(presetId)

    fun setEqBandLevel(index: Int, levelMb: Int) = eqController.setBandLevel(index, levelMb)

    private fun refreshFolderState() {
        _videoFolder.value = libraryStore.displayFolder(MediaType.VIDEO)
        _musicFolder.value = libraryStore.displayFolder(MediaType.AUDIO)
        _usingAppVideoFolder.value =
            libraryStore.storageSettings().isUsingAppFolder(MediaType.VIDEO)
        _usingAppMusicFolder.value =
            libraryStore.storageSettings().isUsingAppFolder(MediaType.AUDIO)
        _libraryFolder.value = libraryStore.displayFolder()
        _usingAppFolder.value = libraryStore.storageSettings().isUsingAppFolder()
    }

    fun playLocal(item: SavedVideo, queue: List<SavedVideo> = library.value) {
        val ordered = queue.ifEmpty { listOf(item) }
        // Keep visible list order; ensure the tapped item is present.
        val playQueue = if (ordered.any { it.id == item.id }) {
            ordered
        } else {
            listOf(item) + ordered
        }.map { PlayableItem.fromLocal(it) }
        playback.play(PlayableItem.fromLocal(item), playQueue)
    }

    fun togglePlayPause() {
        sleepTimer.clearAllowDeviceSleep()
        playback.togglePlayPause()
    }
    fun seekTo(ms: Long) = playback.seekTo(ms)
    fun playNext() = playback.playNext()
    fun playPrevious() = playback.playPrevious()
    fun playQueueItem(item: PlayableItem) = playback.playQueueItem(item)
    fun stopPlayback() = playback.stopAndClear()
    fun toggleShuffle() = playback.toggleShuffle()
    fun cycleRepeat() = playback.cycleRepeatMode()

    fun setSleepTimer(option: SleepTimerOption) = sleepTimer.setOption(option)

    fun clearSleepTimerAllowSleep() = sleepTimer.clearAllowDeviceSleep()

    fun openSharedUrl(url: String) {
        val fake = CatalogVideo(
            id = url.hashCode().toString(),
            title = "Shared link",
            uploader = "",
            durationSeconds = 0,
            thumbnailUrl = null,
            webpageUrl = url,
            viewCount = -1,
        )
        openPreview(fake)
    }

    /** Play a file/content URI opened via the system “Open with” music intents. */
    fun playExternalUri(uri: String, title: String?) {
        val name = title?.takeIf { it.isNotBlank() } ?: "Unknown track"
        val item = PlayableItem(
            id = "ext_${uri.hashCode()}",
            title = name.substringBeforeLast('.').ifBlank { name },
            uploader = "Files",
            durationSeconds = 0,
            thumbnailUrl = null,
            webpageUrl = uri,
            localPath = uri,
        )
        playback.play(item)
    }

    companion object {
        fun factory(
            appContext: Context,
            repository: DownloadRepository,
            libraryStore: LibraryStore,
            playback: PlaybackController,
            appearancePrefs: AppearancePrefs,
            libraryPrefs: LibraryPrefs,
            eqController: EqController,
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return MainViewModel(
                    appContext,
                    repository,
                    libraryStore,
                    playback,
                    appearancePrefs,
                    libraryPrefs,
                    eqController,
                ) as T
            }
        }
    }
}
