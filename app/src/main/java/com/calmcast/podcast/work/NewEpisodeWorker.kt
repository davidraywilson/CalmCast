package com.calmcast.podcast.work

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.calmcast.podcast.api.ItunesApiService
import com.calmcast.podcast.data.PodcastDao
import com.calmcast.podcast.data.PodcastRepository
import com.calmcast.podcast.data.SettingsManager
import com.calmcast.podcast.data.SubscriptionManager
import com.calmcast.podcast.data.download.AndroidDownloadManager
import com.calmcast.podcast.data.download.DownloadDao
import kotlinx.coroutines.flow.first

class NewEpisodeWorker(
    appContext: Context,
    workerParams: WorkerParameters,
    private val subscriptionManager: SubscriptionManager,
    private val settingsManager: SettingsManager,
    private val downloadDao: DownloadDao,
    private val podcastDao: PodcastDao,
    private val downloadManager: AndroidDownloadManager
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        if (!settingsManager.isAutoDownloadEnabled()) {
            return Result.success()
        }

        return try {
            val repository = PodcastRepository(
                ItunesApiService(),
                subscriptionManager,
                podcastDao,
                null
            )
            
            val subscriptions = subscriptionManager.getSubscriptions()
            if (subscriptions.isEmpty()) {
                recordRefreshTimestamp()
                return Result.success()
            }

            subscriptions.forEach { podcastWithEpisodes ->
                val podcast = podcastWithEpisodes.podcast
                try {
                    val podcastDetailsResult = repository.getPodcastDetails(podcast.id, forceRefresh = true).first()
                    podcastDetailsResult.onSuccess { podcastDetails ->
                        if (podcastDetails != null) {
                            val latestEpisode = podcastDetails.episodes.maxByOrNull { it.publishDate }
                            
                            if (latestEpisode != null) {
                                val download = downloadDao.getByEpisodeId(latestEpisode.id)
                                when {
                                    download == null -> {
                                        downloadManager.startDownload(latestEpisode)
                                    }
                                    download.status.name == "DELETED" -> {
                                        Log.d("NewEpisodeWorker", "Skipping DELETED episode for ${podcast.title}: ${latestEpisode.title}")
                                    }
                                    else -> {
                                        Log.d("NewEpisodeWorker", "Latest episode for ${podcast.title} already processed (status: ${download.status}).")
                                    }
                                }
                            } else {
                                Log.d("NewEpisodeWorker", "No episodes found for ${podcast.title}")
                            }
                        }
                    }.onFailure {
                        Log.e("NewEpisodeWorker", "Error fetching details for ${podcast.title}", it)
                    }
                } catch (e: Exception) {
                    Log.e("NewEpisodeWorker", "Error fetching details for ${podcast.title}", e)
                }
            }

            recordRefreshTimestamp()
            Result.success()
        } catch (e: Exception) {
            Log.e("NewEpisodeWorker", "Error checking for new episodes", e)
            Result.failure()
        }
    }

    /**
     * Record the timestamp of the successful episode refresh
     */
    private fun recordRefreshTimestamp() {
        settingsManager.setLastEpisodeRefreshTime(System.currentTimeMillis())
    }
}