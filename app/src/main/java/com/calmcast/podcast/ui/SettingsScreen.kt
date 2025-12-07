package com.calmcast.podcast.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.calmcast.podcast.data.SettingsManager
import com.calmcast.podcast.data.DownloadLocation
import com.calmcast.podcast.ui.common.DashedDivider
import com.mudita.mmd.components.lazy.LazyColumnMMD
import com.mudita.mmd.components.tabs.PrimaryTabRowMMD
import com.mudita.mmd.components.tabs.TabMMD
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.key
import androidx.compose.ui.unit.sp
import com.mudita.mmd.components.text.TextMMD

@OptIn(ExperimentalMaterial3Api::class)
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
    onSleepTimerMinutesChange: (Int) -> Unit = {},
    downloadLocation: DownloadLocation = DownloadLocation.INTERNAL,
    onDownloadLocationChange: (DownloadLocation) -> Unit = {},
    isExternalStorageAvailable: Boolean = false,
    playbackSpeed: Float = 1f,
    onPlaybackSpeedChange: (Float) -> Unit = {}
) {
    val skipOptions = listOf(5, 10, 15, 30)
    val sleepTimerOptions = listOf(5, 10, 15, 30, 45, 60)
    val downloadLocationOptions = listOf(DownloadLocation.INTERNAL, DownloadLocation.EXTERNAL)
    val tabOptions = listOf("Player", "Downloads", "UI")
    var selectedTab by remember { mutableIntStateOf(0) }
    
    // Local state to track playback speed for UI updates
    var localPlaybackSpeed by remember(playbackSpeed) { mutableStateOf(playbackSpeed) }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        PrimaryTabRowMMD(selectedTabIndex = selectedTab) {
            tabOptions.forEachIndexed { index, title ->
                TabMMD(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        TextMMD(
                            text = title,
                            fontSize = 16.sp,
                            fontWeight = if (selectedTab == index) androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.Normal
                        )
                    },
                )
            }
        }

        key(selectedTab) {
            LazyColumnMMD(modifier = Modifier.fillMaxSize()) {
                if (selectedTab == 0) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextMMD(
                                text = "Enable Picture-in-Picture",
                                fontSize = 16.sp,
                                modifier = Modifier.weight(1f)
                            )
                            Switch(
                                checked = isPictureInPictureEnabled,
                                onCheckedChange = onPictureInPictureToggle
                            )
                        }
                        if (!removeHorizontalDividers) {
                            DashedDivider(
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
                            TextMMD(
                                text = "Use Sleep Timer",
                                fontSize = 16.sp,
                                modifier = Modifier.weight(1f),
                            )
                            Switch(
                                checked = sleepTimerEnabled,
                                onCheckedChange = onSleepTimerEnabledChange
                            )
                        }
                        if (!removeHorizontalDividers) {
                            DashedDivider(
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
                                TextMMD(
                                    text = if (minutes == 60) "1 hour" else "$minutes minutes",
                                    fontSize = 16.sp,
                                    modifier = Modifier.padding(start = 8.dp)
                                )
                            }
                        }
                    }
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 16.dp)
                        ) {
                            TextMMD(
                                text = "Skip seconds",
                                fontSize = 16.sp,
                            )
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
                            TextMMD(
                                text = "$seconds seconds",
                                fontSize = 16.sp,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 16.dp)
                        ) {
                            TextMMD(
                                text = "Playback speed",
                                fontSize = 16.sp,
                            )
                        }
                    }
                    items(com.calmcast.podcast.data.SettingsManager.PLAYBACK_SPEEDS) { speed ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    localPlaybackSpeed = speed
                                    onPlaybackSpeedChange(speed)
                                }
                                .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 4.dp)
                        ) {
                            RadioButton(
                                selected = localPlaybackSpeed == speed,
                                onClick = {
                                    localPlaybackSpeed = speed
                                    onPlaybackSpeedChange(speed)
                                }
                            )
                            TextMMD(
                                text = String.format("%.1f", speed) + "x",
                                fontSize = 16.sp,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                }

                if (selectedTab == 1) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextMMD(
                                text = "Enable Auto-Downloads",
                                fontSize = 16.sp,
                                modifier = Modifier.weight(1f)
                            )
                            Switch(
                                checked = isAutoDownloadEnabled,
                                onCheckedChange = onAutoDownloadToggle
                            )
                        }
                        if (!removeHorizontalDividers) {
                            DashedDivider(
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
                            TextMMD(
                                text = "Download Location",
                                fontSize = 16.sp
                            )
                        }
                    }
                    items(downloadLocationOptions) { location ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (location == DownloadLocation.EXTERNAL && !isExternalStorageAvailable) {
                                        return@clickable
                                    }
                                    onDownloadLocationChange(location)
                                }
                                .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 4.dp)
                        ) {
                            RadioButton(
                                selected = downloadLocation == location,
                                onClick = {
                                    if (location == DownloadLocation.EXTERNAL && !isExternalStorageAvailable) {
                                        return@RadioButton
                                    }
                                    onDownloadLocationChange(location)
                                },
                                enabled = location == DownloadLocation.INTERNAL || isExternalStorageAvailable
                            )
                            val displayText = when (location) {
                                DownloadLocation.INTERNAL -> "Internal Storage"
                                DownloadLocation.EXTERNAL -> "External Storage (SD Card)"
                            }
                            val labelText = if (location == DownloadLocation.EXTERNAL && !isExternalStorageAvailable) {
                                "$displayText (Not Available)"
                            } else {
                                displayText
                            }
                            TextMMD(
                                text = labelText,
                                fontSize = 16.sp,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                }

                if (selectedTab == 2) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextMMD(
                                text = "Keep Screen On (Full Player)",
                                fontSize = 16.sp,
                                modifier = Modifier.weight(1f)
                            )
                            Switch(
                                checked = isKeepScreenOnEnabled,
                                onCheckedChange = onKeepScreenOnToggle
                            )
                        }
                        if (!removeHorizontalDividers) {
                            DashedDivider(
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
                            TextMMD(
                                text = "Hide dividers",
                                fontSize = 16.sp,
                                modifier = Modifier.weight(1f)
                            )
                            Switch(
                                checked = removeHorizontalDividers,
                                onCheckedChange = onRemoveHorizontalDividersToggle
                            )
                        }
                        if (!removeHorizontalDividers) {
                            DashedDivider(
                                modifier = Modifier.padding(start = 16.dp),
                                thickness = 1.dp
                            )
                        }
                    }
                }
            }
        }
    }
}
