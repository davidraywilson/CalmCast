package com.calmcast.podcast.data

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(tableName = "podcasts")
data class Podcast(
    @PrimaryKey val id: String,
    val title: String,
    val author: String,
    val description: String,
    val imageUrl: String?,
    val episodeCount: Int
)

@Entity(tableName = "episodes", foreignKeys = [ForeignKey(entity = Podcast::class,
    parentColumns = ["id"],
    childColumns = ["podcastId"],
    onDelete = ForeignKey.CASCADE)])
data class Episode(
    @PrimaryKey val id: String,
    val podcastId: String,
    val podcastTitle: String,
    val title: String,
    val publishDate: String,
    val duration: String,
    val audioUrl: String,
    val downloadedPath: String? = null
)

data class PodcastWithEpisodes(
    @Embedded val podcast: Podcast,
    @Relation(
        parentColumn = "id",
        entityColumn = "podcastId"
    )
    val episodes: List<Episode>
)
