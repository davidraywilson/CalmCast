package com.calmcast.podcast

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
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
        
        val player = ExoPlayer.Builder(this).build()
        
        // Configure audio attributes for podcast content
        val audioAttributes = androidx.media3.common.AudioAttributes.Builder()
            .setUsage(androidx.media3.common.C.USAGE_MEDIA)
            .setContentType(androidx.media3.common.C.AUDIO_CONTENT_TYPE_SPEECH)
            .build()
        // Set audioAttributes with handleAudioFocus=true to let ExoPlayer manage audio focus
        player.setAudioAttributes(audioAttributes, true)
        
        // Handle headphone unplug and audio becoming noisy
        player.setHandleAudioBecomingNoisy(true)
        
        player.addListener(object : Player.Listener {
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
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }
}