package com.calmcast.podcast.data.download

import android.content.Context
import android.net.Uri
import android.util.Log
import com.calmcast.podcast.data.Episode
import com.calmcast.podcast.data.PodcastDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream

class AndroidDownloadManager(
    private val context: Context,
    private val client: OkHttpClient,
    private val downloadDao: DownloadDao,
    private val podcastDao: PodcastDao? = null
) : com.calmcast.podcast.data.download.DownloadManager {

    companion object {
        private const val TAG = "AndroidDownloadManager"
    }

    // Track ongoing downloads and their pause state
    private val activeDownloads = mutableMapOf<String, Job>()
    private val activeCalls = mutableMapOf<String, Call>()
    private val pausedDownloads = mutableSetOf<String>()

    override val downloads: Flow<List<Download>> = downloadDao.getAll()

    override fun startDownload(episode: Episode): Long {
        val downloadId = episode.audioUrl.hashCode().toLong()
        val episodeId = episode.id
        val file = File(context.filesDir, "${episode.id}.mp3")

        val download = Download(episodeId, episode, DownloadStatus.DOWNLOADING, 0f)

        val job = CoroutineScope(Dispatchers.IO).launch {
            downloadDao.insert(download)
            Log.d(TAG, "Starting download for episode: ${episode.title} (${episode.id})")
            try {
                // Determine if we should resume from an existing partial file
                val bytesAlready = if (file.exists()) file.length() else 0L
                val requestBuilder = Request.Builder().url(episode.audioUrl)
                if (bytesAlready > 0) {
                    requestBuilder.addHeader("Range", "bytes=$bytesAlready-")
                }
                val call = client.newCall(requestBuilder.build())
                activeCalls[episodeId] = call
                val response = call.execute()
                if (response.isSuccessful) {
                    val body = response.body
                    if (body != null) {
                        val serverBytes = body.contentLength()
                        val bytesAlready = if (file.exists()) file.length() else 0L
                        var bytesCopied = bytesAlready
                        val totalBytes = if (serverBytes > 0) bytesAlready + serverBytes else -1L
                        val appendMode = bytesAlready > 0
                        if (appendMode) {
                            Log.d(TAG, "Resuming download for ${episode.title} from byte offset: $bytesAlready")
                        }
                        
                        body.byteStream().use { input ->
                            FileOutputStream(file, appendMode).use { output ->
                                val buffer = ByteArray(4 * 1024)
                                var read = input.read(buffer)
                                while (read >= 0) {
                                    // Check if paused
                                    if (pausedDownloads.contains(episodeId)) {
                                        Log.d(TAG, "Download paused for ${episode.title}")
                                        downloadDao.insert(download.copy(status = DownloadStatus.PAUSED, progress = bytesCopied.toFloat() / totalBytes.toFloat(), downloadedBytes = bytesCopied, totalBytes = totalBytes))
                                        return@launch
                                    }
                                    
                                    output.write(buffer, 0, read)
                                    bytesCopied += read
                                    val progress = if (totalBytes > 0) {
                                        bytesCopied.toFloat() / totalBytes.toFloat()
                                    } else {
                                        -1f // Indeterminate progress
                                    }
                                    downloadDao.insert(download.copy(status = DownloadStatus.DOWNLOADING, progress = progress, downloadedBytes = bytesCopied, totalBytes = totalBytes))
                                    read = input.read(buffer)
                                }
                            }
                        }

                        // Verify download was successful
                        if (totalBytes != -1L && bytesCopied != totalBytes) {
                            Log.w(TAG, "Download incomplete for ${episode.title}: $bytesCopied / $totalBytes bytes")
                            downloadDao.insert(download.copy(status = DownloadStatus.FAILED, progress = 0f))
                            file.delete() // Delete partial file
                        } else {
                            // Convert file path to proper file:// URI to prevent MalformedURLException
                            // in ExoPlayer's HttpDataSource when handling local files
                            val fileUri = Uri.fromFile(file).toString()
                            Log.d(TAG, "Download completed for episode: ${episode.title}, saved to: ${file.absolutePath}")
                            Log.d(TAG, "Storing file URI: $fileUri")
                            downloadDao.insert(download.copy(status = DownloadStatus.DOWNLOADED, progress = 1f, downloadUri = fileUri, downloadedBytes = bytesCopied, totalBytes = totalBytes))

                            // Update the Episode entity with the download path (store actual file path for File operations)
                            podcastDao?.updateEpisodeDownloadPath(episode.id, file.absolutePath)
                            Log.d(TAG, "Updated episode ${episode.id} with downloadPath: ${file.absolutePath}")
                        }
                    } else {
                        Log.w(TAG, "No response body for episode: ${episode.title}")
                        downloadDao.insert(download.copy(status = DownloadStatus.FAILED, progress = 0f))
                    }
                } else {
                    Log.w(TAG, "Download failed with status ${response.code} for episode: ${episode.title}")
                    downloadDao.insert(download.copy(status = DownloadStatus.FAILED, progress = 0f))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception during download for episode ${episode.title}: ${e.message}", e)
                downloadDao.insert(download.copy(status = DownloadStatus.FAILED, progress = 0f))
            } finally {
                activeDownloads.remove(episodeId)
                activeCalls.remove(episodeId)
                pausedDownloads.remove(episodeId)
            }
        }
        
        activeDownloads[episodeId] = job
        return downloadId
    }

    override fun cancelDownload(id: Long) {
        // This id-based cancel is unused in current UI.
    }

    override fun cancelDownload(episodeId: String) {
        Log.d(TAG, "Canceling download for episode: $episodeId")
        pausedDownloads.remove(episodeId)
        activeCalls[episodeId]?.cancel()
        activeDownloads[episodeId]?.cancel()
        val file = File(context.filesDir, "$episodeId.mp3")
        if (file.exists()) {
            file.delete()
        }
        CoroutineScope(Dispatchers.IO).launch {
            val download = downloadDao.getByEpisodeId(episodeId)
            if (download != null) {
                downloadDao.insert(download.copy(status = DownloadStatus.CANCELED, progress = 0f, downloadUri = null, downloadedBytes = 0L, totalBytes = -1L))
            }
        }
    }

    override fun pauseDownload(episodeId: String) {
        pausedDownloads.add(episodeId)
        Log.d(TAG, "Download paused for episode: $episodeId")
        CoroutineScope(Dispatchers.IO).launch {
            val download = downloadDao.getByEpisodeId(episodeId)
            if (download != null && download.status == DownloadStatus.DOWNLOADING) {
                downloadDao.insert(download.copy(status = DownloadStatus.PAUSED))
            }
        }
    }

    override fun resumeDownload(episodeId: String) {
        Log.d(TAG, "Download resumed for episode: $episodeId")
        CoroutineScope(Dispatchers.IO).launch {
            val download = downloadDao.getByEpisodeId(episodeId)
            if (download != null && download.status == DownloadStatus.PAUSED) {
                // Restart the download - it will detect the partial file and resume from offset
                pausedDownloads.remove(episodeId)
                startDownload(download.episode)
            }
        }
    }

    override fun getDownload(id: Long): Download? {
        // This is now more complex as we are using a flow.
        // A direct synchronous get is not possible without refactoring.
        // This should be handled in the UI layer by observing the flow.
        return null
    }

    override fun deleteDownload(episode: Episode) {
        val file = File(context.filesDir, "${episode.id}.mp3")
        val oldFile = File(context.filesDir, "${episode.title}.mp3")
        
        if (file.exists()) {
            Log.d(TAG, "Deleting download file for episode: ${episode.title}")
            Log.d(TAG, "Deleting file: ${file.absolutePath}")
            file.delete()
        }
        
        // Also delete old-format file if it exists
        if (oldFile.exists()) {
            Log.d(TAG, "Deleting old-format download file: ${oldFile.absolutePath}")
            oldFile.delete()
        }
        CoroutineScope(Dispatchers.IO).launch {
            val download = downloadDao.getByEpisodeId(episode.id)
            if (download != null) {
                // Mark as DELETED instead of removing - this prevents auto-re-download
                // User can still manually re-download (REPLACE will overwrite DELETED status)
                downloadDao.insert(download.copy(status = DownloadStatus.DELETED, downloadUri = null, progress = 0f, downloadedBytes = 0L, totalBytes = -1L))
                Log.d(TAG, "Marked download as DELETED for episode: ${episode.title}")
            }
            // Clear the downloadedPath from the Episode entity
            podcastDao?.updateEpisodeDownloadPath(episode.id, "")
        }
    }
}
