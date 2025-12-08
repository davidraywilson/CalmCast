package com.calmcast.podcast.ui.common

import androidx.compose.runtime.Composable

@Composable
fun SafeHtmlText(
    html: String,
    forceBlackText: Boolean = false,
    maxLines: Int? = null,
    onTruncated: (Boolean) -> Unit = {}
) {
    HtmlText(
        html = html,
        forceBlackText = forceBlackText,
        maxLines = maxLines,
        onTruncated = onTruncated
    )
}
