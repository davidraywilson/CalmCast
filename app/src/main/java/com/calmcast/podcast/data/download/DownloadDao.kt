package com.calmcast.podcast.data.download

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadDao {
    @Query("SELECT * FROM downloads ORDER BY episode_id ASC")
    fun getAll(): Flow<List<Download>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(download: Download)

    @Query("DELETE FROM downloads WHERE id = :id")
    suspend fun delete(id: String)

    @Query("SELECT * FROM downloads WHERE id = :id")
    suspend fun getDownload(id: String): Download?

    @Query("SELECT * FROM downloads WHERE episode_id = :episodeId")
    suspend fun getByEpisodeId(episodeId: String): Download?

    @Query("DELETE FROM downloads WHERE episode_id = :episodeId")
    suspend fun deleteByEpisodeId(episodeId: String)

    @Query("DELETE FROM downloads WHERE episode_id IS NULL OR episode_id = '' OR episode_title IS NULL OR episode_title = '' OR episode_audioUrl IS NULL OR episode_audioUrl = ''")
    suspend fun deleteInvalidDownloads()
}
