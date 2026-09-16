package com.decibel.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.decibel.data.DownloadJob
import com.decibel.data.DownloadProgressHub
import com.decibel.data.DownloadRepository
import com.decibel.data.LibraryStore
import com.decibel.data.SavedVideo
import com.decibel.player.PlayableItem
import com.decibel.player.PlaybackController
import com.decibel.player.PlaybackState
import com.decibel.service.DownloadForegroundService
import com.decibel.youtube.CatalogVideo
import com.decibel.youtube.VideoFormatOption
import com.decibel.youtube.VideoLookup
import com.decibel.youtube.YoutubeCatalog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.Page

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
    val selectedFormat: VideoFormatOption? = null,
    val error: String? = null,
    val playingOnline: Boolean = false,
)

data class DuplicateDownloadPrompt(
    val lookup: VideoLookup,
    val format: VideoFormatOption,
)

class MainViewModel(
    private val appContext: Context,
    private val repository: DownloadRepository,
    private val libraryStore: LibraryStore,
    private val playback: PlaybackController,
) : ViewModel() {
    private val _browse = MutableStateFlow(BrowseUiState(loading = true))
    val browse: StateFlow<BrowseUiState> = _browse.asStateFlow()

    private val _preview = MutableStateFlow(PreviewUiState())
    val preview: StateFlow<PreviewUiState> = _preview.asStateFlow()

    private val _duplicatePrompt = MutableStateFlow<DuplicateDownloadPrompt?>(null)
    val duplicatePrompt: StateFlow<DuplicateDownloadPrompt?> = _duplicatePrompt.asStateFlow()

    private var nextPage: Page? = null
    private var sourceUrl: String = ""
    private var searchQuery: String = ""

    private val _libraryFolder = MutableStateFlow(libraryStore.displayFolder())
    val libraryFolder: StateFlow<String> = _libraryFolder.asStateFlow()

    private val _usingAppFolder = MutableStateFlow(libraryStore.storageSettings().isUsingAppFolder())
    val usingAppFolder: StateFlow<Boolean> = _usingAppFolder.asStateFlow()

    val library: StateFlow<List<SavedVideo>> = libraryStore.videos
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val downloadJobs: StateFlow<List<DownloadJob>> = DownloadProgressHub.jobs
    val playbackState: StateFlow<PlaybackState> = playback.state

    init {
        refreshFeed()
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
        _preview.value = PreviewUiState(video = video, loading = true)
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) { repository.fetchInfo(video.webpageUrl) }
            }.onSuccess { lookup ->
                _preview.value = PreviewUiState(
                    video = video,
                    lookup = lookup,
                    loading = false,
                    selectedFormat = lookup.videoFormats.firstOrNull(),
                )
            }.onFailure { err ->
                _preview.value = PreviewUiState(
                    video = video,
                    loading = false,
                    error = err.message ?: "Could not load video",
                )
            }
        }
    }

    fun closePreview() {
        _preview.value = PreviewUiState()
    }

    fun selectFormat(format: VideoFormatOption) {
        _preview.value = _preview.value.copy(selectedFormat = format)
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
        val format = _preview.value.selectedFormat ?: return
        if (lookup.videoFormats.isEmpty()) {
            _preview.value = _preview.value.copy(error = "No progressive download available.")
            return
        }
        if (repository.isAlreadyDownloaded(lookup)) {
            _duplicatePrompt.value = DuplicateDownloadPrompt(lookup, format)
            return
        }
        enqueueDownload(lookup, format, overwrite = false)
        closePreview()
    }

    fun confirmDuplicateDownload() {
        val prompt = _duplicatePrompt.value ?: return
        _duplicatePrompt.value = null
        enqueueDownload(prompt.lookup, prompt.format, overwrite = true)
        closePreview()
    }

    fun cancelDuplicateDownload() {
        _duplicatePrompt.value = null
    }

    private fun enqueueDownload(
        lookup: VideoLookup,
        format: VideoFormatOption,
        overwrite: Boolean,
    ) {
        DownloadForegroundService.start(appContext, lookup, format, overwrite = overwrite)
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
        viewModelScope.launch {
            val current = playback.state.value.current
            if (current?.id == id) {
                playback.player.stop()
            }
            libraryStore.delete(id)
        }
    }

    fun toggleFavourite(id: String) {
        viewModelScope.launch {
            libraryStore.toggleFavourite(id)
        }
    }

    fun setLibraryFolder(uri: android.net.Uri) {
        libraryStore.storageSettings().takePersistablePermission(uri)
        refreshFolderState()
        viewModelScope.launch { libraryStore.refreshFromFolder() }
    }

    fun useAppLibraryFolder() {
        libraryStore.storageSettings().clearCustomFolder()
        refreshFolderState()
        viewModelScope.launch { libraryStore.refreshFromFolder() }
    }

    fun refreshLibraryFolder() {
        viewModelScope.launch { libraryStore.refreshFromFolder() }
    }

    private fun refreshFolderState() {
        _libraryFolder.value = libraryStore.displayFolder()
        _usingAppFolder.value = libraryStore.storageSettings().isUsingAppFolder()
    }

    fun playLocal(item: SavedVideo) {
        val queue = library.value.map { PlayableItem.fromLocal(it) }
        playback.play(PlayableItem.fromLocal(item), queue)
    }

    fun togglePlayPause() = playback.togglePlayPause()
    fun seekTo(ms: Long) = playback.seekTo(ms)
    fun playNext() = playback.playNext()
    fun playPrevious() = playback.playPrevious()
    fun toggleShuffle() = playback.toggleShuffle()
    fun cycleRepeat() = playback.cycleRepeatMode()

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

    companion object {
        fun factory(
            appContext: Context,
            repository: DownloadRepository,
            libraryStore: LibraryStore,
            playback: PlaybackController,
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return MainViewModel(appContext, repository, libraryStore, playback) as T
            }
        }
    }
}
