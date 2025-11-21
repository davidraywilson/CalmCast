package com.calmcast.podcast.ui.common

import android.graphics.Color
import android.text.Html
import android.widget.TextView
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun HtmlText(html: String, modifier: Modifier = Modifier) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            TextView(context).apply {
                setTextColor(Color.BLACK)
            }
        },
        update = { it.text = Html.fromHtml(html, Html.FROM_HTML_MODE_COMPACT) }
    )
}