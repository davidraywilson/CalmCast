package com.calmcast.podcast.ui

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.outlined.AutoMode
import androidx.compose.material.icons.outlined.Autorenew
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.SubdirectoryArrowRight
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.SubdirectoryArrowLeft
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.BedroomParent
import androidx.compose.material.icons.outlined.BedtimeOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.calmcast.podcast.data.PodcastRepository
import com.calmcast.podcast.data.PodcastRepository.Episode
import com.calmcast.podcast.ui.common.DashedDivider
import com.calmcast.podcast.utils.DateTimeFormatter
import com.mudita.mmd.components.buttons.ButtonMMD
import com.mudita.mmd.components.checkbox.CheckboxMMD
import com.mudita.mmd.components.divider.HorizontalDividerMMD
import com.mudita.mmd.components.lazy.LazyColumnMMD
import com.mudita.mmd.components.menus.DropdownMenuItemMMD
import com.mudita.mmd.components.menus.DropdownMenuMMD
import com.mudita.mmd.components.progress_indicator.CircularProgressIndicatorMMD
import com.mudita.mmd.components.slider.SliderMMD
import com.mudita.mmd.components.switcher.SwitchMMD
import com.mudita.mmd.components.text.TextMMD

@Composable
fun MiniPlayer(
    episode: Episode?,
    isPlaying: Boolean,
    isLoading: Boolean,
    onPlayPauseClick: () -> Unit,
    onPlayerClick: () -> Unit,
    currentPosition: Long,
    duration: Long
) {
    if (episode == null) {
        return
    }

    HorizontalDividerMMD(thickness = 3.dp)

    Row(
        modifier = Modifier
            .padding(12.dp)
            .clickable(onClick = onPlayerClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = episode.title,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = DateTimeFormatter.formatPlaybackTime(currentPosition, duration),
                fontSize = 12.sp,
                fontWeight = FontWeight.Normal,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (isLoading) {
            Box(
                modifier = Modifier.size(40.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicatorMMD(
                    modifier = Modifier.size(24.dp)
                )
            }
        } else {
            IconButton(
                onClick = onPlayPauseClick,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@SuppressLint("DefaultLocale")
@Composable
fun FullPlayerScreen(
    episode: Episode?,
    isPlaying: Boolean,
    isLoading: Boolean,
    onPlayPauseClick: () -> Unit,
    onSeekForward: () -> Unit,
    onSeekBackward: () -> Unit,
    onMinimizeClick: () -> Unit,
    currentPosition: Long,
    duration: Long,
    onSeek: (Long) -> Unit,
    skipSeconds: Int,
    isKeepScreenOnEnabled: Boolean = false,
    onKeepScreenOnToggle: (Boolean) -> Unit = {},
    isSleepTimerActive: Boolean = false,
    sleepTimerRemainingSeconds: Long = 0L,
    onStartSleepTimer: () -> Unit = {},
    onStopSleepTimer: () -> Unit = {},
    onSleepTimerMinutesChange: (Int) -> Unit = {},
    playbackSpeed: Float = 1f,
    onPlaybackSpeedClick: () -> Unit = {},
    onPlaybackSpeedChange: (Float) -> Unit = {},
    isAutoPlayNextEpisodeEnabled: Boolean = false,
    onAutoPlayNextEpisodeToggle: (Boolean) -> Unit = {}
) {
    if (episode == null) return
    
    var speedMenuExpanded by remember { mutableStateOf(false) }
    var sleepMenuExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {}
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth().padding(horizontal = 8.dp)
                .weight(1f),
            verticalArrangement = Arrangement.Top
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .padding(top = 8.dp)
                    .clickable(onClick = onMinimizeClick),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Now Playing",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )

                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = "Minimize",
                )
            }

            Spacer(modifier = Modifier.height(48.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .padding(bottom = 16.dp),
                verticalArrangement = Arrangement.Bottom,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = episode.title,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = episode.podcastTitle,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Normal,
                    textAlign = TextAlign.Center,
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        if (!isKeepScreenOnEnabled) {
            Column(
                modifier = Modifier.fillMaxWidth(),
            ) {
                SliderMMD(
                    modifier = Modifier.fillMaxWidth(),
                    value = if (duration > 0) currentPosition.toFloat() / duration else 0f,
                    onValueChange = { newValue ->
                        if (duration > 0) {
                            onSeek((newValue * duration).toLong())
                        }
                    },
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = DateTimeFormatter.formatDuration(currentPosition / 1000),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    if (isSleepTimerActive) {
                        Text(
                            text = "Sleep: ${DateTimeFormatter.formatDuration(sleepTimerRemainingSeconds)}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Text(
                        text = DateTimeFormatter.formatDuration(duration / 1000),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ButtonMMD (
                onClick = onSeekBackward,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary
                )
            ) {
                Text("${skipSeconds}s", color = MaterialTheme.colorScheme.onSecondary)

                Icon(
                    imageVector = Icons.Outlined.SubdirectoryArrowLeft,
                    contentDescription = "Rewind ${skipSeconds}s",
                    modifier = Modifier.size(22.dp).padding(start = 4.dp),
                    tint = MaterialTheme.colorScheme.onSecondary
                )
            }

            if (isLoading) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .background(
                            color = MaterialTheme.colorScheme.secondary,
                            shape = RoundedCornerShape(36.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicatorMMD(
                        modifier = Modifier.size(46.dp),
                        color = MaterialTheme.colorScheme.onSecondary
                    )
                }
            } else {
                IconButton(
                    onClick = onPlayPauseClick,
                    modifier = Modifier
                        .size(72.dp)
                        .background(
                            color = MaterialTheme.colorScheme.secondary,
                            shape = RoundedCornerShape(36.dp)
                        )
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        modifier = Modifier.size(46.dp),
                    )
                }
            }

            ButtonMMD (
                onClick = onSeekForward,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary
                )
            ) {
                Icon(
                    imageVector = Icons.Outlined.SubdirectoryArrowRight,
                    contentDescription = "Forward ${skipSeconds}s",
                    modifier = Modifier.size(22.dp).padding(end = 4.dp),
                    tint = MaterialTheme.colorScheme.onSecondary
                )

                Text("${skipSeconds}s", color = MaterialTheme.colorScheme.onSecondary)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .padding(horizontal = 8.dp, vertical = 8.dp)
                    .fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .padding(end = 16.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            onKeepScreenOnToggle(!isKeepScreenOnEnabled)
                        },
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = if (isKeepScreenOnEnabled) Icons.Outlined.Lock else Icons.Outlined.LockOpen,
                            contentDescription = if (isKeepScreenOnEnabled) "Screen always on - click to turn off" else "Screen can turn off - click to turn on",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .padding(end = 16.dp)
                        .pointerInput(isSleepTimerActive) {
                            detectTapGestures(
                                onTap = {
                                    if (isSleepTimerActive) {
                                        onStopSleepTimer()
                                    } else {
                                        onStartSleepTimer()
                                    }
                                },
                                onLongPress = { sleepMenuExpanded = true }
                            )
                        }
                ) {
                    Box() {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = if (isSleepTimerActive) Icons.Outlined.Bedtime else Icons.Outlined.BedtimeOff,
                                contentDescription = if (isSleepTimerActive) "Sleep timer active - long press for options" else "Sleep timer off - long press for options",
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        DropdownMenuMMD(
                            expanded = sleepMenuExpanded,
                            onDismissRequest = { sleepMenuExpanded = false }
                        ) {
                            com.calmcast.podcast.data.SettingsManager.SLEEP_TIMER_OPTIONS.forEach { minutes ->
                                DropdownMenuItemMMD(
                                    text = { TextMMD(if (minutes == 60) "1 hour" else "$minutes min") },
                                    onClick = {
                                        onSleepTimerMinutesChange(minutes)
                                        onStartSleepTimer()
                                        sleepMenuExpanded = false
                                    }
                                )

                                if (minutes != com.calmcast.podcast.data.SettingsManager.SLEEP_TIMER_OPTIONS.last()) {
                                    DashedDivider()
                                }
                            }
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .padding(end = 16.dp)
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onTap = { onPlaybackSpeedClick() },
                                onLongPress = { speedMenuExpanded = true }
                            )
                        }
                ) {
                    Box {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = String.format("%.1f", playbackSpeed) + "x",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Normal
                            )
                        }

                        DropdownMenuMMD(
                            expanded = speedMenuExpanded,
                            onDismissRequest = { speedMenuExpanded = false }
                        ) {
                            com.calmcast.podcast.data.SettingsManager.PLAYBACK_SPEEDS.forEach { speed ->
                                DropdownMenuItemMMD(
                                    text = { TextMMD(String.format("%.2f", speed) + "x") },
                                    onClick = {
                                        onPlaybackSpeedChange(speed)
                                        speedMenuExpanded = false
                                    }
                                )

                                if (speed != com.calmcast.podcast.data.SettingsManager.PLAYBACK_SPEEDS.last()) {
                                    DashedDivider()
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TextMMD(
                            text = "Autoplay",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Normal
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        CheckboxMMD(
                            checked = isAutoPlayNextEpisodeEnabled,
                            onCheckedChange = onAutoPlayNextEpisodeToggle,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}
