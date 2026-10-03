package de.thomashoppe.videobrowser

import de.thomashoppe.videobrowser.data.FavoriteEntity
import de.thomashoppe.videobrowser.data.asVideoEntry
import org.junit.Assert.assertEquals
import org.junit.Test

class VideoEntryTest {
    @Test
    fun favorite_roundTripsToVideoEntry() {
        val favorite = FavoriteEntity("abc", "Titel", "channel", "Kanal", "2026-10-03T10:00:00Z", null)
        val entry = favorite.asVideoEntry()
        assertEquals("abc", entry.videoId)
        assertEquals("Kanal", entry.channelTitle)
    }
}

