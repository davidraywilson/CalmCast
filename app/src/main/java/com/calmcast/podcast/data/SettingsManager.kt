package com.calmcast.podcast.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsManager(context: Context) {

    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _isPictureInPictureEnabled = MutableStateFlow(isPictureInPictureEnabledSync())
    val isPictureInPictureEnabled = _isPictureInPictureEnabled.asStateFlow()

    private val _autoDownloadEnabled = MutableStateFlow(isAutoDownloadEnabled())
    val autoDownloadEnabled = _autoDownloadEnabled.asStateFlow()

    private val _skipSeconds = MutableStateFlow(getSkipSecondsSync())
    val skipSeconds = _skipSeconds.asStateFlow()

    private val _isKeepScreenOnEnabled = MutableStateFlow(isKeepScreenOnEnabledSync())
    val isKeepScreenOnEnabled = _isKeepScreenOnEnabled.asStateFlow()

    private val _removeHorizontalDividers = MutableStateFlow(removeHorizontalDividersSync())
    val removeHorizontalDividers = _removeHorizontalDividers.asStateFlow()

    fun setPictureInPictureEnabled(enabled: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_PIP, enabled).apply()
        _isPictureInPictureEnabled.value = enabled
    }

    fun isPictureInPictureEnabledSync(): Boolean {
        return sharedPreferences.getBoolean(KEY_PIP, true)
    }

    fun setSkipSeconds(seconds: Int) {
        sharedPreferences.edit().putInt(KEY_SKIP_SECONDS, seconds).apply()
        _skipSeconds.value = seconds
    }

    fun getSkipSecondsSync(): Int {
        return sharedPreferences.getInt(KEY_SKIP_SECONDS, 30)
    }

    fun setAutoDownloadEnabled(enabled: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_AUTO_DOWNLOAD, enabled).apply()
        _autoDownloadEnabled.value = enabled
    }

    fun isAutoDownloadEnabled(): Boolean {
        return sharedPreferences.getBoolean(KEY_AUTO_DOWNLOAD, false)
    }

    fun setKeepScreenOnEnabled(enabled: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_KEEP_SCREEN_ON, enabled).apply()
        _isKeepScreenOnEnabled.value = enabled
    }

    fun isKeepScreenOnEnabledSync(): Boolean {
        return sharedPreferences.getBoolean(KEY_KEEP_SCREEN_ON, false)
    }

    fun setRemoveHorizontalDividers(enabled: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_REMOVE_DIVIDERS, enabled).apply()
        _removeHorizontalDividers.value = enabled
    }

    fun removeHorizontalDividersSync(): Boolean {
        return sharedPreferences.getBoolean(KEY_REMOVE_DIVIDERS, false)
    }

    /**
     * Set the timestamp of the last episode refresh
     * @param timestamp Milliseconds since epoch
     */
    fun setLastEpisodeRefreshTime(timestamp: Long) {
        sharedPreferences.edit().putLong(KEY_LAST_EPISODE_REFRESH, timestamp).apply()
    }

    /**
     * Get the timestamp of the last episode refresh
     * @return Milliseconds since epoch, or 0 if never refreshed
     */
    fun getLastEpisodeRefreshTime(): Long {
        return sharedPreferences.getLong(KEY_LAST_EPISODE_REFRESH, 0L)
    }

    /**
     * Check if a daily refresh is needed (if last refresh was more than 24 hours ago)
     * @return True if refresh is needed, false otherwise
     */
    fun isDailyRefreshNeeded(): Boolean {
        val lastRefreshTime = getLastEpisodeRefreshTime()
        if (lastRefreshTime == 0L) return true // Never refreshed before
        
        val currentTime = System.currentTimeMillis()
        val timeSinceLastRefresh = currentTime - lastRefreshTime
        val twentyFourHoursInMillis = 24 * 60 * 60 * 1000L
        
        return timeSinceLastRefresh >= twentyFourHoursInMillis
    }

    companion object {
        private const val PREFS_NAME = "calmcast_settings"
        private const val KEY_PIP = "pip_enabled"
        private const val KEY_SKIP_SECONDS = "skip_seconds"
        private const val KEY_AUTO_DOWNLOAD = "auto_download_enabled"
        private const val KEY_LAST_EPISODE_REFRESH = "last_episode_refresh_time"
        private const val KEY_KEEP_SCREEN_ON = "keep_screen_on_enabled"
        private const val KEY_REMOVE_DIVIDERS = "remove_horizontal_dividers"
    }
}
