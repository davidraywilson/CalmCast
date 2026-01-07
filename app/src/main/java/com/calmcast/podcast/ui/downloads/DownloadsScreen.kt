package com.calmcast.podcast.ui.downloads

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.calmcast.podcast.data.PlaybackPosition
import com.calmcast.podcast.data.PodcastRepository
import com.calmcast.podcast.data.download.Download
import com.calmcast.podcast.ui.podcastdetail.EpisodeItem
import com.mudita.mmd.components.lazy.LazyColumnMMD

@Composable
fun DownloadsScreen(
    downloads: List<Download>,
    playbackPositions: Map<String, PlaybackPosition>,
    currentPlayingEpisodeId: String? = null,
    isBuffering: Boolean = false,
    onEpisodeClick: (PodcastRepository.Episode) -> Unit,
    onDeleteClick: (PodcastRepository.Episode) -> Unit,
    onPauseClick: (PodcastRepository.Episode) -> Unit = {},
    onCancelClick: (PodcastRepository.Episode) -> Unit = {},
    onResumeClick: (PodcastRepository.Episode) -> Unit = {},
    removeDividers: Boolean = false
) {
    val hasError = remember { mutableStateOf(false) }

    val validDownloads = remember(downloads) {
        try {
            downloads.filter { download ->
                try {
                    if (download.status.name == "DELETED") return@filter false

                    download.episode.let { episode ->
                        episode.id.isNotBlank() && episode.title.isNotBlank() && episode.audioUrl.isNotBlank()
                    }
                } catch (e: Exception) {
                    hasError.value = true
                    false
                }
            }.sortedByDescending { it.episode.publishDateMillis }
        } catch (e: Exception) {
            hasError.value = true
            emptyList()
        }
    }

    if (hasError.value && downloads.isNotEmpty() && validDownloads.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Could not load downloads",
                    fontSize = 18.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    text = "Some of your old downloads are incompatible. They will be removed automatically.",
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                Button(
                    onClick = { hasError.value = false }
                ) {
                    Text("Dismiss")
                }
            }
        }
    } else if (validDownloads.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("No downloads yet")
        }
    } else {
        LazyColumnMMD(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 16.dp)
        ) {
            itemsIndexed(
                items = validDownloads,
                key = { _, download -> download.episode.id }
            ) { index, download ->
                val playbackPosition = playbackPositions[download.episode.id]
                val isCurrentlyPlaying = download.episode.id == currentPlayingEpisodeId
                EpisodeItem(
                    episode = download.episode,
                    download = download,
                    isLastItem = index == validDownloads.lastIndex,
                    playbackPosition = playbackPosition,
                    isCurrentlyPlaying = isCurrentlyPlaying,
                    isBuffering = isBuffering && isCurrentlyPlaying,
                    showPodcastName = true,
                    onClick = { onEpisodeClick(download.episode) },
                    onDeleteClick = { onDeleteClick(download.episode) },
                    onPauseClick = { onPauseClick(download.episode) },
                    onCancelClick = { onCancelClick(download.episode) },
                    onResumeClick = { onResumeClick(download.episode) },
                    removeDividers = removeDividers,
                    customPaddingValues = PaddingValues(
                        end = 16.dp,
                    )
                )
            }
        }
    }
}
