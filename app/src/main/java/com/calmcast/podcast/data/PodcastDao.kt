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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEpisodes(episodes: List<Episode>)

    @Query("UPDATE episodes SET downloadedPath = :downloadPath WHERE id = :episodeId")
    suspend fun updateEpisodeDownloadPath(episodeId: String, downloadPath: String)

    @Transaction
    @Query("SELECT * FROM podcasts WHERE id = :podcastId")
    suspend fun getPodcastWithEpisodes(podcastId: String): PodcastWithEpisodes?

    @Query("SELECT * FROM episodes WHERE id = :episodeId")
    suspend fun getEpisodeById(episodeId: String): Episode?

    @Query("SELECT * FROM episodes WHERE podcastId = :podcastId ORDER BY publishDate DESC LIMIT 1")
    suspend fun getLatestEpisodeForPodcast(podcastId: String): Episode?

    @Query("SELECT * FROM podcasts WHERE id = :podcastId")
    suspend fun getPodcast(podcastId: String): Podcast?

    @Query("DELETE FROM episodes WHERE podcastId = :podcastId")
    suspend fun deleteEpisodesForPodcast(podcastId: String)
}
