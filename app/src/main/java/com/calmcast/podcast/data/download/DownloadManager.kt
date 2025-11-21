package com.calmcast.podcast.data.download

import com.calmcast.podcast.data.Episode
import kotlinx.coroutines.flow.Flow

interface DownloadManager {
    val downloads: Flow<List<Download>>
    fun startDownload(episode: Episode): Long
    fun cancelDownload(id: Long)
    fun cancelDownload(episodeId: String)
    fun pauseDownload(episodeId: String)
    fun resumeDownload(episodeId: String)
    fun getDownload(id: Long): Download?
    fun deleteDownload(episode: Episode)
}
