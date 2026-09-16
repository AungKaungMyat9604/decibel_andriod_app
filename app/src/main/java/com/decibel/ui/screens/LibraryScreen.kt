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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.decibel.data.SavedVideo
import com.decibel.ui.coilImageModel
import com.decibel.youtube.formatBytes
import com.decibel.youtube.formatDuration
import com.mariesta.menzies.washui.icons.LucideIcons
import com.mariesta.menzies.washui.icons.WashIcon
import com.mariesta.menzies.washui.icons.lucide.Music4
import com.mariesta.menzies.washui.primitives.WashButton
import com.mariesta.menzies.washui.primitives.WashButtonVariant
import com.mariesta.menzies.washui.primitives.WashDialog
import com.mariesta.menzies.washui.primitives.WashDialogTone
import com.mariesta.menzies.washui.primitives.WashDivider
import com.mariesta.menzies.washui.primitives.WashInput
import com.mariesta.menzies.washui.primitives.WashText
import com.mariesta.menzies.washui.theme.WashTheme
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private val Alphabet = listOf('#') + ('A'..'Z').toList()

private enum class LibraryScope {
    All,
    Favourites,
}

private fun titleLetter(title: String): Char {
    val c = title.trim().firstOrNull()?.uppercaseChar() ?: return '#'
    return if (c in 'A'..'Z') c else '#'
}

@Composable
fun LibraryScreen(
    videos: List<SavedVideo>,
    onPlay: (SavedVideo) -> Unit,
    onDelete: (String) -> Unit,
    onToggleFavourite: (String) -> Unit,
) {
    val colors = WashTheme.colors
    var pendingDelete by remember { mutableStateOf<SavedVideo?>(null) }
    var filter by remember { mutableStateOf("") }
    var scopeFilter by remember { mutableStateOf(LibraryScope.All) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    val filtered = remember(videos, filter, scopeFilter) {
        val q = filter.trim()
        val scoped = when (scopeFilter) {
            LibraryScope.All -> videos
            LibraryScope.Favourites -> videos.filter { it.isFavourite }
        }
        val base = if (q.isEmpty()) {
            scoped
        } else {
            scoped.filter {
                it.title.contains(q, ignoreCase = true) ||
                    it.uploader.contains(q, ignoreCase = true)
            }
        }
        base.sortedBy { it.title.lowercase() }
    }

    val favouriteCount = remember(videos) { videos.count { it.isFavourite } }

    val sections = remember(filtered) {
        filtered.groupBy { titleLetter(it.title) }
            .toSortedMap(compareBy { if (it == '#') '@' else it })
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

    WashDialog(
        open = pendingDelete != null,
        onClose = { pendingDelete = null },
        title = "Delete from library?",
        description = pendingDelete?.let {
            "“${it.title}” will be removed from your library and the file will be deleted."
        },
        tone = WashDialogTone.Error,
        actions = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                WashButton(
                    onClick = { pendingDelete = null },
                    text = "Cancel",
                    variant = WashButtonVariant.Ghost,
                )
                WashButton(
                    onClick = {
                        pendingDelete?.let { onDelete(it.id) }
                        pendingDelete = null
                    },
                    text = "Delete",
                    variant = WashButtonVariant.Primary,
                )
            }
        },
    )

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        WashInput(
            value = filter,
            onValueChange = { filter = it },
            label = "Filter",
            placeholder = "Search titles…",
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ScopeChip(
                label = "All",
                selected = scopeFilter == LibraryScope.All,
                onClick = { scopeFilter = LibraryScope.All },
            )
            ScopeChip(
                label = if (favouriteCount > 0) "Favourites ($favouriteCount)" else "Favourites",
                selected = scopeFilter == LibraryScope.Favourites,
                onClick = { scopeFilter = LibraryScope.Favourites },
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
                    WashText(
                        text = when {
                            scopeFilter == LibraryScope.Favourites -> "No favourites yet"
                            filter.isNotBlank() -> "No matches"
                            else -> "No offline videos"
                        },
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 18.sp,
                    )
                    WashText(
                        text = when {
                            scopeFilter == LibraryScope.Favourites ->
                                "Swipe a row right to add it to Favourites."
                            filter.isNotBlank() ->
                                "No titles match “$filter”."
                            else ->
                                "Download from Browse, or scan a folder in Settings."
                        },
                        color = colors.ink_muted,
                        fontSize = 13.sp,
                    )
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
                    contentPadding = PaddingValues(end = 4.dp, bottom = 16.dp),
                ) {
                    sections.forEach { (letter, items) ->
                        item(key = "header_$letter") {
                            WashText(
                                text = letter.toString(),
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = colors.primary,
                                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
                            )
                        }
                        items(items, key = { it.id }) { video ->
                            SwipeLibraryRow(
                                video = video,
                                onPlay = { onPlay(video) },
                                onFavourite = { onToggleFavourite(video.id) },
                                onRequestDelete = { pendingDelete = video },
                            )
                            WashDivider()
                        }
                    }
                }

                AlphabetScrubber(
                    letters = Alphabet,
                    present = sections.keys,
                    onSelect = { scrollToLetter(it) },
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(22.dp),
                )
            }
        }
    }
}

@Composable
private fun ScopeChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val colors = WashTheme.colors
    WashText(
        text = label,
        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
        fontSize = 13.sp,
        color = if (selected) colors.primary else colors.ink_muted,
        modifier = Modifier
            .clip(RoundedCornerShape(colors.radiusField))
            .background(if (selected) colors.primary.copy(alpha = 0.12f) else colors.base_200)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    )
}

@Composable
private fun AlphabetScrubber(
    letters: List<Char>,
    present: Set<Char>,
    onSelect: (Char) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = WashTheme.colors
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
            WashText(
                text = letter.toString(),
                fontSize = 10.sp,
                fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                color = if (active) colors.primary else colors.ink_muted.copy(alpha = 0.45f),
                modifier = Modifier
                    .clickable(enabled = active) { onSelect(letter) }
                    .padding(vertical = 0.5.dp),
            )
        }
    }
}

@Composable
private fun SwipeLibraryRow(
    video: SavedVideo,
    onPlay: () -> Unit,
    onFavourite: () -> Unit,
    onRequestDelete: () -> Unit,
) {
    val colors = WashTheme.colors
    val density = LocalDensity.current
    val maxSwipePx = with(density) { 96.dp.toPx() }
    val threshold = maxSwipePx * 0.4f

    // Fresh offset whenever favourite changes — prevents stuck mid-swipe rows.
    var offsetX by remember(video.id, video.isFavourite) { mutableFloatStateOf(0f) }
    val onFavouriteLatest = rememberUpdatedState(onFavourite)
    val onDeleteLatest = rememberUpdatedState(onRequestDelete)
    val onPlayLatest = rememberUpdatedState(onPlay)

    LaunchedEffect(video.isFavourite) {
        offsetX = 0f
    }

    val rowBg = if (video.isFavourite) {
        // Opaque tint — alpha would let swipe reveals show through.
        androidx.compose.ui.graphics.lerp(colors.base_100, colors.primary, 0.18f)
    } else {
        colors.base_100
    }

    val dragState = rememberDraggableState { delta ->
        offsetX = (offsetX + delta).coerceIn(-maxSwipePx, maxSwipePx)
    }

    Box(modifier = Modifier.fillMaxWidth()) {
        // Only draw action reveals while the row is actually being swiped.
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
                        WashText(
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
                        WashText(
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
                .offset { IntOffset(offsetX.roundToInt(), 0) }
                .background(rowBg)
                .clickable { onPlayLatest.value() }
                // Horizontal draggable must be outermost so it wins over click / list scroll.
                .draggable(
                    state = dragState,
                    orientation = Orientation.Horizontal,
                    onDragStopped = {
                        val x = offsetX
                        offsetX = 0f // close immediately — never leave stuck open
                        when {
                            x >= threshold -> onFavouriteLatest.value()
                            x <= -threshold -> onDeleteLatest.value()
                        }
                    },
                )
                .padding(horizontal = 4.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            LibraryThumb(
                thumbnailUrl = video.thumbnailUrl,
                modifier = Modifier.size(40.dp),
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                WashText(
                    text = video.title,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    fontSize = 14.sp,
                )
                WashText(
                    text = buildString {
                        append(video.uploader)
                        append(" · ")
                        append(video.durationSeconds.formatDuration())
                        append(" · ")
                        append(video.fileSizeBytes.formatBytes())
                    },
                    color = colors.ink_muted,
                    fontSize = 11.sp,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
fun LibraryThumb(
    thumbnailUrl: String?,
    modifier: Modifier = Modifier,
) {
    val colors = WashTheme.colors
    val model = remember(thumbnailUrl) { coilImageModel(thumbnailUrl) }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(colors.base_300),
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
            WashIcon(
                imageVector = LucideIcons.Music4,
                contentDescription = null,
                tint = colors.ink_muted,
                size = 18.dp,
            )
        }
    }
}
