package com.calmcast.podcast.data

import android.util.Log
import com.calmcast.podcast.api.ItunesApiService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
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

    fun getPodcastDetails(podcastId: String): Flow<Result<PodcastWithEpisodes?>> = flow {
        Log.d("PodcastRepository", "getPodcastDetails called for $podcastId")
        
        // Check cache - but only return if it has episodes
        val cachedPodcast = podcastDao.getPodcastWithEpisodes(podcastId)
        if (cachedPodcast != null && cachedPodcast.episodes.isNotEmpty()) {
            Log.d("PodcastRepository", "Found cached podcast with ${cachedPodcast.episodes.size} episodes")
            emit(Result.success(cachedPodcast))
            return@flow
        }

        // Try to fetch from database
        val podcast = podcastDao.getPodcast(podcastId)
        Log.d("PodcastRepository", "Podcast from DB: ${podcast?.title}, feedUrl: ${podcast?.feedUrl}")
        
        if (podcast != null && podcast.feedUrl != null) {
            // We have the podcast with feedUrl, fetch episodes via RSS
            Log.d("PodcastRepository", "Fetching episodes via RSS for ${podcast.title}")
            val result = apiService.getPodcastDetails(podcastId = podcastId, feedUrl = podcast.feedUrl)
            result.onSuccess { response ->
                Log.d("PodcastRepository", "Got ${response.episodes.size} episodes from API")
                // Update podcast with RSS description and episode count
                val updatedPodcast = podcast.copy(
                    description = if (response.description.isNotEmpty()) response.description else podcast.description,
                    episodeCount = response.episodes.size
                )
                podcastDao.insertPodcast(updatedPodcast)
                val episodes = response.episodes.map { episodeResult ->
                    // Generate deterministic episode ID from podcast ID + title + audio URL
                    // This ensures the ID remains stable across app restarts and minor URL changes
                    val stableId = "${podcast.id}|${episodeResult.title}|${episodeResult.audioUrl}".hashCode().toString()
                    Episode(
                        id = stableId,
                        podcastId = podcast.id,
                        podcastTitle = podcast.title,
                        title = episodeResult.title,
                        description = episodeResult.description,
                        publishDate = episodeResult.pubDate,
                        duration = episodeResult.duration,
                        audioUrl = episodeResult.audioUrl
                    )
                }
                // Cache episodes
                podcastDao.insertEpisodes(episodes)
                emit(Result.success(podcastDao.getPodcastWithEpisodes(podcastId)))
            }.onFailure { exception ->
                Log.w("PodcastRepository", "Error fetching podcast details: ${exception.message}")
                // Always propagate feed errors (404/410) so UI can show appropriate message
                emit(Result.failure<PodcastWithEpisodes?>(exception))
            }
        } else {
            // Podcast not in DB - this can happen if user clicked from search but didn't subscribe
            Log.e("PodcastRepository", "Podcast not found in DB for id: $podcastId")
            emit(Result.failure<PodcastWithEpisodes?>(Exception("Podcast not found in database")))
        }
    }.catch { e -> 
        Log.e("PodcastRepository", "Exception in getPodcastDetails", e)
        emit(Result.failure<PodcastWithEpisodes?>(e)) 
    }

    fun getSubscribedPodcasts(): Flow<Result<List<Podcast>>> = flow {
        val subscriptions = subscriptionManager.getSubscriptions()
        val podcasts = subscriptions.map { it.podcast }
        emit(Result.success(podcasts))
    }.catch { e -> emit(Result.failure<List<Podcast>>(e)) }

    suspend fun subscribeToPodcast(podcast: Podcast): Result<Unit> {
        return try {
            // Save podcast to database first
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
}