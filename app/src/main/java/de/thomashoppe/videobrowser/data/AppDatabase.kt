package de.thomashoppe.videobrowser.data

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface VideoDao {
    @Query("SELECT * FROM favorites ORDER BY savedAt DESC")
    fun observeFavorites(): Flow<List<FavoriteEntity>>

    @Query("SELECT * FROM favorites WHERE videoId = :videoId LIMIT 1")
    suspend fun favorite(videoId: String): FavoriteEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveFavorite(item: FavoriteEntity)

    @Delete
    suspend fun deleteFavorite(item: FavoriteEntity)

    @Query("DELETE FROM favorites")
    suspend fun deleteAllFavorites()

    @Query("SELECT * FROM search_history ORDER BY searchedAt DESC LIMIT 30")
    fun observeHistory(): Flow<List<SearchHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveHistory(item: SearchHistoryEntity)

    @Query("DELETE FROM search_history WHERE query = :query")
    suspend fun deleteHistory(query: String)

    @Query("DELETE FROM search_history")
    suspend fun deleteAllHistory()

    @Query("SELECT * FROM feed_cache ORDER BY publishedAt DESC")
    suspend fun cachedFeed(): List<FeedCacheEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveCachedFeed(items: List<FeedCacheEntity>)

    @Query("DELETE FROM feed_cache")
    suspend fun deleteCachedFeed()

    @Transaction
    suspend fun replaceCachedFeed(items: List<FeedCacheEntity>) {
        deleteCachedFeed()
        saveCachedFeed(items)
    }
}

@Database(entities = [FavoriteEntity::class, SearchHistoryEntity::class, FeedCacheEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun videoDao(): VideoDao
}
