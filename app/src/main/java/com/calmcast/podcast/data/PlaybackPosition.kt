package com.calmcast.podcast.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "playback_position")
data class PlaybackPosition(
    @PrimaryKey
    val episodeId: String,
    val position: Long,
    val lastPlayed: Long
)
