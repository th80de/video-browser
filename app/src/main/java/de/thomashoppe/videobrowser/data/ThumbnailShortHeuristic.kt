package de.thomashoppe.videobrowser.data

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Detects the common Shorts cover treatment: a sharp 9:16 image in the centre with
 * deliberately softened or extended side panels. It is deliberately conservative;
 * a false positive would hide a regular video.
 */
internal fun hasPortraitCenterWithSoftSides(bitmap: Bitmap): Boolean {
    if (bitmap.width < 96 || bitmap.height < 96 || bitmap.width.toFloat() / bitmap.height < 1.15f) return false

    val sampled = Bitmap.createScaledBitmap(bitmap, 120, 90, true)
    val centerWidth = (sampled.height * (9f / 16f)).roundToInt().coerceAtLeast(28)
    val centerStart = (sampled.width - centerWidth) / 2
    val centerEnd = centerStart + centerWidth
    if (centerStart < 12 || centerEnd > sampled.width - 12) return false

    val centerDetail = detailScore(sampled, centerStart, centerEnd)
    val leftDetail = detailScore(sampled, 0, centerStart)
    val rightDetail = detailScore(sampled, centerEnd, sampled.width)
    val sideDetail = (leftDetail + rightDetail) / 2f
    val boundaries = (boundaryScore(sampled, centerStart) + boundaryScore(sampled, centerEnd)) / 2f

    // A portrait insert has a pronounced sharpness jump at both vertical borders.
    return centerDetail > sideDetail * 1.65f && boundaries > max(10f, sideDetail * 1.25f)
}

private fun detailScore(bitmap: Bitmap, startX: Int, endX: Int): Float {
    var sum = 0L
    var count = 0
    for (y in 1 until bitmap.height - 1 step 2) {
        for (x in (startX + 1) until (endX - 1) step 2) {
            val pixel = luma(bitmap.getPixel(x, y))
            sum += abs(pixel - luma(bitmap.getPixel(x + 1, y)))
            sum += abs(pixel - luma(bitmap.getPixel(x, y + 1)))
            count += 2
        }
    }
    return if (count == 0) 0f else sum.toFloat() / count
}

private fun boundaryScore(bitmap: Bitmap, boundaryX: Int): Float {
    var sum = 0L
    var count = 0
    for (y in 0 until bitmap.height step 2) {
        sum += abs(luma(bitmap.getPixel(boundaryX - 1, y)) - luma(bitmap.getPixel(boundaryX, y)))
        count++
    }
    return if (count == 0) 0f else sum.toFloat() / count
}

private fun luma(color: Int): Int =
    ((color shr 16 and 0xff) * 299 + (color shr 8 and 0xff) * 587 + (color and 0xff) * 114) / 1000

internal class ThumbnailShortClassifier {
    private val client = OkHttpClient.Builder()
        .connectTimeout(3, TimeUnit.SECONDS)
        .readTimeout(3, TimeUnit.SECONDS)
        .build()
    private val cache = ConcurrentHashMap<String, Boolean>()

    fun isLikelyShort(thumbnailUrl: String): Boolean = cache.getOrPut(thumbnailUrl) {
        runCatching {
            val request = Request.Builder().url(thumbnailUrl).build()
            client.newCall(request).execute().use { response ->
                response.takeIf { it.isSuccessful }?.body?.byteStream()?.use(BitmapFactory::decodeStream)
            }?.let(::hasPortraitCenterWithSoftSides) ?: false
        }.getOrDefault(false)
    }
}
