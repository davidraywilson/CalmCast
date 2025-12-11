package com.calmcast.podcast.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "podcasts")
data class Podcast(
    @PrimaryKey val id: String,
    val title: String,
    val author: String,
    val description: String,
    val imageUrl: String?,
    val feedUrl: String? = null,
    val lastSeenEpisodeId: String? = null,
    val newEpisodeCount: Int = 0
)
