package com.calmcast.podcast.ui

import android.app.Application
import android.content.ComponentName
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.PlaybackParameters
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.calmcast.podcast.PlaybackService
import com.calmcast.podcast.PlaybackError
import com.calmcast.podcast.api.FeedGoneException
import com.calmcast.podcast.api.FeedNotFoundException
import com.calmcast.podcast.data.PlaybackPosition
import com.calmcast.podcast.data.PlaybackPositionDao
import com.calmcast.podcast.data.Podcast
import com.calmcast.podcast.data.PodcastDao
import com.calmcast.podcast.data.PodcastRepository
import com.calmcast.podcast.data.PodcastRepository.Episode
import com.calmcast.podcast.data.PodcastRepository.PodcastWithEpisodes
import com.calmcast.podcast.data.SettingsManager
import com.calmcast.podcast.data.SubscriptionManager
import com.calmcast.podcast.data.DownloadLocation
import com.calmcast.podcast.data.download.Download
import com.calmcast.podcast.data.download.DownloadDao
import com.calmcast.podcast.data.download.DownloadManager
import com.calmcast.podcast.data.download.StorageManager
import com.calmcast.podcast.utils.DateTimeFormatter
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job

class PodcastViewModel(
    private val application: Application,
    private val podcastDao: PodcastDao,
    private val playbackPositionDao: PlaybackPositionDao,
    private val downloadDao: DownloadDao,
    private val downloadManager: DownloadManager,
    private val settingsManager: SettingsManager
) : ViewModel() {

    companion object {
        private const val TAG = "PodcastViewModel"
        private const val SEARCH_DEBOUNCE_MS = 500L
    }

    private val subscriptionManager = SubscriptionManager(application, podcastDao)
    private val repository = PodcastRepository(
        com.calmcast.podcast.api.ItunesApiService(),
        subscriptionManager,
        podcastDao,
        playbackPositionDao
    )

    private val _subscriptions = mutableStateOf<List<Podcast>>(emptyList())
    val subscriptions: State<List<Podcast>> = _subscriptions

    private val _newEpisodeCounts = mutableStateOf<Map<String, Int>>(emptyMap())
    val newEpisodeCounts: State<Map<String, Int>> = _newEpisodeCounts

    private val _searchResults = mutableStateOf<List<Podcast>>(emptyList())
    val searchResults: State<List<Podcast>> = _searchResults

    private val _searchQuery = mutableStateOf("")
    val searchQuery: State<String> = _searchQuery

    private val _isLoading = mutableStateOf(false)

    private val _errorMessage = mutableStateOf<String?>(null)

    private val _currentPodcastDetails = mutableStateOf<PodcastWithEpisodes?>(null)
    val currentPodcastDetails: State<PodcastWithEpisodes?> = _currentPodcastDetails

    private val _metadataLoading = mutableStateOf(false)

    private val _episodesLoading = mutableStateOf(false)
    val episodesLoading: State<Boolean> = _episodesLoading

    private val _detailError = mutableStateOf<Pair<Int, String>?>(null)
    val detailError: State<Pair<Int, String>?> = _detailError

    private val _downloads = mutableStateOf<List<Download>>(emptyList())
    val downloads: State<List<Download>> = _downloads

    private val _playbackPositions = mutableStateOf<Map<String, PlaybackPosition>>(emptyMap())
    val playbackPositions: State<Map<String, PlaybackPosition>> = _playbackPositions

    // Playback state
    private val _currentEpisode = mutableStateOf<Episode?>(null)
    val currentEpisode: State<Episode?> = _currentEpisode

    private val _currentArtworkUri = mutableStateOf<Uri?>(null)
    val currentArtworkUri: State<Uri?> = _currentArtworkUri

    private val _isPlaying = mutableStateOf(false)
    val isPlaying: State<Boolean> = _isPlaying

    private val _currentPosition = mutableLongStateOf(0L)
    val currentPosition: State<Long> = _currentPosition

    private val _duration = mutableLongStateOf(0L)
    val duration: State<Long> = _duration

    private val _isPlayerReady = mutableStateOf(false)

    private val _isBuffering = mutableStateOf(false)
    val isBuffering: State<Boolean> = _isBuffering

    private val _showFullPlayer = mutableStateOf(false)
    val showFullPlayer: State<Boolean> = _showFullPlayer

    private val _playbackError = mutableStateOf<PlaybackError?>(null)
    val playbackError: State<PlaybackError?> = _playbackError

    private var mediaController: MediaController? = null
    private lateinit var playerListener: Player.Listener
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var positionUpdateJob: Job? = null

    // Settings
    private val _isPictureInPictureEnabled = mutableStateOf(false)
    val isPictureInPictureEnabled: State<Boolean> = _isPictureInPictureEnabled

    private val _isAutoDownloadEnabled = mutableStateOf(false)
    val isAutoDownloadEnabled: State<Boolean> = _isAutoDownloadEnabled

    private val _skipSeconds = mutableIntStateOf(10)
    val skipSeconds: State<Int> = _skipSeconds

    private val _isKeepScreenOnEnabled = mutableStateOf(false)
    val isKeepScreenOnEnabled: State<Boolean> = _isKeepScreenOnEnabled

    private val _removeHorizontalDividers = mutableStateOf(false)
    val removeHorizontalDividers: State<Boolean> = _removeHorizontalDividers

    private val _sleepTimerEnabled = mutableStateOf(false)
    val sleepTimerEnabled: State<Boolean> = _sleepTimerEnabled

    private val _sleepTimerMinutes = mutableIntStateOf(0)
    val sleepTimerMinutes: State<Int> = _sleepTimerMinutes

    private val _isSleepTimerActive = mutableStateOf(false)
    val isSleepTimerActive: State<Boolean> = _isSleepTimerActive

    private val _sleepTimerRemainingSeconds = mutableLongStateOf(0L)
    val sleepTimerRemainingSeconds: State<Long> = _sleepTimerRemainingSeconds

    private val _downloadLocation = mutableStateOf(DownloadLocation.INTERNAL)
    val downloadLocation: State<DownloadLocation> = _downloadLocation

    private val _isExternalStorageAvailable = mutableStateOf(false)
    val isExternalStorageAvailable: State<Boolean> = _isExternalStorageAvailable

    private val _isAddingRSSFeed = mutableStateOf(false)
    val isAddingRSSFeed: State<Boolean> = _isAddingRSSFeed

    private val _playbackSpeed = mutableStateOf(1f)
    val playbackSpeed: State<Float> = _playbackSpeed

    private var searchJob: Job? = null
    private var sleepTimerJob: Job? = null
    private var lastSaveTime: Long = 0L
    private val SAVE_INTERVAL_MS = 10000L // 10 seconds

    init {
        cleanupInvalidDownloads()
        loadInitialData()
        observeDownloads()
        initializeMediaController()
        registerPlaybackServiceErrorCallback()
        loadSettings()
    }

    private fun cleanupInvalidDownloads() {
        viewModelScope.launch {
            try {
                downloadDao.deleteInvalidDownloads()
            } catch (e: Exception) {
                Log.e(TAG, "Error cleaning up invalid downloads", e)
            }
        }
    }

    private fun loadSettings() {
        _isPictureInPictureEnabled.value = settingsManager.isPictureInPictureEnabledSync()
        _skipSeconds.intValue = settingsManager.getSkipSecondsSync()
        _isAutoDownloadEnabled.value = settingsManager.isAutoDownloadEnabled()
        _isKeepScreenOnEnabled.value = settingsManager.isKeepScreenOnEnabledSync()
        _removeHorizontalDividers.value = settingsManager.removeHorizontalDividersSync()
        _sleepTimerEnabled.value = settingsManager.isSleepTimerEnabledSync()
        _sleepTimerMinutes.intValue = settingsManager.getSleepTimerMinutesSync()
        _isSleepTimerActive.value = settingsManager.isSleepTimerActiveSync()
        _downloadLocation.value = settingsManager.getDownloadLocationSync()
        _playbackSpeed.value = settingsManager.getPlaybackSpeedSync()
        _isExternalStorageAvailable.value = StorageManager.isExternalStorageAvailable(application)

        viewModelScope.launch {
            settingsManager.isPictureInPictureEnabled.collect {
                _isPictureInPictureEnabled.value = it
            }
        }
        viewModelScope.launch {
            settingsManager.skipSeconds.collect {
                _skipSeconds.intValue = it
            }
        }
        viewModelScope.launch {
            settingsManager.autoDownloadEnabled.collect {
                _isAutoDownloadEnabled.value = it
            }
        }
        viewModelScope.launch {
            settingsManager.isKeepScreenOnEnabled.collect {
                _isKeepScreenOnEnabled.value = it
            }
        }
        viewModelScope.launch {
            settingsManager.removeHorizontalDividers.collect {
                _removeHorizontalDividers.value = it
            }
        }
        viewModelScope.launch {
            settingsManager.sleepTimerEnabled.collect {
                _sleepTimerEnabled.value = it
                if (!it) {
                    stopSleepTimer()
                }
            }
        }
        viewModelScope.launch {
            settingsManager.sleepTimerMinutes.collect {
                _sleepTimerMinutes.intValue = it
            }
        }
        viewModelScope.launch {
            settingsManager.isSleepTimerActive.collect {
                _isSleepTimerActive.value = it
                if (!it) {
                    stopSleepTimer()
                }
            }
        }
        viewModelScope.launch {
            settingsManager.playbackSpeed.collect {
                _playbackSpeed.value = it
                applyPlaybackSpeed(it)
            }
        }
    }

    private inner class PlayerListener : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _isPlaying.value = isPlaying
            if (isPlaying) {
                startPositionUpdates()
            } else {
                stopPositionUpdates()
            }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            _isPlayerReady.value = playbackState == Player.STATE_READY
            _isBuffering.value = playbackState == Player.STATE_BUFFERING
            if (playbackState == Player.STATE_IDLE || playbackState == Player.STATE_ENDED) {
                _currentEpisode.value = null
                stopPositionUpdates()
            }
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            if (mediaItem != null) {
                _currentEpisode.value = _downloads.value.find { it.episode.id == mediaItem.mediaId }?.episode
            }
        }
    }

    private fun initializeMediaController() {
        playerListener = PlayerListener()
        val sessionToken = SessionToken(application, ComponentName(application, PlaybackService::class.java))
        controllerFuture = MediaController.Builder(application, sessionToken).buildAsync()
        controllerFuture?.addListener({
            mediaController = controllerFuture?.get()
            mediaController?.addListener(playerListener)
            _currentPosition.longValue = mediaController?.currentPosition ?: 0L
            _duration.longValue = mediaController?.duration ?: 0L
        }, MoreExecutors.directExecutor())
    }

    private fun observeDownloads() {
        downloadManager.downloads.onEach { downloadList ->
            _downloads.value = downloadList
        }.launchIn(viewModelScope)
    }

    private fun startPositionUpdates() {
        positionUpdateJob?.cancel()
        positionUpdateJob = viewModelScope.launch {
            while (_isPlaying.value) {
                _currentPosition.longValue = mediaController?.currentPosition ?: 0L
                _duration.longValue = mediaController?.duration ?: 0L

                if (_currentEpisode.value != null && _currentPosition.longValue > 0) {
                    val currentTime = System.currentTimeMillis()
                    if (currentTime - lastSaveTime >= SAVE_INTERVAL_MS) {
                        lastSaveTime = currentTime
                        val episodeId = _currentEpisode.value!!.id
                        val position = _currentPosition.longValue
                        repository.savePlaybackPosition(
                            episodeId = episodeId,
                            position = position
                        )
                        val updatedPosition = PlaybackPosition(
                            episodeId = episodeId,
                            position = position,
                            lastPlayed = currentTime
                        )
                        _playbackPositions.value = _playbackPositions.value + (episodeId to updatedPosition)
                    }
                }
                delay(500)
            }
        }
    }

    private fun stopPositionUpdates() {
        positionUpdateJob?.cancel()
        positionUpdateJob = null
    }

    private fun loadInitialData() {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val subscriptionIds = subscriptionManager.getSubscribedPodcastIds()
                
                val allPodcasts = podcastDao.getAllPodcasts()
                val podcasts = allPodcasts.filter { subscriptionIds.contains(it.id) }.sortedBy { it.title }
                
                _subscriptions.value = podcasts
                _isLoading.value = false
                refreshSubscribedPodcastEpisodes()
            } catch (e: Exception) {
                Log.e(TAG, "Error loading initial data", e)
                _errorMessage.value = e.message
                _isLoading.value = false
            }
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        
        searchJob?.cancel()
        
        if (query.isBlank()) {
            _searchResults.value = emptyList()
            _isLoading.value = false
        } else {
            searchJob = viewModelScope.launch {
                delay(SEARCH_DEBOUNCE_MS)
                _isLoading.value = true
                repository.searchPodcasts(query).collect { result ->
                    result.onSuccess { podcasts ->
                        _searchResults.value = podcasts
                        _isLoading.value = false
                    }.onFailure { exception ->
                        _errorMessage.value = exception.message
                        _isLoading.value = false
                    }
                }
            }
        }
    }

    fun subscribeToPodcast(podcast: Podcast) {
        viewModelScope.launch {
            repository.subscribeToPodcast(podcast).onSuccess {
                if (!_subscriptions.value.any { it.id == podcast.id }) {
                    val updatedList = (_subscriptions.value + podcast).sortedBy { it.title }
                    _subscriptions.value = updatedList
                }
                // Fetch full metadata from API after successful subscription
                if (podcast.feedUrl != null) {
                    try {
                        repository.getPodcastDetails(podcast.id).first().getOrNull()?.let { podcastDetails ->
                            // Update the podcast in subscriptions with fetched metadata
                            val updatedPodcast = podcastDetails.podcast
                            _subscriptions.value = _subscriptions.value.map { p ->
                                if (p.id == podcast.id) updatedPodcast else p
                            }.sortedBy { it.title }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error fetching metadata for new subscription", e)
                    }
                }
            }.onFailure { _errorMessage.value = it.message }
        }
    }

    fun unsubscribeFromPodcast(podcastId: String) {
        viewModelScope.launch {
            repository.unsubscribeFromPodcast(podcastId).onSuccess {
                _subscriptions.value = _subscriptions.value.filter { it.id != podcastId }
            }.onFailure { error ->
                Log.e(TAG, "unsubscribeFromPodcast failed for $podcastId", error)
                _errorMessage.value = error.message
            }
        }
    }

    fun subscribeToRSSUrl(feedUrl: String, onResult: (Result<Podcast>) -> Unit = {}) {
        if (_isAddingRSSFeed.value) return
        _isAddingRSSFeed.value = true
        viewModelScope.launch {
            try {
                val result = repository.subscribeToRSSUrl(feedUrl)
                result.onSuccess { podcast ->
                    if (!_subscriptions.value.any { it.id == podcast.id }) {
                        val updatedList = (_subscriptions.value + podcast).sortedBy { it.title }
                        _subscriptions.value = updatedList
                    }
                    // Fetch full metadata from API after successful subscription
                    try {
                        repository.getPodcastDetails(podcast.id).first().getOrNull()?.let { podcastDetails ->
                            // Update the podcast in subscriptions with fetched metadata
                            val updatedPodcast = podcastDetails.podcast
                            _subscriptions.value = _subscriptions.value.map { p ->
                                if (p.id == podcast.id) updatedPodcast else p
                            }.sortedBy { it.title }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error fetching metadata for RSS subscription", e)
                    }
                }.onFailure {
                    _errorMessage.value = it.message
                }
                onResult(result)
            } finally {
                _isAddingRSSFeed.value = false
            }
        }
    }

    fun isSubscribed(podcastId: String): Boolean = _subscriptions.value.any { it.id == podcastId }

    fun fetchPodcastDetails(podcastId: String) {
        _metadataLoading.value = true
        _episodesLoading.value = true
        _detailError.value = null
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            podcastDao.updateLastViewedAt(podcastId, now)
            _subscriptions.value = _subscriptions.value.map { podcast ->
                if (podcast.id == podcastId) podcast.copy(lastViewedAt = now) else podcast
            }

            var podcast = podcastDao.getPodcast(podcastId)

            if (podcast == null) {
                val searchResult = _searchResults.value.find { it.id == podcastId }
                if (searchResult != null) {
                    podcastDao.insertPodcast(searchResult)
                    podcast = searchResult
                } else {
                    Log.e(TAG, "Podcast not found in search results either")
                }
            }

            podcast = podcastDao.getPodcast(podcastId) ?: podcast

            if (podcast != null) {
                _currentPodcastDetails.value = PodcastWithEpisodes(podcast, emptyList())
                _metadataLoading.value = false

                try {
                    repository.getPodcastDetails(podcastId).collect { result ->
                        result.onSuccess { podcastWithEpisodes ->
                            if (podcastWithEpisodes != null) {
                                _currentPodcastDetails.value = podcastWithEpisodes
                                
                                val episodeIds = podcastWithEpisodes.episodes.map { it.id }
                                val positions = playbackPositionDao.getPlaybackPositions(episodeIds)
                                _playbackPositions.value =
                                    _playbackPositions.value + positions.associateBy { it.episodeId }
                            }
                            _episodesLoading.value = false
                        }.onFailure { exception ->
                            Log.e(TAG, "Error fetching episodes", exception)
                            when (exception) {
                                is FeedNotFoundException -> _detailError.value = 404 to "Podcast feed not found"
                                is FeedGoneException -> {
                                    // Check if it's a 403 error
                                    val errorCode = if (exception.message?.contains("403") == true) 403 else 410
                                    val message = exception.message ?: "Podcast feed unavailable"
                                    _detailError.value = errorCode to message
                                }
                                else -> _errorMessage.value = exception.message
                            }
                            _episodesLoading.value = false
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Exception in fetchPodcastDetails", e)
                    _errorMessage.value = e.message
                    _episodesLoading.value = false
                }
            } else {
                _metadataLoading.value = false
                _episodesLoading.value = false
            }
        }
    }

    fun refreshPodcastEpisodes(podcastId: String) {
        _episodesLoading.value = true
        viewModelScope.launch {
            try {
                // Invalidate cache for this specific podcast (refresh button clicked)
                repository.invalidatePodcastEpisodeCache(podcastId)
                repository.getPodcastDetails(podcastId).collect { result ->
                    result.onSuccess { podcastWithEpisodes ->
                        if (podcastWithEpisodes != null) {
                            _currentPodcastDetails.value = podcastWithEpisodes
                            // Load playback positions for the refreshed episodes
                            val episodeIds = podcastWithEpisodes.episodes.map { it.id }
                            val positions = playbackPositionDao.getPlaybackPositions(episodeIds)
                            _playbackPositions.value = _playbackPositions.value + positions.associateBy { it.episodeId }
                        }
                        _episodesLoading.value = false
                    }.onFailure { exception ->
                        _errorMessage.value = "Failed to refresh episodes: ${exception.message}"
                        _episodesLoading.value = false
                    }
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error refreshing episodes: ${e.message}"
                _episodesLoading.value = false
            }
        }
    }

    fun refreshSubscribedPodcastEpisodes() {
        viewModelScope.launch {
            try {
                // Invalidate cache for all podcasts (app init/foreground)
                repository.invalidateAllEpisodeCache()
                val subscribedPodcasts = repository.getSubscribedPodcasts().first().getOrNull() ?: return@launch
                val newCounts = mutableMapOf<String, Int>()
                
                for (podcast in subscribedPodcasts) {
                    try {
                        val episodes = repository.getPodcastDetails(podcast.id).first().getOrNull()?.episodes

                        if (episodes != null) {
                            val lastSeenEpisode = episodes.find { it.id == podcast.lastSeenEpisodeId }
                            if (lastSeenEpisode != null) {
                                val index = episodes.indexOf(lastSeenEpisode)
                                newCounts[podcast.id] = index
                            }
                        }

                        if (settingsManager.isAutoDownloadEnabled()) {
                            // slice new episodes and download them
                            val newEpisodes = episodes?.slice(
                                (newCounts[podcast.id] ?: (0 until episodes.size)) as IntRange
                            ) ?: emptyList()

                            newEpisodes.forEach { episode ->
                                val existingDownload = _downloads.value.find { it.episode.id == episode.id }
                                // Only auto-download if:
                                // 1. No record exists (never attempted)
                                // 2. Status is not DELETED (user didn't explicitly delete it)
                                if (existingDownload == null || existingDownload.status.name != "DELETED") {
                                    if (existingDownload?.status?.name !in listOf("DOWNLOADING", "DOWNLOADED", "PAUSED")) {
                                        downloadManager.startDownload(episode)
                                    }
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Exception refreshing episodes for ${podcast.title}", e)
                    }
                }
                
                val subscriptionIds = subscriptionManager.getSubscribedPodcastIds()
                val allPodcasts = podcastDao.getAllPodcasts()
                val updatedSubscriptions = allPodcasts.filter { subscriptionIds.contains(it.id) }
                _subscriptions.value = updatedSubscriptions
                
                _newEpisodeCounts.value = newCounts
            } catch (e: Exception) {
                Log.e(TAG, "Error refreshing all podcast episodes", e)
            }
        }
    }

    fun playEpisode(episode: Episode) {
        viewModelScope.launch {
            if (_currentEpisode.value != null && _currentPosition.longValue > 0) {
                repository.savePlaybackPosition(
                    episodeId = _currentEpisode.value!!.id,
                    position = _currentPosition.longValue
                )
            }
            
            var episodeToPlay = episode
            val download = _downloads.value.find { it.episode.id == episode.id }
            if (download?.downloadUri != null) {
                episodeToPlay = episode.copy(audioUrl = download.downloadUri)
            }

            var details = currentPodcastDetails.value
            if (details == null || details.podcast.id != episode.podcastId) {
                details = null
            }

            var lastPosition = repository.getPlaybackPosition(episode.id)?.position ?: 0L
            val durationInSeconds = DateTimeFormatter.parseDuration(episode.duration)
            if (durationInSeconds != null && lastPosition > 0) {
                val durationInMilliseconds = durationInSeconds * 1000
                if (lastPosition.toDouble() / durationInMilliseconds > 0.99) {
                    lastPosition = 0L
                }
            }

            val artworkUri = details?.podcast?.imageUrl?.toUri()
            _currentArtworkUri.value = artworkUri

            val mediaMetadata = MediaMetadata.Builder()
                .setTitle(episode.title)
                .setArtist(details?.podcast?.author)
                .setArtworkUri(artworkUri)
                .build()

            val mediaItem = MediaItem.Builder()
                .setUri(episodeToPlay.audioUrl)
                .setMediaId(episode.id)
                .setMediaMetadata(mediaMetadata)
                .build()

            mediaController?.setMediaItem(mediaItem, lastPosition)
            mediaController?.prepare()
            mediaController?.play()

            _currentEpisode.value = episodeToPlay
            _showFullPlayer.value = true
            
            if (settingsManager.isSleepTimerEnabledSync() && settingsManager.getSleepTimerMinutesSync() > 0) {
                startSleepTimer()
            }
        }
    }

    fun togglePlayPause() {
        if (_isPlaying.value) {
            mediaController?.pause()
        } else {
            mediaController?.play()
        }
        if (!_isPlaying.value && _currentEpisode.value != null && _currentPosition.longValue > 0) {
            viewModelScope.launch {
                repository.savePlaybackPosition(
                    episodeId = _currentEpisode.value!!.id,
                    position = _currentPosition.longValue
                )
            }
        }
    }

    fun seekForward() {
        val newPosition = (mediaController?.currentPosition ?: 0L) + (_skipSeconds.intValue * 1000L)
        mediaController?.seekTo(newPosition)
        saveCurrentPosition()
    }

    fun seekBackward() {
        val newPosition = (mediaController?.currentPosition ?: 0L) - (_skipSeconds.intValue * 1000L)
        mediaController?.seekTo(newPosition)
        saveCurrentPosition()
    }

    private fun saveCurrentPosition() {
        if (_currentEpisode.value != null && _currentPosition.longValue > 0) {
            viewModelScope.launch {
                repository.savePlaybackPosition(
                    episodeId = _currentEpisode.value!!.id,
                    position = _currentPosition.longValue
                )
            }
        }
    }

    fun seekTo(positionMs: Long) {
        mediaController?.seekTo(positionMs)
        saveCurrentPosition()
    }

    fun showFullPlayer() {
        _showFullPlayer.value = true
    }

    fun minimizePlayer() {
        _showFullPlayer.value = false
    }

    fun downloadEpisode(episode: Episode) {
        downloadManager.startDownload(episode)
    }

    fun pauseDownload(episodeId: String) {
        downloadManager.pauseDownload(episodeId)
    }

    fun cancelDownload(episodeId: String) {
        downloadManager.cancelDownload(episodeId)
    }

    fun resumeDownload(episodeId: String) {
        downloadManager.resumeDownload(episodeId)
    }

    fun deleteEpisode(episode: Episode) {
        downloadManager.deleteDownload(episode)
    }

    fun clearPlaybackError() {
        _playbackError.value = null
    }

    fun clearPodcastDetails() {
        _currentPodcastDetails.value = null
        _detailError.value = null
    }

    fun setPictureInPictureEnabled(enabled: Boolean) {
        settingsManager.setPictureInPictureEnabled(enabled)
    }

    fun setAutoDownloadEnabled(enabled: Boolean) {
        settingsManager.setAutoDownloadEnabled(enabled)
    }

    fun setSkipSeconds(seconds: Int) {
        settingsManager.setSkipSeconds(seconds)
    }

    fun setKeepScreenOnEnabled(enabled: Boolean) {
        settingsManager.setKeepScreenOnEnabled(enabled)
    }

    fun setRemoveHorizontalDividers(enabled: Boolean) {
        settingsManager.setRemoveHorizontalDividers(enabled)
    }

    fun setSleepTimerEnabled(enabled: Boolean) {
        settingsManager.setSleepTimerEnabled(enabled)
    }

    fun setSleepTimerMinutes(minutes: Int) {
        settingsManager.setSleepTimerMinutes(minutes)
        _sleepTimerMinutes.intValue = minutes
    }

    fun setDownloadLocation(location: DownloadLocation) {
        settingsManager.setDownloadLocation(location)
        _downloadLocation.value = location
    }

    fun setPlaybackSpeed(speed: Float) {
        settingsManager.setPlaybackSpeed(speed)
        _playbackSpeed.value = speed
        applyPlaybackSpeed(speed)
    }

    fun cyclePlaybackSpeed() {
        val speeds = SettingsManager.PLAYBACK_SPEEDS
        val currentIndex = speeds.indexOf(_playbackSpeed.value)
        val nextIndex = if (currentIndex >= 0 && currentIndex < speeds.size - 1) currentIndex + 1 else 0
        val nextSpeed = speeds[nextIndex]
        setPlaybackSpeed(nextSpeed)
    }

    private fun applyPlaybackSpeed(speed: Float) {
        if (mediaController != null) {
            val params = PlaybackParameters(speed)
            mediaController?.setPlaybackParameters(params)
        }
    }

    fun startSleepTimer() {
        if (_sleepTimerMinutes.intValue <= 0) return
        
        _isSleepTimerActive.value = true
        settingsManager.setSleepTimerActive(true)
        _sleepTimerRemainingSeconds.longValue = (_sleepTimerMinutes.intValue * 60).toLong()
        
        sleepTimerJob?.cancel()
        sleepTimerJob = viewModelScope.launch {
            while (_sleepTimerRemainingSeconds.longValue > 0) {
                delay(1000)
                _sleepTimerRemainingSeconds.longValue -= 1
            }
            
            if (_sleepTimerRemainingSeconds.longValue <= 0) {
                mediaController?.pause()
                _isSleepTimerActive.value = false
                settingsManager.setSleepTimerActive(false)
                _sleepTimerRemainingSeconds.longValue = 0L
            }
        }
    }

    fun stopSleepTimer() {
        sleepTimerJob?.cancel()
        sleepTimerJob = null
        _isSleepTimerActive.value = false
        settingsManager.setSleepTimerActive(false)
        _sleepTimerRemainingSeconds.longValue = 0L
    }

    private fun registerPlaybackServiceErrorCallback() {
        PlaybackService.setErrorCallback { error ->
            val message = when (error.errorCode) {
                PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
                PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT -> {
                    _playbackError.value = PlaybackError.NetworkError("No internet connection")
                    null
                }
                PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS -> "Server returned an error. Try again later."
                PlaybackException.ERROR_CODE_DECODER_INIT_FAILED,
                PlaybackException.ERROR_CODE_DECODING_FAILED,
                PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED -> "Unsupported or corrupted audio file."
                else -> "Playback error occurred."
            }
            if (message != null) {
                _playbackError.value = PlaybackError.GeneralError(message)
            }
            _isPlaying.value = false
            _isBuffering.value = false
        }
    }

    override fun onCleared() {
        super.onCleared()
        mediaController?.removeListener(playerListener)
        PlaybackService.setErrorCallback(null)
        sleepTimerJob?.cancel()
        viewModelScope.launch {
            if (_currentEpisode.value != null && _currentPosition.longValue > 0) {
                repository.savePlaybackPosition(
                    episodeId = _currentEpisode.value!!.id,
                    position = _currentPosition.longValue
                )
            }
        }
        controllerFuture?.let {
            MediaController.releaseFuture(it)
        }
        positionUpdateJob?.cancel()
    }
}
