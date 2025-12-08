package com.calmcast.podcast

import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ProcessLifecycleOwner
import com.calmcast.podcast.data.PodcastDao
import com.calmcast.podcast.data.PodcastRepository
import com.calmcast.podcast.data.SettingsManager
import com.calmcast.podcast.data.SubscriptionManager
import com.calmcast.podcast.data.download.AndroidDownloadManager
import com.calmcast.podcast.api.ItunesApiService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

object AppLifecycleTracker {
    private const val TAG = "AppLifecycleTracker"
    private var settingsManager: SettingsManager? = null
    private var podcastDao: PodcastDao? = null
    private var subscriptionManager: SubscriptionManager? = null
    private var onRefreshCallback: (() -> Unit)? = null

    fun setRefreshCallback(callback: (() -> Unit)?) {
        onRefreshCallback = callback
    }

    fun initialize(
        subscriptionMgr: SubscriptionManager,
        downloadManager: AndroidDownloadManager,
        settings: SettingsManager,
        dao: PodcastDao,
        refreshCallback: (() -> Unit)? = null
    ) {
        settingsManager = settings
        podcastDao = dao
        subscriptionManager = subscriptionMgr
        onRefreshCallback = refreshCallback
        ProcessLifecycleOwner.get().lifecycle.addObserver(
            LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_START -> {
                        Log.d(TAG, "App entered foreground. Triggering episode refresh for subscribed podcasts.")
                        CoroutineScope(Dispatchers.IO).launch {
                            try {
                                val settings = settingsManager ?: return@launch
                                val subMgr = subscriptionManager ?: return@launch
                                val dao = podcastDao ?: return@launch
                                
                                // Try to use the callback first (from ViewModel)
                                if (onRefreshCallback != null) {
                                    Log.d(TAG, "Using ViewModel refresh callback")
                                    onRefreshCallback?.invoke()
                                } else {
                                    // Fallback: refresh directly using repository if callback not set yet
                                    Log.d(TAG, "Callback not set, using direct repository refresh")
                                    refreshAllPodcastEpisodesDirectly(subMgr, dao)
                                }
                                
                                // Record the refresh time
                                settings.setLastEpisodeRefreshTime(System.currentTimeMillis())
                                Log.d(TAG, "Episode refresh completed. Last refresh time recorded.")
                                
                            } catch (e: Exception) {
                                Log.e(TAG, "Error refreshing episodes", e)
                            }
                        }
                    }
                    else -> {}
                }
            }
        )
    }

    private suspend fun refreshAllPodcastEpisodesDirectly(
        subscriptionManager: SubscriptionManager,
        podcastDao: PodcastDao
    ) {
        try {
            val repository = PodcastRepository(
                ItunesApiService(),
                subscriptionManager,
                podcastDao,
                null
            )
            val subscriptions = subscriptionManager.getSubscriptions()
            
            Log.d(TAG, "Directly refreshing episodes for ${subscriptions.size} subscribed podcasts")
            
            for (podcastWithEpisodes in subscriptions) {
                val podcast = podcastWithEpisodes.podcast
                try {
                    Log.d(TAG, "Fetching episodes for: ${podcast.title}")
                    try {
                        repository.getPodcastDetails(podcast.id, forceRefresh = true).first()
                        Log.d(TAG, "Episode fetch complete for ${podcast.title}")
                    } catch (e: Exception) {
                        Log.w(TAG, "Error refreshing episodes for ${podcast.title}", e)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Exception refreshing episodes for ${podcast.title}", e)
                }
            }
            
            Log.d(TAG, "Direct episode refresh completed")
        } catch (e: Exception) {
            Log.e(TAG, "Error in refreshAllPodcastEpisodesDirectly", e)
        }
    }

}
