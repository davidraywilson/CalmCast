package com.calmcast.podcast

import android.app.Application
import androidx.work.Configuration
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.calmcast.podcast.api.TaddyApiService
import com.calmcast.podcast.data.PodcastDatabase
import com.calmcast.podcast.data.SettingsManager
import com.calmcast.podcast.data.SubscriptionManager
import com.calmcast.podcast.data.download.AndroidDownloadManager
import com.calmcast.podcast.data.download.DownloadDatabase
import com.calmcast.podcast.work.NewEpisodeWorker
import com.calmcast.podcast.work.NewEpisodeWorkerFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

class CalmCastApplication : Application(), Configuration.Provider {

    // Centralized dependencies
    lateinit var subscriptionManager: SubscriptionManager
    lateinit var downloadManager: AndroidDownloadManager

    override val workManagerConfiguration: Configuration
        get() {
            val taddyApiService = TaddyApiService()
            val settingsManager = SettingsManager(this)
            val downloadDao = DownloadDatabase.getDatabase(this).downloadDao()
            val podcastDao = PodcastDatabase.getDatabase(this).podcastDao()
            val workerFactory = NewEpisodeWorkerFactory(subscriptionManager, taddyApiService, settingsManager, downloadDao, podcastDao, downloadManager)

            return Configuration.Builder()
                .setWorkerFactory(workerFactory)
                .build()
        }

    override fun onCreate() {
        super.onCreate()

        // Initialize centralized dependencies
        val okHttpClient = OkHttpClient()
        val downloadDao = DownloadDatabase.getDatabase(this).downloadDao()
        val podcastDao = PodcastDatabase.getDatabase(this).podcastDao()
        val settingsManager = SettingsManager(this)
        val taddyApiService = TaddyApiService()
        subscriptionManager = SubscriptionManager(this, podcastDao)
        downloadManager = AndroidDownloadManager(this, okHttpClient, downloadDao, podcastDao)

        // Clean up any invalid download records from previous versions
        CoroutineScope(Dispatchers.IO).launch {
            downloadDao.deleteInvalidDownloads()
        }

        AppLifecycleTracker.initialize(subscriptionManager, downloadManager, settingsManager, taddyApiService, podcastDao)
        setupNewEpisodeWorker()
    }

    private fun setupNewEpisodeWorker() {
        val newEpisodeWorkRequest =
            PeriodicWorkRequestBuilder<NewEpisodeWorker>(1, TimeUnit.DAYS)
                .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "new-episode-worker",
            ExistingPeriodicWorkPolicy.KEEP,
            newEpisodeWorkRequest
        )
    }
}