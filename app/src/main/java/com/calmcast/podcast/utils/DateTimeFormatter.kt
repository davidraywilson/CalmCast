package com.calmcast.podcast.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

object DateTimeFormatter {

    /**
     * Formats a duration in seconds to a human-readable string (HH:MM:SS or MM:SS)
     * @param seconds Duration in seconds
     * @return Formatted duration string
     */
    fun formatDuration(seconds: Long): String {
        if (seconds < 0) return "00:00"

        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val secs = seconds % 60

        return if (hours > 0) {
            String.format("%02d:%02d:%02d", hours, minutes, secs)
        } else {
            String.format("%02d:%02d", minutes, secs)
        }
    }

    /**
     * Formats a duration string (e.g., "3600" or "PT1H") to a readable format
     * @param durationStr Duration as string (can be seconds or ISO 8601 format)
     * @return Formatted duration string
     */
    fun formatDurationFromString(durationStr: String?): String {
        if (durationStr.isNullOrBlank()) return "Unknown"

        return try {
            // Try parsing as seconds first
            val seconds = durationStr.toLongOrNull()
            if (seconds != null) {
                formatDuration(seconds)
            } else {
                // Try parsing ISO 8601 duration format (PT1H30M45S)
                formatISO8601Duration(durationStr)
            }
        } catch (e: Exception) {
            durationStr
        }
    }

    fun parseDuration(durationStr: String?): Long? {
        if (durationStr.isNullOrBlank()) return null

        return durationStr.toLongOrNull() ?: parseISO8601Duration(durationStr)
    }

    private fun parseISO8601Duration(iso8601Duration: String): Long? {
        try {
            val pattern = """PT(?:(\d+)H)?(?:(\d+)M)?(?:(\d+(?:\.\d+)?)S)?""".toRegex()
            val matchResult = pattern.find(iso8601Duration) ?: return null

            val hours = matchResult.groupValues[1].toLongOrNull() ?: 0
            val minutes = matchResult.groupValues[2].toLongOrNull() ?: 0
            val seconds = matchResult.groupValues[3].toDoubleOrNull()?.toLong() ?: 0

            return hours * 3600 + minutes * 60 + seconds
        } catch (e: Exception) {
            return null
        }
    }

    /**
     * Formats an ISO 8601 duration string
     * @param iso8601Duration Duration in ISO 8601 format (e.g., "PT1H30M45S")
     * @return Formatted duration string
     */
    private fun formatISO8601Duration(iso8601Duration: String): String {
        val totalSeconds = parseISO8601Duration(iso8601Duration) ?: return iso8601Duration
        return formatDuration(totalSeconds)
    }

    /**
     * Formats a publish date to a human-readable string
     * Handles multiple date formats and returns relative dates for recent episodes
     * @param dateStr Date string in various formats (can be Unix timestamp or formatted date)
     * @return Formatted date string (e.g., "2 days ago", "Jan 15, 2024")
     */
    fun formatPublishDate(dateStr: String?): String {
        if (dateStr.isNullOrBlank()) return "Unknown date"

        return try {
            val date = parseDate(dateStr)
            val now = Date()
            val diffMs = abs(now.time - date.time)
            val diffDays = diffMs / (1000 * 60 * 60 * 24)

            when {
                diffDays == 0L -> {
                    val diffHours = diffMs / (1000 * 60 * 60)
                    when {
                        diffHours < 1 -> "Today"
                        diffHours == 1L -> "1 hour ago"
                        else -> "$diffHours hours ago"
                    }
                }
                diffDays == 1L -> "Yesterday"
                diffDays < 7 -> "$diffDays days ago"
                diffDays < 30 -> {
                    val weeks = diffDays / 7
                    if (weeks == 1L) "1 week ago" else "$weeks weeks ago"
                }
                diffDays < 365 -> formatDateShort(date)
                else -> formatDateLong(date)
            }
        } catch (e: Exception) {
            dateStr
        }
    }

    /**
     * Formats a date to short format (e.g., "Jan 15")
     * @param date Date to format
     * @return Short date format
     */
    private fun formatDateShort(date: Date): String {
        val sdf = SimpleDateFormat("MMM d", Locale.getDefault())
        return sdf.format(date)
    }

    /**
     * Formats a date to long format (e.g., "Jan 15, 2024")
     * @param date Date to format
     * @return Long date format
     */
    private fun formatDateLong(date: Date): String {
        val sdf = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
        return sdf.format(date)
    }

    /**
     * Formats a date to detailed format with time (e.g., "Jan 15, 2024 at 2:30 PM")
     * @param date Date to format
     * @return Detailed date format with time
     */
    fun formatDateWithTime(date: Date): String {
        val sdf = SimpleDateFormat("MMM d, yyyy 'at' h:mm a", Locale.getDefault())
        return sdf.format(date)
    }

    /**
     * Formats a date to detailed format with time (e.g., "Jan 15, 2024 at 2:30 PM")
     * @param dateStr Date string to parse and format
     * @return Detailed date format with time
     */
    fun formatDateWithTimeString(dateStr: String?): String {
        if (dateStr.isNullOrBlank()) return "Unknown date"

        return try {
            val date = parseDate(dateStr)
            formatDateWithTime(date)
        } catch (e: Exception) {
            dateStr
        }
    }

    /**
     * Formats current playback position and total duration
     * @param currentPosition Current position in milliseconds
     * @param totalDuration Total duration in milliseconds
     * @return Formatted string (e.g., "2:45 / 45:30")
     */
    fun formatPlaybackTime(currentPosition: Long, totalDuration: Long): String {
        val currentSeconds = currentPosition / 1000
        val totalSeconds = totalDuration / 1000

        return "${formatDuration(currentSeconds)} / ${formatDuration(totalSeconds)}"
    }

    /**
     * Formats remaining time for playback
     * @param currentPosition Current position in milliseconds
     * @param totalDuration Total duration in milliseconds
     * @return Formatted remaining time (e.g., "-42:45")
     */
    fun formatRemainingTime(currentPosition: Long, totalDuration: Long): String {
        val remainingMs = totalDuration - currentPosition
        val remainingSeconds = remainingMs / 1000

        return "-${formatDuration(remainingSeconds)}"
    }

    /**
     * Tries to parse various date formats including Unix timestamps
     * @param dateStr Date string to parse (can be Unix timestamp in seconds or formatted date)
     * @return Parsed Date object
     */
    private fun parseDate(dateStr: String): Date {
        // First, try to parse as Unix timestamp (seconds since epoch)
        try {
            val timestamp = dateStr.toLong()
            // Check if it's a reasonable Unix timestamp (between 2000 and 2100)
            if (timestamp in 946684800..4102444800L) {
                return Date(timestamp * 1000) // Convert seconds to milliseconds
            }
        } catch (e: NumberFormatException) {
            // Not a Unix timestamp, continue to try other formats
        }

        // List of common date formats used by podcast APIs
        val formats = listOf(
            "yyyy-MM-dd'T'HH:mm:ss'Z'",           // ISO 8601 (UTC)
            "yyyy-MM-dd'T'HH:mm:ssX",              // ISO 8601 with timezone
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",        // ISO 8601 with milliseconds
            "yyyy-MM-dd'T'HH:mm:ss.SSSZ",          // ISO 8601 with milliseconds and timezone
            "EEE, dd MMM yyyy HH:mm:ss Z",         // RFC 2822 (common in RSS)
            "dd MMM yyyy HH:mm:ss Z",              // Alternative RFC format
            "yyyy-MM-dd HH:mm:ss",                 // Simple format
            "yyyy-MM-dd",                          // Date only
            "MMM dd, yyyy",                        // Short format
            "MMMM dd, yyyy"                        // Long format
        )

        var lastException: Exception? = null

        for (format in formats) {
            try {
                val sdf = SimpleDateFormat(format, Locale.getDefault())
                sdf.isLenient = false
                return sdf.parse(dateStr) ?: Date()
            } catch (e: Exception) {
                lastException = e
                continue
            }
        }

        // If all formats fail, throw the last exception
        throw lastException ?: Exception("Unable to parse date: $dateStr")
    }
}
