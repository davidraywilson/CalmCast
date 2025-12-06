package com.calmcast.podcast.ui.subscriptions

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mudita.mmd.components.bottom_sheet.ModalBottomSheetMMD

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddRSSFeedModal(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
    isLoading: Boolean = false
) {
    val feedUrl = remember { mutableStateOf("") }
    val hasCalledOnSave = remember { mutableStateOf(false) }

    androidx.compose.runtime.LaunchedEffect(isVisible) {
        if (!isVisible) {
            feedUrl.value = ""
            hasCalledOnSave.value = false
        }
    }

    LaunchedEffect(isLoading) {
        if (!isLoading && hasCalledOnSave.value) {
            onDismiss()
            hasCalledOnSave.value = false
        }
    }

    if (isVisible) {
        ModalBottomSheetMMD(
            onDismissRequest = onDismiss
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Add RSS Feed URL",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                TextField(
                    value = feedUrl.value,
                    onValueChange = { feedUrl.value = it },
                    placeholder = { Text("https://example.com/feed.xml") },
                    label = { Text("Feed URL") },
                    enabled = !isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = {
                            if (!isLoading) {
                                feedUrl.value = ""
                                onDismiss()
                            }
                        },
                        enabled = !isLoading,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors()
                    ) {
                        Text("Cancel")
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            if (feedUrl.value.isNotBlank()) {
                                hasCalledOnSave.value = true
                                onSave(feedUrl.value)
                            }
                        },
                        enabled = !isLoading && feedUrl.value.isNotBlank(),
                        modifier = Modifier.weight(1f)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .height(20.dp)
                                    .width(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Text("Save")
                        }
                    }
                }
            }
        }
    }
}
