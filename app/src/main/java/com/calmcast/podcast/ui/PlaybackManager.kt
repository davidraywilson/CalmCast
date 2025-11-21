package com.calmcast.podcast.ui

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.State
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.FileDataSource
import androidx.media3.datasource.TransferListener
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.session.MediaSession
import com.calmcast.podcast.data.Episode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

class PlaybackManager(private val context: Context) {

    companion object {
        private const val TAG = "PlaybackManager"
    }

    private val player: ExoPlayer
    
    private val mediaSession: MediaSession
    
    private val scope = CoroutineScope(Dispatchers.Main)

    private val _currentEpisode = mutableStateOf<Episode?>(null)
    val currentEpisode: State<Episode?> = _currentEpisode

    private val _isPlaying = mutableStateOf(false)
    val isPlaying: State<Boolean> = _isPlaying

    private val _currentPosition = mutableStateOf(0L)
    val currentPosition: State<Long> = _currentPosition

    private val _duration = mutableStateOf<Long>(0L)
    val duration: State<Long> = _duration

    private val _isPlayerReady = mutableStateOf(false)
    val isPlayerReady: State<Boolean> = _isPlayerReady

    private val _isBuffering = mutableStateOf(false)
    val isBuffering: State<Boolean> = _isBuffering

    private val _playbackError = mutableStateOf<PlaybackError?>(null)
    val playbackError: State<PlaybackError?> = _playbackError

    private var playerListener: PlayerEventListener? = null

    init {
        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setAllowCrossProtocolRedirects(true)

        val fileDataSourceFactory = FileDataSource.Factory()

        // Create a composite data source factory that routes based on URI scheme
        val compositeDataSourceFactory = object : DataSource.Factory {
            override fun createDataSource(): DataSource {
                return CompositeDataSource(
                    fileDataSourceFactory.createDataSource(),
                    httpDataSourceFactory.createDataSource()
                )
            }
        }

        val mediaSourceFactory = ProgressiveMediaSource.Factory(compositeDataSourceFactory)

        player = ExoPlayer.Builder(context)
            .setMediaSourceFactory(mediaSourceFactory)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_SPEECH)
                    .build(),
                true
            )
            .build()
        mediaSession = MediaSession.Builder(context, player).build()
        setupPlayerListener()
    }

    /**
     * Composite data source that routes requests to the appropriate data source based on URI scheme
     */
    private class CompositeDataSource(
        private val fileDataSource: DataSource,
        private val httpDataSource: DataSource
    ) : DataSource {
        private var currentDataSource: DataSource? = null

        override fun addTransferListener(transferListener: TransferListener) {
            fileDataSource.addTransferListener(transferListener)
            httpDataSource.addTransferListener(transferListener)
        }

        override fun open(dataSpec: DataSpec): Long {
            val scheme = dataSpec.uri.scheme
            val path = dataSpec.uri.path
            Log.d("CompositeDataSource", "Opening URI: ${dataSpec.uri}, scheme: $scheme, path: $path")
            
            currentDataSource = when {
                scheme == "file" -> {
                    Log.d("CompositeDataSource", "Using FileDataSource for file:// URI")
                    fileDataSource
                }
                scheme == null -> {
                    // Handle raw file paths without scheme (e.g., /data/user/0/...)
                    // When Uri.parse() receives an absolute path, it creates a URI with null scheme
                    Log.d("CompositeDataSource", "Detected raw file path without scheme, using FileDataSource")
                    fileDataSource
                }
                scheme == "http" || scheme == "https" -> {
                    Log.d("CompositeDataSource", "Using HttpDataSource for HTTP(S) URL")
                    httpDataSource
                }
                else -> {
                    Log.d("CompositeDataSource", "Using HttpDataSource for unknown scheme: $scheme")
                    httpDataSource
                }
            }
            return currentDataSource?.open(dataSpec) ?: 0L
        }

        override fun read(buffer: ByteArray, offset: Int, readLength: Int): Int {
            return currentDataSource?.read(buffer, offset, readLength) ?: -1
        }

        override fun getUri(): Uri? {
            return currentDataSource?.uri
        }

        override fun close() {
            try {
                fileDataSource.close()
            } catch (e: Exception) {
                Log.w("CompositeDataSource", "Error closing file data source: ${e.message}")
            }
            try {
                httpDataSource.close()
            } catch (e: Exception) {
                Log.w("CompositeDataSource", "Error closing http data source: ${e.message}")
            }
            currentDataSource = null
        }
    }

    private fun setupPlayerListener() {
        player.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_READY -> {
                        _isPlayerReady.value = true
                        _duration.value = player.duration
                        _isBuffering.value = false
                        Log.d(TAG, "Player ready. Duration: ${player.duration}ms")
                    }
                    Player.STATE_ENDED -> {
                        _isPlaying.value = false
                        _isBuffering.value = false
                        Log.d(TAG, "Playback ended")
                        playerListener?.onPlaybackEnded()
                    }
                    Player.STATE_IDLE -> {
                        _isPlayerReady.value = false
                        _isBuffering.value = false
                        Log.d(TAG, "Player idle")
                    }
                    Player.STATE_BUFFERING -> {
                        Log.d(TAG, "Player buffering")
                    }
                }
            }

            override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                _isPlaying.value = playWhenReady
                Log.d(TAG, "Play when ready: $playWhenReady, Reason: $reason, PlaybackState: ${player.playbackState}")
            }

            override fun onPositionDiscontinuity(oldPosition: Player.PositionInfo, newPosition: Player.PositionInfo, reason: Int) {
                _currentPosition.value = player.currentPosition
            }

            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                Log.e(TAG, "Player error: ${error.message}", error)
                
                // Check if this is a file not found error for a downloaded episode
                val currentEp = _currentEpisode.value
                if (currentEp != null && currentEp.downloadedPath?.isNotEmpty() == true) {
                    val file = File(currentEp.downloadedPath)
                    if (!file.exists()) {
                        Log.w(TAG, "Downloaded file missing at: ${currentEp.downloadedPath}, falling back to streaming")
                        // Fall back to streaming
                        try {
                            val streamUri = Uri.parse(currentEp.audioUrl)
                            val mediaItem = MediaItem.fromUri(streamUri)
                            player.setMediaItem(mediaItem)
                            player.prepare()
                            player.play()
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to fall back to streaming: ${e.message}", e)
                        }
                    }
                }
                
                super.onPlayerError(error)
            }
        })
    }
    
    fun prepareEpisode(episode: Episode, startPositionMs: Long = 0L) {
        if (_currentEpisode.value?.id == episode.id) {
            return
        }

        _currentEpisode.value = episode
        Log.d(TAG, "prepareEpisode: downloadedPath = '${episode.downloadedPath}', downloadedPath?.isNotEmpty() = ${episode.downloadedPath?.isNotEmpty()}, audioUrl = '${episode.audioUrl}'")
        val mediaUrl = if (episode.downloadedPath?.isNotEmpty() == true) episode.downloadedPath else episode.audioUrl
        val source = if (episode.downloadedPath?.isNotEmpty() == true) "download" else "stream"
        Log.d(TAG, "Preparing episode: ${episode.title} (${episode.id})")
        Log.d(TAG, "Source: $source, URL: $mediaUrl")
        
        // Show loading indicator when starting to load
        _isBuffering.value = true
        
        // Stop the current playback and reset player state before switching sources
        // This is critical to prevent ExoPlayer from trying to access the old media source
        // while we're preparing a new one, which can cause FileNotFoundException errors
        try {
            if (player.playbackState != Player.STATE_IDLE) {
                Log.d(TAG, "Stopping current playback before switching sources")
                player.stop()
            }
            player.clearMediaItems()
        } catch (e: Exception) {
            Log.w(TAG, "Error clearing player state: ${e.message}")
        }
        
        // Convert file path to proper URI for local files
        // IMPORTANT: Check for both null AND empty string, as empty string can come from database
        val uri = if (episode.downloadedPath?.isNotEmpty() == true) {
            Log.d(TAG, "Creating URI from downloadedPath: '${episode.downloadedPath}'")
            val file = File(episode.downloadedPath!!)
            if (file.exists()) {
                Log.d(TAG, "Local file verified at: ${file.absolutePath}, Size: ${file.length()} bytes")
                // CRITICAL: Always use Uri.fromFile() for file paths to ensure proper file:// scheme
                // Do NOT use Uri.parse() for file paths, as it will lose the scheme and cause
                // MalformedURLException: no protocol errors in ExoPlayer's HttpDataSource
                val fileUri = Uri.fromFile(file)
                Log.d(TAG, "Created file:// URI: $fileUri")
                fileUri
            } else {
                // Try to find old-format file (using episode title)
                val oldFile = File(context.filesDir, "${episode.title}.mp3")
                if (oldFile.exists()) {
                    Log.d(TAG, "Old-format file found at: ${oldFile.absolutePath}, Size: ${oldFile.length()} bytes")
                    // CRITICAL: Always use Uri.fromFile() for file paths
                    val fileUri = Uri.fromFile(oldFile)
                    Log.d(TAG, "Created file:// URI from old-format: $fileUri")
                    fileUri
                } else {
                    Log.w(TAG, "Local file not found at: ${episode.downloadedPath}, falling back to stream")
                    Log.d(TAG, "Using audioUrl for streaming: ${episode.audioUrl}")
                    Uri.parse(episode.audioUrl)
                }
            }
        } else {
            Log.d(TAG, "No downloadedPath (isNotEmpty=${episode.downloadedPath?.isNotEmpty()}), using audioUrl: '${episode.audioUrl}'")
            Uri.parse(episode.audioUrl)
        }
        
        try {
            Log.d(TAG, "Creating MediaItem with URI: $uri, scheme: ${uri.scheme}, path: ${uri.path}")
            val mediaItem = MediaItem.fromUri(uri)
            Log.d(TAG, "MediaItem created successfully")
            player.setMediaItem(mediaItem)
            player.prepare()
            _duration.value = 0
            _currentPosition.value = 0
            _isPlayerReady.value = false
            Log.d(TAG, "Episode prepared successfully: ${episode.title}")
        } catch (e: Exception) {
            Log.e(TAG, "Error preparing episode ${episode.title}: ${e.message}", e)
            _isBuffering.value = false
            // Try to recover with streaming as fallback
            if (episode.downloadedPath?.isNotEmpty() == true) {
                Log.w(TAG, "Falling back to streaming URL for episode: ${episode.title}")
                try {
                    val fallbackUri = Uri.parse(episode.audioUrl)
                    val mediaItem = MediaItem.fromUri(fallbackUri)
                    player.setMediaItem(mediaItem)
                    player.prepare()
                } catch (fallbackE: Exception) {
                    Log.e(TAG, "Fallback to streaming also failed: ${fallbackE.message}")
                }
            }
        }
        
        if (startPositionMs > 0) {
            Log.d(TAG, "Seeking to position: ${startPositionMs}ms")
            player.seekTo(startPositionMs)
        }
    }

    fun playEpisode(episode: Episode, startPositionMs: Long = 0L) {
        Log.d(TAG, "Playing episode: ${episode.title} (${episode.id})")
        // Show loading indicator immediately
        _isBuffering.value = true
        prepareEpisode(episode, startPositionMs)
        player.play()
    }

    fun togglePlayPause() {
        if (_currentEpisode.value == null) return

        if (player.isPlaying) {
            player.pause()
            _isPlaying.value = false
        } else {
            player.play()
            _isPlaying.value = true
        }
    }

    fun seekForward(seconds: Int = 15) {
        val newPosition = (player.currentPosition + (seconds * 1000)).coerceAtMost(player.duration)
        player.seekTo(newPosition)
        _currentPosition.value = newPosition
    }

    fun seekBackward(seconds: Int = 15) {
        val newPosition = (player.currentPosition - (seconds * 1000)).coerceAtLeast(0)
        player.seekTo(newPosition)
        _currentPosition.value = newPosition
    }

    fun seekTo(positionMs: Long) {
        player.seekTo(positionMs.coerceIn(0, player.duration))
    }

    fun stop() {
        Log.d(TAG, "Stopping playback")
        player.stop()
        _isPlaying.value = false
        _currentEpisode.value = null
    }

    fun release() {
        mediaSession.release()
        player.release()
    }

    fun setPlayerEventListener(listener: PlayerEventListener) {
        this.playerListener = listener
    }

    fun updateCurrentPosition() {
        if (player.isPlaying) {
            _currentPosition.value = player.currentPosition
        }
    }

    fun clearError() {
        _playbackError.value = null
    }

    interface PlayerEventListener {
        fun onPlaybackEnded()
        fun onPlaybackError(error: PlaybackError) {}
    }

    sealed class PlaybackError {
        data class NetworkError(val message: String) : PlaybackError()
        data class GeneralError(val message: String) : PlaybackError()
    }
}