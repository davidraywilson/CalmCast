package com.calmcast.podcast

import android.app.Application
import androidx.work.Configuration
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.calmcast.podcast.data.PodcastDatabase
import com.calmcast.podcast.data.SettingsManager
import com.calmcast.podcast.data.SubscriptionManager
import com.calmcast.podcast.data.download.AndroidDownloadManager
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
        get() = Configuration.Builder().build()

    override fun onCreate() {
        super.onCreate()

        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
        val database = PodcastDatabase.getDatabase(this)
        val downloadDao = database.downloadDao()
        val podcastDao = database.podcastDao()
        val settingsManager = SettingsManager(this)
        subscriptionManager = SubscriptionManager(this, podcastDao)
        downloadManager = AndroidDownloadManager(this, okHttpClient, downloadDao, podcastDao, settingsManager)

        CoroutineScope(Dispatchers.IO).launch {
            downloadDao.deleteInvalidDownloads()
        }

        AppLifecycleTracker.initialize(subscriptionManager, settingsManager, podcastDao, null)
    }
}