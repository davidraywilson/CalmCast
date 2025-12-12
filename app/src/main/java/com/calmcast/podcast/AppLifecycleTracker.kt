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
    private var apiServiceProvider: (() -> ItunesApiService)? = null

    fun setRefreshCallback(callback: (() -> Unit)?) {
        onRefreshCallback = callback
    }

    fun initialize(
        subscriptionMgr: SubscriptionManager,
        settings: SettingsManager,
        dao: PodcastDao,
        refreshCallback: (() -> Unit)? = null,
        apiProvider: (() -> ItunesApiService)? = null
    ) {
        settingsManager = settings
        podcastDao = dao
        subscriptionManager = subscriptionMgr
        onRefreshCallback = refreshCallback
        apiServiceProvider = apiProvider
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
        // Refresh is now handled through PodcastViewModel.refreshSubscribedPodcastEpisodes()
        // This method is kept for backward compatibility but does nothing
    }

}
