package com.mindblownproductions.union

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.net.Uri
import android.provider.OpenableColumns
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

data class IptvChannel(val name: String, val url: String)
data class LocalVideo(val title: String, val uri: Uri)

@Composable
fun TvHubScreen(
    isFullscreen: Boolean,
    onFullscreenChange: (Boolean) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Live TV", "Local Videos", "YouTube")

    FullscreenEffect(isFullscreen)
    BackHandler(enabled = isFullscreen) { onFullscreenChange(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        if (!isFullscreen) {
            PrimaryTabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) },
                        icon = {
                            when (index) {
                                0 -> Icon(Icons.Default.LiveTv, contentDescription = null)
                                1 -> Icon(Icons.Default.Movie, contentDescription = null)
                                else -> Icon(Icons.Default.VideoLibrary, contentDescription = null)
                            }
                        }
                    )
                }
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            when (selectedTab) {
                0 -> LiveTvTab(isFullscreen, onFullscreenChange)
                1 -> LocalVideosTab(isFullscreen, onFullscreenChange)
                2 -> YouTubeTab()
            }
        }
    }
}

// ==========================================
// FULLSCREEN HELPERS
// ==========================================
@Composable
fun FullscreenEffect(isFullscreen: Boolean) {
    val context = LocalContext.current
    DisposableEffect(isFullscreen) {
        val activity = context.findActivity()
        val window = activity?.window
        if (activity != null && window != null) {
            WindowCompat.setDecorFitsSystemWindows(window, false)
            val controller = WindowCompat.getInsetsController(window, window.decorView)
            if (isFullscreen) {
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                controller.systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                controller.hide(WindowInsetsCompat.Type.systemBars())
            } else {
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                controller.show(WindowInsetsCompat.Type.systemBars())
            }
        }
        onDispose {
            // Restore everything if the composable leaves while fullscreen
            if (activity != null && window != null) {
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                WindowCompat.getInsetsController(window, window.decorView)
                    .show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }
}

tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

// ==========================================
// CONTROL OVERLAY COMPOSABLE
// ==========================================
@Composable
fun PlayerControlOverlay(
    areControlsVisible: Boolean,
    isDesktopFullscreen: Boolean,
    onToggleFullscreen: () -> Unit
) {
    AnimatedVisibility(
        visible = areControlsVisible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = Modifier.fillMaxSize()
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            IconButton(
                onClick = onToggleFullscreen,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
                    .background(Color.Black.copy(alpha = 0.6f), shape = MaterialTheme.shapes.small)
            ) {
                Icon(
                    imageVector = if (isDesktopFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                    contentDescription = "Toggle Fullscreen",
                    tint = Color.White
                )
            }
        }
    }
}

// ==========================================
// TAB 1: LIVE TV
// ==========================================
@OptIn(UnstableApi::class)
@Composable
fun LiveTvTab(
    isDesktopFullscreen: Boolean,
    onFullscreenChange: (Boolean) -> Unit
) {
    val context = LocalContext.current
    var channels by remember { mutableStateOf<List<IptvChannel>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    var selectedChannel by remember { mutableStateOf<IptvChannel?>(null) }
    var areControlsVisible by remember { mutableStateOf(true) }

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            playWhenReady = true
        }
    }

    LaunchedEffect(Unit) {
        channels = fetchIptvChannels()
        isLoading = false
    }

    DisposableEffect(Unit) {
        onDispose { exoPlayer.release() }
    }

    val filteredChannels = channels.filter {
        it.name.contains(searchQuery, ignoreCase = true)
    }

    Box(
        modifier = if (isDesktopFullscreen) {
            Modifier.fillMaxSize().background(Color.Black).zIndex(100f)
        } else {
            Modifier.fillMaxSize()
        }
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            if (selectedChannel != null) {
                Box(
                    modifier = if (isDesktopFullscreen) {
                        Modifier.fillMaxSize()
                    } else {
                        Modifier.fillMaxWidth().height(240.dp)
                    }
                ) {
                    AndroidView(
                        factory = { ctx ->
                            PlayerView(ctx).apply {
                                player = exoPlayer
                                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                                layoutParams = FrameLayout.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                                useController = true
                                setControllerVisibilityListener(
                                    PlayerView.ControllerVisibilityListener { visibility ->
                                        areControlsVisible = (visibility == View.VISIBLE)
                                    }
                                )
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )

                    PlayerControlOverlay(
                        areControlsVisible = areControlsVisible,
                        isDesktopFullscreen = isDesktopFullscreen,
                        onToggleFullscreen = { onFullscreenChange(!isDesktopFullscreen) }
                    )
                }
            }

            if (!isDesktopFullscreen) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    placeholder = { Text("Search channels...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true
                )

                Box(modifier = Modifier.fillMaxSize().weight(1f)) {
                    when {
                        isLoading -> {
                            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                        }
                        filteredChannels.isEmpty() -> {
                            Text(
                                text = "No channels found.",
                                modifier = Modifier.align(Alignment.Center)
                            )
                        }
                        else -> {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(filteredChannels) { channel ->
                                    val isSelected = selectedChannel?.url == channel.url
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                selectedChannel = channel
                                                exoPlayer.setMediaItem(MediaItem.fromUri(channel.url))
                                                exoPlayer.prepare()
                                                exoPlayer.play()
                                            },
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                            else MaterialTheme.colorScheme.surfaceVariant
                                        )
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(16.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Text(
                                                text = channel.name,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

suspend fun fetchIptvChannels(): List<IptvChannel> = withContext(Dispatchers.IO) {
    val channelList = mutableListOf<IptvChannel>()
    try {
        val url = URL("https://iptv-org.github.io/iptv/index.m3u")
        val connection = url.openConnection() as HttpURLConnection
        connection.connectTimeout = 8000
        connection.readTimeout = 8000

        val reader = connection.inputStream.bufferedReader()
        var line: String? = reader.readLine()
        var currentTitle = "Unknown Channel"

        while (line != null) {
            if (line.startsWith("#EXTINF")) {
                val commaIndex = line.lastIndexOf(',')
                if (commaIndex != -1 && commaIndex < line.length - 1) {
                    currentTitle = line.substring(commaIndex + 1).trim()
                }
            } else if (line.isNotEmpty() && !line.startsWith("#")) {
                channelList.add(IptvChannel(name = currentTitle, url = line.trim()))
                currentTitle = "Unknown Channel"
            }
            line = reader.readLine()
        }
        reader.close()
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return@withContext channelList
}


// ==========================================
// TAB 2: LOCAL VIDEOS
// ==========================================
@OptIn(UnstableApi::class)
@Composable
fun LocalVideosTab(
    isDesktopFullscreen: Boolean,
    onFullscreenChange: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val videoDir = remember { File(context.filesDir, "union_videos").apply { if (!exists()) mkdirs() } }

    val videoList = remember { mutableStateListOf<LocalVideo>() }
    var activeVideo by remember { mutableStateOf<LocalVideo?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var areControlsVisible by remember { mutableStateOf(true) }

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            playWhenReady = true
            addListener(object : Player.Listener {
                override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                    errorMessage = "Playback error: Unsupported video format/codec."
                    error.printStackTrace()
                }
            })
        }
    }

    LaunchedEffect(Unit) {
        val files = videoDir.listFiles { f -> f.extension.lowercase() == "mp4" } ?: emptyArray()
        videoList.clear()
        videoList.addAll(files.map { LocalVideo(it.nameWithoutExtension, Uri.fromFile(it)) })
    }

    DisposableEffect(Unit) {
        onDispose { exoPlayer.release() }
    }

    val videoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        for (uri in uris) {
            try {
                val originalName = getFileNameFromUri(context, uri) ?: "video_${System.currentTimeMillis()}"
                val safeName = originalName.substringBeforeLast('.')
                val destFile = File(videoDir, "$safeName.mp4")

                context.contentResolver.openInputStream(uri)?.use { input ->
                    destFile.outputStream().use { output -> input.copyTo(output) }
                }
                if (destFile.exists() && destFile.length() > 0) {
                    videoList.add(LocalVideo(destFile.nameWithoutExtension, Uri.fromFile(destFile)))
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    Box(
        modifier = if (isDesktopFullscreen) {
            Modifier.fillMaxSize().background(Color.Black).zIndex(100f)
        } else {
            Modifier.fillMaxSize()
        }
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(if (isDesktopFullscreen) 0.dp else 16.dp)) {
            if (!isDesktopFullscreen) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Local Video Library", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Button(onClick = { videoPicker.launch("video/*") }) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add MP4")
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            if (activeVideo != null) {
                Box(
                    modifier = if (isDesktopFullscreen) {
                        Modifier.fillMaxSize()
                    } else {
                        Modifier.fillMaxWidth().height(240.dp)
                    }
                ) {
                    AndroidView(
                        factory = { ctx ->
                            PlayerView(ctx).apply {
                                player = exoPlayer
                                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                                layoutParams = FrameLayout.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                                useController = true
                                setControllerVisibilityListener(
                                    PlayerView.ControllerVisibilityListener { visibility ->
                                        areControlsVisible = (visibility == View.VISIBLE)
                                    }
                                )
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )

                    PlayerControlOverlay(
                        areControlsVisible = areControlsVisible,
                        isDesktopFullscreen = isDesktopFullscreen,
                        onToggleFullscreen = { onFullscreenChange(!isDesktopFullscreen) }
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (errorMessage != null && !isDesktopFullscreen) {
                Text(
                    text = errorMessage!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (!isDesktopFullscreen) {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(videoList) { _, video ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    errorMessage = null
                                    activeVideo = video
                                    exoPlayer.stop()
                                    exoPlayer.setMediaItem(MediaItem.fromUri(video.uri))
                                    exoPlayer.prepare()
                                    exoPlayer.play()
                                }
                        ) {
                            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Movie, contentDescription = null)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(video.title, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }
        }
    }
}


// ==========================================
// TAB 3: YOUTUBE WEB VIEW
// ==========================================
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun YouTubeTab() {
    AndroidView(
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                webViewClient = WebViewClient()
                loadUrl("https://www.youtube.com")
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}

// ==========================================
// HELPER FUNCTIONS
// ==========================================
fun getFileNameFromUri(context: Context, uri: Uri): String? {
    var fileName: String? = null
    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (nameIndex != -1 && cursor.moveToFirst()) {
            fileName = cursor.getString(nameIndex)
        }
    }
    return fileName ?: uri.lastPathSegment
}