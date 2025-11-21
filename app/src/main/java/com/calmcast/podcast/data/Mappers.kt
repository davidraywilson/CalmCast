package com.calmcast.podcast.data

import com.calmcast.podcast.api.TaddyEpisodeData
import com.calmcast.podcast.api.TaddyPodcastData

fun TaddyPodcastData.toPodcast(): Podcast {
    return Podcast(
        id = this.id,
        title = this.title,
        author = this.author,
        description = this.description,
        imageUrl = this.imageUrl,
        episodeCount = this.episodeCount
    )
}

fun TaddyEpisodeData.toEpisode(podcastId: String, podcastTitle: String): Episode {
    return Episode(
        id = this.id,
        podcastId = podcastId,
        podcastTitle = podcastTitle,
        title = this.title,
        publishDate = this.publishDate,
        duration = this.duration,
        audioUrl = this.audioUrl
    )
}