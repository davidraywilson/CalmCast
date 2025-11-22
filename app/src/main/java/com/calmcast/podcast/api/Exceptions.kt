package com.calmcast.podcast.api

/**
 * Exception thrown when a podcast feed returns a 404 Not Found error
 */
class FeedNotFoundException(message: String = "Podcast feed not found") : Exception(message)

/**
 * Exception thrown when a podcast feed returns a 410 Gone error
 */
class FeedGoneException(message: String = "Podcast feed no longer available") : Exception(message)
