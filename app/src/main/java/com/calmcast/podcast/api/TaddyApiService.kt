package com.calmcast.podcast.api

import android.util.Log
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit

class TaddyApiService(
    private val apiKey: String = ApiConfig.TADDY_API_KEY,
    private val userId: String = ApiConfig.TADDY_USER_ID
) {
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

    private val mediaType = "application/json".toMediaType()

    /**
     * Executes a GraphQL query against the Taddy API.
     * Taddy uses simple API key authentication via headers.
     */
    private suspend fun executeGraphQLQuery(query: String): TaddyPodsResponse =
        withContext(Dispatchers.IO) {
            val requestBody = JsonObject().apply {
                addProperty("query", query)
            }.toString().toRequestBody(mediaType)

            val request = Request.Builder()
                .url(ApiConfig.TADDY_API_URL)
                .addHeader("Content-Type", "application/json")
                .addHeader("X-API-KEY", apiKey)
                .addHeader("X-USER-ID", userId)
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()

            if (!response.isSuccessful) {
                throw Exception("API request failed with code ${response.code}: ${response.message}")
            }

            val responseBody = response.body?.string()
                ?: throw Exception("Empty response body")

            parseGraphQLResponse(responseBody)
        }

    suspend fun getPopularPodcasts(limit: Int = 10): Result<TaddyPodsResponse> =
        withContext(Dispatchers.IO) {
            try {
                val query = """
                    {
                      mostPopularPodcasts(limitPerPage: $limit, page: 1) {
                        podcastSeries {
                          uuid
                          name
                          description
                          imageUrl
                          totalEpisodesCount
                          itunesInfo {
                            uuid
                            publisherName
                          }
                        }
                      }
                    }
                """.trimIndent()

                val response = executeGraphQLQuery(query)
                Result.success(response)
            } catch (e: Exception) {
                Log.e("TaddyApi", "Error fetching popular podcasts", e)
                Result.failure(e)
            }
        }

    suspend fun searchPodcasts(
        searchQuery: String,
        limit: Int = 20
    ): Result<TaddyPodsResponse> = withContext(Dispatchers.IO) {
        if (searchQuery.length < 2) {
            return@withContext Result.success(
                TaddyPodsResponse(
                    podcasts = TaddyPodcastsContainer(
                        data = emptyList()
                    )
                )
            )
        }
        try {
            // Properly escape the search query to prevent GraphQL syntax errors
            val escapedQuery = escapeGraphQLString(searchQuery)

            val graphqlQuery = """
                {
                  search(term: "$escapedQuery", filterForTypes: PODCASTSERIES, limitPerPage: $limit, page: 1, sortBy: POPULARITY) {
                    searchId
                    podcastSeries {
                      uuid
                      name
                      description
                      imageUrl
                      totalEpisodesCount
                      itunesInfo {
                        uuid
                        publisherName
                      }
                    }
                  }
                }
            """.trimIndent()

            val response = executeGraphQLQuery(graphqlQuery)
            Result.success(response)
        } catch (e: Exception) {
            Log.e("TaddyApi", "Error searching podcasts", e)
            Result.failure(e)
        }
    }

    suspend fun getPodcastDetails(podcastId: String): Result<TaddyPodcastDetailsResponse> =
        withContext(Dispatchers.IO) {
            try {
                // Properly escape the podcast ID to prevent GraphQL syntax errors
                val escapedId = escapeGraphQLString(podcastId)

                val query = """
                    {
                      getPodcastSeries(uuid: "$escapedId") {
                        uuid
                        name
                        description
                        imageUrl
                        totalEpisodesCount
                        itunesInfo {
                          uuid
                          publisherName
                        }
                        episodes(limitPerPage: 25, page: 1) {
                          uuid
                          name
                          datePublished
                          duration
                          audioUrl
                        }
                      }
                    }
                """.trimIndent()

                val requestBody = JsonObject().apply {
                    addProperty("query", query)
                }.toString().toRequestBody(mediaType)

                val request = Request.Builder()
                    .url(ApiConfig.TADDY_API_URL)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("X-API-KEY", apiKey)
                    .addHeader("X-USER-ID", userId)
                    .post(requestBody)
                    .build()

                val response = client.newCall(request).execute()

                if (!response.isSuccessful) {
                    throw Exception("API request failed with code ${response.code}: ${response.message}")
                }

                val responseBody = response.body?.string()
                    ?: throw Exception("Empty response body")

                val jsonObject = JsonParser.parseString(responseBody).asJsonObject

                // Check for GraphQL errors
                if (jsonObject.has("errors")) {
                    val errors = jsonObject.getAsJsonArray("errors")
                    val errorMessages = errors.map { error ->
                        val errorObj = error.asJsonObject
                        val message = errorObj.get("message")?.asString ?: "Unknown error"
                        val locations = errorObj.getAsJsonArray("locations")
                        val locInfo = if (locations != null && locations.size() > 0) {
                            val loc = locations[0].asJsonObject
                            " at line ${loc.get("line")?.asInt}, column ${loc.get("column")?.asInt}"
                        } else ""
                        "$message$locInfo"
                    }
                    val fullError = errorMessages.joinToString("; ")
                    Log.e("TaddyApi", "GraphQL Error: $fullError")
                    throw Exception("GraphQL Error: $fullError")
                }

                val data = jsonObject.getAsJsonObject("data")
                    ?: throw Exception("No data in response")

                val podcastData = data.getAsJsonObject("getPodcastSeries")
                    ?: throw Exception("No podcast data in response")

                val episodesArray = podcastData.getAsJsonArray("episodes")
                val episodesList = episodesArray?.map { element ->
                    val obj = element.asJsonObject
                    TaddyEpisodeData(
                        id = obj.get("uuid")?.asString ?: "",
                        title = obj.get("name")?.asString ?: "",
                        publishDate = obj.get("datePublished")?.asString ?: "",
                        duration = obj.get("duration")?.asString ?: "",
                        audioUrl = obj.get("audioUrl")?.asString ?: ""
                    )
                } ?: emptyList()

                val itunesInfo = podcastData.getAsJsonObject("itunesInfo")
                val publisherName = itunesInfo?.get("publisherName")?.asString ?: ""

                val podcast = TaddyPodcastData(
                    id = podcastData.get("uuid")?.asString ?: "",
                    title = podcastData.get("name")?.asString ?: "",
                    author = publisherName,
                    description = podcastData.get("description")?.asString ?: "",
                    imageUrl = podcastData.get("imageUrl")?.asString ?: "",
                    episodeCount = podcastData.get("totalEpisodesCount")?.asInt ?: 0,
                    episodes = episodesList
                )

                Result.success(TaddyPodcastDetailsResponse(podcast = podcast))
            } catch (e: Exception) {
                Log.e("TaddyApi", "Error fetching podcast details", e)
                Result.failure(e)
            }
        }

    private fun parseGraphQLResponse(responseBody: String): TaddyPodsResponse {
        return try {
            val jsonObject = JsonParser.parseString(responseBody).asJsonObject

            // Check for GraphQL errors in the response
            if (jsonObject.has("errors")) {
                val errors = jsonObject.getAsJsonArray("errors")
                val errorMessages = errors.map { error ->
                    val errorObj = error.asJsonObject
                    val message = errorObj.get("message")?.asString ?: "Unknown error"
                    val locations = errorObj.getAsJsonArray("locations")
                    val locInfo = if (locations != null && locations.size() > 0) {
                        val loc = locations[0].asJsonObject
                        " at line ${loc.get("line")?.asInt}, column ${loc.get("column")?.asInt}"
                    } else ""
                    "$message$locInfo"
                }
                val fullError = errorMessages.joinToString("; ")
                Log.e("TaddyApi", "GraphQL Error: $fullError")
                throw Exception("GraphQL Error: $fullError")
            }

            val data = jsonObject.getAsJsonObject("data")
                ?: throw Exception("No data in response")

            // Handle both mostPopularPodcasts and search endpoints
            val podcastsData = data.getAsJsonObject("mostPopularPodcasts")
                ?: data.getAsJsonObject("search")
                ?: throw Exception("No podcasts data in response")

            val podcastsArray = podcastsData.getAsJsonArray("podcastSeries")
                ?: throw Exception("No podcastSeries array in response")

            val podcasts = podcastsArray.mapNotNull { element ->
                val obj = element.asJsonObject
                val itunesInfo = obj.getAsJsonObject("itunesInfo")
                val authorName = itunesInfo?.get("publisherName")?.asString ?: "Unknown"
                val uuid = obj.get("uuid")?.asString ?: return@mapNotNull null

                TaddyPodcastData(
                    id = uuid,
                    title = obj.get("name")?.asString ?: "",
                    author = authorName,
                    description = obj.get("description")?.asString ?: "",
                    imageUrl = obj.get("imageUrl")?.asString ?: "",
                    episodeCount = obj.get("totalEpisodesCount")?.asInt ?: 0,
                    episodes = emptyList()
                )
            }

            TaddyPodsResponse(
                podcasts = TaddyPodcastsContainer(
                    data = podcasts
                )
            )
        } catch (e: Exception) {
            Log.e("TaddyApi", "Error parsing GraphQL response: $responseBody", e)
            throw e
        }
    }

    /**
     * Escapes a string for use in a GraphQL query.
     * Handles backslashes, quotes, newlines, and other special characters.
     */
    private fun escapeGraphQLString(input: String): String {
        return input
            .replace("\\", "\\\\")  // Backslash must be escaped first
            .replace("\"", "\\\"")  // Escape double quotes
            .replace("\n", "\\n")   // Escape newlines
            .replace("\r", "\\r")   // Escape carriage returns
            .replace("\t", "\\t")   // Escape tabs
    }
}

// Data classes for Taddy API responses
data class TaddyPodsResponse(
    val podcasts: TaddyPodcastsContainer?
)

data class TaddyPodcastsContainer(
    val data: List<TaddyPodcastData>
)

data class TaddyEpisodeData(
    val id: String,
    val title: String,
    val publishDate: String,
    val duration: String,
    val audioUrl: String
)

data class TaddyPodcastData(
    val id: String,
    val title: String,
    val author: String,
    val description: String,
    val imageUrl: String,
    val episodeCount: Int,
    val episodes: List<TaddyEpisodeData> = emptyList()
)

data class TaddyPodcastDetailsResponse(
    val podcast: TaddyPodcastData?
)