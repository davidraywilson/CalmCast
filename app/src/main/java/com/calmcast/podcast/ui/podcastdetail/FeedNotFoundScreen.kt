package com.calmcast.podcast.ui.podcastdetail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun FeedNotFoundScreen(
    errorCode: Int,
    onBackClick: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            // Error code as headline
            Text(
                text = errorCode.toString(),
                fontSize = 72.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Error message
            Text(
                text = when (errorCode) {
                    404 -> "Podcast Feed Not Found"
                    410 -> "Podcast Feed Gone"
                    403 -> "Feed Access Denied"
                    else -> "Error"
                },
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Description
            Text(
                text = when (errorCode) {
                    404 -> "The podcast feed is no longer available at this location."
                    410 -> "The podcast feed has been permanently removed."
                    403 -> "The server denied access to this feed (403)."
                    else -> "Unable to load this podcast."
                },
                fontSize = 16.sp,
                fontWeight = FontWeight.Normal,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            // Additional help text
            Text(
                text = "Please check if the podcast is available under a different name or source.",
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}
