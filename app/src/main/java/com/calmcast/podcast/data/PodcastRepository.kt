package com.calmcast.podcast.data

import com.calmcast.podcast.api.TaddyApiService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import java.util.Date

class PodcastRepository(
    private val apiService: TaddyApiService,
    private val subscriptionManager: SubscriptionManager,
    private val podcastDao: PodcastDao,
    private val playbackPositionDao: PlaybackPositionDao
) {
    fun searchPodcasts(query: String): Flow<Result<List<Podcast>>> = flow {
        if (query.isBlank()) {
            emit(Result.success(emptyList()))
            return@flow
        }

        val result = apiService.searchPodcasts(searchQuery = query, limit = 20)
        result.onSuccess { response ->
            val podcasts = response.podcasts?.data?.map { podcastData ->
                Podcast(
                    id = podcastData.id,
                    title = podcastData.title,
                    author = podcastData.author,
                    description = podcastData.description,
                    imageUrl = podcastData.imageUrl,
                    episodeCount = podcastData.episodeCount
                )
            } ?: emptyList()
            emit(Result.success(podcasts))
        }.onFailure { exception ->
            emit(Result.failure<List<Podcast>>(exception))
        }
    }.catch { e -> emit(Result.failure<List<Podcast>>(e)) }

    fun getPodcastDetails(podcastId: String): Flow<Result<PodcastWithEpisodes?>> = flow {
        // Check cache first
        val cachedPodcast = podcastDao.getPodcastWithEpisodes(podcastId)
        if (cachedPodcast != null) {
            emit(Result.success(cachedPodcast))
            return@flow
        }

        // If not cached, fetch from API
        val result = apiService.getPodcastDetails(podcastId = podcastId)
        result.onSuccess { response ->
            val podcastData = response.podcast
            if (podcastData != null) {
                val podcast = Podcast(
                    id = podcastData.id,
                    title = podcastData.title,
                    author = podcastData.author,
                    description = podcastData.description,
                    imageUrl = podcastData.imageUrl,
                    episodeCount = podcastData.episodeCount
                )
                val episodes = podcastData.episodes.map { episodeData ->
                    Episode(
                        id = episodeData.id,
                        podcastId = podcast.id,
                        podcastTitle = podcast.title,
                        title = episodeData.title,
                        publishDate = episodeData.publishDate,
                        duration = episodeData.duration,
                        audioUrl = episodeData.audioUrl
                    )
                }
                // Cache the podcast and episodes
                podcastDao.insertPodcast(podcast)
                podcastDao.insertEpisodes(episodes)
                emit(Result.success(podcastDao.getPodcastWithEpisodes(podcastId)))
            } else {
                emit(Result.success(null))
            }
        }.onFailure { exception ->
            emit(Result.failure<PodcastWithEpisodes?>(exception))
        }
    }.catch { e -> emit(Result.failure<PodcastWithEpisodes?>(e)) }

    fun getSubscribedPodcasts(): Flow<Result<List<Podcast>>> = flow {
        val subscriptions = subscriptionManager.getSubscriptions()
        val podcasts = subscriptions.map { it.podcast }
        emit(Result.success(podcasts))
    }.catch { e -> emit(Result.failure<List<Podcast>>(e)) }

    suspend fun subscribeToPodcast(podcast: Podcast): Result<Unit> {
        return try {
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
        val playbackPosition = PlaybackPosition(episodeId, position, System.currentTimeMillis())
        playbackPositionDao.insert(playbackPosition)
    }

    suspend fun getPlaybackPosition(episodeId: String): PlaybackPosition? {
        return playbackPositionDao.getPlaybackPosition(episodeId)
    }

    suspend fun getPodcastWithEpisodes(podcastId: String): PodcastWithEpisodes? {
        return podcastDao.getPodcastWithEpisodes(podcastId)
    }
}