package com.calmcast.podcast.utils

/**
 * Extension functions for convenient time and duration formatting
 */

/**
 * Formats a duration in seconds to HH:MM:SS or MM:SS format
 */
fun Long.formatAsPlaybackTime(): String {
    return DateTimeFormatter.formatDuration(this)
}

/**
 * Formats a duration in milliseconds to HH:MM:SS or MM:SS format
 */
fun Long.formatAsPlaybackTimeMs(): String {
    return DateTimeFormatter.formatDuration(this / 1000)
}

/**
 * Formats a publish date string to a human-readable format
 */
fun String?.formatAsPublishDate(): String {
    return DateTimeFormatter.formatPublishDate(this)
}

/**
 * Formats a duration string (seconds or ISO 8601) to readable format
 */
fun String?.formatAsDuration(): String {
    return DateTimeFormatter.formatDurationFromString(this)
}

/**
 * Formats current position and total duration for display
 * @receiver current position in milliseconds
 * @param totalDuration total duration in milliseconds
 * @return formatted string like "2:45 / 45:30"
 */
fun Long.formatPlaybackProgress(totalDuration: Long): String {
    return DateTimeFormatter.formatPlaybackTime(this, totalDuration)
}

/**
 * Formats remaining time for playback
 * @receiver current position in milliseconds
 * @param totalDuration total duration in milliseconds
 * @return formatted remaining time like "-42:45"
 */
fun Long.formatRemainingPlaybackTime(totalDuration: Long): String {
    return DateTimeFormatter.formatRemainingTime(this, totalDuration)
}

/**
 * Checks if a duration string is valid
 */
fun String?.isValidDuration(): Boolean {
    return !this.isNullOrBlank() && this != "Unknown"
}

/**
 * Checks if a publish date string is valid
 */
fun String?.isValidPublishDate(): Boolean {
    return !this.isNullOrBlank() && this != "Unknown date"
}