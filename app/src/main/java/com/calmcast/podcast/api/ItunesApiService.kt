package com.calmcast.podcast.api

import android.util.Log
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.StringReader
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

class ItunesApiService {
    private val client: OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(HttpLoggingInterceptor { message ->
            Log.d("OkHttp", message)
        }.apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val ITUNES_SEARCH_URL = "https://itunes.apple.com/search"

    suspend fun searchPodcasts(
        searchQuery: String,
        limit: Int = 20
    ): Result<ItunesPodcastSearchResponse> = withContext(Dispatchers.IO) {
        if (searchQuery.length < 2) {
            return@withContext Result.success(
                ItunesPodcastSearchResponse(results = emptyList())
            )
        }

        try {
            val encodedQuery = URLEncoder.encode(searchQuery, "UTF-8")
            val url = "$ITUNES_SEARCH_URL?media=podcast&term=$encodedQuery&limit=$limit"
            Log.d("ItunesApi", "Searching iTunes with URL: $url")
            
            val request = Request.Builder()
                .url(url)
                .get()
                .build()

            val response = client.newCall(request).execute()

            if (!response.isSuccessful) {
                throw Exception("API request failed with code ${response.code}: ${response.message}")
            }

            val responseBody = response.body?.string()
                ?: throw Exception("Empty response body")

            val jsonObject = JsonParser.parseString(responseBody).asJsonObject
            val resultsArray = jsonObject.getAsJsonArray("results") ?: return@withContext Result.success(
                ItunesPodcastSearchResponse(results = emptyList())
            )
            
            Log.d("ItunesApi", "Found ${resultsArray.size()} results")

            val podcasts = resultsArray.mapNotNull { element ->
                val obj = element.asJsonObject
                val trackId = obj.get("trackId")?.asLong
                val trackName = obj.get("trackName")?.asString
                val artistName = obj.get("artistName")?.asString
                val summary = obj.get("summary")?.asString
                val artworkUrl = obj.get("artworkUrl600")?.asString
                val feedUrl = obj.get("feedUrl")?.asString

                if (trackId != null && trackName != null && feedUrl != null) {
                    ItunesPodcastResult(
                        id = trackId.toString(),
                        title = trackName,
                        author = artistName ?: "Unknown",
                        description = summary ?: "",
                        imageUrl = artworkUrl,
                        feedUrl = feedUrl
                    )
                } else {
                    if (trackName != null) {
                        Log.d("ItunesApi", "Skipping result: $trackName (feedUrl: $feedUrl)")
                    }
                    null
                }
            }
            
            Log.d("ItunesApi", "Returning ${podcasts.size} podcasts with feedUrl")
            Result.success(ItunesPodcastSearchResponse(results = podcasts))
        } catch (e: Exception) {
            Log.e("ItunesApi", "Error searching podcasts", e)
            Result.failure(e)
        }
    }

    suspend fun getPodcastDetails(podcastId: String, feedUrl: String?): Result<ItunesPodcastDetailsResponse> =
        withContext(Dispatchers.IO) {
            try {
                if (feedUrl == null) {
                    Log.e("ItunesApi", "Feed URL is null for podcast $podcastId")
                    throw Exception("Feed URL required for podcast details")
                }

                Log.d("ItunesApi", "Fetching podcast details from feedUrl: $feedUrl")
                val request = Request.Builder()
                    .url(feedUrl)
                    .get()
                    .build()

                val response = client.newCall(request).execute()

                if (!response.isSuccessful) {
                    Log.e("ItunesApi", "Failed to fetch RSS: ${response.code} ${response.message}")
                    when (response.code) {
                        404 -> throw FeedNotFoundException("Podcast feed not found (404)")
                        410 -> throw FeedGoneException("Podcast feed is no longer available (410)")
                        else -> throw Exception("Failed to fetch RSS feed: ${response.code}")
                    }
                }

                val feedBody = response.body?.string()
                    ?: throw Exception("Empty feed response")

                Log.d("ItunesApi", "Parsing RSS feed, size: ${feedBody.length} bytes")
                val parseResult = parseRssFeed(feedBody)
                Log.d("ItunesApi", "Parsed ${parseResult.episodes.size} episodes from RSS feed")
                Result.success(ItunesPodcastDetailsResponse(episodes = parseResult.episodes, description = parseResult.description))
            } catch (e: Exception) {
                Log.e("ItunesApi", "Error fetching podcast details for $podcastId", e)
                Result.failure(e)
            }
        }

    private fun parseRssFeed(feedXml: String): RssFeedParseResult {
        val episodes = mutableListOf<ItunesEpisodeResult>()
        var podcastDescription = ""

        try {
            val factory = XmlPullParserFactory.newInstance()
            factory.isNamespaceAware = true
            val parser = factory.newPullParser()
            parser.setInput(StringReader(feedXml))

            var eventType = parser.eventType
            var currentTitle = ""
            var currentDescription = ""
            var currentPubDate = ""
            var currentDuration = ""
            var currentAudioUrl = ""
            var inItem = false
            var inChannel = false
            var itemCount = 0

            while (eventType != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        val tagName = parser.name.lowercase()
                        when (tagName) {
                            "channel" -> {
                                inChannel = true
                            }
                            "item" -> {
                                inItem = true
                                itemCount++
                            }
                            "title" -> {
                                if (inItem && currentTitle.isEmpty()) {
                                    currentTitle = parser.nextText()
                                }
                            }
                            "description", "summary" -> {
                                if (inItem && currentDescription.isEmpty()) {
                                    currentDescription = parser.nextText()
                                } else if (inChannel && !inItem && podcastDescription.isEmpty()) {
                                    podcastDescription = parser.nextText()
                                }
                            }
                            "pubdate", "pubDate" -> {
                                if (inItem && currentPubDate.isEmpty()) {
                                    currentPubDate = parser.nextText()
                                }
                            }
                            "duration" -> {
                                if (inItem && currentDuration.isEmpty()) {
                                    currentDuration = parser.nextText()
                                }
                            }
                            "enclosure" -> {
                                if (inItem && currentAudioUrl.isEmpty()) {
                                    val url = parser.getAttributeValue(null, "url")
                                    val type = parser.getAttributeValue(null, "type")
                                    // Accept enclosures that are audio type or contain audio file extensions
                                    if (url != null && (type?.contains("audio", ignoreCase = true) == true ||
                                            url.contains(".mp3", ignoreCase = true) ||
                                            url.contains(".m4a", ignoreCase = true))) {
                                        currentAudioUrl = url
                                    }
                                }
                            }
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        val tagName = parser.name.lowercase()
                        when (tagName) {
                            "channel" -> {
                                inChannel = false
                            }
                            "item" -> {
                                if (inItem) {
                                    if (currentTitle.isNotEmpty() && currentAudioUrl.isNotEmpty()) {
                                        Log.d("ItunesApi", "Adding episode: $currentTitle")
                                        episodes.add(
                                            ItunesEpisodeResult(
                                                title = currentTitle,
                                                description = currentDescription,
                                                pubDate = currentPubDate,
                                                duration = currentDuration,
                                                audioUrl = currentAudioUrl
                                            )
                                        )
                                    }
                                    inItem = false
                                    currentTitle = ""
                                    currentDescription = ""
                                    currentPubDate = ""
                                    currentDuration = ""
                                    currentAudioUrl = ""
                                }
                            }
                        }
                    }
                }
                eventType = parser.next()
            }
            Log.d("ItunesApi", "Parsed $itemCount items, found ${episodes.size} valid episodes")
        } catch (e: Exception) {
            Log.e("ItunesApi", "Error parsing RSS feed", e)
        }

        return RssFeedParseResult(episodes = episodes, description = podcastDescription)
    }
}

data class ItunesPodcastSearchResponse(
    val results: List<ItunesPodcastResult>
)

data class ItunesPodcastResult(
    val id: String,
    val title: String,
    val author: String,
    val description: String,
    val imageUrl: String?,
    val feedUrl: String?
)

data class ItunesPodcastDetailsResponse(
    val episodes: List<ItunesEpisodeResult>,
    val description: String = ""
)

data class ItunesEpisodeResult(
    val title: String,
    val description: String,
    val pubDate: String,
    val duration: String,
    val audioUrl: String
)

data class RssFeedParseResult(
    val episodes: List<ItunesEpisodeResult>,
    val description: String
)
