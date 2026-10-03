package de.thomashoppe.videobrowser

import android.app.Application
import androidx.room.Room
import de.thomashoppe.videobrowser.data.AppDatabase
import de.thomashoppe.videobrowser.data.YouTubeRepository

class VideoBrowserApplication : Application() {
    val database: AppDatabase by lazy {
        Room.databaseBuilder(this, AppDatabase::class.java, "video-browser.db").build()
    }

    val repository: YouTubeRepository by lazy {
        YouTubeRepository(database.videoDao(), this)
    }
}
