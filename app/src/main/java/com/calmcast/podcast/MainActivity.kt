package com.calmcast.podcast

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material.icons.sharp.Clear
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.calmcast.podcast.data.PodcastDatabase
import com.calmcast.podcast.data.SettingsManager
import com.calmcast.podcast.ui.FullPlayerScreen
import com.calmcast.podcast.ui.PictureInPictureContent
import com.calmcast.podcast.ui.PictureInPictureHelper
import com.calmcast.podcast.ui.PodcastViewModel
import com.calmcast.podcast.ui.PodcastViewModelFactory
import com.calmcast.podcast.ui.SettingsScreen
import com.calmcast.podcast.ui.downloads.DownloadsScreen
import com.calmcast.podcast.ui.podcastdetail.FeedNotFoundScreen
import com.calmcast.podcast.ui.podcastdetail.PodcastDetailScreen
import com.calmcast.podcast.ui.search.SearchScreen
import com.calmcast.podcast.ui.subscriptions.SubscriptionsScreen
import com.mudita.mmd.ThemeMMD
import com.mudita.mmd.components.divider.HorizontalDividerMMD
import com.mudita.mmd.components.nav_bar.NavigationBarItemMMD
import com.mudita.mmd.components.nav_bar.NavigationBarMMD
import com.mudita.mmd.components.search_bar.SearchBarDefaultsMMD
import com.mudita.mmd.components.snackbar.SnackbarHostMMD
import com.mudita.mmd.components.snackbar.SnackbarHostStateMMD
import com.mudita.mmd.components.text.TextMMD
import com.mudita.mmd.components.top_app_bar.TopAppBarMMD
import kotlinx.coroutines.launch

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    object Subscriptions : Screen("subscriptions", "Following", Icons.Outlined.StarBorder)
//    object Search : Screen("search", "Search", Icons.Outlined.Search)
    object Downloads : Screen("downloads", "Downloads", Icons.Outlined.Download)
    object Settings : Screen("settings", "Settings", Icons.Outlined.Settings)
}

val navItems = listOf(
    Screen.Subscriptions,
//    Screen.Search,
    Screen.Downloads,
    Screen.Settings
)

class MainActivity : ComponentActivity() {

    private val pipStateHolder = mutableStateOf(false)
    private lateinit var settingsManager: SettingsManager
    private var isPlayingFlag: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        settingsManager = SettingsManager(this)
        setContent {
            ThemeMMD {
                CalmCastApp(pipStateHolder, settingsManager, onIsPlayingChanged = { isPlaying ->
                    isPlayingFlag = isPlaying
                })
            }
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        attemptEnterPictureInPictureMode()
    }

    private fun attemptEnterPictureInPictureMode() {
        try {
            if (isPlayingFlag &&
                settingsManager.isPictureInPictureEnabledSync() &&
                PictureInPictureHelper.isPictureInPictureSupported(this)
            ) {
                PictureInPictureHelper.enterPictureInPictureMode(this)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @Deprecated("Deprecated in Java")
    @Suppress("DEPRECATION")
    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode)
        pipStateHolder.value = isInPictureInPictureMode
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalmCastApp(pipStateHolder: androidx.compose.runtime.MutableState<Boolean>, settingsManager: SettingsManager, onIsPlayingChanged: (Boolean) -> Unit) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostStateMMD() }
    val isInPiP = pipStateHolder

    val application = LocalContext.current.applicationContext as Application
    val database = PodcastDatabase.getDatabase(application)
    val podcastDao = database.podcastDao()
    val playbackPositionDao = database.playbackPositionDao()
    val downloadDao = database.downloadDao()
    val viewModel: PodcastViewModel = viewModel(factory = PodcastViewModelFactory(application, podcastDao, playbackPositionDao, downloadDao, settingsManager))

    LaunchedEffect(Unit) {
        snapshotFlow { viewModel.isPlaying.value }
            .collect { playing -> onIsPlayingChanged(playing) }
    }

    // Keep screen on when requested in full player
    val activity = LocalContext.current as? android.app.Activity
    DisposableEffect(viewModel.isKeepScreenOnEnabled.value, viewModel.showFullPlayer.value) {
        val shouldKeepOn = viewModel.isKeepScreenOnEnabled.value && viewModel.showFullPlayer.value
        if (shouldKeepOn) {
            activity?.window?.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            activity?.window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            activity?.window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }
    val navController = rememberNavController()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val canNavigateBack = navController.previousBackStackEntry != null
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(currentDestination) {
        if (currentDestination?.route == "search") {
            focusRequester.requestFocus()
        }
    }

    LaunchedEffect(Unit) {
        snapshotFlow { viewModel.playbackError.value }
            .collect { error ->
                if (error != null) {
                    val result = snackbarHostState.showSnackbar(
                        message = "No internet connection. Go to Downloads to play offline episodes.",
                        actionLabel = "Downloads"
                    )
                    if (result == com.mudita.mmd.components.snackbar.SnackbarResultMMD.ActionPerformed) {
                        navController.navigate("downloads")
                    }
                    viewModel.clearPlaybackError()
                }
            }
    }

    if (isInPiP.value && viewModel.currentEpisode.value != null) {
        PictureInPictureContent(
            podcast = viewModel.currentPodcastDetails.value?.podcast,
            episode = viewModel.currentEpisode.value,
            isPlaying = viewModel.isPlaying.value,
            onPlayPauseClick = { viewModel.togglePlayPause() },
            artworkUri = viewModel.currentArtworkUri.value
        )
    } else {
        Box(modifier = Modifier.fillMaxSize()) {
            Scaffold(
                snackbarHost = { SnackbarHostMMD(hostState = snackbarHostState) },
                topBar = {
                    Column {
                        TopAppBarMMD(
                            title = {
                                if (currentDestination?.route == "search") {
                                    SearchBarDefaultsMMD.InputField(
                                        query = viewModel.searchQuery.value,
                                        onQueryChange = { viewModel.updateSearchQuery(it) },
                                        onSearch = { },
                                        expanded = true,
                                        onExpandedChange = { },
                                        placeholder = { TextMMD("Search") },
                                        trailingIcon = {
                                            if (viewModel.searchQuery.value.isNotEmpty()) {
                                                IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                                                    Icon(
                                                        Icons.Sharp.Clear,
                                                        contentDescription = "Clear search"
                                                    )
                                                }
                                            }
                                        },
                                        modifier = Modifier.focusRequester(focusRequester)
                                    )
                                 } else {
                                    Column {
                                        val title = getAppBarTitle(currentDestination?.route, viewModel)
                                        Text(
                                            text = title,
                                            fontSize = if (currentDestination?.route?.startsWith("detail/") == true) 18.sp else 24.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )

                                        if (currentDestination?.route?.startsWith("detail/") == true) {
                                            viewModel.currentPodcastDetails.value?.podcast?.author?.let { author ->
                                                Text(
                                                    text = "by $author",
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Normal
                                                )
                                            }
                                        }
                                    }
                                 }
                            },
                            navigationIcon = {
                                if (canNavigateBack) {
                                    IconButton(onClick = { navController.navigateUp() }) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                            contentDescription = "Back"
                                        )
                                    }
                                }
                            },
                            actions = {
                                if (currentDestination?.route == "subscriptions") {
                                    IconButton(onClick = { navController.navigate("search") }) {
                                        Icon(
                                            imageVector = Icons.Outlined.Search,
                                            contentDescription = "Search"
                                        )
                                    }
                                }
                                if (currentDestination?.route?.startsWith("detail/") == true) {
                                    val podcast = viewModel.currentPodcastDetails.value?.podcast
                                    if (podcast != null) {
                                        val isFollowed = viewModel.isSubscribed(podcast.id)
                                        IconButton(onClick = {
                                            if (isFollowed) {
                                                viewModel.unsubscribeFromPodcast(podcast.id)
                                                scope.launch {
                                                    snackbarHostState.showSnackbar("Unfollowed ${podcast.title}")
                                                }
                                            } else {
                                                viewModel.subscribeToPodcast(podcast)
                                                scope.launch {
                                                    snackbarHostState.showSnackbar("Followed ${podcast.title}")
                                                }
                                            }
                                        }) {
                                            Icon(
                                                imageVector = if (isFollowed) Icons.Filled.Star else Icons.Outlined.StarBorder,
                                                contentDescription = if (isFollowed) "Unfollow" else "Follow"
                                            )
                                        }
                                    }
                                }
                                if (viewModel.currentEpisode.value != null) {
                                    Row {
                                        IconButton(onClick = { viewModel.togglePlayPause() }) {
                                            Icon(
                                                imageVector = if (viewModel.isPlaying.value) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
                                                contentDescription = if (viewModel.isPlaying.value) "Pause" else "Play"
                                            )
                                        }
                                        IconButton(onClick = { viewModel.showFullPlayer() }) {
                                            Icon(
                                                imageVector = Icons.Filled.Fullscreen,
                                                contentDescription = "Open full screen player"
                                            )
                                        }
                                    }
                                }
                            },
                            showDivider = false
                        )
                        HorizontalDividerMMD(thickness = 3.dp, color = MaterialTheme.colorScheme.outlineVariant)
                    }
                },
                bottomBar = {
                    if (currentDestination?.route?.startsWith("detail/") != true) {
                        NavigationBarMMD {
                            navItems.forEach { screen ->
                                val isSelected =
                                    currentDestination?.hierarchy?.any { it.route == screen.route } == true
                                NavigationBarItemMMD(
                                    icon = {
                                        Icon(
                                            painter = rememberVectorPainter(image = screen.icon),
                                            contentDescription = screen.label,
                                            tint = Color.Unspecified
                                        )
                                    },
                                    label = {
                                        Text(
                                            screen.label,
                                            fontWeight = if (isSelected) FontWeight.Black else FontWeight.Medium
                                        )
                                    },
                                    selected = isSelected,
                                    onClick = {
                                        navController.navigate(screen.route) {
                                            popUpTo(navController.graph.findStartDestination().id)
                                            launchSingleTop = true
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            ) { paddingValues ->
                NavHost(
                    navController = navController,
                    startDestination = "subscriptions",
                    modifier = Modifier.padding(paddingValues)
                ) {
                    composable("subscriptions") {
                        SubscriptionsScreen(
                            podcasts = viewModel.subscriptions.value,
                            onPodcastClick = { podcast ->
                                navController.navigate("detail/${podcast.id}")
                            },
                            removeDividers = viewModel.removeHorizontalDividers.value
                        )
                    }
                    composable("search") {
                        SearchScreen(
                            searchQuery = viewModel.searchQuery.value,
                            searchResults = viewModel.searchResults.value,
                            onPodcastClick = { podcast ->
                                navController.navigate("detail/${podcast.id}")
                            },
                            isFollowed = viewModel::isSubscribed,
                            onFollowClick = { podcast ->
                                if (viewModel.isSubscribed(podcast.id)) {
                                    viewModel.unsubscribeFromPodcast(podcast.id)
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Unfollowed ${podcast.title}")
                                    }
                                 } else {
                                    viewModel.subscribeToPodcast(podcast)
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Followed ${podcast.title}")
                                    }
                                }
                            },
                            removeDividers = viewModel.removeHorizontalDividers.value
                        )
                    }
                    composable("detail/{podcastId}") { backStackEntry ->
                        val podcastId = backStackEntry.arguments?.getString("podcastId") ?: return@composable

                        LaunchedEffect(podcastId) {
                            viewModel.fetchPodcastDetails(podcastId)
                        }

                        DisposableEffect(Unit) {
                            onDispose {
                                viewModel.clearPodcastDetails()
                            }
                        }

                        val detailError = viewModel.detailError.value
                        if (detailError != null) {
                            FeedNotFoundScreen(
                                errorCode = detailError.first,
                                onBackClick = { navController.navigateUp() }
                            )
                        } else {
                            val podcastWithEpisodes = viewModel.currentPodcastDetails.value ?: return@composable

                            val podcast = podcastWithEpisodes.podcast
                            val episodes = podcastWithEpisodes.episodes
                            val isLoadingEpisodes = viewModel.episodesLoading.value
                            val downloads = viewModel.downloads.value
                            val playbackPositions = viewModel.playbackPositions.value

                            PodcastDetailScreen(
                                podcast = podcast,
                                episodes = episodes,
                                downloads = downloads,
                                playbackPositions = playbackPositions,
                                isLoadingEpisodes = isLoadingEpisodes,
                                isBuffering = viewModel.isBuffering.value,
                                currentPlayingEpisodeId = viewModel.currentEpisode.value?.id,
                                onEpisodeClick = { episode ->
                                    viewModel.playEpisode(episode)
                                },
                                onDownloadClick = { episode ->
                                    viewModel.downloadEpisode(episode)
                                },
                                onDeleteClick = { episode ->
                                    viewModel.deleteEpisode(episode)
                                },
                                onPauseClick = { episode ->
                                    viewModel.pauseDownload(episode.id)
                                },
                                onCancelClick = { episode ->
                                    viewModel.cancelDownload(episode.id)
                                },
                                onResumeClick = { episode ->
                                    viewModel.resumeDownload(episode.id)
                                },
                                onRefreshClick = {
                                    viewModel.refreshPodcastEpisodes(podcast.id)
                                },
                                removeDividers = viewModel.removeHorizontalDividers.value
                            )
                        }
                    }
                    composable("downloads") {
                        val downloads = viewModel.downloads.value.sortedByDescending { (it.episode.publishDate?.toLongOrNull() ?: 0L) }
                        val playbackPositions = viewModel.playbackPositions.value
                        DownloadsScreen(
                            downloads = downloads,
                            playbackPositions = playbackPositions,
                            currentPlayingEpisodeId = viewModel.currentEpisode.value?.id,
                            isBuffering = viewModel.isBuffering.value,
                            onEpisodeClick = { episode ->
                                viewModel.playEpisode(episode)
                            },
                            onDeleteClick = { episode ->
                                viewModel.deleteEpisode(episode)
                            },
                            onPauseClick = { episode ->
                                viewModel.pauseDownload(episode.id)
                            },
                            onCancelClick = { episode ->
                                viewModel.cancelDownload(episode.id)
                            },
                            onResumeClick = { episode ->
                                viewModel.resumeDownload(episode.id)
                            },
                            removeDividers = viewModel.removeHorizontalDividers.value
                        )
                    }
                    composable("settings") {
                        SettingsScreen(
                            settingsManager = settingsManager,
                            isPictureInPictureEnabled = viewModel.isPictureInPictureEnabled.value,
                            onPictureInPictureToggle = { enabled ->
                                viewModel.setPictureInPictureEnabled(enabled)
                            },
                            isAutoDownloadEnabled = viewModel.isAutoDownloadEnabled.value,
                            onAutoDownloadToggle = { enabled ->
                                viewModel.setAutoDownloadEnabled(enabled)
                            },
                            skipSeconds = viewModel.skipSeconds.value,
                            onSkipSecondsChange = { seconds ->
                                viewModel.setSkipSeconds(seconds)
                            },
                            isKeepScreenOnEnabled = viewModel.isKeepScreenOnEnabled.value,
                            onKeepScreenOnToggle = { enabled ->
                                viewModel.setKeepScreenOnEnabled(enabled)
                            },
                            removeHorizontalDividers = viewModel.removeHorizontalDividers.value,
                            onRemoveHorizontalDividersToggle = { enabled ->
                                viewModel.setRemoveHorizontalDividers(enabled)
                            },
                            sleepTimerEnabled = viewModel.sleepTimerEnabled.value,
                            onSleepTimerEnabledChange = { enabled ->
                                viewModel.setSleepTimerEnabled(enabled)
                            },
                            sleepTimerMinutes = viewModel.sleepTimerMinutes.value,
                            onSleepTimerMinutesChange = { minutes ->
                                viewModel.setSleepTimerMinutes(minutes)
                            }
                        )
                    }
                }
            }

            if (viewModel.showFullPlayer.value) {
                BackHandler {
                    viewModel.minimizePlayer()
                }
                FullPlayerScreen(
                    episode = viewModel.currentEpisode.value,
                    isPlaying = viewModel.isPlaying.value,
                    isLoading = viewModel.isBuffering.value,
                    onPlayPauseClick = { viewModel.togglePlayPause() },
                    onSeekForward = { viewModel.seekForward() },
                    onSeekBackward = { viewModel.seekBackward() },
                    onMinimizeClick = { viewModel.minimizePlayer() },
                    currentPosition = viewModel.currentPosition.value,
                    duration = viewModel.duration.value,
                    onSeek = { positionMs -> viewModel.seekTo(positionMs) },
                    skipSeconds = viewModel.skipSeconds.value,
                    isKeepScreenOnEnabled = viewModel.isKeepScreenOnEnabled.value,
                    onKeepScreenOnToggle = { enabled -> viewModel.setKeepScreenOnEnabled(enabled) },
                    isSleepTimerActive = viewModel.isSleepTimerActive.value,
                    sleepTimerRemainingSeconds = viewModel.sleepTimerRemainingSeconds.value,
                    onStartSleepTimer = { viewModel.startSleepTimer() },
                    onStopSleepTimer = { viewModel.stopSleepTimer() }
                )
            }
        }
    }
}

@Composable
fun getAppBarTitle(currentRoute: String?, viewModel: PodcastViewModel): String {
    return when {
        currentRoute == "subscriptions" -> "Following"
        currentRoute == "search" -> "Search"
        currentRoute == "downloads" -> "Downloads"
        currentRoute == "settings" -> "Settings"
        currentRoute?.startsWith("detail/") == true -> {
            viewModel.currentPodcastDetails.value?.podcast?.title ?: ""
        }

        else -> "CalmCast"
    }
}
