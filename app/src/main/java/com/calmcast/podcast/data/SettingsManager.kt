package com.calmcast.podcast.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import androidx.core.content.edit

enum class DownloadLocation(val value: String) {
    INTERNAL("internal"),
    EXTERNAL("external")
}

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

    private val _sleepTimerEnabled = MutableStateFlow(isSleepTimerEnabledSync())
    val sleepTimerEnabled = _sleepTimerEnabled.asStateFlow()

    private val _sleepTimerMinutes = MutableStateFlow(getSleepTimerMinutesSync())
    val sleepTimerMinutes = _sleepTimerMinutes.asStateFlow()

    private val _isSleepTimerActive = MutableStateFlow(isSleepTimerActiveSync())
    val isSleepTimerActive = _isSleepTimerActive.asStateFlow()

    private val _downloadLocation = MutableStateFlow(getDownloadLocationSync())
    val downloadLocation = _downloadLocation.asStateFlow()

    private val _playbackSpeed = MutableStateFlow(getPlaybackSpeedSync())
    val playbackSpeed = _playbackSpeed.asStateFlow()

    private val _isAutoPlayNextEpisodeEnabled = MutableStateFlow(isAutoPlayNextEpisodeEnabledSync())
    val isAutoPlayNextEpisodeEnabled = _isAutoPlayNextEpisodeEnabled.asStateFlow()

    private val _isWiFiOnlyDownloadsEnabled = MutableStateFlow(isWiFiOnlyDownloadsEnabledSync())
    val isWiFiOnlyDownloadsEnabled = _isWiFiOnlyDownloadsEnabled.asStateFlow()


    fun setPictureInPictureEnabled(enabled: Boolean) {
        sharedPreferences.edit { putBoolean(KEY_PIP, enabled) }
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
        sharedPreferences.edit { putBoolean(KEY_AUTO_DOWNLOAD, enabled) }
        _autoDownloadEnabled.value = enabled
    }

    fun isAutoDownloadEnabled(): Boolean {
        return sharedPreferences.getBoolean(KEY_AUTO_DOWNLOAD, false)
    }

    fun setKeepScreenOnEnabled(enabled: Boolean) {
        sharedPreferences.edit { putBoolean(KEY_KEEP_SCREEN_ON, enabled) }
        _isKeepScreenOnEnabled.value = enabled
    }

    fun isKeepScreenOnEnabledSync(): Boolean {
        return sharedPreferences.getBoolean(KEY_KEEP_SCREEN_ON, false)
    }

    fun setRemoveHorizontalDividers(enabled: Boolean) {
        sharedPreferences.edit { putBoolean(KEY_REMOVE_DIVIDERS, enabled) }
        _removeHorizontalDividers.value = enabled
    }

    fun removeHorizontalDividersSync(): Boolean {
        return sharedPreferences.getBoolean(KEY_REMOVE_DIVIDERS, false)
    }

    fun setSleepTimerEnabled(enabled: Boolean) {
        sharedPreferences.edit { putBoolean(KEY_SLEEP_TIMER_ENABLED, enabled) }
        _sleepTimerEnabled.value = enabled
    }

    fun isSleepTimerEnabledSync(): Boolean {
        return sharedPreferences.getBoolean(KEY_SLEEP_TIMER_ENABLED, false)
    }

    fun setSleepTimerMinutes(minutes: Int) {
        sharedPreferences.edit { putInt(KEY_SLEEP_TIMER_MINUTES, minutes) }
        _sleepTimerMinutes.value = minutes
    }

    fun getSleepTimerMinutesSync(): Int {
        return sharedPreferences.getInt(KEY_SLEEP_TIMER_MINUTES, 30)
    }

    fun setSleepTimerActive(active: Boolean) {
        sharedPreferences.edit { putBoolean(KEY_SLEEP_TIMER_ACTIVE, active) }
        _isSleepTimerActive.value = active
    }

    fun isSleepTimerActiveSync(): Boolean {
        return sharedPreferences.getBoolean(KEY_SLEEP_TIMER_ACTIVE, false)
    }

    fun setDownloadLocation(location: DownloadLocation) {
        sharedPreferences.edit { putString(KEY_DOWNLOAD_LOCATION, location.value) }
        _downloadLocation.value = location
    }

    fun getDownloadLocationSync(): DownloadLocation {
        val value = sharedPreferences.getString(KEY_DOWNLOAD_LOCATION, DownloadLocation.INTERNAL.value)
        return if (value == DownloadLocation.EXTERNAL.value) DownloadLocation.EXTERNAL else DownloadLocation.INTERNAL
    }

    fun setPlaybackSpeed(speed: Float) {
        sharedPreferences.edit { putFloat(KEY_PLAYBACK_SPEED, speed) }
        _playbackSpeed.value = speed
    }

    fun getPlaybackSpeedSync(): Float {
        return sharedPreferences.getFloat(KEY_PLAYBACK_SPEED, 1f)
    }

    fun setAutoPlayNextEpisodeEnabled(enabled: Boolean) {
        sharedPreferences.edit { putBoolean(KEY_AUTOPLAY_NEXT_EPISODE, enabled) }
        _isAutoPlayNextEpisodeEnabled.value = enabled
    }

    fun isAutoPlayNextEpisodeEnabledSync(): Boolean {
        return sharedPreferences.getBoolean(KEY_AUTOPLAY_NEXT_EPISODE, true)
    }

    fun setWiFiOnlyDownloadsEnabled(enabled: Boolean) {
        sharedPreferences.edit { putBoolean(KEY_WIFI_ONLY_DOWNLOADS, enabled) }
        _isWiFiOnlyDownloadsEnabled.value = enabled
    }

    fun isWiFiOnlyDownloadsEnabledSync(): Boolean {
        return sharedPreferences.getBoolean(KEY_WIFI_ONLY_DOWNLOADS, true)
    }


    companion object {
        private const val PREFS_NAME = "calmcast_settings"
        private const val KEY_PIP = "pip_enabled"
        private const val KEY_SKIP_SECONDS = "skip_seconds"
        private const val KEY_AUTO_DOWNLOAD = "auto_download_enabled"
        private const val KEY_KEEP_SCREEN_ON = "keep_screen_on_enabled"
        private const val KEY_REMOVE_DIVIDERS = "remove_horizontal_dividers"
        private const val KEY_SLEEP_TIMER_ENABLED = "sleep_timer_enabled"
        private const val KEY_SLEEP_TIMER_MINUTES = "sleep_timer_minutes"
        private const val KEY_SLEEP_TIMER_ACTIVE = "sleep_timer_active"
        private const val KEY_DOWNLOAD_LOCATION = "download_location"
        private const val KEY_PLAYBACK_SPEED = "playback_speed"
        private const val KEY_AUTOPLAY_NEXT_EPISODE = "auto_play_next_episode"
        private const val KEY_WIFI_ONLY_DOWNLOADS = "wifi_only_downloads"

        val PLAYBACK_SPEEDS = listOf(0.5f, 0.75f, 1f, 1.5f, 2f, 2.5f)
        val SLEEP_TIMER_OPTIONS = listOf(5, 10, 15, 30, 45, 60)
    }
}
