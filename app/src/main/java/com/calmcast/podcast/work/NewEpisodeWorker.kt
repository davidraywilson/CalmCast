package com.calmcast.podcast.work

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.calmcast.podcast.api.TaddyApiService
import com.calmcast.podcast.data.PodcastDao
import com.calmcast.podcast.data.SettingsManager
import com.calmcast.podcast.data.SubscriptionManager
import com.calmcast.podcast.data.download.AndroidDownloadManager
import com.calmcast.podcast.data.download.DownloadDao
import com.calmcast.podcast.data.toEpisode

class NewEpisodeWorker(
    appContext: Context,
    workerParams: WorkerParameters,
    private val subscriptionManager: SubscriptionManager,
    private val taddyApiService: TaddyApiService,
    private val settingsManager: SettingsManager,
    private val downloadDao: DownloadDao,
    private val podcastDao: PodcastDao,
    private val downloadManager: AndroidDownloadManager
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        Log.d("NewEpisodeWorker", "Starting daily episode refresh.")

        if (!settingsManager.isAutoDownloadEnabled()) {
            Log.d("NewEpisodeWorker", "Auto-download is disabled. Skipping work.")
            return Result.success()
        }

        return try {
            val subscriptions = subscriptionManager.getSubscriptions()
            if (subscriptions.isEmpty()) {
                Log.d("NewEpisodeWorker", "No subscriptions to check.")
                recordRefreshTimestamp()
                return Result.success()
            }

            subscriptions.forEach { podcastWithEpisodes ->
                val podcast = podcastWithEpisodes.podcast
                val result = taddyApiService.getPodcastDetails(podcast.id)
                result.onSuccess { podcastDetailsResponse ->
                    val latestEpisode = podcastDetailsResponse.podcast?.episodes?.maxByOrNull {
                        it.publishDate
                    }

                    if (latestEpisode != null) {
                        val download = downloadDao.getByEpisodeId(latestEpisode.id)
                        when {
                            download == null -> {
                                // No record - never attempted - auto-download
                                Log.d("NewEpisodeWorker", "Downloading new episode for ${podcast.title}: ${latestEpisode.title}")
                                downloadManager.startDownload(latestEpisode.toEpisode(podcast.id, podcast.title))
                            }
                            download.status.name == "DELETED" -> {
                                // User deleted it - don't auto-download again
                                Log.d("NewEpisodeWorker", "Skipping DELETED episode for ${podcast.title}: ${latestEpisode.title}")
                            }
                            else -> {
                                // Other status (DOWNLOADING, DOWNLOADED, FAILED, PAUSED, CANCELED)
                                Log.d("NewEpisodeWorker", "Latest episode for ${podcast.title} already processed (status: ${download.status}).")
                            }
                        }
                    } else {
                        Log.d("NewEpisodeWorker", "No episodes found for ${podcast.title}")
                    }
                }.onFailure {
                    Log.e("NewEpisodeWorker", "Error fetching details for ${podcast.title}", it)
                }
            }

            // Record the successful refresh timestamp
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
        Log.d("NewEpisodeWorker", "Recorded episode refresh timestamp: ${System.currentTimeMillis()}")
    }
}