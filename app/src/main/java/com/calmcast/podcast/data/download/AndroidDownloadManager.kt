package com.calmcast.podcast.data.download

import android.content.Context
import android.net.Uri
import android.util.Log
import com.calmcast.podcast.data.Episode
import com.calmcast.podcast.data.PodcastDao
import com.calmcast.podcast.data.SettingsManager
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
    private val podcastDao: PodcastDao? = null,
    private val settingsManager: SettingsManager
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
        val baseDir: File
        try {
            baseDir = StorageManager.getDownloadBaseDir(context, settingsManager)
        } catch (e: StorageUnavailableException) {
            Log.w(TAG, "External storage unavailable for episode: ${episode.title}")
            CoroutineScope(Dispatchers.IO).launch {
                val download = Download(episodeId, episode, DownloadStatus.STORAGE_UNAVAILABLE, 0f)
                downloadDao.insert(download)
            }
            return downloadId
        }
        val file = File(baseDir, "${episode.id}.mp3")

        val download = Download(episodeId, episode, DownloadStatus.DOWNLOADING, 0f)

        val job = CoroutineScope(Dispatchers.IO).launch {
            downloadDao.insert(download)
            try {
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
                        
                        body.byteStream().use { input ->
                            FileOutputStream(file, appendMode).use { output ->
                                val buffer = ByteArray(64 * 1024)
                                var read = input.read(buffer)
                                var lastDbUpdateTime = System.currentTimeMillis()
                                while (read >= 0) {
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
                                    val currentTime = System.currentTimeMillis()
                                    if (currentTime - lastDbUpdateTime >= 1000) {
                                        downloadDao.insert(download.copy(status = DownloadStatus.DOWNLOADING, progress = progress, downloadedBytes = bytesCopied, totalBytes = totalBytes))
                                        lastDbUpdateTime = currentTime
                                    }
                                    read = input.read(buffer)
                                }
                            }
                        }

                        if (totalBytes != -1L && bytesCopied != totalBytes) {
                            Log.w(TAG, "Download incomplete for ${episode.title}: $bytesCopied / $totalBytes bytes")
                            downloadDao.insert(download.copy(status = DownloadStatus.FAILED, progress = 0f))
                            file.delete() // Delete partial file
                        } else {
                            val fileUri = Uri.fromFile(file).toString()
                            downloadDao.insert(download.copy(status = DownloadStatus.DOWNLOADED, progress = 1f, downloadUri = fileUri, downloadedBytes = bytesCopied, totalBytes = totalBytes))

                            podcastDao?.updateEpisodeDownloadPath(episode.id, file.absolutePath)
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
        try {
            val baseDir = StorageManager.getDownloadBaseDir(context, settingsManager)
            val file = File(baseDir, "$episodeId.mp3")
            if (file.exists()) {
                file.delete()
            }
        } catch (e: StorageUnavailableException) {
            Log.d(TAG, "Cannot delete file - external storage unavailable")
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
        try {
            val baseDir = StorageManager.getDownloadBaseDir(context, settingsManager)
            val file = File(baseDir, "${episode.id}.mp3")
            val oldFile = File(baseDir, "${episode.title}.mp3")
            
            if (file.exists()) {
                file.delete()
            }
            
            if (oldFile.exists()) {
                oldFile.delete()
            }
        } catch (e: StorageUnavailableException) {
            Log.d(TAG, "Cannot delete file - external storage unavailable")
        }
        CoroutineScope(Dispatchers.IO).launch {
            val download = downloadDao.getByEpisodeId(episode.id)
            if (download != null) {
                downloadDao.insert(download.copy(status = DownloadStatus.DELETED, downloadUri = null, progress = 0f, downloadedBytes = 0L, totalBytes = -1L))
            }
            podcastDao?.updateEpisodeDownloadPath(episode.id, "")
        }
    }
}
