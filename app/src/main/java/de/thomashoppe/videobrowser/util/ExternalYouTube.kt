package de.thomashoppe.videobrowser.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import de.thomashoppe.videobrowser.data.VideoEntry

object ExternalYouTube {
    fun openVideo(context: Context, video: VideoEntry) = open(context, "https://www.youtube.com/watch?v=${video.videoId}")
    fun openChannel(context: Context, channelId: String) = open(context, "https://www.youtube.com/channel/$channelId")
    fun openWatchLater(context: Context) = open(context, "https://www.youtube.com/playlist?list=WL")

    private fun open(context: Context, url: String) {
        val uri = Uri.parse(url)
        val appIntent = Intent(Intent.ACTION_VIEW, uri).setPackage("com.google.android.youtube")
        if (appIntent.resolveActivity(context.packageManager) != null) {
            context.startActivity(appIntent)
        } else {
            context.startActivity(Intent(Intent.ACTION_VIEW, uri))
        }
    }
}
