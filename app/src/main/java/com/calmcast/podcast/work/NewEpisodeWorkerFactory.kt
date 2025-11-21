package com.calmcast.podcast.work

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import com.calmcast.podcast.api.TaddyApiService
import com.calmcast.podcast.data.PodcastDao
import com.calmcast.podcast.data.SettingsManager
import com.calmcast.podcast.data.SubscriptionManager
import com.calmcast.podcast.data.download.AndroidDownloadManager
import com.calmcast.podcast.data.download.DownloadDao

class NewEpisodeWorkerFactory(
    private val subscriptionManager: SubscriptionManager,
    private val taddyApiService: TaddyApiService,
    private val settingsManager: SettingsManager,
    private val downloadDao: DownloadDao,
    private val podcastDao: PodcastDao,
    private val downloadManager: AndroidDownloadManager
) : WorkerFactory() {

    override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters
    ): ListenableWorker? {
        return when (workerClassName) {
            NewEpisodeWorker::class.java.name -> {
                NewEpisodeWorker(
                    appContext,
                    workerParameters,
                    subscriptionManager,
                    taddyApiService,
                    settingsManager,
                    downloadDao,
                    podcastDao,
                    downloadManager
                )
            }
            else -> null
        }
    }
}