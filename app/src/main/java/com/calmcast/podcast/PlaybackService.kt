package com.calmcast.podcast

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.Player
import androidx.media3.common.PlaybackException
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

sealed class PlaybackError {
    data class NetworkError(val message: String) : PlaybackError()
    data class GeneralError(val message: String) : PlaybackError()
}

class PlaybackService : MediaSessionService() {
    private var mediaSession: MediaSession? = null
    private var audioManager: AudioManager? = null
    private var audioFocusRequest: AudioFocusRequest? = null

    companion object {
        private const val NOTIFICATION_ID = 123
        private const val CHANNEL_ID = "playback_channel"
        private const val TAG = "PlaybackService"
        
        private var errorCallback: ((PlaybackException) -> Unit)? = null
        
        fun setErrorCallback(callback: ((PlaybackException) -> Unit)?) {
            errorCallback = callback
        }
    }

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        
        val player = ExoPlayer.Builder(this).build()
        
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (isPlaying) {
                    requestAudioFocus()
                } else {
                    abandonAudioFocus()
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                errorCallback?.invoke(error)
                super.onPlayerError(error)
            }
        })
        
        mediaSession = MediaSession.Builder(this, player).build()

        val notificationProvider = DefaultMediaNotificationProvider.Builder(this)
            .setChannelId(CHANNEL_ID)
            .setNotificationId(NOTIFICATION_ID)
            .build()

        setMediaNotificationProvider(notificationProvider)
    }

    private fun requestAudioFocus() {
        if (audioManager == null) return
        
        val audioAttributes = android.media.AudioAttributes.Builder()
            .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
            .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SPEECH)
            .build()

        audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(audioAttributes)
            .setOnAudioFocusChangeListener { focusChange ->
                when (focusChange) {
                    AudioManager.AUDIOFOCUS_LOSS -> {
                        Log.d(TAG, "Audio focus lost")
                        mediaSession?.player?.pause()
                    }
                    AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                        Log.d(TAG, "Audio focus lost temporarily")
                        mediaSession?.player?.pause()
                    }
                    AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                        Log.d(TAG, "Audio focus lost (can duck)")
                        // For podcast, we pause rather than duck
                        mediaSession?.player?.pause()
                    }
                    AudioManager.AUDIOFOCUS_GAIN -> {
                        Log.d(TAG, "Audio focus gained")
                        // We do not auto-resume to respect user intent
                    }
                }
            }
            .build()

        val result = audioManager?.requestAudioFocus(audioFocusRequest!!)
        if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            Log.d(TAG, "Audio focus request granted")
        } else {
            Log.w(TAG, "Audio focus request denied")
        }
    }

    private fun abandonAudioFocus() {
        if (audioManager != null && audioFocusRequest != null) {
            val result = audioManager?.abandonAudioFocusRequest(audioFocusRequest!!)
            if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                Log.d(TAG, "Audio focus abandoned")
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Playback"
            val descriptionText = "Podcast playback controls"
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        mediaSession?.player?.let { player ->
            if (!player.playWhenReady || player.mediaItemCount == 0) {
                stopSelf()
            }
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? =
        mediaSession

    override fun onDestroy() {
        abandonAudioFocus()
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }
}