package com.calmcast.podcast.data

import android.util.Log
import com.calmcast.podcast.api.ItunesApiService
import com.calmcast.podcast.utils.DateTimeFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.util.Date

class PodcastRepository(
    private val apiService: ItunesApiService,
    private val subscriptionManager: SubscriptionManager,
    private val podcastDao: PodcastDao,
    private val playbackPositionDao: PlaybackPositionDao?
) {
    // In-memory cache for episode lists
    private val episodeCache = mutableMapOf<String, PodcastWithEpisodes>()
    // Track which podcasts have cache invalidation requested
    private val invalidationRequests = mutableSetOf<String>()
    private var invalidateAllCache = false

    /**
     * Invalidate cache for all podcasts. Called when app is initialized or foregrounded.
     */
    fun invalidateAllEpisodeCache() {
        invalidateAllCache = true
        episodeCache.clear()
        invalidationRequests.clear()
    }

    /**
     * Invalidate cache for a specific podcast. Called when user clicks refresh button.
     */
    fun invalidatePodcastEpisodeCache(podcastId: String) {
        invalidationRequests.add(podcastId)
        episodeCache.remove(podcastId)
    }

    /**
     * Check if a podcast's cache is valid.
     */
    private fun isCacheValid(podcastId: String): Boolean {
        if (invalidateAllCache) return false
        if (invalidationRequests.contains(podcastId)) return false
        return episodeCache.containsKey(podcastId)
    }

    /**
     * Mark invalidation request as processed.
     */
    private fun markInvalidationProcessed(podcastId: String) {
        invalidationRequests.remove(podcastId)
        if (invalidationRequests.isEmpty()) {
            invalidateAllCache = false
        }
    }

    fun searchPodcasts(query: String): Flow<Result<List<Podcast>>> = flow {
        if (query.isBlank()) {
            emit(Result.success(emptyList()))
            return@flow
        }

        val result = apiService.searchPodcasts(searchQuery = query, limit = 20)
        result.onSuccess { response ->
            val podcasts = response.results.map { podcastResult ->
                Podcast(
                    id = podcastResult.id,
                    title = podcastResult.title,
                    author = podcastResult.author,
                    description = podcastResult.description,
                    imageUrl = podcastResult.imageUrl,
                    feedUrl = podcastResult.feedUrl
                )
            }
            emit(Result.success(podcasts))
        }.onFailure { exception ->
            emit(Result.failure<List<Podcast>>(exception))
        }
    }.catch { e -> emit(Result.failure<List<Podcast>>(e)) }

    // Return a wrapper to include both podcast and fresh episode list
    data class PodcastWithEpisodes(
        val podcast: Podcast,
        val episodes: List<Episode>
    )

    // Unified entrypoint: always fetches fresh episodes from API
    fun getPodcastDetails(podcastId: String, forceRefresh: Boolean = false): Flow<Result<PodcastWithEpisodes?>> =
        fetchAndUpdateEpisodes(podcastId = podcastId)

    fun getSubscribedPodcasts(): Flow<Result<List<Podcast>>> = flow<Result<List<Podcast>>> {
        val podcasts = subscriptionManager.getSubscriptions()
        emit(Result.success(podcasts))
    }.catch { e -> emit(Result.failure<List<Podcast>>(e)) }

    // Data class for fresh episode data from API (without database persistence)
    data class Episode(
        val id: String,
        val podcastId: String,
        val podcastTitle: String,
        val title: String,
        val description: String? = null,
        val publishDate: String,
        val publishDateMillis: Long = 0L,
        val duration: String,
        val audioUrl: String,
        val downloadedPath: String? = null
    )

    suspend fun subscribeToPodcast(podcast: Podcast): Result<Unit> {
        return try {
            podcastDao.insertPodcast(podcast)
            val success = subscriptionManager.addSubscription(podcast)
            if (success) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Podcast already subscribed or subscription failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun unsubscribeFromPodcast(podcastId: String): Result<Unit> {
        return try {
            val success = subscriptionManager.removeSubscription(podcastId)
            if (success) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Unsubscription failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun savePlaybackPosition(episodeId: String, position: Long) {
        val dao = playbackPositionDao ?: return
        val playbackPosition = PlaybackPosition(episodeId, position, System.currentTimeMillis())
        dao.insert(playbackPosition)
    }

    suspend fun getPlaybackPosition(episodeId: String): PlaybackPosition? {
        val dao = playbackPositionDao ?: return null
        return dao.getPlaybackPosition(episodeId)
    }

    suspend fun subscribeToRSSUrl(feedUrl: String): Result<Podcast> {
        return try {
            apiService.getPodcastFromRSSUrl(feedUrl).fold(
                onSuccess = { customPodcast ->
                    val podcast = Podcast(
                        id = customPodcast.id,
                        title = customPodcast.title,
                        author = customPodcast.author,
                        description = customPodcast.description,
                        imageUrl = customPodcast.imageUrl,
                        feedUrl = customPodcast.feedUrl
                    )
                    podcastDao.insertPodcast(podcast)
                    val success = subscriptionManager.addSubscription(podcast)
                    if (success) Result.success(podcast)
                    else Result.failure(Exception("Podcast already subscribed or subscription failed"))
                },
                onFailure = { e -> Result.failure(e) }
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun fetchAndUpdateEpisodes(podcastId: String): Flow<Result<PodcastWithEpisodes?>> =
        flow<Result<PodcastWithEpisodes?>> {
            val podcast = podcastDao.getPodcast(podcastId)
            if (podcast == null || podcast.feedUrl == null) {
                Log.e("PodcastRepository", "Podcast not found or no feed URL for $podcastId")
                emit(Result.failure(Exception("Podcast not found or no feed URL available")))
                return@flow
            }

            if (isCacheValid(podcastId)) {
                val cachedData = episodeCache[podcastId]
                if (cachedData != null) {
                    emit(Result.success(cachedData))
                    return@flow
                }
            }

            markInvalidationProcessed(podcastId)

            val result = apiService.getPodcastDetails(podcastId = podcastId, feedUrl = podcast.feedUrl)
            result.fold(
                onSuccess = { response ->
                    val episodes = response.episodes.map { episodeResult ->
                        val stableId = "${podcast.id}|${episodeResult.title}|${episodeResult.audioUrl}".hashCode().toString()
                        Episode(
                            id = stableId,
                            podcastId = podcast.id,
                            podcastTitle = podcast.title,
                            title = episodeResult.title,
                            description = episodeResult.description,
                            publishDate = episodeResult.pubDate,
                            publishDateMillis = DateTimeFormatter.parseDateToMillis(episodeResult.pubDate) ?: 0L,
                            duration = episodeResult.duration,
                            audioUrl = episodeResult.audioUrl
                        )
                    }

                    val mostRecentEpisodeId = episodes.maxByOrNull { it.publishDateMillis }?.id
                    val updatedPodcast = if (mostRecentEpisodeId != null && mostRecentEpisodeId != podcast.lastSeenEpisodeId) {
                        podcastDao.updateLastSeenEpisodeId(podcast.id, mostRecentEpisodeId)
                        podcast.copy(
                            lastSeenEpisodeId = mostRecentEpisodeId,
                            description = if (response.description.isNotEmpty()) response.description else podcast.description
                        )
                    } else {
                        podcast.copy(
                            description = if (response.description.isNotEmpty()) response.description else podcast.description
                        )
                    }
                    
                    podcastDao.insertPodcast(updatedPodcast)

                    val episodesSorted = episodes.sortedByDescending { it.publishDateMillis }
                    val podcastWithEpisodes = PodcastWithEpisodes(updatedPodcast, episodesSorted)
                    episodeCache[podcastId] = podcastWithEpisodes
                    emit(Result.success(podcastWithEpisodes))
                },
                onFailure = { exception ->
                    Log.e("PodcastRepository", "Error fetching podcast episodes", exception)
                    emit(Result.failure(exception))
                }
            )
        }
            .catch { e ->
                Log.e("PodcastRepository", "Exception in fetchAndUpdateEpisodes", e)
                emit(Result.failure<PodcastWithEpisodes?>(e))
            }
            .flowOn(Dispatchers.IO)

    suspend fun updateNewEpisodeCount(podcastId: String, episodes: List<Episode>): Int {
        val podcast = podcastDao.getPodcast(podcastId) ?: return 0
        if (podcast.lastSeenEpisodeId == null) return episodes.size

        val lastSeenIndex = episodes.indexOfFirst { it.id == podcast.lastSeenEpisodeId }
        return if (lastSeenIndex == -1) episodes.size else lastSeenIndex
    }
}
