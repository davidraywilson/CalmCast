package com.calmcast.podcast.data.download

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.calmcast.podcast.data.Episode

@Entity(tableName = "downloads")
data class Download(
    @PrimaryKey
    val id: String, // episode.id - unique identifier for each download
    @Embedded(prefix = "episode_")
    val episode: Episode,
    val status: DownloadStatus,
    val progress: Float,
    val downloadUri: String? = null,
    val downloadedBytes: Long = 0L, // Tracks bytes downloaded for resume capability
    val totalBytes: Long = -1L // Tracks total file size for resume capability
)

enum class DownloadStatus {
    DOWNLOADING,
    DOWNLOADED,
    FAILED,
    PAUSED,
    CANCELED,
    DELETED,  // User deleted - prevent auto-re-download, but allow manual re-download
    STORAGE_UNAVAILABLE  // External storage selected but not available
}
