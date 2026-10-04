package com.decibel.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.LibraryBooks
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.Equalizer
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.decibel.data.LibrarySort
import com.decibel.data.StandbyAutoEnter
import com.decibel.data.StandbyWaveStyle
import com.decibel.data.ThemeMode
import com.decibel.data.ThemePreset
import com.decibel.player.EqPrefs
import com.decibel.player.EqUiState
import com.decibel.ui.theme.DecibelFieldTextStyle
import kotlin.math.roundToInt

@Composable
fun SettingsHubScreen(
    onOpenAppearance: () -> Unit,
    onOpenLibrary: () -> Unit,
    onOpenStandby: () -> Unit,
    onOpenEqualizer: () -> Unit,
    onOpenStorage: () -> Unit,
    onOpenUserGuide: () -> Unit,
    onOpenAbout: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SettingsNavRow(
            title = "Appearance",
            subtitle = "Mode and theme",
            icon = Icons.Outlined.Palette,
            onClick = onOpenAppearance,
        )
        SettingsNavRow(
            title = "Standby",
            subtitle = "AMOLED screensaver waves",
            icon = Icons.Outlined.DarkMode,
            onClick = onOpenStandby,
        )
        SettingsNavRow(
            title = "Library",
            subtitle = "Sort order",
            icon = Icons.Outlined.LibraryMusic,
            onClick = onOpenLibrary,
        )
        SettingsNavRow(
            title = "Equalizer",
            subtitle = "Presets and band levels",
            icon = Icons.Outlined.Equalizer,
            onClick = onOpenEqualizer,
        )
        SettingsNavRow(
            title = "Storage",
            subtitle = "Video & Music folders",
            icon = Icons.Outlined.FolderOpen,
            onClick = onOpenStorage,
        )
        SettingsNavRow(
            title = "Full User Guide",
            subtitle = "How to use Decibel",
            icon = Icons.AutoMirrored.Outlined.MenuBook,
            onClick = onOpenUserGuide,
        )
        SettingsNavRow(
            title = "About",
            subtitle = "Version and credits",
            icon = Icons.Outlined.Info,
            onClick = onOpenAbout,
        )
    }
}

@Composable
private fun SettingsNavRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
    ) {
        ListItem(
            headlineContent = {
                Text(title, fontWeight = FontWeight.SemiBold)
            },
            supportingContent = {
                Text(subtitle, color = colors.onSurfaceVariant)
            },
            leadingContent = {
                Icon(imageVector = icon, contentDescription = null, tint = colors.primary)
            },
            trailingContent = {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                    contentDescription = null,
                    tint = colors.onSurfaceVariant,
                )
            },
            colors = ListItemDefaults.colors(containerColor = colors.surface),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibrarySettingsScreen(
    sortMode: LibrarySort,
    onSortChange: (LibrarySort) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "Custom order lets you drag rows in Library. A–Z sorts by title.",
            color = colors.onSurfaceVariant,
            fontSize = 13.sp,
        )
        SettingsDropdown(
            label = "Sort order",
            selectedLabel = sortMode.label,
            options = LibrarySort.entries.map { it.label to it },
            onSelect = onSortChange,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StandbySettingsScreen(
    waveStyle: StandbyWaveStyle,
    autoEnter: StandbyAutoEnter,
    cycleWavePerTrack: Boolean,
    onWaveStyle: (StandbyWaveStyle) -> Unit,
    onAutoEnter: (StandbyAutoEnter) -> Unit,
    onCycleWavePerTrack: (Boolean) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "Standby is a black AMOLED screensaver while music plays. Waves follow the track from playback audio (no microphone). Open it from Now Playing, or auto-enter after idle.",
            color = colors.onSurfaceVariant,
            fontSize = 13.sp,
        )
        SettingsDropdown(
            label = "Sound wave",
            selectedLabel = waveStyle.label,
            options = StandbyWaveStyle.entries.map { it.label to it },
            onSelect = onWaveStyle,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 12.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = "Change style each track",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = colors.onSurface,
                )
                Text(
                    text = "Cycles to the next wave style when the track changes",
                    color = colors.onSurfaceVariant,
                    fontSize = 12.sp,
                )
            }
            Switch(
                checked = cycleWavePerTrack,
                onCheckedChange = onCycleWavePerTrack,
            )
        }
        SettingsDropdown(
            label = "Auto-enter while playing",
            selectedLabel = autoEnter.label,
            options = StandbyAutoEnter.entries.map { it.label to it },
            onSelect = onAutoEnter,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceSettingsScreen(
    themeMode: ThemeMode,
    themePreset: ThemePreset,
    onThemeMode: (ThemeMode) -> Unit,
    onThemePreset: (ThemePreset) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "Choose how Decibel looks. Theme picks the color palette; mode controls light or dark.",
            color = colors.onSurfaceVariant,
            fontSize = 13.sp,
        )
        SettingsDropdown(
            label = "Mode",
            selectedLabel = themeMode.label,
            options = ThemeMode.entries.map { it.label to it },
            onSelect = onThemeMode,
        )
        SettingsDropdown(
            label = "Theme",
            selectedLabel = themePreset.label,
            options = ThemePreset.entries.map { it.label to it },
            onSelect = onThemePreset,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> SettingsDropdown(
    label: String,
    selectedLabel: String,
    options: List<Pair<String, T>>,
    onSelect: (T) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
    ) {
        OutlinedTextField(
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable),
            readOnly = true,
            value = selectedLabel,
            onValueChange = {},
            textStyle = DecibelFieldTextStyle,
            label = { Text(label, style = DecibelFieldTextStyle) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            options.forEach { (optionLabel, value) ->
                DropdownMenuItem(
                    text = { Text(optionLabel) },
                    onClick = {
                        onSelect(value)
                        expanded = false
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EqualizerSettingsScreen(
    eqState: EqUiState,
    onEqEnabled: (Boolean) -> Unit,
    onEqPreset: (String) -> Unit,
    onEqBandLevel: (Int, Int) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (!eqState.available) {
            Text(
                text = "EQ unavailable on this device",
                color = colors.onSurfaceVariant,
                fontSize = 13.sp,
            )
        } else {
            Text(
                text = "If sound crackles with notifications, turn off Dolby Atmos / phone EQ while using this — they fight the app equalizer.",
                color = colors.onSurfaceVariant,
                fontSize = 12.sp,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "Enable EQ",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = colors.onSurface,
                )
                Switch(
                    checked = eqState.enabled,
                    onCheckedChange = onEqEnabled,
                )
            }
            Text(
                text = "Preset",
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = colors.onSurface,
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                eqPresetChips().forEach { (id, label) ->
                    FilterChip(
                        selected = eqState.presetId == id,
                        onClick = { onEqPreset(id) },
                        enabled = eqState.enabled,
                        label = { Text(label) },
                    )
                }
            }
            eqState.bands.forEach { band ->
                val label = formatBandHz(band.centerHz)
                Text(
                    text = "$label · ${(band.levelMb / 100f).let { "%.1f".format(it) }} dB",
                    color = if (eqState.enabled) colors.onSurface else colors.onSurfaceVariant,
                    fontSize = 12.sp,
                )
                Slider(
                    value = band.levelMb.toFloat(),
                    onValueChange = { onEqBandLevel(band.index, it.roundToInt()) },
                    valueRange = band.minLevelMb.toFloat()..band.maxLevelMb.toFloat(),
                    enabled = eqState.enabled,
                )
            }
        }
    }
}

@Composable
fun StorageSettingsScreen(
    videoFolderLabel: String,
    musicFolderLabel: String,
    videoUsingAppFolder: Boolean,
    musicUsingAppFolder: Boolean,
    onChooseVideoFolder: () -> Unit,
    onChooseMusicFolder: () -> Unit,
    onUseAppVideoFolder: () -> Unit,
    onUseAppMusicFolder: () -> Unit,
    onRefreshFolder: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        StorageFolderCard(
            title = "Video folder",
            folderLabel = videoFolderLabel,
            usingAppFolder = videoUsingAppFolder,
            hint = "MP4 downloads are saved here. Scan imports video files already in the folder.",
            onChooseFolder = onChooseVideoFolder,
            onUseAppFolder = onUseAppVideoFolder,
            onRefreshFolder = onRefreshFolder,
        )
        StorageFolderCard(
            title = "Music folder",
            folderLabel = musicFolderLabel,
            usingAppFolder = musicUsingAppFolder,
            hint = "MP3 downloads are saved here. Scan imports audio files already in the folder.",
            onChooseFolder = onChooseMusicFolder,
            onUseAppFolder = onUseAppMusicFolder,
            onRefreshFolder = onRefreshFolder,
        )
    }
}

@Composable
private fun StorageFolderCard(
    title: String,
    folderLabel: String,
    usingAppFolder: Boolean,
    hint: String,
    onChooseFolder: () -> Unit,
    onUseAppFolder: () -> Unit,
    onRefreshFolder: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.FolderOpen,
                    contentDescription = null,
                    tint = colors.primary,
                )
                Text(
                    text = title,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = colors.onSurface,
                )
            }
            Text(
                text = folderLabel,
                color = colors.onSurfaceVariant,
                fontSize = 12.sp,
                maxLines = 3,
            )
            Text(
                text = hint,
                color = colors.onSurfaceVariant,
                fontSize = 12.sp,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onChooseFolder,
                    modifier = Modifier.weight(1f),
                ) { Text("Choose folder") }
                OutlinedButton(onClick = onRefreshFolder) { Text("Scan") }
            }
            if (!usingAppFolder) {
                TextButton(onClick = onUseAppFolder) { Text("Use app folder") }
            }
        }
    }
}

private fun eqPresetChips(): List<Pair<String, String>> = listOf(
    EqPrefs.PRESET_FLAT to "Flat",
    EqPrefs.PRESET_BASS to "Bass",
    EqPrefs.PRESET_TREBLE to "Treble",
    EqPrefs.PRESET_VOCAL to "Vocal",
    EqPrefs.PRESET_ROCK to "Rock",
    EqPrefs.PRESET_POP to "Pop",
    EqPrefs.PRESET_ELECTRONIC to "Electronic",
    EqPrefs.PRESET_HIP_HOP to "Hip-Hop",
    EqPrefs.PRESET_JAZZ to "Jazz",
    EqPrefs.PRESET_CLASSICAL to "Classical",
    EqPrefs.PRESET_ACOUSTIC to "Acoustic",
    EqPrefs.PRESET_LOUDNESS to "Loudness",
    EqPrefs.PRESET_CUSTOM to "Custom",
)

private fun formatBandHz(hz: Int): String =
    when {
        hz >= 1000 -> {
            val k = hz / 1000f
            if (k == k.toInt().toFloat()) "${k.toInt()} kHz" else "%.1f kHz".format(k)
        }
        else -> "$hz Hz"
    }

@Composable
fun UserGuideScreen() {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Text(
            text = "Every control and action in Decibel, grouped by screen. Icons match what you see in the app.",
            color = colors.onSurfaceVariant,
            fontSize = 13.sp,
            lineHeight = 18.sp,
        )

        GuideSection(
            title = "Navigation",
            icon = Icons.Outlined.Settings,
            items = listOf(
                GuideItem(Icons.Outlined.Search, "Browse", "Search YouTube and open results to preview, play, or download."),
                GuideItem(Icons.AutoMirrored.Outlined.LibraryBooks, "Library", "Offline Video, Music, and Favourites collection."),
                GuideItem(Icons.Outlined.PlayCircle, "Playing", "Full Now Playing controls, queue, sleep timer, and standby."),
                GuideItem(Icons.Outlined.Notifications, "Alerts", "Download progress, completed jobs, and failures. Badge shows unseen activity."),
                GuideItem(Icons.Outlined.Settings, "Settings", "Appearance, Standby, Library sort, Equalizer, Storage, this guide, and About."),
                GuideItem(Icons.AutoMirrored.Filled.ArrowBack, "Back", "On a settings subpage, returns to the Settings hub."),
            ),
        )

        GuideSection(
            title = "Browse",
            icon = Icons.Outlined.Search,
            items = listOf(
                GuideItem(Icons.Filled.Search, "Search", "Type a query in the top bar and press Search to find YouTube videos."),
                GuideItem(Icons.Outlined.TouchApp, "Open result", "Tap a card to open the preview sheet with stream options."),
                GuideItem(Icons.Filled.DownloadDone, "Downloaded badge", "Marks items already saved in your library."),
                GuideItem(Icons.Filled.HourglassTop, "Queued / downloading", "Shows when a download is waiting or in progress."),
                GuideItem(Icons.Outlined.Search, "Load more", "Scroll near the bottom to fetch the next page of results."),
                GuideItem(Icons.Outlined.Share, "Share into Decibel", "Share a YouTube link from another app to open it in Browse preview."),
            ),
        )

        GuideSection(
            title = "Preview sheet",
            icon = Icons.Filled.PlayArrow,
            items = listOf(
                GuideItem(Icons.Filled.Close, "Close", "Dismisses the preview. Cancelled loads will not reopen."),
                GuideItem(Icons.Filled.PlayArrow, "Play online", "Streams the video immediately and opens Now Playing."),
                GuideItem(Icons.Filled.Videocam, "HQ video", "Download high-quality merged MP4 when available."),
                GuideItem(Icons.Filled.MusicNote, "Music (MP3)", "Download audio only as MP3."),
                GuideItem(Icons.Filled.Videocam, "Quick video", "Download a progressive single-file video (often lower resolution)."),
                GuideItem(Icons.Filled.DownloadDone, "Download", "Starts the selected option. Progress appears under Alerts."),
                GuideItem(Icons.Outlined.Info, "Already in library", "If the file exists, choose Overwrite or Cancel."),
            ),
        )

        GuideSection(
            title = "Library",
            icon = Icons.Outlined.LibraryMusic,
            items = listOf(
                GuideItem(Icons.Filled.Videocam, "Videos scope", "Show only video files from the top-bar dropdown."),
                GuideItem(Icons.Filled.MusicNote, "Music scope", "Show only audio / MP3 files."),
                GuideItem(Icons.Filled.Favorite, "Favourites scope", "Show only favourited items."),
                GuideItem(Icons.Filled.Search, "Filter", "Filter by title, uploader, filename, format, quality, or URL."),
                GuideItem(Icons.Outlined.TouchApp, "Tap to play", "Plays the item with the current visible list as the queue."),
                GuideItem(Icons.Filled.Favorite, "Swipe right", "Favourite or unfavourite a row."),
                GuideItem(Icons.Filled.Delete, "Swipe left", "Prompt to delete the file and library entry."),
                GuideItem(Icons.Outlined.TouchApp, "Long-press", "Enter multi-select starting with that item."),
                GuideItem(Icons.Filled.MoreVert, "Row menu", "Select, Convert to MP3, or Delete for one item."),
                GuideItem(Icons.Filled.DragHandle, "Reorder", "In Custom sort (no filter), drag the handle to rearrange."),
                GuideItem(Icons.Filled.GraphicEq, "Now playing mark", "Shows on the row that is currently playing or paused."),
                GuideItem(Icons.Filled.Favorite, "Bulk favourite", "In select mode, favourite or unfavourite all selected."),
                GuideItem(Icons.Filled.MusicNote, "Bulk convert", "Convert selected non-MP3 items to MP3."),
                GuideItem(Icons.Filled.Delete, "Bulk delete", "Delete selected items after confirmation."),
                GuideItem(Icons.Filled.Close, "Cancel selection", "Exit multi-select mode."),
            ),
        )

        GuideSection(
            title = "Mini player",
            icon = Icons.Filled.PlayArrow,
            items = listOf(
                GuideItem(Icons.Outlined.TouchApp, "Open Playing", "Tap artwork or title to open Now Playing."),
                GuideItem(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Previous", "Jump to the previous track."),
                GuideItem(Icons.Filled.PlayArrow, "Play / Pause", "Toggle playback without leaving the current tab."),
                GuideItem(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Next", "Jump to the next track."),
                GuideItem(Icons.Filled.Close, "Close", "Stop playback and hide the mini player."),
            ),
        )

        GuideSection(
            title = "Now Playing",
            icon = Icons.Outlined.PlayCircle,
            items = listOf(
                GuideItem(Icons.Filled.Shuffle, "Shuffle", "Toggle shuffled queue order."),
                GuideItem(Icons.Filled.SkipPrevious, "Previous", "Tap for previous track. Hold to scrub backward (speed ramps up)."),
                GuideItem(Icons.Filled.PlayArrow, "Play / Pause", "Toggle playback."),
                GuideItem(Icons.Filled.SkipNext, "Next", "Tap for next track. Hold to scrub forward (speed ramps up)."),
                GuideItem(Icons.Filled.Repeat, "Repeat", "Cycle Off → All → One. Repeat One uses a single-track icon."),
                GuideItem(Icons.Filled.RepeatOne, "Repeat one", "Loop the current track only."),
                GuideItem(Icons.Outlined.TouchApp, "Seek bar", "Drag to jump within the track (portrait)."),
                GuideItem(Icons.Filled.Timer, "Sleep timer", "Pick Off, 15, 30, 45, 60, or 90 minutes. Remaining time shows in the bar / HUD."),
                GuideItem(Icons.Filled.Fullscreen, "Landscape", "Force immersive landscape playback."),
                GuideItem(Icons.Filled.DarkMode, "Standby", "Open the AMOLED wave screensaver while music plays."),
                GuideItem(Icons.AutoMirrored.Filled.QueueMusic, "Up next", "Shows up to five upcoming queue items. Tap one to jump."),
                GuideItem(Icons.Outlined.TouchApp, "Landscape tap", "Show or hide player chrome. Chrome auto-hides after a few seconds."),
                GuideItem(Icons.Filled.FullscreenExit, "Exit landscape", "Return to portrait orientation."),
                GuideItem(Icons.AutoMirrored.Filled.ArrowBack, "Landscape back", "Leave Playing for the previous tab (hidden if you entered via Fullscreen)."),
            ),
        )

        GuideSection(
            title = "Standby",
            icon = Icons.Outlined.DarkMode,
            items = listOf(
                GuideItem(Icons.Filled.DarkMode, "Enter", "From Now Playing, or automatically after idle (see Settings → Standby)."),
                GuideItem(Icons.Outlined.TouchApp, "Tap", "Reveal transport controls. Tip: long-press to exit."),
                GuideItem(Icons.Outlined.TouchApp, "Long-press", "Exit standby and return to Now Playing."),
                GuideItem(Icons.Filled.PlayArrow, "Play / Pause", "Toggle playback in standby."),
                GuideItem(Icons.Filled.SkipPrevious, "Skip / hold-seek", "Tap to skip; hold previous/next to scrub with an xN speed label."),
                GuideItem(Icons.Filled.GraphicEq, "Wave visualizer", "Audio-reactive waves from playback (no microphone). Standby dims the screen and runs waves at a lower frame rate to save heat/battery."),
                GuideItem(Icons.Filled.Timer, "Sleep remaining", "Shows countdown when a sleep timer is active."),
            ),
        )

        GuideSection(
            title = "Alerts",
            icon = Icons.Outlined.Notifications,
            items = listOf(
                GuideItem(Icons.Outlined.Notifications, "Job list", "Active downloads first, then completed and failed jobs."),
                GuideItem(Icons.Filled.HourglassTop, "Progress", "Queued and running jobs show percent complete."),
                GuideItem(Icons.Filled.Close, "Dismiss", "Remove one finished notification card."),
                GuideItem(Icons.Outlined.DeleteSweep, "Clear done", "Top-bar icon clears all finished download statuses."),
            ),
        )

        GuideSection(
            title = "Settings",
            icon = Icons.Outlined.Settings,
            items = listOf(
                GuideItem(Icons.Outlined.Palette, "Appearance", "Mode: System, Light, or Dark. Themes: Teal, Ocean, Ember, Slate, Forest, Violet, Rose, Amber, Indigo, Lime, Crimson, Midnight, Sand, Mint, Graphite, Coral, Sky, Lavender, Copper, Plum, Moss, Cherry, Arctic, Honey, Orchid, Steel, Twilight, Sage, Magenta, Espresso, Aqua, Peach."),
                GuideItem(
                    Icons.Outlined.DarkMode,
                    "Standby settings",
                    "Wave styles: Equalizer bars, Dense bars, Peak bars, Floor bars, LED bars, Soft wave, Pulse rings, Orbit, Breathing pulse, Mirrored bars, Spectrum fill, Spectrum area, Center spectrum, Split spectrum, Neon bars, Spiral, Lattice, Ribbon, Starburst. Toggle Change style each track to cycle on every new song. Auto-enter: Off, 1, 5, or 15 minutes.",
                ),
                GuideItem(Icons.Outlined.LibraryMusic, "Library sort", "Custom order (drag to rearrange) or A–Z with letter scrubber."),
                GuideItem(Icons.Outlined.Equalizer, "Equalizer", "Enable EQ, pick a preset (Flat, Bass, Treble, Vocal, Rock, Pop, Electronic, Hip-Hop, Jazz, Classical, Acoustic, Loudness, Custom), or drag band levels."),
                GuideItem(Icons.Outlined.FolderOpen, "Storage", "Choose Video and Music folders, Scan to refresh the library, or Use app folder."),
                GuideItem(Icons.AutoMirrored.Outlined.MenuBook, "Full User Guide", "This screen — every function with matching icons."),
                GuideItem(Icons.Outlined.Info, "About", "App version and developer credit."),
            ),
        )

        GuideSection(
            title = "System & extras",
            icon = Icons.Outlined.Info,
            items = listOf(
                GuideItem(Icons.Outlined.Notifications, "Download notification", "Foreground progress while downloads run."),
                GuideItem(Icons.Filled.PlayArrow, "Lock screen / headset", "System media controls for play, pause, and skip while playing."),
                GuideItem(Icons.Outlined.Share, "Open media file", "Opening a media file with Decibel starts playback in Now Playing."),
                GuideItem(Icons.Filled.Timer, "Keep awake", "Screen stays on in Standby / Playing unless the sleep timer allows sleep."),
            ),
        )

        Text(
            text = "Decibel is for personal offline playback of media you are allowed to download.",
            color = colors.onSurfaceVariant,
            fontSize = 12.sp,
        )
    }
}

private data class GuideItem(
    val icon: ImageVector,
    val title: String,
    val body: String,
)

@Composable
private fun GuideSection(
    title: String,
    icon: ImageVector,
    items: List<GuideItem>,
) {
    val colors = MaterialTheme.colorScheme
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = colors.primary,
                modifier = Modifier.size(20.dp),
            )
            Text(
                text = title,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                color = colors.onSurface,
            )
        }
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = colors.surface),
        ) {
            Column(
                modifier = Modifier.padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp),
            ) {
                items.forEachIndexed { index, item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = null,
                            tint = colors.primary,
                            modifier = Modifier
                                .padding(top = 2.dp)
                                .size(20.dp),
                        )
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            Text(
                                text = item.title,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                color = colors.onSurface,
                            )
                            Text(
                                text = item.body,
                                color = colors.onSurfaceVariant,
                                fontSize = 12.sp,
                                lineHeight = 16.sp,
                            )
                        }
                    }
                    if (index < items.lastIndex) {
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 44.dp),
                            color = colors.outline.copy(alpha = 0.18f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AboutScreen(
    versionName: String,
    developerName: String,
) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = colors.surface),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "Decibel",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "Offline music & video player",
                    color = colors.onSurfaceVariant,
                    fontSize = 14.sp,
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                AboutMetaRow(label = "Version", value = versionName)
                AboutMetaRow(label = "Developed by", value = developerName)
            }
        }
        Text(
            text = "Download YouTube media for personal use, organize a local library, and play with sleep timer, standby waves, and equalizer controls.",
            color = colors.onSurfaceVariant,
            fontSize = 13.sp,
        )
    }
}

@Composable
private fun AboutMetaRow(
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 13.sp,
        )
        Text(
            text = value,
            fontWeight = FontWeight.Medium,
            fontSize = 13.sp,
        )
    }
}
