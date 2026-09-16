package com.decibel.ui

import android.Manifest
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.mariesta.menzies.washui.components.WashBackground
import com.mariesta.menzies.washui.icons.LucideIcons
import com.mariesta.menzies.washui.icons.WashIcon
import com.mariesta.menzies.washui.icons.lucide.Music4
import com.mariesta.menzies.washui.primitives.WashButton
import com.mariesta.menzies.washui.primitives.WashButtonVariant
import com.mariesta.menzies.washui.primitives.WashDialog
import com.mariesta.menzies.washui.primitives.WashDialogTone
import com.mariesta.menzies.washui.primitives.WashScaffold
import com.mariesta.menzies.washui.primitives.WashTopBar
import com.mariesta.menzies.washui.primitives.WashToastProvider
import com.mariesta.menzies.washui.primitives.rememberWashToastState
import com.mariesta.menzies.washui.theme.WashTheme
import com.decibel.DecibelApp
import com.decibel.data.DownloadProgressHub
import com.decibel.ui.components.BottomDock
import com.decibel.ui.components.DockTab
import com.decibel.ui.components.MiniPlayerBar
import com.decibel.ui.screens.BrowseScreen
import com.decibel.ui.screens.LibraryScreen
import com.decibel.ui.screens.NotificationsScreen
import com.decibel.ui.screens.NowPlayingScreen
import com.decibel.ui.screens.SettingsScreen
import com.decibel.ui.screens.VideoPreviewSheet

private object Routes {
    const val Browse = "browse"
    const val Library = "library"
    const val NowPlaying = "now_playing"
    const val Notifications = "notifications"
    const val Settings = "settings"
}

@Composable
fun DecibelAppRoot(initialUrl: String? = null) {
    val context = LocalContext.current
    val app = context.applicationContext as DecibelApp
    val vm: MainViewModel = viewModel(
        factory = MainViewModel.factory(
            app,
            app.downloadRepository,
            app.libraryStore,
            app.playbackController,
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
    val jobs by vm.downloadJobs.collectAsStateWithLifecycle()
    val seenNotificationIds by DownloadProgressHub.seenIds.collectAsStateWithLifecycle()
    val notificationBadge = remember(jobs, seenNotificationIds) {
        DownloadProgressHub.badgeCount(jobs, seenNotificationIds)
    }
    val playback by vm.playbackState.collectAsStateWithLifecycle()
    val preview by vm.preview.collectAsStateWithLifecycle()
    val duplicate by vm.duplicatePrompt.collectAsStateWithLifecycle()
    val landscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val immersiveNowPlaying = landscape && route == Routes.NowPlaying && playback.current != null

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { }

    val folderPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree(),
    ) { uri ->
        if (uri != null) vm.setLibraryFolder(uri)
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

    val dockTab = when (route) {
        Routes.Library -> DockTab.Library
        Routes.NowPlaying -> DockTab.NowPlaying
        Routes.Notifications -> DockTab.Notifications
        Routes.Settings -> DockTab.Settings
        else -> DockTab.Browse
    }
    val title = when (dockTab) {
        DockTab.Browse -> "Browse"
        DockTab.Library -> "Library"
        DockTab.NowPlaying -> "Now Playing"
        DockTab.Notifications -> "Alerts"
        DockTab.Settings -> "Settings"
    }
    DecibelSystemBars()

    fun go(tab: DockTab) {
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

    WashDialog(
        open = duplicate != null,
        onClose = vm::cancelDuplicateDownload,
        title = "Already in library",
        description = duplicate?.let {
            "“${it.lookup.title}” is already saved. Download again and overwrite?"
        },
        tone = WashDialogTone.Primary,
        actions = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                WashButton(
                    onClick = vm::cancelDuplicateDownload,
                    text = "Cancel",
                    variant = WashButtonVariant.Ghost,
                )
                WashButton(
                    onClick = vm::confirmDuplicateDownload,
                    text = "Overwrite",
                    variant = WashButtonVariant.Primary,
                )
            }
        },
    )

    WashToastProvider(alignment = Alignment.TopCenter) {
        val toast = rememberWashToastState()
        DownloadJobToasts(jobs = jobs, toast = toast)

        WashBackground(modifier = Modifier.fillMaxSize()) {
        WashScaffold(
            topBar = {
                if (!immersiveNowPlaying) {
                    WashTopBar(
                        title = title,
                        subtitle = if (dockTab == DockTab.Browse) "Decibel" else null,
                        brand = {
                            WashIcon(
                                imageVector = LucideIcons.Music4,
                                contentDescription = "Decibel",
                                tint = WashTheme.colors.primary,
                                size = 22.dp,
                            )
                        },
                    )
                }
            },
            contentPadding = PaddingValues(0.dp),
        ) { _ ->
            Box(modifier = Modifier.fillMaxSize()) {
                Column(modifier = Modifier.fillMaxSize()) {
                    NavHost(
                        navController = navController,
                        startDestination = Routes.Browse,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize()
                            .then(
                                when {
                                    immersiveNowPlaying -> Modifier
                                    route == Routes.Library -> Modifier.padding(
                                        start = 16.dp,
                                        top = 12.dp,
                                        end = 2.dp,
                                        bottom = 12.dp,
                                    )
                                    else -> Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                                },
                            ),
                    ) {
                        composable(Routes.Browse) {
                            val browse by vm.browse.collectAsStateWithLifecycle()
                            BrowseScreen(
                                state = browse,
                                onQueryChange = vm::setQuery,
                                onSearch = vm::search,
                                onRefresh = vm::refreshFeed,
                                onLoadMore = vm::loadMore,
                                onOpenItem = vm::openPreview,
                            )
                        }
                        composable(Routes.Library) {
                            val videos by vm.library.collectAsStateWithLifecycle()
                            LibraryScreen(
                                videos = videos,
                                onPlay = { item, queue ->
                                    vm.playLocal(item, queue)
                                    go(DockTab.NowPlaying)
                                },
                                onDelete = vm::deleteVideo,
                                onToggleFavourite = vm::toggleFavourite,
                            )
                        }
                        composable(Routes.NowPlaying) {
                            NowPlayingScreen(
                                playback = playback,
                                player = app.playbackController.player,
                                onTogglePlay = vm::togglePlayPause,
                                onSeek = vm::seekTo,
                                onPrevious = vm::playPrevious,
                                onNext = vm::playNext,
                                onToggleShuffle = vm::toggleShuffle,
                                onCycleRepeat = vm::cycleRepeat,
                            )
                        }
                        composable(Routes.Settings) {
                            val folder by vm.libraryFolder.collectAsStateWithLifecycle()
                            val usingApp by vm.usingAppFolder.collectAsStateWithLifecycle()
                            SettingsScreen(
                                folderLabel = folder,
                                usingAppFolder = usingApp,
                                onChooseFolder = { folderPickerLauncher.launch(null) },
                                onUseAppFolder = vm::useAppLibraryFolder,
                                onRefreshFolder = vm::refreshLibraryFolder,
                            )
                        }
                        composable(Routes.Notifications) {
                            NotificationsScreen(
                                jobs = jobs,
                                onMarkSeen = vm::markNotificationsSeen,
                                onClearFinished = vm::clearDownloadStatus,
                                onDismiss = vm::dismissNotification,
                            )
                        }
                    }

                    if (!immersiveNowPlaying && playback.current != null && dockTab != DockTab.NowPlaying) {
                        MiniPlayerBar(
                            playback = playback,
                            onOpen = { go(DockTab.NowPlaying) },
                            onTogglePlay = vm::togglePlayPause,
                            onPrevious = vm::playPrevious,
                            onNext = vm::playNext,
                            onClose = vm::stopPlayback,
                        )
                    }

                    if (!immersiveNowPlaying) {
                        BottomDock(
                            selected = dockTab,
                            notificationBadge = notificationBadge,
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
                            onSelectFormat = vm::selectFormat,
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
        }
    }
}
