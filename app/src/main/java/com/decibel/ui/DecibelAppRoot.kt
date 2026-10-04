package com.decibel.ui

import android.Manifest
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.decibel.DecibelApp
import com.decibel.R
import com.decibel.data.DownloadJobState
import com.decibel.data.DownloadProgressHub
import com.decibel.data.LibraryScope
import com.decibel.data.MediaType
import com.decibel.player.formatSleepRemaining
import com.decibel.ui.components.BottomDock
import com.decibel.ui.components.DockTab
import com.decibel.ui.components.LandscapeStatusHud
import com.decibel.ui.components.MiniPlayerBar
import com.decibel.ui.components.SleepTimerTopBarAction
import com.decibel.ui.screens.AboutScreen
import com.decibel.ui.screens.AppearanceSettingsScreen
import com.decibel.ui.screens.BrowseScreen
import com.decibel.ui.screens.EqualizerSettingsScreen
import com.decibel.ui.screens.LibraryScreen
import com.decibel.ui.screens.LibrarySettingsScreen
import com.decibel.ui.screens.NotificationsScreen
import com.decibel.ui.screens.NowPlayingScreen
import com.decibel.ui.screens.SettingsHubScreen
import com.decibel.ui.screens.StandbyScreen
import com.decibel.ui.screens.StandbySettingsScreen
import com.decibel.ui.screens.StorageSettingsScreen
import com.decibel.ui.screens.UserGuideScreen
import com.decibel.ui.screens.VideoPreviewSheet
import com.decibel.ui.theme.DecibelFieldTextStyle
import kotlinx.coroutines.delay

data class IncomingMedia(
    val uri: String,
    val title: String?,
)

private object Routes {
    const val Browse = "browse"
    const val Library = "library"
    const val NowPlaying = "now_playing"
    const val Notifications = "notifications"
    const val Settings = "settings"
    const val SettingsAppearance = "settings/appearance"
    const val SettingsLibrary = "settings/library"
    const val SettingsStandby = "settings/standby"
    const val SettingsEqualizer = "settings/equalizer"
    const val SettingsStorage = "settings/storage"
    const val SettingsUserGuide = "settings/guide"
    const val SettingsAbout = "settings/about"
}

/** Short peer-tab crossfade — suits bottom dock better than Navigation's default ~300ms fade. */
private const val TabFadeMs = 120

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DecibelAppRoot(
    initialUrl: String? = null,
    incomingMedia: IncomingMedia? = null,
    onIncomingMediaConsumed: () -> Unit = {},
) {
    val context = LocalContext.current
    val app = context.applicationContext as DecibelApp
    val vm: MainViewModel = viewModel(
        factory = MainViewModel.factory(
            app,
            app.downloadRepository,
            app.libraryStore,
            app.playbackController,
            app.appearancePrefs,
            app.libraryPrefs,
            app.eqController,
        ),
    )
    var seeded by remember { mutableStateOf(false) }
    if (!seeded && !initialUrl.isNullOrBlank()) {
        seeded = true
        vm.openSharedUrl(initialUrl)
    }

    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val route = backStack?.destination?.route
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(incomingMedia) {
        val media = incomingMedia ?: return@LaunchedEffect
        vm.playExternalUri(media.uri, media.title)
        navController.navigate(Routes.NowPlaying) {
            popUpTo(Routes.Browse) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
        onIncomingMediaConsumed()
    }
    val jobs by vm.downloadJobs.collectAsStateWithLifecycle()
    val seenNotificationIds by DownloadProgressHub.seenIds.collectAsStateWithLifecycle()
    val notificationBadge = remember(jobs, seenNotificationIds) {
        DownloadProgressHub.badgeCount(jobs, seenNotificationIds)
    }
    val playback by vm.playbackState.collectAsStateWithLifecycle()
    val browse by vm.browse.collectAsStateWithLifecycle()
    val preview by vm.preview.collectAsStateWithLifecycle()
    val duplicate by vm.duplicatePrompt.collectAsStateWithLifecycle()
    val landscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val immersiveNowPlaying = landscape && route == Routes.NowPlaying && playback.current != null
    val keyboardController = LocalSoftwareKeyboardController.current
    var standbyActive by remember { mutableStateOf(false) }
    var playingChromeVisible by remember { mutableStateOf(true) }
    var playingChromeEpoch by remember { mutableLongStateOf(0L) }
    var playingReturnTab by remember { mutableStateOf(DockTab.Library) }
    var fullscreenLandscapeLock by remember { mutableStateOf(false) }
    val standbyWaveStyle by vm.standbyWaveStyle.collectAsStateWithLifecycle()
    val standbyAutoEnter by vm.standbyAutoEnter.collectAsStateWithLifecycle()
    val standbyCycleWave by vm.standbyCycleWave.collectAsStateWithLifecycle()
    val sleepTimer by vm.sleepTimerState.collectAsStateWithLifecycle()

    fun bumpPlayingChrome() {
        playingChromeVisible = true
        playingChromeEpoch++
    }

    fun togglePlayingChrome() {
        if (playingChromeVisible) {
            playingChromeVisible = false
        } else {
            bumpPlayingChrome()
        }
    }

    LaunchedEffect(immersiveNowPlaying) {
        if (immersiveNowPlaying) {
            bumpPlayingChrome()
        } else {
            fullscreenLandscapeLock = false
        }
    }

    LaunchedEffect(immersiveNowPlaying, playingChromeVisible, playingChromeEpoch) {
        if (!immersiveNowPlaying || !playingChromeVisible) return@LaunchedEffect
        delay(5_000)
        playingChromeVisible = false
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { }

    fun enterStandby() {
        if (playback.current == null) return
        app.waveCapture.setEcoMode(true)
        app.waveCapture.setActive(true)
        standbyActive = true
    }

    // Exit standby when nothing is playing.
    LaunchedEffect(playback.current) {
        if (playback.current == null) {
            standbyActive = false
            app.waveCapture.setActive(false)
            app.waveCapture.setEcoMode(false)
        }
    }

    LaunchedEffect(standbyActive) {
        app.waveCapture.setActive(standbyActive)
        app.waveCapture.setEcoMode(standbyActive)
    }

    // Auto-enter standby after idle while music plays.
    LaunchedEffect(playback.isPlaying, playback.current?.id, standbyAutoEnter, standbyActive) {
        if (standbyActive) return@LaunchedEffect
        val delayMs = standbyAutoEnter.delayMs ?: return@LaunchedEffect
        if (!playback.isPlaying || playback.current == null) return@LaunchedEffect
        kotlinx.coroutines.delay(delayMs)
        if (playback.isPlaying && playback.current != null && !standbyActive) {
            enterStandby()
        }
    }

    var pendingFolderType by remember { mutableStateOf<MediaType?>(null) }
    val folderPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree(),
    ) { uri ->
        val type = pendingFolderType
        pendingFolderType = null
        if (uri != null && type != null) {
            vm.setLibraryFolder(type, uri)
        }
    }

    fun pickFolder(type: MediaType) {
        pendingFolderType = type
        folderPickerLauncher.launch(null)
    }

    fun ensureNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val dockTab = when {
        route == Routes.Library -> DockTab.Library
        route == Routes.NowPlaying -> DockTab.NowPlaying
        route == Routes.Notifications -> DockTab.Notifications
        route == Routes.Settings || route?.startsWith("settings/") == true -> DockTab.Settings
        else -> DockTab.Browse
    }
    val settingsSubpage = route?.startsWith("settings/") == true
    val showBrowseSearch = dockTab == DockTab.Browse && !settingsSubpage
    val showLibraryFilter = dockTab == DockTab.Library && !settingsSubpage
    val libraryFilter by vm.libraryFilter.collectAsStateWithLifecycle()
    val libraryItems by vm.library.collectAsStateWithLifecycle()
    val libraryScope by vm.libraryScope.collectAsStateWithLifecycle()
    val libraryVideoCount = remember(libraryItems) {
        libraryItems.count { it.mediaType == MediaType.VIDEO }
    }
    val libraryMusicCount = remember(libraryItems) {
        libraryItems.count { it.mediaType == MediaType.AUDIO }
    }
    val libraryFavouriteCount = remember(libraryItems) {
        libraryItems.count { it.isFavourite }
    }
    val downloadedIds = remember(libraryItems) {
        buildSet {
            val ytId = Regex("""(?:v=|/shorts/|youtu\.be/)([A-Za-z0-9_-]{6,})""")
            for (item in libraryItems) {
                add(item.id)
                when {
                    item.id.endsWith("_video") -> add(item.id.removeSuffix("_video"))
                    item.id.endsWith("_audio") -> add(item.id.removeSuffix("_audio"))
                }
                ytId.find(item.webpageUrl)?.groupValues?.getOrNull(1)?.let { add(it) }
            }
        }
    }
    val activeDownloadStates = remember(jobs) {
        val map = linkedMapOf<String, DownloadJobState>()
        for (job in jobs) {
            if (!job.isActive) continue
            val base = when {
                job.entryId.endsWith("_video") -> job.entryId.removeSuffix("_video")
                job.entryId.endsWith("_audio") -> job.entryId.removeSuffix("_audio")
                else -> job.entryId
            }
            // Running wins over Queued if both audio+video jobs exist.
            val existing = map[base]
            if (existing != DownloadJobState.Running) {
                map[base] = job.state
                map[job.entryId] = job.state
            }
        }
        map
    }
    val title = when (route) {
        Routes.SettingsAppearance -> "Appearance"
        Routes.SettingsLibrary -> "Library"
        Routes.SettingsStandby -> "Standby"
        Routes.SettingsEqualizer -> "Equalizer"
        Routes.SettingsStorage -> "Storage"
        Routes.SettingsUserGuide -> "User Guide"
        Routes.SettingsAbout -> "About"
        Routes.Library -> "Library"
        Routes.NowPlaying -> "Now Playing"
        Routes.Notifications -> "Alerts"
        Routes.Settings -> "Settings"
        else -> "Browse"
    }
    val alertsActiveCount = remember(jobs) { jobs.count { it.isActive } }
    val alertsFinishedCount = remember(jobs) { jobs.count { it.isTerminal } }
    val alertsStatusLabel = when {
        alertsActiveCount > 0 -> "$alertsActiveCount in progress"
        alertsFinishedCount > 0 -> "$alertsFinishedCount recent"
        else -> "Nothing yet"
    }
    val appVersionName = remember(context) {
        runCatching {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull().orEmpty().ifBlank { "1.0.0" }
    }
    val developerName = stringResource(R.string.developer_name)
    DecibelSystemBars(hideStatusBar = landscape || standbyActive)
    // Keep the display awake only for Standby (intentional screensaver) or
    // immersive landscape Playing (watching video). Portrait Now Playing / other
    // tabs must allow the normal screen timeout — otherwise the phone cooks.
    StandbyKeepAwake(
        active = (standbyActive || immersiveNowPlaying) && !sleepTimer.allowDeviceSleep,
        dimForOled = standbyActive,
    )

    fun go(tab: DockTab) {
        if (tab == DockTab.NowPlaying && dockTab != DockTab.NowPlaying) {
            playingReturnTab = dockTab
        }
        val dest = when (tab) {
            DockTab.Browse -> Routes.Browse
            DockTab.Library -> Routes.Library
            DockTab.NowPlaying -> Routes.NowPlaying
            DockTab.Notifications -> Routes.Notifications
            DockTab.Settings -> Routes.Settings
        }
        if (route != dest) {
            navController.navigate(dest) {
                popUpTo(Routes.Browse) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        }
    }

    fun openSettingsSubpage(dest: String) {
        navController.navigate(dest) {
            launchSingleTop = true
        }
    }

    if (duplicate != null) {
        AlertDialog(
            onDismissRequest = vm::cancelDuplicateDownload,
            title = { Text("Already in library") },
            text = {
                Text("“${duplicate!!.lookup.title}” is already saved. Download again and overwrite?")
            },
            confirmButton = {
                TextButton(onClick = vm::confirmDuplicateDownload) { Text("Overwrite") }
            },
            dismissButton = {
                TextButton(onClick = vm::cancelDuplicateDownload) { Text("Cancel") }
            },
        )
    }

    DownloadJobToasts(jobs = jobs, snackbarHostState = snackbarHostState)

    Box(modifier = Modifier.fillMaxSize()) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = if (landscape && route == Routes.NowPlaying) {
            Color.Black
        } else {
            MaterialTheme.colorScheme.background
        },
        // Nav-bar inset is applied inside BottomDock so the dock sits on the screen edge.
        // Landscape: true edge-to-edge width on every page (draw under punch-hole).
        // Portrait keeps safe top/horizontal insets for the status bar.
        contentWindowInsets = when {
            landscape || immersiveNowPlaying -> WindowInsets(0.dp)
            else -> WindowInsets.safeDrawing.only(
                WindowInsetsSides.Horizontal + WindowInsetsSides.Top,
            )
        },
        topBar = {
            if (!immersiveNowPlaying) {
                val appName = stringResource(R.string.app_name)
                val barColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
                val titleSubtitle = when {
                    route == Routes.Settings ->
                        "v$appVersionName · Developed by $developerName"
                    route == Routes.Notifications ->
                        "| Alerts · $alertsStatusLabel"
                    title.isNotBlank() && title != appName -> "| $title"
                    else -> null
                }
                Column {
                    TopAppBar(
                        // Zero insets in landscape so the bar background spans full width.
                        windowInsets = if (landscape) {
                            WindowInsets(0.dp)
                        } else {
                            TopAppBarDefaults.windowInsets
                        },
                        title = {
                            if (landscape) {
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(2.dp),
                                    horizontalAlignment = Alignment.Start,
                                ) {
                                    LandscapeStatusHud()
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        Image(
                                            painter = painterResource(R.drawable.ic_launcher_foreground),
                                            contentDescription = appName,
                                            modifier = Modifier.size(28.dp),
                                            colorFilter = ColorFilter.tint(
                                                MaterialTheme.colorScheme.primary,
                                            ),
                                        )
                                        Column(verticalArrangement = Arrangement.Center) {
                                            Text(
                                                text = appName,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                maxLines = 1,
                                            )
                                            if (titleSubtitle != null) {
                                                Text(
                                                    text = titleSubtitle,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                                        alpha = 0.65f,
                                                    ),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                )
                                            }
                                        }
                                    }
                                }
                            } else {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Image(
                                        painter = painterResource(R.drawable.ic_launcher_foreground),
                                        contentDescription = appName,
                                        modifier = Modifier.size(28.dp),
                                        colorFilter = ColorFilter.tint(
                                            MaterialTheme.colorScheme.primary,
                                        ),
                                    )
                                    Column(verticalArrangement = Arrangement.Center) {
                                        Text(
                                            text = appName,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                        )
                                        if (titleSubtitle != null) {
                                            Text(
                                                text = titleSubtitle,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                                    alpha = 0.65f,
                                                ),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                            )
                                        }
                                    }
                                }
                            }
                        },
                        navigationIcon = {
                            if (settingsSubpage) {
                                IconButton(onClick = { navController.popBackStack() }) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Back",
                                    )
                                }
                            } else {
                                Spacer(modifier = Modifier.width(4.dp))
                            }
                        },
                        actions = {
                            if (showBrowseSearch) {
                                TopBarField(
                                    value = browse.query,
                                    onValueChange = vm::setQuery,
                                    placeholder = "Search",
                                    enabled = !browse.loading,
                                    imeAction = ImeAction.Search,
                                    onImeAction = {
                                        keyboardController?.hide()
                                        vm.search()
                                    },
                                )
                            } else if (showLibraryFilter) {
                                LibraryScopeDropdown(
                                    scope = libraryScope,
                                    videoCount = libraryVideoCount,
                                    musicCount = libraryMusicCount,
                                    favouriteCount = libraryFavouriteCount,
                                    onScopeChange = vm::setLibraryScope,
                                )
                                TopBarField(
                                    value = libraryFilter,
                                    onValueChange = vm::setLibraryFilter,
                                    placeholder = "Filter",
                                    enabled = true,
                                    imeAction = ImeAction.Done,
                                    onImeAction = { keyboardController?.hide() },
                                )
                            } else if (dockTab == DockTab.NowPlaying && !landscape) {
                                SleepTimerTopBarAction(
                                    sleepTimer = sleepTimer,
                                    onSetSleepTimer = vm::setSleepTimer,
                                )
                            } else if (dockTab == DockTab.Notifications && alertsFinishedCount > 0) {
                                IconButton(onClick = vm::clearDownloadStatus) {
                                    Icon(
                                        imageVector = Icons.Outlined.DeleteSweep,
                                        contentDescription = "Clear done",
                                    )
                                }
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = barColor,
                            scrolledContainerColor = barColor,
                        ),
                    )
                    HorizontalDivider(
                        thickness = 1.dp,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.22f),
                    )
                }
            }
        },
        snackbarHost = {},
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (immersiveNowPlaying) Modifier else Modifier.padding(padding),
                ),
        ) {
            Row(modifier = Modifier.fillMaxSize()) {
                val showMiniPlayer =
                    !immersiveNowPlaying && playback.current != null && dockTab != DockTab.NowPlaying

                if (landscape && showMiniPlayer) {
                    MiniPlayerBar(
                        playback = playback,
                        landscape = true,
                        onOpen = { go(DockTab.NowPlaying) },
                        onTogglePlay = vm::togglePlayPause,
                        onPrevious = vm::playPrevious,
                        onNext = vm::playNext,
                        onClose = vm::stopPlayback,
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize(),
                ) {
                    // Short fade for peer tabs (not Navigation's slower default, no slide).
                    NavHost(
                        navController = navController,
                        startDestination = Routes.Browse,
                        enterTransition = { fadeIn(animationSpec = tween(TabFadeMs)) },
                        exitTransition = { fadeOut(animationSpec = tween(TabFadeMs)) },
                        popEnterTransition = { fadeIn(animationSpec = tween(TabFadeMs)) },
                        popExitTransition = { fadeOut(animationSpec = tween(TabFadeMs)) },
                        sizeTransform = { null },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize()
                            .then(
                                if (immersiveNowPlaying) {
                                    Modifier
                                } else {
                                    Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                                },
                            ),
                    ) {
                        composable(Routes.Browse) {
                            BrowseScreen(
                                state = browse,
                                downloadedIds = downloadedIds,
                                activeDownloadStates = activeDownloadStates,
                                onRetry = vm::refreshFeed,
                                onLoadMore = vm::loadMore,
                                onOpenItem = vm::openPreview,
                            )
                        }
                        composable(Routes.Library) {
                            val librarySort by vm.librarySort.collectAsStateWithLifecycle()
                            LibraryScreen(
                                videos = libraryItems,
                                scopeFilter = libraryScope,
                                sortMode = librarySort,
                                filter = libraryFilter,
                                playingId = playback.current?.id,
                                isPlaying = playback.isPlaying,
                                onPlay = { item, queue ->
                                    vm.playLocal(item, queue)
                                    go(DockTab.NowPlaying)
                                },
                                onDelete = vm::deleteVideo,
                                onDeleteMany = vm::deleteVideos,
                                onToggleFavourite = vm::toggleFavourite,
                                onSetFavourite = vm::setFavourite,
                                onReorder = vm::reorderLibrary,
                                onConvertToMp3 = {
                                    ensureNotificationPermission()
                                    vm.convertToMp3(it)
                                },
                            )
                        }
                        composable(Routes.NowPlaying) {
                            NowPlayingScreen(
                                playback = playback,
                                player = app.playbackController.player,
                                sleepTimer = sleepTimer,
                                chromeVisible = playingChromeVisible,
                                onToggleChrome = { togglePlayingChrome() },
                                onChromeInteract = { bumpPlayingChrome() },
                                onFullscreenLandscapeLock = { locked ->
                                    fullscreenLandscapeLock = locked
                                },
                                onTogglePlay = vm::togglePlayPause,
                                onSeek = vm::seekTo,
                                onPrevious = vm::playPrevious,
                                onNext = vm::playNext,
                                onPlayQueueItem = vm::playQueueItem,
                                onToggleShuffle = vm::toggleShuffle,
                                onCycleRepeat = vm::cycleRepeat,
                                onEnterStandby = { enterStandby() },
                                onSetSleepTimer = vm::setSleepTimer,
                            )
                        }
                        composable(Routes.Settings) {
                            SettingsHubScreen(
                                onOpenAppearance = { openSettingsSubpage(Routes.SettingsAppearance) },
                                onOpenLibrary = { openSettingsSubpage(Routes.SettingsLibrary) },
                                onOpenStandby = { openSettingsSubpage(Routes.SettingsStandby) },
                                onOpenEqualizer = { openSettingsSubpage(Routes.SettingsEqualizer) },
                                onOpenStorage = { openSettingsSubpage(Routes.SettingsStorage) },
                                onOpenUserGuide = { openSettingsSubpage(Routes.SettingsUserGuide) },
                                onOpenAbout = { openSettingsSubpage(Routes.SettingsAbout) },
                            )
                        }
                        composable(Routes.SettingsAppearance) {
                            val themeMode by vm.themeMode.collectAsStateWithLifecycle()
                            val themePreset by vm.themePreset.collectAsStateWithLifecycle()
                            AppearanceSettingsScreen(
                                themeMode = themeMode,
                                themePreset = themePreset,
                                onThemeMode = vm::setThemeMode,
                                onThemePreset = vm::setThemePreset,
                            )
                        }
                        composable(Routes.SettingsLibrary) {
                            val librarySort by vm.librarySort.collectAsStateWithLifecycle()
                            LibrarySettingsScreen(
                                sortMode = librarySort,
                                onSortChange = vm::setLibrarySort,
                            )
                        }
                        composable(Routes.SettingsStandby) {
                            val cycleWave by vm.standbyCycleWave.collectAsStateWithLifecycle()
                            StandbySettingsScreen(
                                waveStyle = standbyWaveStyle,
                                autoEnter = standbyAutoEnter,
                                cycleWavePerTrack = cycleWave,
                                onWaveStyle = vm::setStandbyWaveStyle,
                                onAutoEnter = vm::setStandbyAutoEnter,
                                onCycleWavePerTrack = vm::setStandbyCycleWave,
                            )
                        }
                        composable(Routes.SettingsEqualizer) {
                            val eqState by vm.eqState.collectAsStateWithLifecycle()
                            EqualizerSettingsScreen(
                                eqState = eqState,
                                onEqEnabled = vm::setEqEnabled,
                                onEqPreset = vm::applyEqPreset,
                                onEqBandLevel = vm::setEqBandLevel,
                            )
                        }
                        composable(Routes.SettingsStorage) {
                            val videoFolder by vm.videoFolder.collectAsStateWithLifecycle()
                            val musicFolder by vm.musicFolder.collectAsStateWithLifecycle()
                            val usingAppVideo by vm.usingAppVideoFolder.collectAsStateWithLifecycle()
                            val usingAppMusic by vm.usingAppMusicFolder.collectAsStateWithLifecycle()
                            StorageSettingsScreen(
                                videoFolderLabel = videoFolder,
                                musicFolderLabel = musicFolder,
                                videoUsingAppFolder = usingAppVideo,
                                musicUsingAppFolder = usingAppMusic,
                                onChooseVideoFolder = { pickFolder(MediaType.VIDEO) },
                                onChooseMusicFolder = { pickFolder(MediaType.AUDIO) },
                                onUseAppVideoFolder = { vm.useAppLibraryFolder(MediaType.VIDEO) },
                                onUseAppMusicFolder = { vm.useAppLibraryFolder(MediaType.AUDIO) },
                                onRefreshFolder = vm::refreshLibraryFolder,
                            )
                        }
                        composable(Routes.SettingsUserGuide) {
                            UserGuideScreen()
                        }
                        composable(Routes.SettingsAbout) {
                            AboutScreen(
                                versionName = appVersionName,
                                developerName = developerName,
                            )
                        }
                        composable(Routes.Notifications) {
                            NotificationsScreen(
                                jobs = jobs,
                                onMarkSeen = vm::markNotificationsSeen,
                                onDismiss = vm::dismissNotification,
                            )
                        }
                    }

                    if (!landscape && showMiniPlayer) {
                        MiniPlayerBar(
                            playback = playback,
                            onOpen = { go(DockTab.NowPlaying) },
                            onTogglePlay = vm::togglePlayPause,
                            onPrevious = vm::playPrevious,
                            onNext = vm::playNext,
                            onClose = vm::stopPlayback,
                        )
                    }

                    if (!immersiveNowPlaying && !landscape) {
                        BottomDock(
                            selected = dockTab,
                            notificationBadge = notificationBadge,
                            onSelect = { tab -> go(tab) },
                        )
                    }
                }

                if (!immersiveNowPlaying && landscape) {
                    BottomDock(
                        selected = dockTab,
                        notificationBadge = notificationBadge,
                        landscape = true,
                        onSelect = { tab -> go(tab) },
                    )
                }
            }

            if (preview.video != null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.BottomCenter,
                ) {
                    VideoPreviewSheet(
                        state = preview,
                        activeDownloads = jobs.count { it.isActive },
                        onClose = vm::closePreview,
                        onSelectOption = vm::selectDownloadOption,
                        onPlayOnline = {
                            vm.playOnlineFromPreview()
                            go(DockTab.NowPlaying)
                        },
                        onDownload = {
                            ensureNotificationPermission()
                            vm.requestDownloadFromPreview()
                        },
                    )
                }
            }
        }
    }

        // Back + HUD hide together with bottom controls after idle / tap.
        // Hide back when landscape was entered via the fullscreen button.
        if (immersiveNowPlaying && playingChromeVisible) {
            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 4.dp, top = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                if (!fullscreenLandscapeLock) {
                    IconButton(
                        onClick = { go(playingReturnTab) },
                        modifier = Modifier.background(
                            Color.Black.copy(alpha = 0.62f),
                            CircleShape,
                        ),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White,
                        )
                    }
                }
                LandscapeStatusHud(
                    sleepRemainingLabel = if (sleepTimer.isActive) {
                        formatSleepRemaining(sleepTimer.remainingMs)
                    } else {
                        null
                    },
                    overlayScrim = true,
                )
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        )

        val standbyTrack = playback.current
        if (standbyActive && standbyTrack != null) {
            StandbyScreen(
                track = standbyTrack,
                isPlaying = playback.isPlaying,
                positionMs = playback.positionMs,
                durationMs = playback.durationMs,
                waveStyle = standbyWaveStyle,
                cycleWavePerTrack = standbyCycleWave,
                waveCapture = app.waveCapture,
                sleepRemainingLabel = if (sleepTimer.isActive) {
                    formatSleepRemaining(sleepTimer.remainingMs)
                } else {
                    null
                },
                onDismiss = { standbyActive = false },
                onTogglePlay = vm::togglePlayPause,
                onSeek = vm::seekTo,
                onPrevious = vm::playPrevious,
                onNext = vm::playNext,
            )
        }
    }
}

@Composable
private fun LibraryScopeDropdown(
    scope: LibraryScope,
    videoCount: Int,
    musicCount: Int,
    favouriteCount: Int,
    onScopeChange: (LibraryScope) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedIcon = when (scope) {
        LibraryScope.Videos -> Icons.Default.Videocam
        LibraryScope.Music -> Icons.Default.MusicNote
        LibraryScope.Favourites -> Icons.Default.Favorite
    }
    val selectedCount = when (scope) {
        LibraryScope.Videos -> videoCount
        LibraryScope.Music -> musicCount
        LibraryScope.Favourites -> favouriteCount
    }
    val selectedLabel = when (scope) {
        LibraryScope.Videos -> "Videos"
        LibraryScope.Music -> "Music"
        LibraryScope.Favourites -> "Favourites"
    }

    Box(modifier = Modifier.padding(end = 4.dp)) {
        TextButton(
            onClick = { expanded = true },
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
        ) {
            Icon(
                imageVector = selectedIcon,
                contentDescription = selectedLabel,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "($selectedCount)",
                style = DecibelFieldTextStyle,
            )
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            LibraryScopeMenuItem(
                icon = Icons.Default.Videocam,
                count = videoCount,
                label = "Videos",
                selected = scope == LibraryScope.Videos,
                onClick = {
                    onScopeChange(LibraryScope.Videos)
                    expanded = false
                },
            )
            LibraryScopeMenuItem(
                icon = Icons.Default.MusicNote,
                count = musicCount,
                label = "Music",
                selected = scope == LibraryScope.Music,
                onClick = {
                    onScopeChange(LibraryScope.Music)
                    expanded = false
                },
            )
            LibraryScopeMenuItem(
                icon = Icons.Default.Favorite,
                count = favouriteCount,
                label = "Favourites",
                selected = scope == LibraryScope.Favourites,
                onClick = {
                    onScopeChange(LibraryScope.Favourites)
                    expanded = false
                },
            )
        }
    }
}

@Composable
private fun LibraryScopeMenuItem(
    icon: ImageVector,
    count: Int,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    DropdownMenuItem(
        text = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    text = "($count)",
                    style = DecibelFieldTextStyle,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                )
            }
        },
        onClick = onClick,
    )
}

@Composable
private fun TopBarField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    enabled: Boolean,
    imeAction: ImeAction,
    onImeAction: () -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        textStyle = DecibelFieldTextStyle,
        placeholder = {
            Text(
                text = placeholder,
                style = DecibelFieldTextStyle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        enabled = enabled,
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = imeAction),
        keyboardActions = KeyboardActions(
            onSearch = { onImeAction() },
            onDone = { onImeAction() },
        ),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
        ),
        modifier = Modifier
            .padding(end = 8.dp)
            .widthIn(min = 160.dp, max = 240.dp)
            .height(48.dp),
    )
}
