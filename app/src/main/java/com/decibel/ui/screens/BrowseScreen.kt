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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.decibel.data.DownloadJobState
import com.decibel.ui.BrowseUiState
import com.decibel.youtube.CatalogVideo
import com.decibel.youtube.formatDuration

/** Library / in-flight download badge for browse rows. */
enum class BrowseDownloadBadge {
    None,
    Downloaded,
    Queued,
    Downloading,
}

@Composable
fun BrowseScreen(
    state: BrowseUiState,
    downloadedIds: Set<String>,
    /** YouTube / catalog ids → active job state (Queued or Running). */
    activeDownloadStates: Map<String, DownloadJobState> = emptyMap(),
    onRetry: () -> Unit,
    onLoadMore: () -> Unit,
    onOpenItem: (CatalogVideo) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
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
        Text(
            text = state.feedTitle,
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp,
            color = colors.onSurface,
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
                Text(text = "Loading…", color = colors.onSurfaceVariant)
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
                    Text(text = errorText, color = colors.error)
                    OutlinedButton(onClick = onRetry) { Text("Retry") }
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
                    val badge = remember(video.id, downloadedIds, activeDownloadStates) {
                        browseBadgeFor(video.id, downloadedIds, activeDownloadStates)
                    }
                    VideoCard(
                        video = video,
                        badge = badge,
                        onClick = { onOpenItem(video) },
                    )
                }
                if (state.loadingMore) {
                    item {
                        Text(
                            text = "Loading more…",
                            color = colors.onSurfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            fontSize = 12.sp,
                        )
                    }
                }
            }
        }
    }
}

private fun browseBadgeFor(
    videoId: String,
    downloadedIds: Set<String>,
    activeDownloadStates: Map<String, DownloadJobState>,
): BrowseDownloadBadge {
    val active = activeDownloadStates[videoId]
    return when {
        active == DownloadJobState.Running -> BrowseDownloadBadge.Downloading
        active == DownloadJobState.Queued -> BrowseDownloadBadge.Queued
        videoId in downloadedIds -> BrowseDownloadBadge.Downloaded
        else -> BrowseDownloadBadge.None
    }
}

@Composable
private fun VideoCard(
    video: CatalogVideo,
    badge: BrowseDownloadBadge,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .weight(0.42f)
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(colors.surfaceVariant),
            ) {
                if (!video.thumbnailUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = video.thumbnailUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                if (badge != BrowseDownloadBadge.None) {
                    BrowseStatusChip(
                        badge = badge,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(6.dp),
                    )
                }
            }
            Column(
                modifier = Modifier.weight(0.58f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = video.title,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 14.sp,
                    color = colors.onSurface,
                )
                Text(
                    text = video.uploader,
                    color = colors.onSurfaceVariant,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (video.durationSeconds > 0) {
                        Text(
                            text = video.durationSeconds.formatDuration(),
                            color = colors.onSurfaceVariant,
                            fontSize = 12.sp,
                        )
                    }
                    when (badge) {
                        BrowseDownloadBadge.Downloading -> Text(
                            text = "Downloading",
                            color = colors.primary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                        )
                        BrowseDownloadBadge.Queued -> Text(
                            text = "Queued",
                            color = colors.onSurfaceVariant,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                        )
                        BrowseDownloadBadge.Downloaded -> Text(
                            text = "Downloaded",
                            color = colors.primary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                        )
                        BrowseDownloadBadge.None -> Unit
                    }
                }
            }
        }
    }
}

@Composable
private fun BrowseStatusChip(
    badge: BrowseDownloadBadge,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val container = when (badge) {
        BrowseDownloadBadge.Downloaded -> colors.primary
        BrowseDownloadBadge.Downloading -> colors.primary
        BrowseDownloadBadge.Queued -> colors.secondaryContainer
        BrowseDownloadBadge.None -> colors.primary
    }
    val content = when (badge) {
        BrowseDownloadBadge.Downloaded -> colors.onPrimary
        BrowseDownloadBadge.Downloading -> colors.onPrimary
        BrowseDownloadBadge.Queued -> colors.onSecondaryContainer
        BrowseDownloadBadge.None -> colors.onPrimary
    }
    Box(
        modifier = modifier
            .size(22.dp)
            .clip(CircleShape)
            .background(container),
        contentAlignment = Alignment.Center,
    ) {
        when (badge) {
            BrowseDownloadBadge.Downloaded -> Icon(
                imageVector = Icons.Default.DownloadDone,
                contentDescription = "Downloaded",
                tint = content,
                modifier = Modifier.size(14.dp),
            )
            BrowseDownloadBadge.Queued -> Icon(
                imageVector = Icons.Default.HourglassTop,
                contentDescription = "Queued",
                tint = content,
                modifier = Modifier.size(13.dp),
            )
            BrowseDownloadBadge.Downloading -> CircularProgressIndicator(
                modifier = Modifier.size(14.dp),
                color = content,
                strokeWidth = 2.dp,
                strokeCap = StrokeCap.Round,
            )
            BrowseDownloadBadge.None -> Icon(
                imageVector = Icons.Default.Download,
                contentDescription = null,
                tint = content,
                modifier = Modifier.size(14.dp),
            )
        }
    }
}
