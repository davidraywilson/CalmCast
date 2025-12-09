package com.calmcast.podcast.ui.subscriptions

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.calmapps.calmcast.R
import com.calmcast.podcast.data.Podcast
import com.calmcast.podcast.ui.common.DashedDivider
import com.mudita.mmd.components.badge.BadgeMMD
import com.mudita.mmd.components.lazy.LazyColumnMMD
import com.mudita.mmd.components.text.TextMMD

@Composable
fun SubscriptionsScreen(
    podcasts: List<Podcast>,
    onPodcastClick: (Podcast) -> Unit,
    removeDividers: Boolean = false,
    newEpisodeCounts: Map<String, Int> = emptyMap()
) {
    Box(modifier = Modifier.fillMaxSize()) {
        if (podcasts.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(stringResource(R.string.no_podcasts))
            }
        } else {
            LazyColumnMMD(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 16.dp),
            ) {
                itemsIndexed(podcasts) { index, podcast ->
                    PodcastCard(podcast = podcast, newEpisodeCount = newEpisodeCounts[podcast.id] ?: 0, onClick = { onPodcastClick(podcast) })

                    if (index < podcasts.size - 1) {
                        if (!removeDividers) {
                            DashedDivider(
                                modifier = Modifier.padding(start = 16.dp),
                                thickness = 1.dp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PodcastCard(
    podcast: Podcast,
    newEpisodeCount: Int = 0,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = podcast.title,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = podcast.author,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Column() {
                Row() {
                    if (newEpisodeCount > 0) {
                        Column() {
                            Box(
                                modifier = Modifier.padding(start = 8.dp),
                            ) {
                                BadgeMMD {
                                    TextMMD(
                                        text = newEpisodeCount.toString(),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    Column() {
                        Icon(
                            imageVector = Icons.Outlined.KeyboardArrowRight,
                            contentDescription = null,
                            modifier = Modifier
                                .size(28.dp)
                        )
                    }
                }
            }
        }
    }
}
