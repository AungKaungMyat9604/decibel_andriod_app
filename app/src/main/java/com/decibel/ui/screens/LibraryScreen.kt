package com.decibel.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.decibel.data.LibraryScope
import com.decibel.data.LibrarySort
import com.decibel.data.MediaType
import com.decibel.data.SavedVideo
import com.decibel.ui.coilImageModel
import com.decibel.youtube.formatBytes
import com.decibel.youtube.formatDuration
import kotlinx.coroutines.launch
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

private val Alphabet = listOf('#') + ('A'..'Z').toList()

private fun titleLetter(title: String): Char {
    val c = title.trim().firstOrNull()?.uppercaseChar() ?: return '#'
    return if (c in 'A'..'Z') c else '#'
}

/** Lowercase letters/digits only — so "jme" matches "J.M.E", "j_m_e", "JME", etc. */
private fun String.normalizeForSearch(): String =
    lowercase().filter { it.isLetterOrDigit() }

private fun SavedVideo.matchesQuery(rawQuery: String): Boolean {
    val q = rawQuery.trim()
    if (q.isEmpty()) return true

    val fields = listOf(
        title,
        uploader,
        fileName,
        format,
        quality,
        webpageUrl,
        id,
    )

    if (fields.any { it.contains(q, ignoreCase = true) }) return true

    val normQ = q.normalizeForSearch()
    if (normQ.isNotEmpty() && fields.any { it.normalizeForSearch().contains(normQ) }) {
        return true
    }

    val tokens = q.split(Regex("\\s+")).map { it.trim() }.filter { it.isNotEmpty() }
    if (tokens.size <= 1) return false
    return tokens.all { token ->
        fields.any { it.contains(token, ignoreCase = true) } ||
            token.normalizeForSearch().let { n ->
                n.isNotEmpty() && fields.any { it.normalizeForSearch().contains(n) }
            }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LibraryScreen(
    videos: List<SavedVideo>,
    scopeFilter: LibraryScope,
    sortMode: LibrarySort,
    filter: String,
    playingId: String? = null,
    isPlaying: Boolean = false,
    onPlay: (item: SavedVideo, queue: List<SavedVideo>) -> Unit,
    onDelete: (String) -> Unit,
    onDeleteMany: (Collection<String>) -> Unit = { ids -> ids.forEach(onDelete) },
    onToggleFavourite: (String) -> Unit,
    onSetFavourite: (id: String, favourite: Boolean) -> Unit = { id, _ -> onToggleFavourite(id) },
    onReorder: (orderedVisibleIds: List<String>) -> Unit,
    onConvertToMp3: (String) -> Unit = {},
) {
    val colors = MaterialTheme.colorScheme
    var pendingDelete by remember { mutableStateOf<SavedVideo?>(null) }
    var pendingBulkDelete by remember { mutableStateOf(false) }
    var selecting by remember { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf(emptySet<String>()) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    val scoped = remember(videos, scopeFilter) {
        when (scopeFilter) {
            LibraryScope.Videos -> videos.filter { it.mediaType == MediaType.VIDEO }
            LibraryScope.Music -> videos.filter { it.mediaType == MediaType.AUDIO }
            LibraryScope.Favourites -> videos.filter { it.isFavourite }
        }
    }

    val filteredBase = remember(scoped, filter) {
        val q = filter.trim()
        if (q.isEmpty()) scoped else scoped.filter { it.matchesQuery(q) }
    }

    val filtered = remember(filteredBase, sortMode) {
        when (sortMode) {
            LibrarySort.Custom -> filteredBase
            LibrarySort.Az -> filteredBase
                .groupBy { titleLetter(it.title) }
                .toSortedMap(compareBy { if (it == '#') '@' else it })
                .flatMap { (_, items) -> items.sortedBy { it.title.lowercase() } }
        }
    }

    // Drop selections that left the visible list.
    LaunchedEffect(filtered, selecting) {
        if (!selecting) return@LaunchedEffect
        val visible = filtered.map { it.id }.toSet()
        selectedIds = selectedIds.intersect(visible)
    }

    // Local order for Custom mode; library updates live via rememberReorderableLazyListState.
    var customItems by remember { mutableStateOf(filtered) }
    var listDragging by remember { mutableStateOf(false) }
    val onReorderLatest = rememberUpdatedState(onReorder)

    LaunchedEffect(filtered, sortMode, listDragging) {
        if (!listDragging) {
            customItems = filtered
        }
    }

    val reorderableLazyListState = rememberReorderableLazyListState(listState) { from, to ->
        customItems = customItems.toMutableList().apply {
            add(to.index, removeAt(from.index))
        }
        onReorderLatest.value(customItems.map { it.id })
    }

    val reorderEnabled = !selecting && sortMode == LibrarySort.Custom && filter.isBlank()
    val listScrolling by remember {
        derivedStateOf { listState.isScrollInProgress }
    }
    val allowRowSwipe = !listScrolling && !listDragging
    val visibleIds = remember(filtered) { filtered.map { it.id } }
    val allVisibleSelected =
        visibleIds.isNotEmpty() && visibleIds.all { it in selectedIds }
    val selectedItems = remember(videos, selectedIds) {
        videos.filter { it.id in selectedIds }
    }
    val selectedConvertible = remember(selectedItems) {
        selectedItems.filterNot { it.format.equals("mp3", ignoreCase = true) }
    }
    val selectedAllFavourite = selectedItems.isNotEmpty() && selectedItems.all { it.isFavourite }

    fun enterSelect(initialId: String? = null) {
        selecting = true
        selectedIds = if (initialId != null) setOf(initialId) else emptySet()
    }

    fun exitSelect() {
        selecting = false
        selectedIds = emptySet()
        pendingBulkDelete = false
    }

    fun toggleSelected(id: String) {
        selectedIds = if (id in selectedIds) selectedIds - id else selectedIds + id
    }

    val sections = remember(filtered, sortMode) {
        if (sortMode != LibrarySort.Az) {
            emptyMap()
        } else {
            filtered.groupBy { titleLetter(it.title) }
                .toSortedMap(compareBy { if (it == '#') '@' else it })
        }
    }

    val flatKeys = remember(sections) {
        buildList {
            sections.forEach { (letter, items) ->
                add("header_$letter")
                items.forEach { add("item_${it.id}") }
            }
        }
    }

    fun scrollToLetter(letter: Char) {
        val key = "header_$letter"
        val index = flatKeys.indexOf(key)
        if (index >= 0) {
            scope.launch { listState.animateScrollToItem(index) }
        } else {
            val next = flatKeys.indexOfFirst {
                it.startsWith("header_") && it.removePrefix("header_")[0] >= letter
            }
            if (next >= 0) scope.launch { listState.animateScrollToItem(next) }
        }
    }

    if (pendingDelete != null) {
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Delete from library?") },
            text = {
                Text(
                    "“${pendingDelete!!.title}” will be removed from your library and the file will be deleted.",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingDelete?.let { onDelete(it.id) }
                        pendingDelete = null
                    },
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("Cancel") }
            },
        )
    }

    if (pendingBulkDelete) {
        val count = selectedIds.size
        AlertDialog(
            onDismissRequest = { pendingBulkDelete = false },
            title = { Text("Delete $count items?") },
            text = {
                Text("Selected items will be removed from your library and their files will be deleted.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteMany(selectedIds)
                        pendingBulkDelete = false
                        exitSelect()
                    },
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { pendingBulkDelete = false }) { Text("Cancel") }
            },
        )
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (selecting) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = if (selectedIds.isEmpty()) {
                        "Select items"
                    } else {
                        "${selectedIds.size} selected"
                    },
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    color = colors.onSurface,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(
                        onClick = {
                            selectedIds = if (allVisibleSelected) {
                                emptySet()
                            } else {
                                visibleIds.toSet()
                            }
                        },
                        enabled = visibleIds.isNotEmpty(),
                    ) {
                        Text(if (allVisibleSelected) "Clear" else "Select all")
                    }
                    IconButton(onClick = { exitSelect() }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cancel selection",
                        )
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(
                    onClick = {
                        val makeFav = !selectedAllFavourite
                        selectedIds.forEach { onSetFavourite(it, makeFav) }
                    },
                    enabled = selectedIds.isNotEmpty(),
                ) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        text = if (selectedAllFavourite) " Unfavourite" else " Favourite",
                        modifier = Modifier.padding(start = 4.dp),
                    )
                }
                TextButton(
                    onClick = {
                        selectedConvertible.forEach { onConvertToMp3(it.id) }
                        exitSelect()
                    },
                    enabled = selectedConvertible.isNotEmpty(),
                ) {
                    Text("MP3 (${selectedConvertible.size})")
                }
                TextButton(
                    onClick = { pendingBulkDelete = true },
                    enabled = selectedIds.isNotEmpty(),
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = colors.error,
                    )
                    Text(
                        text = " Delete",
                        color = colors.error,
                        modifier = Modifier.padding(start = 4.dp),
                    )
                }
            }
        } else if (reorderEnabled) {
            Text(
                text = "Drag the ≡ handle to reorder. Long-press a row to select.",
                color = colors.onSurfaceVariant,
                fontSize = 12.sp,
            )
        }

        if (filtered.isEmpty()) {
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
                    Text(
                        text = when {
                            scopeFilter == LibraryScope.Favourites -> "No favourites yet"
                            scopeFilter == LibraryScope.Music -> "No music yet"
                            filter.isNotBlank() -> "No matches"
                            else -> "No offline videos"
                        },
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 18.sp,
                        color = colors.onSurface,
                    )
                    Text(
                        text = when {
                            scopeFilter == LibraryScope.Favourites ->
                                "Swipe a row right to add it to Favourites."
                            scopeFilter == LibraryScope.Music ->
                                "Download MP3 from Browse, or scan a Music folder in Settings."
                            filter.isNotBlank() ->
                                "No items match “$filter”."
                            else ->
                                "Download from Browse, or scan a Video folder in Settings."
                        },
                        color = colors.onSurfaceVariant,
                        fontSize = 13.sp,
                    )
                }
            }
        } else if (sortMode == LibrarySort.Custom && reorderEnabled) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(bottom = 16.dp),
            ) {
                itemsIndexed(
                    items = customItems,
                    key = { _, video -> video.id },
                    contentType = { _, _ -> "library_row" },
                ) { _, video ->
                    ReorderableItem(reorderableLazyListState, key = video.id) { isDragging ->
                        Column(
                            modifier = if (isDragging) Modifier.shadow(4.dp) else Modifier,
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(modifier = Modifier.weight(1f)) {
                                    SwipeLibraryRow(
                                        video = video,
                                        swipeEnabled = allowRowSwipe,
                                        selectionMode = selecting,
                                        selected = video.id in selectedIds,
                                        isCurrent = video.id == playingId,
                                        isPlaying = video.id == playingId && isPlaying,
                                        onToggleSelect = { toggleSelected(video.id) },
                                        onLongPressSelect = { enterSelect(video.id) },
                                        onPlay = { onPlay(video, customItems) },
                                        onFavourite = { onToggleFavourite(video.id) },
                                        onRequestDelete = { pendingDelete = video },
                                        onConvertToMp3 = { onConvertToMp3(video.id) },
                                    )
                                }
                                IconButton(
                                    onClick = {},
                                    modifier = Modifier.draggableHandle(
                                        onDragStarted = { listDragging = true },
                                        onDragStopped = { listDragging = false },
                                    ),
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DragHandle,
                                        contentDescription = "Reorder",
                                    )
                                }
                            }
                            HorizontalDivider()
                        }
                    }
                }
            }
        } else if (sortMode == LibrarySort.Custom) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(bottom = 16.dp),
            ) {
                items(
                    items = customItems,
                    key = { it.id },
                    contentType = { "library_row" },
                ) { video ->
                    SwipeLibraryRow(
                        video = video,
                        swipeEnabled = allowRowSwipe && !selecting,
                        selectionMode = selecting,
                        selected = video.id in selectedIds,
                        isCurrent = video.id == playingId,
                        isPlaying = video.id == playingId && isPlaying,
                        onToggleSelect = { toggleSelected(video.id) },
                        onLongPressSelect = { enterSelect(video.id) },
                        onPlay = { onPlay(video, customItems) },
                        onFavourite = { onToggleFavourite(video.id) },
                        onRequestDelete = { pendingDelete = video },
                        onConvertToMp3 = { onConvertToMp3(video.id) },
                    )
                    HorizontalDivider()
                }
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    contentPadding = PaddingValues(bottom = 16.dp),
                ) {
                    sections.forEach { (letter, items) ->
                        item(key = "header_$letter") {
                            Text(
                                text = letter.toString(),
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = colors.primary,
                                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
                            )
                        }
                        items(items, key = { it.id }, contentType = { "library_row" }) { video ->
                            SwipeLibraryRow(
                                video = video,
                                swipeEnabled = allowRowSwipe && !selecting,
                                selectionMode = selecting,
                                selected = video.id in selectedIds,
                                isCurrent = video.id == playingId,
                                isPlaying = video.id == playingId && isPlaying,
                                onToggleSelect = { toggleSelected(video.id) },
                                onLongPressSelect = { enterSelect(video.id) },
                                onPlay = { onPlay(video, filtered) },
                                onFavourite = { onToggleFavourite(video.id) },
                                onRequestDelete = { pendingDelete = video },
                                onConvertToMp3 = { onConvertToMp3(video.id) },
                            )
                            HorizontalDivider()
                        }
                    }
                }

                AlphabetScrubber(
                    letters = Alphabet,
                    present = sections.keys,
                    onSelect = { scrollToLetter(it) },
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(20.dp),
                )
            }
        }
    }
}

@Composable
private fun AlphabetScrubber(
    letters: List<Char>,
    present: Set<Char>,
    onSelect: (Char) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    var heightPx by remember { mutableStateOf(1) }

    fun letterAt(y: Float): Char {
        val idx = ((y / heightPx.coerceAtLeast(1)) * letters.size)
            .toInt()
            .coerceIn(0, letters.lastIndex)
        return letters[idx]
    }

    Column(
        modifier = modifier
            .onSizeChanged { heightPx = it.height }
            .pointerInput(letters, heightPx) {
                detectVerticalDragGestures(
                    onDragStart = { offset -> onSelect(letterAt(offset.y)) },
                    onVerticalDrag = { change, _ ->
                        change.consume()
                        onSelect(letterAt(change.position.y))
                    },
                )
            }
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        letters.forEach { letter ->
            val active = letter in present
            Text(
                text = letter.toString(),
                fontSize = 10.sp,
                fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                color = if (active) colors.primary else colors.onSurfaceVariant.copy(alpha = 0.45f),
                modifier = Modifier
                    .clickable(enabled = active) { onSelect(letter) }
                    .padding(vertical = 0.5.dp),
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SwipeLibraryRow(
    video: SavedVideo,
    onPlay: () -> Unit,
    onFavourite: () -> Unit,
    onRequestDelete: () -> Unit,
    onConvertToMp3: () -> Unit = {},
    swipeEnabled: Boolean = true,
    selectionMode: Boolean = false,
    selected: Boolean = false,
    isCurrent: Boolean = false,
    isPlaying: Boolean = false,
    onToggleSelect: () -> Unit = {},
    onLongPressSelect: () -> Unit = {},
) {
    val colors = MaterialTheme.colorScheme
    val density = LocalDensity.current
    val maxSwipePx = with(density) { 96.dp.toPx() }
    val threshold = maxSwipePx * 0.4f

    var offsetX by remember(video.id, video.isFavourite) { mutableFloatStateOf(0f) }
    var menuOpen by remember { mutableStateOf(false) }
    val onFavouriteLatest = rememberUpdatedState(onFavourite)
    val onDeleteLatest = rememberUpdatedState(onRequestDelete)
    val onPlayLatest = rememberUpdatedState(onPlay)
    val onToggleSelectLatest = rememberUpdatedState(onToggleSelect)
    val onLongPressLatest = rememberUpdatedState(onLongPressSelect)
    val canConvert = !video.format.equals("mp3", ignoreCase = true)
    val canSwipe = swipeEnabled && !selectionMode

    LaunchedEffect(video.isFavourite) {
        offsetX = 0f
    }

    LaunchedEffect(canSwipe, selectionMode) {
        if (!canSwipe) offsetX = 0f
    }

    val rowBg = when {
        selectionMode && selected ->
            androidx.compose.ui.graphics.lerp(colors.surface, colors.primary, 0.22f)
        isCurrent ->
            androidx.compose.ui.graphics.lerp(colors.surface, colors.primary, 0.20f)
        video.isFavourite ->
            androidx.compose.ui.graphics.lerp(colors.surface, colors.primary, 0.12f)
        else -> colors.surface
    }

    val dragState = rememberDraggableState { delta ->
        if (canSwipe) {
            offsetX = (offsetX + delta).coerceIn(-maxSwipePx, maxSwipePx)
        }
    }

    Box(modifier = Modifier.fillMaxWidth()) {
        if (kotlin.math.abs(offsetX) > 0.5f) {
            Row(modifier = Modifier.matchParentSize()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(
                            if (offsetX > 0f) Color(0xFF2E7D57) else Color.Transparent,
                        ),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    if (offsetX > 12f) {
                        Text(
                            text = if (video.isFavourite) "Unfavourite" else "Favourite",
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(start = 20.dp),
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(
                            if (offsetX < 0f) colors.error else Color.Transparent,
                        ),
                    contentAlignment = Alignment.CenterEnd,
                ) {
                    if (offsetX < -12f) {
                        Text(
                            text = "Delete",
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(end = 20.dp),
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer { translationX = offsetX }
                .background(rowBg)
                .combinedClickable(
                    onClick = {
                        if (selectionMode) {
                            onToggleSelectLatest.value()
                        } else {
                            onPlayLatest.value()
                        }
                    },
                    onLongClick = {
                        if (selectionMode) {
                            onToggleSelectLatest.value()
                        } else {
                            onLongPressLatest.value()
                        }
                    },
                )
                .then(
                    if (canSwipe) {
                        Modifier.draggable(
                            state = dragState,
                            orientation = Orientation.Horizontal,
                            onDragStopped = {
                                val x = offsetX
                                offsetX = 0f
                                when {
                                    x >= threshold -> onFavouriteLatest.value()
                                    x <= -threshold -> onDeleteLatest.value()
                                }
                            },
                        )
                    } else {
                        Modifier
                    },
                )
                .padding(horizontal = 4.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (selectionMode) {
                Checkbox(
                    checked = selected,
                    onCheckedChange = { onToggleSelectLatest.value() },
                )
            }
            LibraryThumb(
                thumbnailUrl = video.thumbnailUrl,
                isCurrent = isCurrent,
                isPlaying = isPlaying,
                modifier = Modifier.size(40.dp),
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = video.title,
                    fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 14.sp,
                    color = if (isCurrent) colors.primary else colors.onSurface,
                )
                Text(
                    text = buildString {
                        if (isCurrent) {
                            append(if (isPlaying) "Playing · " else "Paused · ")
                        }
                        append(video.uploader)
                        append(" · ")
                        append(video.durationSeconds.formatDuration())
                        append(" · ")
                        append(video.fileSizeBytes.formatBytes())
                        if (video.mediaType == MediaType.AUDIO || video.format.equals("mp3", true)) {
                            append(" · MP3")
                        }
                    },
                    color = if (isCurrent) colors.primary.copy(alpha = 0.85f) else colors.onSurfaceVariant,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (!selectionMode) {
                Box {
                    IconButton(
                        onClick = { menuOpen = true },
                        modifier = Modifier.size(36.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "More",
                            tint = colors.onSurfaceVariant,
                        )
                    }
                    DropdownMenu(
                        expanded = menuOpen,
                        onDismissRequest = { menuOpen = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text("Select") },
                            onClick = {
                                menuOpen = false
                                onLongPressLatest.value()
                            },
                        )
                        if (canConvert) {
                            DropdownMenuItem(
                                text = { Text("Convert to MP3") },
                                onClick = {
                                    menuOpen = false
                                    onConvertToMp3()
                                },
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("Delete") },
                            onClick = {
                                menuOpen = false
                                onRequestDelete()
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LibraryThumb(
    thumbnailUrl: String?,
    modifier: Modifier = Modifier,
    isCurrent: Boolean = false,
    isPlaying: Boolean = false,
) {
    val colors = MaterialTheme.colorScheme
    val context = LocalContext.current
    val model = remember(thumbnailUrl, context) {
        coilImageModel(thumbnailUrl)?.let { data ->
            ImageRequest.Builder(context)
                .data(data)
                .size(120)
                .crossfade(false)
                .build()
        }
    }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(colors.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        if (model != null) {
            AsyncImage(
                model = model,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Icon(
                imageVector = Icons.Default.MusicNote,
                contentDescription = null,
                tint = colors.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
        }
        if (isCurrent) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.45f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.GraphicEq else Icons.Default.Pause,
                    contentDescription = if (isPlaying) "Playing" else "Paused",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}
