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

class PodchaserApiService(
    private val clientId: String = ApiConfig.PODCHASER_CLIENT_ID,
    private val clientSecret: String = ApiConfig.PODCHASER_CLIENT_SECRET
) {
    private var accessToken: String = ApiConfig.PODCHASER_ACCESS_TOKEN
    private var tokenExpiresAt: Long = 0

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
     * Ensures we have a valid access token before making API calls.
     * Requests a new token if necessary or if the current one has expired.
     */
    private suspend fun ensureValidToken(): String = withContext(Dispatchers.IO) {
        // If we have a valid token that hasn't expired, return it
        if (accessToken.isNotEmpty() && System.currentTimeMillis() < tokenExpiresAt) {
            return@withContext accessToken
        }

        // Request a new token with limited scope for mobile app security
        // Limited scope tokens are read-only and expire in 1 hour
        val mutation = """
            mutation {
                requestAccessToken(input: {
                    grant_type: CLIENT_CREDENTIALS
                    client_id: "${escapeGraphQLString(clientId)}"
                    client_secret: "${escapeGraphQLString(clientSecret)}"
                    limited_scope: true
                }) {
                    access_token
                    token_type
                    expires_in
                }
            }
        """.trimIndent()

        try {
            val requestBody = JsonObject().apply {
                addProperty("query", mutation)
            }.toString().toRequestBody(mediaType)

            val request = Request.Builder()
                .url(ApiConfig.PODCHASER_API_URL)
                .addHeader("Content-Type", "application/json")
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()

            if (!response.isSuccessful) {
                throw Exception("Token request failed with code ${response.code}: ${response.message}")
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
                Log.e("PodchaserApi", "Token request error: $fullError")
                throw Exception("Failed to get access token: $fullError")
            }

            val data = jsonObject.getAsJsonObject("data")
                ?: throw Exception("No data in token response")

            val tokenData = data.getAsJsonObject("requestAccessToken")
                ?: throw Exception("No token data in response")

            accessToken = tokenData.get("access_token")?.asString
                ?: throw Exception("No access token in response")

            val expiresIn = tokenData.get("expires_in")?.asLong ?: 31536000 // Default 1 year
            tokenExpiresAt = System.currentTimeMillis() + (expiresIn * 1000)

            Log.d("PodchaserApi", "Successfully obtained new access token")
            accessToken
        } catch (e: Exception) {
            Log.e("PodchaserApi", "Error requesting access token", e)
            throw e
        }
    }

    private suspend fun executeGraphQLQuery(query: String): PopcastsResponse =
        withContext(Dispatchers.IO) {
            // Ensure we have a valid token before making the query
            val token = ensureValidToken()

            val requestBody = JsonObject().apply {
                addProperty("query", query)
            }.toString().toRequestBody(mediaType)

            val request = Request.Builder()
                .url(ApiConfig.PODCHASER_API_URL)
                .addHeader("Authorization", "Bearer $token")
                .addHeader("Content-Type", "application/json")
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

    suspend fun getPopularPodcasts(limit: Int = 10, page: Int = 0): Result<PopcastsResponse> =
        withContext(Dispatchers.IO) {
            try {
                val query = """
                    query {
                        podcasts(first: $limit, page: $page) {
                            data {
                                id
                                title
                                author {
                                    name
                                }
                                description
                                imageUrl
                                numberOfEpisodes
                            }
                            paginatorInfo {
                                hasMorePages
                            }
                        }
                    }
                """.trimIndent()

                val response = executeGraphQLQuery(query)
                Result.success(response)
            } catch (e: Exception) {
                Log.e("PodchaserApi", "Error fetching popular podcasts", e)
                Result.failure(e)
            }
        }

    suspend fun searchPodcasts(
        searchQuery: String,
        limit: Int = 20,
        page: Int = 0
    ): Result<PopcastsResponse> = withContext(Dispatchers.IO) {
        if (searchQuery.length < 2) {
            return@withContext Result.success(
                PopcastsResponse(
                    podcasts = PodcastsContainer(
                        data = emptyList(),
                        pageInfo = PageInfo(hasNextPage = false)
                    )
                )
            )
        }
        try {
            // Properly escape the search query to prevent GraphQL syntax errors
            val escapedQuery = escapeGraphQLString(searchQuery)

            val graphqlQuery = """
                query {
                    podcasts(searchTerm: "$escapedQuery", first: $limit, page: $page) {
                        data {
                            id
                            title
                            author {
                                name
                            }
                            description
                            imageUrl
                            numberOfEpisodes
                        }
                        paginatorInfo {
                            hasMorePages
                        }
                    }
                }
            """.trimIndent()

            val response = executeGraphQLQuery(graphqlQuery)
            Result.success(response)
        } catch (e: Exception) {
            Log.e("PodchaserApi", "Error searching podcasts", e)
            Result.failure(e)
        }
    }

    suspend fun getPodcastDetails(podcastId: String): Result<PodcastDetailsResponse> =
        withContext(Dispatchers.IO) {
            try {
                // Properly escape the podcast ID to prevent GraphQL syntax errors
                val escapedId = escapeGraphQLString(podcastId)

                val query = """
                    query {
                        podcast(identifier: {id: "$escapedId", type: PODCHASER}) {
                            id
                            title
                            author {
                                name
                            }
                            description
                            imageUrl
                            numberOfEpisodes
                            episodes(first: 40, sort: {sortBy: AIR_DATE, direction: DESCENDING}) {
                                data {
                                    id
                                    title
                                    description
                                    airDate
                                    length
                                    audioUrl
                                }
                            }
                        }
                    }
                """.trimIndent()

                val token = ensureValidToken()

                val requestBody = JsonObject().apply {
                    addProperty("query", query)
                }.toString().toRequestBody(mediaType)

                val request = Request.Builder()
                    .url(ApiConfig.PODCHASER_API_URL)
                    .addHeader("Authorization", "Bearer $token")
                    .addHeader("Content-Type", "application/json")
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
                    Log.e("PodchaserApi", "GraphQL Error: $fullError")
                    throw Exception("GraphQL Error: $fullError")
                }

                val data = jsonObject.getAsJsonObject("data")
                    ?: throw Exception("No data in response")

                val podcastData = data.getAsJsonObject("podcast")
                    ?: throw Exception("No podcast data in response")

                val episodesObj = podcastData.getAsJsonObject("episodes")
                val episodesList = if (episodesObj != null) {
                    val episodesArray = episodesObj.getAsJsonArray("data")
                    episodesArray?.map { element ->
                        val obj = element.asJsonObject
                        EpisodeData(
                            id = obj.get("id")?.asString ?: "",
                            title = obj.get("title")?.asString ?: "",
                            description = obj.get("description")?.asString ?: "",
                            publishDate = obj.get("airDate")?.asString ?: "",
                            duration = obj.get("length")?.asString ?: "",
                            audioUrl = obj.get("audioUrl")?.asString ?: ""
                        )
                    } ?: emptyList()
                } else {
                    emptyList()
                }

                val podcast = PodcastData(
                    id = podcastData.get("id")?.asString ?: "",
                    title = podcastData.get("title")?.asString ?: "",
                    author = podcastData.getAsJsonObject("author")?.get("name")?.asString ?: "Unknown",
                    description = podcastData.get("description")?.asString ?: "",
                    imageUrl = podcastData.get("imageUrl")?.asString ?: "",
                    episodeCount = podcastData.get("numberOfEpisodes")?.asInt ?: 0,
                    episodes = episodesList
                )

                Result.success(PodcastDetailsResponse(podcast = podcast))
            } catch (e: Exception) {
                Log.e("PodchaserApi", "Error fetching podcast details", e)
                Result.failure(e)
            }
        }

    private fun parseGraphQLResponse(responseBody: String): PopcastsResponse {
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
                Log.e("PodchaserApi", "GraphQL Error: $fullError")
                throw Exception("GraphQL Error: $fullError")
            }

            val data = jsonObject.getAsJsonObject("data")
                ?: throw Exception("No data in response")

            val podcastsData = data.getAsJsonObject("podcasts")
                ?: data.getAsJsonObject("podcast")
                ?: throw Exception("No podcasts data in response")

            val podcastsArray = podcastsData.getAsJsonArray("data")
            val paginatorInfo = podcastsData.getAsJsonObject("paginatorInfo")

            val podcasts = podcastsArray.map { element ->
                val obj = element.asJsonObject
                val authorObj = obj.getAsJsonObject("author")
                val authorName = authorObj?.get("name")?.asString ?: "Unknown"

                PodcastData(
                    id = obj.get("id")?.asString ?: "",
                    title = obj.get("title")?.asString ?: "",
                    author = authorName,
                    description = obj.get("description")?.asString ?: "",
                    imageUrl = obj.get("imageUrl")?.asString ?: "",
                    episodeCount = obj.get("numberOfEpisodes")?.asInt ?: 0,
                    episodes = emptyList()
                )
            }

            PopcastsResponse(
                podcasts = PodcastsContainer(
                    data = podcasts,
                    pageInfo = PageInfo(
                        hasNextPage = paginatorInfo?.get("hasMorePages")?.asBoolean ?: false
                    )
                )
            )
        } catch (e: Exception) {
            Log.e("PodchaserApi", "Error parsing GraphQL response: $responseBody", e)
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

// Data classes for API responses
data class PopcastsResponse(
    val podcasts: PodcastsContainer?
)

data class PodcastsContainer(
    val data: List<PodcastData>,
    val pageInfo: PageInfo
)

data class PageInfo(
    val hasNextPage: Boolean
)

data class EpisodeData(
    val id: String,
    val title: String,
    val description: String,
    val publishDate: String,
    val duration: String,
    val audioUrl: String
)

data class PodcastData(
    val id: String,
    val title: String,
    val author: String,
    val description: String,
    val imageUrl: String,
    val episodeCount: Int,
    val episodes: List<EpisodeData> = emptyList()
)

data class PodcastDetailsResponse(
    val podcast: PodcastData?
)
