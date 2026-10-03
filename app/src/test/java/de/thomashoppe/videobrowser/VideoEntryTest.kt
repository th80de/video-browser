package de.thomashoppe.videobrowser

import de.thomashoppe.videobrowser.data.FavoriteEntity
import de.thomashoppe.videobrowser.data.asVideoEntry
import de.thomashoppe.videobrowser.data.parseExcludedTerms
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

    @Test
    fun exclusionTerms_supportWordsAndQuotedPhrases() {
        assertEquals(
            listOf("wildberry", "remix", "live session"),
            parseExcludedTerms("wildberry remix \"live session\""),
        )
    }
}
