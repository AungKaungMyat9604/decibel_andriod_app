package com.decibel.youtube

import org.schabi.newpipe.extractor.InfoItem
import org.schabi.newpipe.extractor.Page
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.kiosk.KioskInfo
import org.schabi.newpipe.extractor.search.SearchInfo
import org.schabi.newpipe.extractor.stream.StreamInfoItem

data class CatalogVideo(
    val id: String,
    val title: String,
    val uploader: String,
    val durationSeconds: Long,
    val thumbnailUrl: String?,
    val webpageUrl: String,
    val viewCount: Long,
)

data class CatalogPage(
    val items: List<CatalogVideo>,
    val nextPage: Page?,
    val sourceUrl: String,
    val isSearch: Boolean,
    val searchQuery: String = "",
)

object YoutubeCatalog {
    private const val TRENDING_MUSIC_ID = "trending_music"

    fun loadTrending(): CatalogPage {
        ExtractorBootstrap.init()
        val service = ServiceList.YouTube
        val kioskList = service.kioskList
        val extractor = runCatching {
            kioskList.getExtractorById(TRENDING_MUSIC_ID, null)
        }.getOrElse {
            kioskList.defaultKioskExtractor
        }
        extractor.fetchPage()
        val info = KioskInfo.getInfo(extractor)
        return CatalogPage(
            items = info.relatedItems.mapNotNull { it.toCatalogVideo() },
            nextPage = info.nextPage,
            sourceUrl = info.url,
            isSearch = false,
        )
    }

    fun loadMoreKiosk(sourceUrl: String, page: Page): CatalogPage {
        ExtractorBootstrap.init()
        val more = KioskInfo.getMoreItems(ServiceList.YouTube, sourceUrl, page)
        return CatalogPage(
            items = more.items.mapNotNull { it.toCatalogVideo() },
            nextPage = more.nextPage,
            sourceUrl = sourceUrl,
            isSearch = false,
        )
    }

    fun search(query: String): CatalogPage {
        ExtractorBootstrap.init()
        val service = ServiceList.YouTube
        val handler = service.searchQHFactory.fromQuery(query.trim(), listOf("videos"), "")
        val info = SearchInfo.getInfo(service, handler)
        return CatalogPage(
            items = info.relatedItems.mapNotNull { it.toCatalogVideo() },
            nextPage = info.nextPage,
            sourceUrl = info.url,
            isSearch = true,
            searchQuery = query.trim(),
        )
    }

    fun loadMoreSearch(query: String, page: Page): CatalogPage {
        ExtractorBootstrap.init()
        val service = ServiceList.YouTube
        val handler = service.searchQHFactory.fromQuery(query.trim(), listOf("videos"), "")
        val more = SearchInfo.getMoreItems(service, handler, page)
        return CatalogPage(
            items = more.items.mapNotNull { it.toCatalogVideo() },
            nextPage = more.nextPage,
            sourceUrl = "",
            isSearch = true,
            searchQuery = query.trim(),
        )
    }

    private fun InfoItem.toCatalogVideo(): CatalogVideo? {
        val stream = this as? StreamInfoItem ?: return null
        val url = stream.url ?: return null
        val id = Regex("""(?:v=|/shorts/|youtu\.be/)([A-Za-z0-9_-]{6,})""")
            .find(url)?.groupValues?.get(1)
            ?: url.hashCode().toString()
        return CatalogVideo(
            id = id,
            title = stream.name.orEmpty().ifBlank { "Untitled" },
            uploader = stream.uploaderName.orEmpty().ifBlank { "Unknown" },
            durationSeconds = stream.duration.coerceAtLeast(0),
            thumbnailUrl = stream.thumbnails.maxByOrNull { it.height }?.url,
            webpageUrl = url,
            viewCount = stream.viewCount,
        )
    }
}
