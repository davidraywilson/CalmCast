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
                    episodeCount = 0,
                    feedUrl = podcastResult.feedUrl
                )
            }
            emit(Result.success(podcasts))
        }.onFailure { exception ->
            emit(Result.failure<List<Podcast>>(exception))
        }
    }.catch { e -> emit(Result.failure<List<Podcast>>(e)) }

    // Unified entrypoint retained for compatibility; delegates to fetchAndUpdateEpisodes
    fun getPodcastDetails(podcastId: String, forceRefresh: Boolean = false): Flow<Result<PodcastWithEpisodes?>> =
        fetchAndUpdateEpisodes(podcastId = podcastId, skipCache = forceRefresh)

    fun getSubscribedPodcasts(): Flow<Result<List<Podcast>>> = flow {
        val subscriptions = subscriptionManager.getSubscriptions()
        val podcasts = subscriptions.map { it.podcast }
        emit(Result.success(podcasts))
    }.catch { e -> emit(Result.failure<List<Podcast>>(e)) }

    suspend fun subscribeToPodcast(podcast: Podcast): Result<Unit> {
        return try {
            // Save podcast to database. Don't set lastViewedAt yet (keep it 0).
            // No badge will show until user views the details for the first time.
            podcastDao.insertPodcast(podcast)
            // Then add to subscriptions
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

    suspend fun isSubscribed(podcastId: String): Boolean {
        return try {
            subscriptionManager.isSubscribed(podcastId)
        } catch (e: Exception) {
            false
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

    suspend fun getPodcastWithEpisodes(podcastId: String): PodcastWithEpisodes? {
        return podcastDao.getPodcastWithEpisodes(podcastId)
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
                        episodeCount = 0,
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

    fun fetchAndUpdateEpisodes(podcastId: String, skipCache: Boolean = false): Flow<Result<PodcastWithEpisodes?>> =
        flow<Result<PodcastWithEpisodes?>> {
            Log.d("PodcastRepository", "fetchAndUpdateEpisodes called for $podcastId (skipCache=$skipCache)")

            val podcast = podcastDao.getPodcast(podcastId)
            if (podcast == null || podcast.feedUrl == null) {
                emit(Result.failure(Exception("Podcast not found or no feed URL available")))
                return@flow
            }

            if (!skipCache) {
                val cachedEpisodes = podcastDao.getEpisodesForPodcast(podcast.id)
                if (cachedEpisodes.isNotEmpty()) {
                    Log.d("PodcastRepository", "Returning ${cachedEpisodes.size} cached episodes from DB for ${podcast.title}")
                    emit(Result.success(PodcastWithEpisodes(podcast, cachedEpisodes)))
                    return@flow
                }
            }
            val result = apiService.getPodcastDetails(podcastId = podcastId, feedUrl = podcast.feedUrl)
            result.fold(
                onSuccess = { response ->
                    val existingEpisodeIds = podcastDao.getEpisodeIdsByPodcast(podcast.id).toSet()

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

                    val newEpisodes = episodes.filterNot { existingEpisodeIds.contains(it.id) }

                    if (newEpisodes.isNotEmpty()) {
                        podcastDao.insertEpisodes(newEpisodes)
                    }

                    val viewedCutoff = podcast.lastViewedAt
                    val unseenCount = if (skipCache && viewedCutoff > 0) {
                        newEpisodes.count { ep ->
                            val millis = DateTimeFormatter.parseDateToMillis(ep.publishDate)
                            millis?.let { it > viewedCutoff } ?: true
                        }
                    } else 0

                    val updatedPodcast = podcast.copy(
                        episodeCount = response.episodes.size,
                        description = if (response.description.isNotEmpty()) response.description else podcast.description,
                        newEpisodeCount = if (skipCache) podcast.newEpisodeCount + unseenCount else podcast.newEpisodeCount
                    )
                    podcastDao.insertPodcast(updatedPodcast)

                    val existingEpisodes = podcastDao.getEpisodesForPodcast(podcast.id).filterNot { ep ->
                        newEpisodes.any { it.id == ep.id }
                    }
                    val allEpisodes = (newEpisodes + existingEpisodes).sortedByDescending { it.publishDateMillis }
                    val podcastWithEpisodes = PodcastWithEpisodes(updatedPodcast, allEpisodes)
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
}
