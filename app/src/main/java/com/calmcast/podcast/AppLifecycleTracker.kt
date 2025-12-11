package com.calmcast.podcast

import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ProcessLifecycleOwner
import com.calmcast.podcast.data.PodcastDao
import com.calmcast.podcast.data.PodcastRepository
import com.calmcast.podcast.data.SettingsManager
import com.calmcast.podcast.data.SubscriptionManager
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
                Log.d(TAG, "Lifecycle event: $event, callback is null: ${onRefreshCallback == null}")
                when (event) {
                    Lifecycle.Event.ON_START -> {
                        Log.d(TAG, "ON_START triggered")
                        CoroutineScope(Dispatchers.IO).launch {
                            try {
                                val settings = settingsManager ?: return@launch
                                val subMgr = subscriptionManager ?: return@launch
                                val dao = podcastDao ?: return@launch
                                
                                Log.d(TAG, "About to call refresh callback")
                                if (onRefreshCallback != null) {
                                    Log.d(TAG, "Calling onRefreshCallback")
                                    onRefreshCallback?.invoke()
                                } else {
                                    Log.d(TAG, "Calling refreshAllPodcastEpisodesDirectly")
                                    refreshAllPodcastEpisodesDirectly(subMgr, dao)
                                }
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

            for (podcast in subscriptions) {
                try {
                    try {
                        repository.getPodcastDetails(podcast.id, forceRefresh = true).first()
                    } catch (e: Exception) {
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Exception refreshing episodes for ${podcast.title}", e)
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error in refreshAllPodcastEpisodesDirectly", e)
        }
    }

}
