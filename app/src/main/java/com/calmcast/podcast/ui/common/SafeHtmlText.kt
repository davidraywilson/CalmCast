package com.calmcast.podcast.ui.common

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.core.text.HtmlCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun SafeHtmlText(
    html: String,
    forceBlackText: Boolean = false,
    maxLines: Int? = null,
    onTruncated: (Boolean) -> Unit = {}
) {
    val parsedHtml = remember(html) {
        runCatching {
            HtmlCompat.fromHtml(html, HtmlCompat.FROM_HTML_MODE_COMPACT)
        }.getOrNull()
    }

    if (parsedHtml != null) {
        HtmlText(html = html, forceBlackText = forceBlackText, maxLines = maxLines, onTruncated = onTruncated)
    } else {
        Text("Error loading description")
    }
}
