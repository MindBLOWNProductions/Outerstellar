package com.mindblownproductions.union

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.delay
import java.io.File
import java.util.Locale

data class LocalTrack(
    val title: String,
    val uri: Uri
)

@OptIn(UnstableApi::class)
@Composable
fun MusicScreen() {
    val context = LocalContext.current

    val playlist = remember { mutableStateListOf<LocalTrack>() }
    var currentTrackIndex by remember { mutableIntStateOf(-1) }
    var isPlaying by remember { mutableStateOf(false) }
    var currentPosition by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }

    // Load saved tracks from app-specific directory on startup
    LaunchedEffect(Unit) {
        val loadedTracks = loadSavedTracks(context)
        playlist.clear()
        playlist.addAll(loadedTracks)
        if (playlist.isNotEmpty() && currentTrackIndex == -1) {
            currentTrackIndex = 0
        }
    }

    // ExoPlayer setup
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            addListener(object : Player.Listener {
                override fun onIsPlayingChanged(playing: Boolean) {
                    isPlaying = playing
                }

                override fun onPlaybackStateChanged(state: Int) {
                    if (state == Player.STATE_READY) {
                        duration = this@apply.duration.coerceAtLeast(0L)
                    }
                }
            })
        }
    }

    // System file picker to select MP3 files and copy them to app storage
    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNotEmpty()) {
            val newlySavedTracks = mutableListOf<LocalTrack>()
            for (uri in uris) {
                val savedFile = copyUriToAppStorage(context, uri)
                if (savedFile != null) {
                    newlySavedTracks.add(
                        LocalTrack(
                            title = savedFile.nameWithoutExtension,
                            uri = Uri.fromFile(savedFile)
                        )
                    )
                }
            }

            if (newlySavedTracks.isNotEmpty()) {
                val startPlaying = playlist.isEmpty()
                playlist.addAll(newlySavedTracks)
                if (startPlaying) {
                    currentTrackIndex = playlist.size - newlySavedTracks.size
                }
            }
        }
    }

    // Load track whenever index changes
    LaunchedEffect(currentTrackIndex) {
        if (currentTrackIndex in playlist.indices) {
            val track = playlist[currentTrackIndex]
            val mediaItem = MediaItem.fromUri(track.uri)
            exoPlayer.setMediaItem(mediaItem)
            exoPlayer.prepare()
            exoPlayer.playWhenReady = true
        }
    }

    // Track playback progress
    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            currentPosition = exoPlayer.currentPosition.coerceAtLeast(0L)
            duration = exoPlayer.duration.coerceAtLeast(0L)
            delay(500)
        }
    }

    // Release player on exit
    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header & Add Music Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Music Library",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Button(
                onClick = { audioPickerLauncher.launch("audio/*") }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add MP3")
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add MP3s")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (playlist.isEmpty()) {
            // Empty State
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = "No Music",
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "No songs added yet",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Text(
                        text = "Tap 'Add MP3s' to save audio files to UNION",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        } else {
            // Playlist view
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(playlist) { index, track ->
                    val isSelected = index == currentTrackIndex
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { currentTrackIndex = index },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.MusicNote,
                                contentDescription = null,
                                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = track.title,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Player Bar at the bottom
            val currentTrack = playlist.getOrNull(currentTrackIndex)
            if (currentTrack != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = currentTrack.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Slider(
                            value = if (duration > 0) currentPosition.toFloat() / duration.toFloat() else 0f,
                            onValueChange = { fraction ->
                                val targetMs = (fraction * duration).toLong()
                                exoPlayer.seekTo(targetMs)
                                currentPosition = targetMs
                            },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = formatMillis(currentPosition),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                            Text(
                                text = formatMillis(duration),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            IconButton(
                                onClick = {
                                    if (currentTrackIndex > 0) {
                                        currentTrackIndex--
                                    } else {
                                        currentTrackIndex = playlist.size - 1
                                    }
                                }
                            ) {
                                Icon(Icons.Default.SkipPrevious, contentDescription = "Previous")
                            }

                            FilledIconButton(
                                onClick = {
                                    if (isPlaying) {
                                        exoPlayer.pause()
                                    } else {
                                        exoPlayer.play()
                                    }
                                },
                                modifier = Modifier.size(56.dp)
                            ) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = if (isPlaying) "Pause" else "Play"
                                )
                            }

                            IconButton(
                                onClick = {
                                    if (currentTrackIndex < playlist.size - 1) {
                                        currentTrackIndex++
                                    } else {
                                        currentTrackIndex = 0
                                    }
                                }
                            ) {
                                Icon(Icons.Default.SkipNext, contentDescription = "Next")
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- Storage & Helper Functions ---

fun getMusicDirectory(context: Context): File {
    val dir = File(context.filesDir, "union_music")
    if (!dir.exists()) {
        dir.mkdirs()
    }
    return dir
}

fun copyUriToAppStorage(context: Context, uri: Uri): File? {
    try {
        val musicDir = getMusicDirectory(context)
        // Get original file name or generate one
        var fileName = "track_${System.currentTimeMillis()}.mp3"

        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            if (nameIndex != -1 && cursor.moveToFirst()) {
                fileName = cursor.getString(nameIndex)
            }
        }

        val destinationFile = File(musicDir, fileName)
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            destinationFile.outputStream().use { outputStream ->
                inputStream.copyTo(outputStream)
            }
        }
        return destinationFile
    } catch (e: Exception) {
        e.printStackTrace()
        return null
    }
}

fun loadSavedTracks(context: Context): List<LocalTrack> {
    val musicDir = getMusicDirectory(context)
    val files = musicDir.listFiles { file -> file.extension.lowercase() == "mp3" } ?: return emptyList()

    return files.sortedBy { it.name }.map { file ->
        LocalTrack(
            title = file.nameWithoutExtension,
            uri = Uri.fromFile(file)
        )
    }
}

fun formatMillis(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
}