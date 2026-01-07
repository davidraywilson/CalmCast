package com.calmcast.podcast.data.download

import android.content.Context
import android.net.Uri
import android.util.Log
import com.calmcast.podcast.data.PodcastRepository.Episode
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
import java.io.IOException

class AndroidDownloadManager(
    private val context: Context,
    private val client: OkHttpClient,
    private val downloadDao: DownloadDao,
    private val settingsManager: SettingsManager
) : DownloadManager {

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
                val attemptResume = file.exists() && file.length() > 0
                val bytesAlready = if (attemptResume) file.length() else 0L

                val requestBuilder = Request.Builder().url(episode.audioUrl)
                if (bytesAlready > 0) {
                    requestBuilder.addHeader("Range", "bytes=$bytesAlready-")
                }

                val call = client.newCall(requestBuilder.build())
                activeCalls[episodeId] = call

                val response = call.execute()
                if (response.isSuccessful) {
                    val code = response.code
                    val body = response.body

                    if (body != null) {
                        val isResume = code == 206

                        val finalBytesAlready = if (isResume) bytesAlready else 0L
                        val appendMode = isResume

                        val serverBytes = body.contentLength()
                        val totalBytes = if (serverBytes != -1L) finalBytesAlready + serverBytes else -1L

                        var bytesCopied = finalBytesAlready

                        body.byteStream().use { input ->
                            FileOutputStream(file, appendMode).use { output ->
                                val buffer = ByteArray(64 * 1024)
                                var read = input.read(buffer)
                                var lastDbUpdateTime = System.currentTimeMillis()

                                while (read >= 0) {
                                    if (pausedDownloads.contains(episodeId)) {
                                        downloadDao.insert(download.copy(
                                            status = DownloadStatus.PAUSED,
                                            progress = if (totalBytes > 0) bytesCopied.toFloat() / totalBytes.toFloat() else 0f,
                                            downloadedBytes = bytesCopied,
                                            totalBytes = totalBytes
                                        ))
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
                                        downloadDao.insert(download.copy(
                                            status = DownloadStatus.DOWNLOADING,
                                            progress = progress,
                                            downloadedBytes = bytesCopied,
                                            totalBytes = totalBytes
                                        ))
                                        lastDbUpdateTime = currentTime
                                    }
                                    read = input.read(buffer)
                                }
                            }
                        }

                        if (totalBytes != -1L && bytesCopied < totalBytes) {
                            if (pausedDownloads.contains(episodeId)) {
                                Log.d(TAG, "Download paused for ${episode.title}")
                                downloadDao.insert(download.copy(
                                    status = DownloadStatus.PAUSED,
                                    progress = bytesCopied.toFloat() / totalBytes.toFloat(),
                                    downloadedBytes = bytesCopied,
                                    totalBytes = totalBytes
                                ))
                            } else {
                                Log.w(TAG, "Download incomplete for ${episode.title}: $bytesCopied / $totalBytes bytes")
                                downloadDao.insert(download.copy(status = DownloadStatus.FAILED, progress = 0f))
                            }
                        } else {
                            val fileUri = Uri.fromFile(file).toString()
                            downloadDao.insert(download.copy(
                                status = DownloadStatus.DOWNLOADED,
                                progress = 1f,
                                downloadUri = fileUri,
                                downloadedBytes = bytesCopied,
                                totalBytes = totalBytes
                            ))
                        }
                    } else {
                        Log.w(TAG, "No response body for episode: ${episode.title}")
                        downloadDao.insert(download.copy(status = DownloadStatus.FAILED, progress = 0f))
                    }
                } else if (response.code == 416) {
                    Log.w(TAG, "Range not satisfiable for episode: ${episode.title}. Retrying from scratch.")
                    file.delete()
                    downloadDao.insert(download.copy(status = DownloadStatus.FAILED, progress = 0f))
                } else {
                    Log.w(TAG, "Download failed with status ${response.code} for episode: ${episode.title}")
                    downloadDao.insert(download.copy(status = DownloadStatus.FAILED, progress = 0f))
                }
            } catch (e: Exception) {
                val isCancelled = (e is IOException && e.message == "Canceled") ||
                        (e is java.net.SocketException && e.message?.contains("closed") == true)

                if (pausedDownloads.contains(episodeId) || isCancelled) {
                    Log.d(TAG, "Download paused (exception caught) for ${episode.title}")
                    val currentBytes = if (file.exists()) file.length() else 0L
                    downloadDao.insert(download.copy(
                        status = DownloadStatus.PAUSED,
                        downloadedBytes = currentBytes
                    ))
                } else {
                    Log.e(TAG, "Exception during download for episode ${episode.title}: ${e.message}", e)
                    downloadDao.insert(download.copy(status = DownloadStatus.FAILED, progress = 0f))
                }
            } finally {
                activeDownloads.remove(episodeId)
                activeCalls.remove(episodeId)
                // Do not remove from pausedDownloads here; it's needed for resume check
            }
        }

        activeDownloads[episodeId] = job
        return downloadId
    }

    override fun cancelDownload(id: Long) {
        TODO("Not yet implemented")
    }

    override fun cancelDownload(episodeId: String) {
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
                downloadDao.insert(download.copy(status = DownloadStatus.DELETED, progress = 0f, downloadUri = null, downloadedBytes = 0L, totalBytes = -1L))
            }
        }
    }

    override fun pauseDownload(episodeId: String) {
        pausedDownloads.add(episodeId)
        // Optimistically update DB
        CoroutineScope(Dispatchers.IO).launch {
            val download = downloadDao.getByEpisodeId(episodeId)
            if (download != null && download.status == DownloadStatus.DOWNLOADING) {
                downloadDao.insert(download.copy(status = DownloadStatus.PAUSED))
            }
        }
        // Cancel the call to stop network traffic immediately
        activeCalls[episodeId]?.cancel()
    }

    override fun resumeDownload(episodeId: String) {
        pausedDownloads.remove(episodeId)
        CoroutineScope(Dispatchers.IO).launch {
            val download = downloadDao.getByEpisodeId(episodeId)
            if (download != null && download.status == DownloadStatus.PAUSED) {
                // Restart the download - it will detect the partial file and resume from offset
                startDownload(download.episode)
            }
        }
    }

    override fun getDownload(id: Long): Download? {
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
        }
    }
}