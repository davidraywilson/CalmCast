package com.calmcast.podcast.api

import com.calmcast.podcast.BuildConfig

object ApiConfig {
    // Taddy GraphQL API endpoint
    const val TADDY_API_URL = "https://api.taddy.org"

    // Taddy API Credentials
    // Get your API credentials from: https://taddy.org/dashboard
    // Credentials are loaded from .env file at build time via BuildConfig
    const val TADDY_USER_ID = "3624"
    val TADDY_API_KEY = BuildConfig.TADDY_API_KEY

    // Legacy Podchaser config (kept for reference/fallback)
    const val PODCHASER_API_URL = "https://api.podchaser.com/graphql"
    const val PODCHASER_CLIENT_ID = "a04956e7-37e7-4b65-a5a1-23509741cb5d"
    const val PODCHASER_CLIENT_SECRET = "6BwaM3IjCcmY8ppGL6jEhbAd3WUvswWf52DtlhtF"
    const val PODCHASER_ACCESS_TOKEN = ""

    // Taddy GraphQL Query Templates
    object TaddyQueries {
        // Query to get most popular podcasts
        val getPopularPodcasts = """
            {
              mostPopularPodcasts(limitPerPage: %d, page: 1) {
                podcastSeries {
                  uuid
                  name
                  description
                  imageUrl
                  totalEpisodesCount
                  itunesInfo {
                    publisherName
                  }
                }
              }
            }
        """.trimIndent()

        // Query to search podcasts and episodes
        val searchPodcasts = """
            {
              search(term: "%s", filterForTypes: PODCASTSERIES, limitPerPage: %d, page: 1, sortBy: POPULARITY) {
                podcastSeries {
                  uuid
                  name
                  description
                  imageUrl
                  totalEpisodesCount
                  itunesInfo {
                    publisherName
                  }
                }
              }
            }
        """.trimIndent()

        // Query to get podcast details with episodes
        val getPodcastDetails = """
            {
              getPodcastSeries(uuid: "%s") {
                uuid
                name
                description
                imageUrl
                totalEpisodesCount
                itunesInfo {
                  publisherName
                }
                episodes(limitPerPage: 40, page: 1) {
                  uuid
                  name
                  description
                  datePublished
                  duration
                  audioUrl
                }
              }
            }
        """.trimIndent()
    }

    // Legacy Podchaser Query Templates (kept for reference)
    object PodchaserQueries {
        val requestAccessToken = """
            mutation {
                requestAccessToken(input: {
                    grant_type: CLIENT_CREDENTIALS
                    client_id: "%s"
                    client_secret: "%s"
                }) {
                    access_token
                    token_type
                    expires_in
                }
            }
        """.trimIndent()

        // Query to get featured podcasts
        val getFeaturedPodcasts = """
            query GetPodcasts(${'$'}limit: Int = 10, ${'$'}offset: Int = 0) {
                podcasts(first: ${'$'}limit, page: ${'$'}offset) {
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

        // Query to search podcasts
        val searchPodcasts = """
            query SearchPodcasts(${'$'}searchTerm: String!, ${'$'}limit: Int = 20, ${'$'}offset: Int = 0) {
                podcasts(searchTerm: ${'$'}searchTerm, first: ${'$'}limit, page: ${'$'}offset) {
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

        // Query to get podcast details with episodes
        val getPodcastDetails = """
            query GetPodcastDetails(${'$'}id: ID!) {
                podcast(identifier: {podchasterId: ${'$'}id}) {
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
    }
}
