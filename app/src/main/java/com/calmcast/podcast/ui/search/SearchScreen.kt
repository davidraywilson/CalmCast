package com.calmcast.podcast.ui.search

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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
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
import com.calmcast.podcast.data.Podcast
import com.mudita.mmd.components.divider.HorizontalDividerMMD
import com.mudita.mmd.components.lazy.LazyColumnMMD

@Composable
fun SearchScreen(
    searchQuery: String,
    searchResults: List<Podcast>,
    onPodcastClick: (Podcast) -> Unit,
    isFollowed: (String) -> Boolean,
    onFollowClick: (Podcast) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        if (searchResults.isEmpty() && searchQuery.isNotBlank()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(stringResource(R.string.no_results))
            }
        } else if (searchResults.isNotEmpty()) {
            LazyColumnMMD(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(searchResults) { podcast ->
                    SearchResultCard(
                        podcast = podcast,
                        isFollowed = isFollowed(podcast.id),
                        onFollowClick = { onFollowClick(podcast) },
                        onClick = { onPodcastClick(podcast) },
                        showDivider = searchResults.last() != podcast
                    )
                }
            }
        }
    }
}

@Composable
fun SearchResultCard(
    podcast: Podcast,
    isFollowed: Boolean,
    onFollowClick: () -> Unit,
    onClick: () -> Unit,
    showDivider: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(start = 16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.Start
        ) {
            IconButton(
                onClick = onFollowClick,
                modifier = Modifier
                    .padding(end = 16.dp)
                    .size(24.dp)
            ) {
                Icon(
                    imageVector = if (isFollowed) Icons.Filled.Star else Icons.Filled.StarBorder,
                    contentDescription = stringResource(if (isFollowed) R.string.unsubscribe else R.string.subscribe),
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .clickable(onClick = onClick)
        ) {
            Column(
                modifier = Modifier.padding(top = 16.dp, end = 16.dp, bottom = 16.dp)
            ) {
                Text(
                    text = podcast.title,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = podcast.author,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (showDivider) {
                HorizontalDividerMMD(thickness = 1.dp)
            }
        }
    }
}