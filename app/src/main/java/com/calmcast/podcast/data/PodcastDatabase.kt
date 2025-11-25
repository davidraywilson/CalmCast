package com.calmcast.podcast.data

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.calmcast.podcast.data.download.Download
import com.calmcast.podcast.data.download.DownloadDao

@Database(entities = [com.calmcast.podcast.data.Podcast::class, com.calmcast.podcast.data.Episode::class, com.calmcast.podcast.data.PlaybackPosition::class, Download::class], version = 7)
@TypeConverters(com.calmcast.podcast.data.DateConverter::class)
abstract class PodcastDatabase : RoomDatabase() {

    abstract fun podcastDao(): PodcastDao
    abstract fun playbackPositionDao(): PlaybackPositionDao
    abstract fun downloadDao(): DownloadDao

    companion object {
        @Volatile
        private var INSTANCE: PodcastDatabase? = null

        fun getDatabase(context: Context): PodcastDatabase {
            return INSTANCE ?: synchronized(this) {
                try {
                    val instance = Room.databaseBuilder(
                        context.applicationContext,
                        PodcastDatabase::class.java,
                        "podcast_database"
                    )
                        .fallbackToDestructiveMigration()
                        .build()
                    INSTANCE = instance
                    instance
                } catch (e: Exception) {
                    Log.e("PodcastDatabase", "Error opening database, clearing and retrying", e)
                    // Clear corrupted database files
                    try {
                        context.applicationContext.deleteDatabase("podcast_database")
                        context.applicationContext.deleteDatabase("podcast_database-shm")
                        context.applicationContext.deleteDatabase("podcast_database-wal")
                    } catch (deleteError: Exception) {
                        Log.e("PodcastDatabase", "Error deleting corrupted database", deleteError)
                    }
                    // Retry building the database
                    val retryInstance = Room.databaseBuilder(
                        context.applicationContext,
                        PodcastDatabase::class.java,
                        "podcast_database"
                    )
                        .fallbackToDestructiveMigration()
                        .build()
                    INSTANCE = retryInstance
                    retryInstance
                }
            }
        }
    }
}
