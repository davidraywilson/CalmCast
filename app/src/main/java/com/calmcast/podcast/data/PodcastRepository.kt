package com.calmcast.podcast.data

import android.util.Log
import com.calmcast.podcast.api.ItunesApiService
import com.calmcast.podcast.utils.DateTimeFormatter
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
            // Save podcast to database with current timestamp as lastViewedAt
            // This ensures newly subscribed podcasts don't show a huge badge count
            val now = System.currentTimeMillis()
            podcastDao.insertPodcast(podcast.copy(lastViewedAt = now))
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
                    // Set lastViewedAt to current time so newly subscribed podcasts don't show a huge badge
                    val now = System.currentTimeMillis()
                    val podcast = Podcast(
                        id = customPodcast.id,
                        title = customPodcast.title,
                        author = customPodcast.author,
                        description = customPodcast.description,
                        imageUrl = customPodcast.imageUrl,
                        episodeCount = 0,
                        feedUrl = customPodcast.feedUrl,
                        lastViewedAt = now
                    )
                    // Save podcast to database
                    podcastDao.insertPodcast(podcast)
                    // Add to subscriptions
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

    suspend fun refreshPodcastEpisodesIncrementally(podcastId: String): Result<Int> {
        return try {
            val podcast = podcastDao.getPodcast(podcastId) ?: return Result.failure(Exception("Podcast not found"))
            
            if (podcast.feedUrl == null) {
                return Result.failure(Exception("Podcast feed URL not available"))
            }

            val result = apiService.getPodcastDetails(podcastId = podcastId, feedUrl = podcast.feedUrl)
            result.fold(
                onSuccess = { response ->
                    Log.d("PodcastRepository", "Fetched ${response.episodes.size} episodes for ${podcast.title}")
                    
                    // Get existing episode IDs to identify new ones
                    val existingEpisodeIds = podcastDao.getEpisodeIdsByPodcast(podcast.id).toSet()
                    
                    // Create new episodes, filtering out existing ones
                    val newEpisodes = response.episodes.mapNotNull { episodeResult ->
                        val stableId = "${podcast.id}|${episodeResult.title}|${episodeResult.audioUrl}".hashCode().toString()
                        if (!existingEpisodeIds.contains(stableId)) {
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
                        } else {
                            null
                        }
                    }
                    
                    Log.d("PodcastRepository", "Found ${newEpisodes.size} new episodes for ${podcast.title}")
                    
                    // Insert only new episodes
                    if (newEpisodes.isNotEmpty()) {
                        podcastDao.insertEpisodes(newEpisodes)
                    }
                    
                    // Update podcast with latest episode count and increment newEpisodeCount
                    val updatedPodcast = podcast.copy(
                        episodeCount = response.episodes.size,
                        description = if (response.description.isNotEmpty()) response.description else podcast.description,
                        newEpisodeCount = podcast.newEpisodeCount + newEpisodes.size
                    )
                    podcastDao.insertPodcast(updatedPodcast)
                    
                    Result.success(newEpisodes.size)
                },
                onFailure = { e -> 
                    Log.e("PodcastRepository", "Error refreshing episodes for ${podcast.title}", e)
                    Result.failure(e) 
                }
            )
        } catch (e: Exception) {
            Log.e("PodcastRepository", "Exception in refreshPodcastEpisodesIncrementally", e)
            Result.failure(e)
        }
    }

    fun fetchAndUpdateEpisodes(podcastId: String, skipCache: Boolean = false): Flow<Result<PodcastWithEpisodes?>> = flow<Result<PodcastWithEpisodes?>> {
        Log.d("PodcastRepository", "fetchAndUpdateEpisodes called for $podcastId (skipCache=$skipCache)")
        
        // Get podcast from database
        val podcast = podcastDao.getPodcast(podcastId)
        Log.d("PodcastRepository", "Query param podcastId=$podcastId, podcast from DB: id=${podcast?.id}, title=${podcast?.title}")
        if (podcast == null || podcast.feedUrl == null) {
            emit(Result.failure(Exception("Podcast not found or no feed URL available")))
            return@flow
        }
        
        // If not skipping cache, check if we have episodes in the database
        if (!skipCache) {
            val cachedEpisodes = podcastDao.getEpisodesForPodcast(podcast.id)
            if (cachedEpisodes.isNotEmpty()) {
                Log.d("PodcastRepository", "Returning ${cachedEpisodes.size} cached episodes from DB for ${podcast.title}")
                emit(Result.success(PodcastWithEpisodes(podcast, cachedEpisodes)))
                return@flow
            }
        }

        // Fetch from API
        
        val result = apiService.getPodcastDetails(podcastId = podcastId, feedUrl = podcast.feedUrl)
        result.fold(
            onSuccess = { response ->
                // Get existing episode IDs to identify new ones
                val existingEpisodeIds = podcastDao.getEpisodeIdsByPodcast(podcast.id).toSet()
                
                // Create episodes from API response
                Log.d("PodcastRepository", "Creating episodes with podcast.id=${podcast.id}")
                val episodes = response.episodes.map { episodeResult ->
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
                
                // Filter out episodes that already exist in the database
                val newEpisodes = episodes.filterNot { existingEpisodeIds.contains(it.id) }
                Log.d("PodcastRepository", "Total episodes from API: ${episodes.size}, new: ${newEpisodes.size}")
                
                // Insert new episodes
                if (newEpisodes.isNotEmpty()) {
                    Log.d("PodcastRepository", "Inserting ${newEpisodes.size} episodes")
                    podcastDao.insertEpisodes(newEpisodes)
                    Log.d("PodcastRepository", "Episodes inserted successfully")
                } else {
                    Log.d("PodcastRepository", "No new episodes to insert")
                }
                
                // Compute unseenCount as episodes published after lastViewedAt
                // Only count episodes as unseen if lastViewedAt > 0 (podcast was actually viewed before)
                // This prevents showing 2000+ badge count for newly subscribed podcasts
                val viewedCutoff = podcast.lastViewedAt
                val unseenCount = if (skipCache && viewedCutoff > 0) {
                    newEpisodes.count { ep ->
                        val millis = DateTimeFormatter.parseDateToMillis(ep.publishDate)
                        millis?.let { it > viewedCutoff } ?: true
                    }
                } else 0

                // Update podcast metadata
                val updatedPodcast = podcast.copy(
                    episodeCount = response.episodes.size,
                    description = if (response.description.isNotEmpty()) response.description else podcast.description,
                    newEpisodeCount = if (skipCache) podcast.newEpisodeCount + unseenCount else podcast.newEpisodeCount
                )
                podcastDao.insertPodcast(updatedPodcast)
                
                // Combine newly inserted episodes with any existing ones from before
                val existingEpisodes = podcastDao.getEpisodesForPodcast(podcast.id).filterNot { ep ->
                    newEpisodes.any { it.id == ep.id }
                }
                val allEpisodes = (newEpisodes + existingEpisodes).sortedByDescending { it.publishDate }
                Log.d("PodcastRepository", "Total episodes to return: ${allEpisodes.size} (${newEpisodes.size} new + ${existingEpisodes.size} existing)")
                val podcastWithEpisodes = PodcastWithEpisodes(updatedPodcast, allEpisodes)
                Log.d("PodcastRepository", "Emitting PodcastWithEpisodes: podcast=${podcastWithEpisodes.podcast.title}, episodes=${podcastWithEpisodes.episodes.size}")
                emit(Result.success(podcastWithEpisodes))
            },
            onFailure = { exception ->
                Log.e("PodcastRepository", "Error fetching podcast episodes", exception)
                emit(Result.failure(exception))
            }
        )
    }.catch { e -> 
        Log.e("PodcastRepository", "Exception in fetchAndUpdateEpisodes", e)
        emit(Result.failure<PodcastWithEpisodes?>(e)) 
    }
}
