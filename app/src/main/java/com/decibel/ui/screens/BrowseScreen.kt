package com.decibel.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.mariesta.menzies.washui.primitives.WashButton
import com.mariesta.menzies.washui.primitives.WashButtonVariant
import com.mariesta.menzies.washui.primitives.WashInput
import com.mariesta.menzies.washui.primitives.WashPanel
import com.mariesta.menzies.washui.primitives.WashText
import com.mariesta.menzies.washui.theme.WashTheme
import com.decibel.ui.BrowseUiState
import com.decibel.youtube.CatalogVideo
import com.decibel.youtube.formatDuration

@Composable
fun BrowseScreen(
    state: BrowseUiState,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    onOpenItem: (CatalogVideo) -> Unit,
) {
    val colors = WashTheme.colors
    val listState = rememberLazyListState()
    val shouldLoadMore by remember {
        derivedStateOf {
            val info = listState.layoutInfo
            val last = info.visibleItemsInfo.lastOrNull()?.index ?: 0
            last >= info.totalItemsCount - 3
        }
    }
    LaunchedEffect(shouldLoadMore, state.items.size) {
        if (shouldLoadMore && !state.loading && !state.loadingMore && state.items.isNotEmpty()) {
            onLoadMore()
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        WashPanel(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                WashInput(
                    value = state.query,
                    onValueChange = onQueryChange,
                    label = "Search YouTube",
                    placeholder = "Songs, artists, videos…",
                    enabled = !state.loading,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    WashButton(
                        onClick = onSearch,
                        text = "Search",
                        variant = WashButtonVariant.Primary,
                        loading = state.loading && state.query.isNotBlank(),
                        enabled = !state.loading,
                        modifier = Modifier.weight(1f),
                    )
                    WashButton(
                        onClick = onRefresh,
                        text = "Trending",
                        variant = WashButtonVariant.Outline,
                        enabled = !state.loading,
                    )
                }
            }
        }

        WashText(
            text = state.feedTitle,
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp,
        )

        val showLoading = state.loading && state.items.isEmpty()
        val showError = !state.error.isNullOrBlank() && state.items.isEmpty()
        val errorText = state.error.orEmpty()

        if (showLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                WashText(text = "Loading…", color = colors.ink_muted)
            }
        } else if (showError) {
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
                    WashText(text = errorText, color = colors.error)
                    WashButton(onClick = onRefresh, text = "Retry", variant = WashButtonVariant.Outline)
                }
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 12.dp),
            ) {
                items(state.items, key = { it.id + it.webpageUrl }) { video ->
                    VideoCard(video = video, onClick = { onOpenItem(video) })
                }
                if (state.loadingMore) {
                    item {
                        WashText(
                            text = "Loading more…",
                            color = colors.ink_muted,
                            modifier = Modifier.padding(8.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VideoCard(video: CatalogVideo, onClick: () -> Unit) {
    val colors = WashTheme.colors
    WashPanel(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .weight(0.42f)
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(colors.radiusField))
                    .background(colors.base_300),
            ) {
                if (!video.thumbnailUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = video.thumbnailUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
            Column(
                modifier = Modifier.weight(0.58f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                WashText(
                    text = video.title,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    fontSize = 14.sp,
                )
                WashText(
                    text = video.uploader,
                    color = colors.ink_muted,
                    fontSize = 12.sp,
                    maxLines = 1,
                )
                if (video.durationSeconds > 0) {
                    WashText(
                        text = video.durationSeconds.formatDuration(),
                        color = colors.ink_muted,
                        fontSize = 12.sp,
                    )
                }
            }
        }
    }
}
