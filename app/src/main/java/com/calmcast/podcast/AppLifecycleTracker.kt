package com.calmcast.podcast

import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ProcessLifecycleOwner
import com.calmcast.podcast.api.ItunesApiService
import com.calmcast.podcast.data.Episode
import com.calmcast.podcast.data.PodcastDao
import com.calmcast.podcast.data.PodcastRepository
import com.calmcast.podcast.data.SettingsManager
import com.calmcast.podcast.data.SubscriptionManager
import com.calmcast.podcast.data.download.AndroidDownloadManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

object AppLifecycleTracker {
    private const val TAG = "AppLifecycleTracker"
    private var settingsManager: SettingsManager? = null
    private var podcastDao: PodcastDao? = null
    private var subscriptionManager: SubscriptionManager? = null

    fun initialize(
        subscriptionMgr: SubscriptionManager,
        downloadManager: AndroidDownloadManager,
        settings: SettingsManager,
        dao: PodcastDao
    ) {
        settingsManager = settings
        podcastDao = dao
        subscriptionManager = subscriptionMgr
        ProcessLifecycleOwner.get().lifecycle.addObserver(
            LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_START -> {
                        Log.d(TAG, "App entered foreground. Checking if daily episode refresh is needed.")
                        CoroutineScope(Dispatchers.IO).launch {
                            try {
                                val settings = settingsManager ?: return@launch
                                val subMgr = subscriptionManager ?: return@launch
                                
                                // Check if a daily refresh is needed
                                if (settings.isDailyRefreshNeeded()) {
                                    Log.d(TAG, "Daily refresh needed. Triggering episode refresh for all subscribed podcasts.")
                                    refreshAllPodcastEpisodes(subMgr, settings)
                                } else {
                                    Log.d(TAG, "Daily refresh not needed yet. Last refresh was less than 24 hours ago.")
                                }
                                
                            } catch (e: Exception) {
                                Log.e(TAG, "Error checking for new episodes to download", e)
                            }
                        }
                    }
                    else -> {}
                }
            }
        )
    }

    /**
     * Refresh episodes for all subscribed podcasts by fetching from API and updating database.
     * This ensures we always have the latest episode data.
     */
    private suspend fun refreshAllPodcastEpisodes(
        subscriptionManager: SubscriptionManager,
        settingsManager: SettingsManager
    ) {
        try {
            val dao = podcastDao ?: return
            val repository = PodcastRepository(
                ItunesApiService(),
                subscriptionManager,
                dao,
                null
            )
            val subscriptions = subscriptionManager.getSubscriptions()
            
            Log.d(TAG, "Refreshing episodes for ${subscriptions.size} subscribed podcasts from API.")
            
            // Fetch fresh data from API for each subscribed podcast and update database
            subscriptions.forEach { podcastWithEpisodes ->
                val podcast = podcastWithEpisodes.podcast
                try {
                    Log.d(TAG, "Fetching episodes for: ${podcast.title}")
                    val podcastDetailsResult = repository.getPodcastDetails(podcast.id).first()
                    
                    podcastDetailsResult.onSuccess { podcastDetails ->
                        if (podcastDetails != null) {
                            Log.d(TAG, "Updated ${podcastDetails.episodes.size} episodes for ${podcast.title}")
                        }
                    }.onFailure {
                        Log.e(TAG, "Error fetching episodes for ${podcast.title}", it)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Exception refreshing episodes for ${podcast.title}", e)
                }
            }
            
            // Record the refresh time
            settingsManager.setLastEpisodeRefreshTime(System.currentTimeMillis())
            Log.d(TAG, "Daily refresh completed. Last refresh time recorded.")
        } catch (e: Exception) {
            Log.e(TAG, "Error refreshing all podcast episodes", e)
        }
    }
}
