package com.calmcast.podcast.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.calmcast.podcast.data.SettingsManager
import com.mudita.mmd.components.divider.HorizontalDividerMMD
import com.mudita.mmd.components.lazy.LazyColumnMMD
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height

@Composable
fun SettingsScreen(
    settingsManager: SettingsManager,
    isPictureInPictureEnabled: Boolean,
    onPictureInPictureToggle: (Boolean) -> Unit,
    isAutoDownloadEnabled: Boolean,
    onAutoDownloadToggle: (Boolean) -> Unit,
    skipSeconds: Int,
    onSkipSecondsChange: (Int) -> Unit,
    isKeepScreenOnEnabled: Boolean = false,
    onKeepScreenOnToggle: (Boolean) -> Unit = {},
    removeHorizontalDividers: Boolean = false,
    onRemoveHorizontalDividersToggle: (Boolean) -> Unit = {},
    sleepTimerEnabled: Boolean = false,
    onSleepTimerEnabledChange: (Boolean) -> Unit = {},
    sleepTimerMinutes: Int = 0,
    onSleepTimerMinutesChange: (Int) -> Unit = {}
) {
    val skipOptions = listOf(5, 10, 15, 30)
    val sleepTimerOptions = listOf(5, 10, 15, 30, 45, 60)

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        LazyColumnMMD {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Enable Picture-in-Picture", modifier = Modifier.weight(1f))
                Switch(
                    checked = isPictureInPictureEnabled,
                    onCheckedChange = onPictureInPictureToggle
                )
            }
            if (removeHorizontalDividers) {
                Spacer(modifier = Modifier.height(1.dp).padding(start = 16.dp))
            } else {
                HorizontalDividerMMD(
                    modifier = Modifier.padding(start = 16.dp),
                    thickness = 1.dp
                )
            }
        }
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Enable Auto-Downloads", modifier = Modifier.weight(1f))
                Switch(
                    checked = isAutoDownloadEnabled,
                    onCheckedChange = onAutoDownloadToggle
                )
            }
            if (removeHorizontalDividers) {
                Spacer(modifier = Modifier.height(1.dp).padding(start = 16.dp))
            } else {
                HorizontalDividerMMD(
                    modifier = Modifier.padding(start = 16.dp),
                    thickness = 1.dp
                )
            }
        }
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Keep Screen On (Full Player)", modifier = Modifier.weight(1f))
                Switch(
                    checked = isKeepScreenOnEnabled,
                    onCheckedChange = onKeepScreenOnToggle
                )
            }
            if (removeHorizontalDividers) {
                Spacer(modifier = Modifier.height(1.dp).padding(start = 16.dp))
            } else {
                HorizontalDividerMMD(
                    modifier = Modifier.padding(start = 16.dp),
                    thickness = 1.dp
                )
            }
        }
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Remove horizontal dividers", modifier = Modifier.weight(1f))
                Switch(
                    checked = removeHorizontalDividers,
                    onCheckedChange = onRemoveHorizontalDividersToggle
                )
            }
            if (!removeHorizontalDividers) {
                HorizontalDividerMMD(
                    modifier = Modifier.padding(start = 16.dp),
                    thickness = 1.dp
                )
            }
        }
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 16.dp)
            ) {
                Text(text = "Skip seconds")
            }
        }
        items(skipOptions) { seconds ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSkipSecondsChange(seconds) }
                    .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 4.dp)
            ) {
                RadioButton(
                    selected = skipSeconds == seconds,
                    onClick = { onSkipSecondsChange(seconds) }
                )
                Text(text = "$seconds seconds", modifier = Modifier.padding(start = 8.dp))
            }
        }
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Sleep Timer", modifier = Modifier.weight(1f))
                Switch(
                    checked = sleepTimerEnabled,
                    onCheckedChange = onSleepTimerEnabledChange
                )
            }
            if (!removeHorizontalDividers) {
                HorizontalDividerMMD(
                    modifier = Modifier.padding(start = 16.dp),
                    thickness = 1.dp
                )
            }
        }
        if (sleepTimerEnabled) {
            items(sleepTimerOptions) { minutes ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSleepTimerMinutesChange(minutes) }
                        .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 4.dp)
                ) {
                    RadioButton(
                        selected = sleepTimerMinutes == minutes,
                        onClick = { onSleepTimerMinutesChange(minutes) }
                    )
                    Text(text = if (minutes == 60) "1 hour" else "$minutes minutes", modifier = Modifier.padding(start = 8.dp))
                }
            }
        }
        }
    }
}
