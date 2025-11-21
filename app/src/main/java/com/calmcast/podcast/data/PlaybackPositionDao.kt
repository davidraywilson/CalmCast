package com.calmcast.podcast.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface PlaybackPositionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(playbackPosition: PlaybackPosition)

    @Query("SELECT * FROM playback_position WHERE episodeId = :episodeId")
    suspend fun getPlaybackPosition(episodeId: String): PlaybackPosition?

    @Query("SELECT * FROM playback_position WHERE episodeId IN (:episodeIds)")
    suspend fun getPlaybackPositions(episodeIds: List<String>): List<PlaybackPosition>
    
    @Query("SELECT * FROM playback_position ORDER BY lastPlayed DESC LIMIT 1")
    suspend fun getLastPlayed(): PlaybackPosition?
}
