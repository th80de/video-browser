package de.thomashoppe.videobrowser.data

import androidx.room.Entity
import androidx.room.PrimaryKey

data class VideoEntry(
    val videoId: String,
    val title: String,
    val channelId: String,
    val channelTitle: String,
    val publishedAt: String,
    val thumbnailUrl: String?,
    val liveStatus: String = "none",
    val scheduledStartAt: String? = null,
    val likelyShort: Boolean = false,
)

data class SearchFilters(
    val publishedAfter: String? = null,
    val publishedBefore: String? = null,
    val excludedTerms: String = "",
)

data class SubscriptionEntry(
    val channelId: String,
    val channelTitle: String,
    val thumbnailUrl: String?,
    val subscribedAt: String,
    val latestVideo: VideoEntry?,
    val subscriberCount: Long?,
    val videoCount: Long?,
    val viewCount: Long?,
)

@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey val videoId: String,
    val title: String,
    val channelId: String,
    val channelTitle: String,
    val publishedAt: String,
    val thumbnailUrl: String?,
    val savedAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "search_history")
data class SearchHistoryEntity(
    @PrimaryKey val query: String,
    val searchedAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "feed_cache")
data class FeedCacheEntity(
    @PrimaryKey val videoId: String,
    val title: String,
    val channelId: String,
    val channelTitle: String,
    val publishedAt: String,
    val thumbnailUrl: String?,
    val cachedAt: Long = System.currentTimeMillis(),
)

fun FavoriteEntity.asVideoEntry() = VideoEntry(videoId, title, channelId, channelTitle, publishedAt, thumbnailUrl)
fun FeedCacheEntity.asVideoEntry() = VideoEntry(videoId, title, channelId, channelTitle, publishedAt, thumbnailUrl)
