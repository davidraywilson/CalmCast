package com.calmcast.podcast.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.calmcast.podcast.data.download.Download
import com.calmcast.podcast.data.download.DownloadDao

@Database(entities = [com.calmcast.podcast.data.Podcast::class, com.calmcast.podcast.data.Episode::class, com.calmcast.podcast.data.PlaybackPosition::class, Download::class], version = 6)
@TypeConverters(com.calmcast.podcast.data.DateConverter::class)
abstract class PodcastDatabase : RoomDatabase() {

    abstract fun podcastDao(): PodcastDao
    abstract fun downloadDao(): DownloadDao
    abstract fun playbackPositionDao(): PlaybackPositionDao

    companion object {
        @Volatile
        private var INSTANCE: PodcastDatabase? = null

        fun getDatabase(context: Context): PodcastDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PodcastDatabase::class.java,
                    "podcast_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
