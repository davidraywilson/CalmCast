package com.calmcast.podcast.ui.subscriptions

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mudita.mmd.components.bottom_sheet.ModalBottomSheetMMD
import com.mudita.mmd.components.buttons.ButtonMMD
import com.mudita.mmd.components.buttons.OutlinedButtonMMD
import com.mudita.mmd.components.text_field.TextFieldMMD

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
                    text = "Add Custom Podcast",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                TextFieldMMD(
                    value = feedUrl.value,
                    onValueChange = { feedUrl.value = it },
                    placeholder = { Text("https://example.com/feed.xml") },
                    label = { Text("Feed URL") },
                    enabled = !isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                OutlinedButtonMMD(
                    onClick = {
                        if (!isLoading) {
                            feedUrl.value = ""
                            onDismiss()
                        }
                    },
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(vertical = 16.dp)
                ) {
                    Text("Cancel")
                }

                Spacer(modifier = Modifier.height(16.dp))

                ButtonMMD(
                    onClick = {
                        if (feedUrl.value.isNotBlank()) {
                            hasCalledOnSave.value = true
                            onSave(feedUrl.value)
                        }
                    },
                    enabled = !isLoading && feedUrl.value.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(vertical = 16.dp)
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
                        Text("Follow")
                    }
                }
            }
        }
    }
}
