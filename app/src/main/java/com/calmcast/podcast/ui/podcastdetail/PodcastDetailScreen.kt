package com.calmcast.podcast.ui.podcastdetail

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Headphones
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.calmcast.podcast.R
import com.calmcast.podcast.data.Episode
import com.calmcast.podcast.data.PlaybackPosition
import com.calmcast.podcast.data.Podcast
import com.calmcast.podcast.data.download.Download
import com.calmcast.podcast.data.download.DownloadStatus
import com.calmcast.podcast.ui.common.HtmlText
import com.calmcast.podcast.utils.DateTimeFormatter
import com.mudita.mmd.components.lazy.LazyColumnMMD
import com.mudita.mmd.components.progress_indicator.CircularProgressIndicatorMMD
import com.mudita.mmd.components.progress_indicator.LinearProgressIndicatorMMD

@Composable
fun PodcastDetailScreen(
    podcast: Podcast,
    episodes: List<Episode> = emptyList(),
    downloads: List<Download> = emptyList(),
    playbackPositions: Map<String, PlaybackPosition> = emptyMap(),
    isLoadingEpisodes: Boolean = false,
    isBuffering: Boolean = false,
    currentPlayingEpisodeId: String? = null,
    onEpisodeClick: (Episode) -> Unit = {},
    onDownloadClick: (Episode) -> Unit = {},
    onDeleteClick: (Episode) -> Unit = {},
    onPauseClick: (Episode) -> Unit = {},
    onCancelClick: (Episode) -> Unit = {},
    onResumeClick: (Episode) -> Unit = {},
    onRefreshClick: () -> Unit = {}
) {
    if (isLoadingEpisodes) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicatorMMD()
        }
    } else {
        LazyColumnMMD(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 16.dp, end = 0.dp, top = 0.dp, bottom = 0.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(16.dp))

                HtmlText(
                    html = podcast.description
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Episodes (${podcast.episodeCount})",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    IconButton(
                        onClick = onRefreshClick,
                        modifier = Modifier.size(20.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Refresh,
                            contentDescription = "Refresh episodes",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                HorizontalDivider(thickness = 3.dp)
            }

            if (episodes.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(stringResource(R.string.no_episodes))
                    }
                }
            } else {
                itemsIndexed(episodes) { index, episode ->
                    val download = downloads.find { it.episode.id == episode.id }
                    val playbackPosition = playbackPositions[episode.id]
                    val isCurrentlyPlaying = episode.id == currentPlayingEpisodeId
                    EpisodeItem(
                        episode = episode,
                        download = download,
                        isLastItem = index == episodes.lastIndex,
                        playbackPosition = playbackPosition,
                        isCurrentlyPlaying = isCurrentlyPlaying,
                        isBuffering = isBuffering && isCurrentlyPlaying,
                        onClick = { onEpisodeClick(episode) },
                        onDownloadClick = { onDownloadClick(episode) },
                        onDeleteClick = { onDeleteClick(episode) },
                        onPauseClick = { onPauseClick(episode) },
                        onCancelClick = { onCancelClick(episode) },
                        onResumeClick = { onResumeClick(episode) }
                    )
                }
            }
        }
    }
}

@Composable
fun EpisodeItem(
    episode: Episode,
    download: Download?,
    isLastItem: Boolean,
    playbackPosition: PlaybackPosition?,
    isCurrentlyPlaying: Boolean,
    isBuffering: Boolean = false,
    showPodcastName: Boolean = false,
    onClick: () -> Unit = {},
    onDownloadClick: () -> Unit = {},
    onDeleteClick: () -> Unit = {},
    onPauseClick: () -> Unit = {},
    onCancelClick: () -> Unit = {},
    onResumeClick: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .padding(start = 0.dp, end = 0.dp, top = 16.dp, bottom = 0.dp)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(end = 16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.Start
            ) {
                if (isCurrentlyPlaying) {
                    if (isBuffering) {
                        Box(
                            modifier = Modifier.size(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicatorMMD()
                        }
                    } else {
                        Icon(
                            imageVector = Icons.Outlined.Headphones,
                            contentDescription = "Playing",
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                }
                when (download?.status) {
                    DownloadStatus.DOWNLOADED -> {
                        IconButton(
                            onClick = onDeleteClick,
                            modifier = Modifier
                                .size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Delete,
                                contentDescription = "Delete",
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    DownloadStatus.DOWNLOADING -> {
                        Box(
                            modifier = Modifier.size(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicatorMMD()
                        }
                    }
                    DownloadStatus.PAUSED -> {
                        // Cancel button will be rendered inline with progress bar
                    }
                    else -> {
                        IconButton(
                            onClick = onDownloadClick,
                            modifier = Modifier
                                .size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Download,
                                contentDescription = "Download",
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = episode.title,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (showPodcastName) {
                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = episode.podcastTitle,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = DateTimeFormatter.formatPublishDate(episode.publishDate),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Normal
                    )
                    Text(
                        text = DateTimeFormatter.formatDurationFromString(episode.duration),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Normal
                    )
                }

                if (download?.status == DownloadStatus.DOWNLOADING || download?.status == DownloadStatus.PAUSED) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LinearProgressIndicatorMMD(
                            progress = { download.progress.coerceIn(0f, 1f) },
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = onCancelClick,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Close,
                                contentDescription = "Cancel",
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                } else if (playbackPosition != null && playbackPosition.position > 0) {
                    val durationInSeconds = DateTimeFormatter.parseDuration(episode.duration)
                    if (durationInSeconds != null && durationInSeconds > 0) {
                        val durationInMilliseconds = durationInSeconds * 1000
                        Spacer(modifier = Modifier.height(16.dp))
                        LinearProgressIndicatorMMD(
                            progress = { playbackPosition.position.toFloat() / durationInMilliseconds },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (!isLastItem) {
                    HorizontalDivider(
                        thickness = 1.dp,
                        modifier = Modifier.padding(vertical = 0.dp)
                    )
                }
            }
        }
    }
}
