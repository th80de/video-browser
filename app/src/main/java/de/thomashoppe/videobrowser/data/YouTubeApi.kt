package de.thomashoppe.videobrowser.data

import com.squareup.moshi.Json
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

interface YouTubeApi {
    @GET("youtube/v3/search")
    suspend fun search(
        @Query("part") part: String = "snippet",
        @Query("type") type: String = "video",
        @Query("maxResults") maxResults: Int = 25,
        @Query("q") query: String,
        @Query("publishedAfter") publishedAfter: String? = null,
        @Query("publishedBefore") publishedBefore: String? = null,
    ): SearchResponse

    @GET("youtube/v3/subscriptions")
    suspend fun subscriptions(
        @Query("part") part: String = "snippet",
        @Query("mine") mine: Boolean = true,
        @Query("maxResults") maxResults: Int = 50,
        @Query("pageToken") pageToken: String? = null,
        @Header("Authorization") authorization: String,
    ): SubscriptionResponse

    @GET("youtube/v3/channels")
    suspend fun channels(
        @Query("part") part: String = "contentDetails,statistics",
        @Query("id") channelIds: String,
    ): ChannelResponse

    @GET("youtube/v3/playlistItems")
    suspend fun playlistItems(
        @Query("part") part: String = "snippet,contentDetails",
        @Query("playlistId") playlistId: String,
        @Query("maxResults") maxResults: Int = 1,
    ): PlaylistResponse

    @GET("youtube/v3/videos")
    suspend fun videos(
        @Query("part") part: String = "snippet,liveStreamingDetails",
        @Query("id") videoIds: String,
    ): VideoResponse
}

data class SearchResponse(val items: List<SearchItem> = emptyList())
data class SearchItem(val id: VideoId?, val snippet: Snippet)
data class VideoId(@Json(name = "videoId") val videoId: String?)

data class SubscriptionResponse(
    val items: List<SubscriptionItem> = emptyList(),
    val nextPageToken: String? = null,
)
data class SubscriptionItem(val snippet: SubscriptionSnippet)
data class SubscriptionSnippet(
    val resourceId: ChannelResourceId,
    val title: String,
    val publishedAt: String,
    val thumbnails: Thumbnails? = null,
)
data class ChannelResourceId(@Json(name = "channelId") val channelId: String)

data class ChannelResponse(val items: List<ChannelItem> = emptyList())
data class ChannelItem(
    val id: String,
    val contentDetails: ChannelContentDetails,
    val statistics: ChannelStatistics? = null,
)
data class ChannelContentDetails(val relatedPlaylists: RelatedPlaylists)
data class RelatedPlaylists(val uploads: String?)
data class ChannelStatistics(
    val viewCount: String? = null,
    val subscriberCount: String? = null,
    val videoCount: String? = null,
)

data class PlaylistResponse(val items: List<PlaylistItem> = emptyList())
data class PlaylistItem(val snippet: Snippet, val contentDetails: PlaylistContentDetails)
data class PlaylistContentDetails(
    @Json(name = "videoId") val videoId: String?,
    val videoPublishedAt: String? = null,
)

data class VideoResponse(val items: List<VideoItem> = emptyList())
data class VideoItem(
    val id: String,
    val snippet: Snippet,
    val liveStreamingDetails: LiveStreamingDetails? = null,
)
data class LiveStreamingDetails(val scheduledStartTime: String? = null)

data class Snippet(
    val title: String,
    val channelId: String,
    val channelTitle: String,
    val publishedAt: String,
    val thumbnails: Thumbnails? = null,
    val liveBroadcastContent: String? = null,
)
data class Thumbnails(
    val medium: Thumbnail? = null,
    val high: Thumbnail? = null,
    val default: Thumbnail? = null,
)
data class Thumbnail(
    val url: String,
    val width: Int? = null,
    val height: Int? = null,
)

fun Snippet.asVideo(videoId: String, publishedAtOverride: String? = null, scheduledStartAt: String? = null): VideoEntry {
    val thumbnail = thumbnails?.high ?: thumbnails?.medium ?: thumbnails?.default
    val titleSignalsShort = title.contains("#shorts", ignoreCase = true) || title.contains("#short", ignoreCase = true)
    val portraitOrSquareThumbnail = thumbnail?.let { image ->
        image.width != null && image.height != null && image.height >= image.width
    } ?: false
    return VideoEntry(
        videoId = videoId,
        title = title,
        channelId = channelId,
        channelTitle = channelTitle,
        publishedAt = publishedAtOverride ?: publishedAt,
        thumbnailUrl = thumbnail?.url,
        liveStatus = liveBroadcastContent ?: "none",
        scheduledStartAt = scheduledStartAt,
        likelyShort = titleSignalsShort || portraitOrSquareThumbnail,
    )
}
