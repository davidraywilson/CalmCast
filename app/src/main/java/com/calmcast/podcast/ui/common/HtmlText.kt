package com.calmcast.podcast.ui.common

import android.graphics.Color
import android.text.Html
import android.widget.TextView
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun HtmlText(
    html: String,
    modifier: Modifier = Modifier,
    maxLines: Int? = null,
    onTruncated: (Boolean) -> Unit = {}
) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            TextView(context).apply {
                setTextColor(Color.BLACK)
                if (maxLines != null) {
                    setMaxLines(maxLines)
                    setEllipsize(android.text.TextUtils.TruncateAt.END)
                }
            }
        },
        update = { textView ->
            textView.text = Html.fromHtml(html, Html.FROM_HTML_MODE_COMPACT)
            if (maxLines != null) {
                textView.post {
                    val layout = textView.layout
                    if (layout != null) {
                        val lineCount = layout.lineCount
                        val lastLine = if (lineCount > 0) minOf(maxLines - 1, lineCount - 1) else 0
                        val isEllipsized =
                            (lineCount > maxLines) ||
                            (lineCount >= maxLines && layout.getEllipsisCount(lastLine) > 0)
                        onTruncated(isEllipsized)
                    } else {
                        onTruncated(false)
                    }
                }
            }
        }
    )
}
