package com.calmcast.podcast.ui

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.calmcast.podcast.CalmCastApplication
import com.calmcast.podcast.data.PlaybackPositionDao
import com.calmcast.podcast.data.PodcastDao
import com.calmcast.podcast.data.SettingsManager
import com.calmcast.podcast.data.download.DownloadDao

class PodcastViewModelFactory(
    private val application: Application,
    private val podcastDao: PodcastDao,
    private val playbackPositionDao: PlaybackPositionDao,
    private val downloadDao: DownloadDao,
    private val settingsManager: SettingsManager
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PodcastViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            val app = application as CalmCastApplication
            return PodcastViewModel(
                application,
                podcastDao,
                playbackPositionDao,
                downloadDao,
                app.downloadManager,
                settingsManager
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}