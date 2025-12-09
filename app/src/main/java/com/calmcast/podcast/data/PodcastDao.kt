package com.calmcast.podcast.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface PodcastDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPodcast(podcast: Podcast)

    @Query("SELECT * FROM podcasts WHERE id = :podcastId")
    suspend fun getPodcast(podcastId: String): Podcast?

    @Query("UPDATE podcasts SET lastSeenEpisodeId = :episodeId WHERE id = :podcastId")
    suspend fun updateLastSeenEpisodeId(podcastId: String, episodeId: String)

    @Query("UPDATE podcasts SET lastViewedAt = :timestamp WHERE id = :podcastId")
    suspend fun updateLastViewedAt(podcastId: String, timestamp: Long)

    @Query("SELECT * FROM podcasts ORDER BY title ASC")
    suspend fun getAllPodcasts(): List<Podcast>
}
