package de.thomashoppe.videobrowser.data

import de.thomashoppe.videobrowser.BuildConfig
import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.time.Instant

class MissingApiKeyException : IllegalStateException(
    "YouTube API-Schlüssel fehlt. Lege YOUTUBE_API_KEY in secrets.properties an."
)

class YouTubeRepository(private val dao: VideoDao, appContext: Context) {
    private val api = Network.api(appContext)

    val favorites: Flow<List<VideoEntry>> = dao.observeFavorites().map { list -> list.map { it.asVideoEntry() } }
    val history: Flow<List<String>> = dao.observeHistory().map { list -> list.map { it.query } }

    suspend fun search(query: String, filters: SearchFilters = SearchFilters()): List<VideoEntry> = withContext(Dispatchers.IO) {
        val normalized = query.trim()
        if (normalized.isBlank()) return@withContext emptyList()
        requireApiKey()
        dao.saveHistory(SearchHistoryEntity(normalized))
        val excludedTerms = filters.excludedTerms
            .split(Regex("[,\\n]+"))
            .map { it.trim().removePrefix("-") }
            .filter { it.isNotBlank() }
        // The API receives the documented negative terms, but its relevance engine does not
        // guarantee that these are applied consistently. Apply the same rule to displayed cards.
        val exclusions = excludedTerms.flatMap { term -> term.split(Regex("\\s+")) }
            .joinToString(" ") { term -> "-$term" }
        val apiQuery = listOf(normalized, exclusions).filter { it.isNotBlank() }.joinToString(" ")
        api.search(
            query = apiQuery,
            publishedAfter = filters.publishedAfter,
            publishedBefore = filters.publishedBefore,
        )
            .items.mapNotNull { item -> item.id?.videoId?.let { item.snippet.asVideo(it) } }
            .filterNot { it.likelyShort }
            .filterNot { video ->
                excludedTerms.any { term ->
                    video.title.contains(term, ignoreCase = true) ||
                        video.channelTitle.contains(term, ignoreCase = true)
                }
            }
    }

    /** Loads subscription dates, channel statistics, and one newest public upload per channel. */
    suspend fun subscriptionOverview(accessToken: String): List<SubscriptionEntry> = withContext(Dispatchers.IO) {
        requireApiKey()
        val subscriptions = loadAllSubscriptions(accessToken)
        if (subscriptions.isEmpty()) {
            dao.deleteCachedFeed()
            return@withContext emptyList()
        }

        val channelsById = subscriptions.map { it.channelId }.distinct().chunked(50).flatMap { ids ->
            api.channels(channelIds = ids.joinToString(",")).items
        }.associateBy { it.id }

        val latestVideoByChannel = coroutineScope {
            channelsById.values.mapNotNull { channel ->
                channel.contentDetails.relatedPlaylists.uploads?.let { uploads ->
                    async { channel.id to newestPublicUpload(uploads) }
                }
            }.awaitAll().toMap()
        }
        val videoDetailsById = latestVideoByChannel.values.filterNotNull().map { it.videoId }.distinct().chunked(50)
            .flatMap { ids -> api.videos(videoIds = ids.joinToString(",")).items }
            .associateBy { it.id }

        subscriptions.map { subscription ->
            val channel = channelsById[subscription.channelId]
            SubscriptionEntry(
                channelId = subscription.channelId,
                channelTitle = subscription.title,
                thumbnailUrl = subscription.thumbnailUrl,
                subscribedAt = subscription.subscribedAt,
                latestVideo = latestVideoByChannel[subscription.channelId]?.let { video ->
                    videoDetailsById[video.videoId]?.let { details ->
                        val detailedVideo = details.snippet.asVideo(
                            details.id,
                            scheduledStartAt = details.liveStreamingDetails?.scheduledStartTime,
                        )
                        detailedVideo.copy(likelyShort = video.likelyShort || detailedVideo.likelyShort)
                    } ?: video
                }?.takeUnless { it.likelyShort },
                subscriberCount = channel?.statistics?.subscriberCount?.toLongOrNull(),
                videoCount = channel?.statistics?.videoCount?.toLongOrNull(),
                viewCount = channel?.statistics?.viewCount?.toLongOrNull(),
            )
        }
    }

    suspend fun newestSubscriptionVideos(accessToken: String): List<VideoEntry> {
        val entries = newestVideos(subscriptionOverview(accessToken))
        cacheFeed(entries)
        return entries
    }

    fun newestVideos(subscriptions: List<SubscriptionEntry>): List<VideoEntry> =
        subscriptions.mapNotNull { it.latestVideo }.filterNot { it.likelyShort }.distinctBy { it.videoId }
            .sortedByDescending { entry -> runCatching { Instant.parse(entry.publishedAt) }.getOrNull() }

    suspend fun cacheFeed(entries: List<VideoEntry>) {
        dao.replaceCachedFeed(entries.map { item ->
            FeedCacheEntity(item.videoId, item.title, item.channelId, item.channelTitle, item.publishedAt, item.thumbnailUrl)
        })
    }

    /** A subscription can outlive a deleted or unavailable upload playlist. */
    private suspend fun newestPublicUpload(playlistId: String): VideoEntry? = try {
        api.playlistItems(playlistId = playlistId)
            .items.firstOrNull()?.let { item ->
                item.contentDetails.videoId?.let { id ->
                    item.snippet.asVideo(id, publishedAtOverride = item.contentDetails.videoPublishedAt)
                }
            }
    } catch (error: HttpException) {
        if (error.code() == 404) null else throw error
    }

    suspend fun cachedFeed(): List<VideoEntry> = dao.cachedFeed().map { it.asVideoEntry() }

    private suspend fun loadAllSubscriptions(accessToken: String): List<SubscriptionMetadata> {
        val subscriptions = mutableListOf<SubscriptionMetadata>()
        var pageToken: String? = null
        do {
            val page = api.subscriptions(
                pageToken = pageToken,
                authorization = "Bearer $accessToken",
            )
            page.items.forEach { item ->
                val snippet = item.snippet
                val channelId = snippet.resourceId.channelId
                subscriptions += SubscriptionMetadata(
                    channelId = channelId,
                    title = snippet.title,
                    subscribedAt = snippet.publishedAt,
                    thumbnailUrl = snippet.thumbnails?.high?.url
                        ?: snippet.thumbnails?.medium?.url
                        ?: snippet.thumbnails?.default?.url,
                )
            }
            pageToken = page.nextPageToken
        } while (pageToken != null)
        val seen = mutableSetOf<String>()
        return subscriptions.filter { seen.add(it.channelId) }
    }

    suspend fun isFavorite(videoId: String): Boolean = dao.favorite(videoId) != null

    suspend fun toggleFavorite(video: VideoEntry): Boolean {
        val existing = dao.favorite(video.videoId)
        if (existing == null) {
            dao.saveFavorite(
                FavoriteEntity(video.videoId, video.title, video.channelId, video.channelTitle, video.publishedAt, video.thumbnailUrl)
            )
            return true
        }
        dao.deleteFavorite(existing)
        return false
    }

    suspend fun removeFavorite(video: VideoEntry) {
        val existing = dao.favorite(video.videoId)
        if (existing != null) dao.deleteFavorite(existing)
    }

    suspend fun clearFavorites() = dao.deleteAllFavorites()
    suspend fun removeHistory(query: String) = dao.deleteHistory(query)
    suspend fun clearHistory() = dao.deleteAllHistory()

    private fun requireApiKey() {
        if (BuildConfig.YOUTUBE_API_KEY.isBlank()) throw MissingApiKeyException()
    }

    private data class SubscriptionMetadata(
        val channelId: String,
        val title: String,
        val subscribedAt: String,
        val thumbnailUrl: String?,
    )
}
