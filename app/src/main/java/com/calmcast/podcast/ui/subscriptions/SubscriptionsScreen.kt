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
import com.mudita.mmd.components.lazy.LazyColumnMMD

@Composable
fun SubscriptionsScreen(
    podcasts: List<Podcast>,
    onPodcastClick: (Podcast) -> Unit,
    showAddRSSModal: Boolean,
    onShowAddRSSModal: (Boolean) -> Unit,
    onAddRSSFeed: (String) -> Unit,
    isAddingRSSFeed: Boolean,
    removeDividers: Boolean = false
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
                modifier = Modifier.fillMaxSize(),
            ) {
                itemsIndexed(podcasts) { index, podcast ->
                    PodcastCard(podcast = podcast, onClick = { onPodcastClick(podcast) })

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

        AddRSSFeedModal(
            isVisible = showAddRSSModal,
            onDismiss = { onShowAddRSSModal(false) },
            onSave = { url ->
                onAddRSSFeed(url)
            },
            isLoading = isAddingRSSFeed
        )
    }
}

@Composable
fun PodcastCard(
    podcast: Podcast,
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
                    
                    if (podcast.newEpisodeCount > 0) {
                        Box(
                            modifier = Modifier
                                .padding(start = 8.dp)
                                .size(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.primary
                            ) {
                                Text(
                                    text = podcast.newEpisodeCount.toString(),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.padding(2.dp)
                                )
                            }
                        }
                    }
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

            Icon(
                imageVector = Icons.Outlined.KeyboardArrowRight,
                contentDescription = null,
                modifier = Modifier
                    .size(28.dp)
            )
        }
    }
}
