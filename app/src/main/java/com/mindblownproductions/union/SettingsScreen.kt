package com.mindblownproductions.union

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.io.File

enum class ThemeSetting { LIGHT, DARK, AUTO }

@Composable
fun SettingsScreen(
    currentTheme: ThemeSetting,
    onThemeChanged: (ThemeSetting) -> Unit
) {
    val context = LocalContext.current
    var showClearCacheDialog by remember { mutableStateOf(false) }
    var cacheSizeText by remember { mutableStateOf(calculateCacheSize(context)) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Settings",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        // ==========================================
        // 1. APPEARANCE SECTION
        // ==========================================
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Appearance",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Theme Mode", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = currentTheme == ThemeSetting.LIGHT,
                            onClick = { onThemeChanged(ThemeSetting.LIGHT) },
                            label = { Text("Light") },
                            leadingIcon = { Icon(Icons.Default.LightMode, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = currentTheme == ThemeSetting.DARK,
                            onClick = { onThemeChanged(ThemeSetting.DARK) },
                            label = { Text("Dark") },
                            leadingIcon = { Icon(Icons.Default.DarkMode, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = currentTheme == ThemeSetting.AUTO,
                            onClick = { onThemeChanged(ThemeSetting.AUTO) },
                            label = { Text("Auto") },
                            leadingIcon = { Icon(Icons.Default.BrightnessAuto, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // ==========================================
        // 2. PLAYBACK & MEDIA SECTION
        // ==========================================
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Playback",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Tv, contentDescription = null)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Hardware Acceleration", fontWeight = FontWeight.Bold)
                            Text("Use hardware decoders for smooth IPTV & local streaming", style = MaterialTheme.typography.bodySmall)
                        }
                        var hwEnabled by remember { mutableStateOf(true) }
                        Switch(checked = hwEnabled, onCheckedChange = { hwEnabled = it })
                    }
                }
            }
        }

        // ==========================================
        // 3. STORAGE & CACHE SECTION
        // ==========================================
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Storage & Data",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showClearCacheDialog = true },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Storage, contentDescription = null)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Local Video Cache", fontWeight = FontWeight.Bold)
                            Text("Current size: $cacheSizeText", style = MaterialTheme.typography.bodySmall)
                        }
                        Icon(Icons.Default.Delete, contentDescription = "Clear Cache", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }

        // ==========================================
        // 4. ABOUT Outerstellar SECTION
        // ==========================================
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "About Outerstellar",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, contentDescription = null)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Outerstellar Platform v1.01", fontWeight = FontWeight.Bold)
                            Text("Android-exclusive immersive media & arcade suite", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }

    // Clear Cache Confirmation Dialog
    if (showClearCacheDialog) {
        AlertDialog(
            onDismissRequest = { showClearCacheDialog = false },
            title = { Text("Clear Local Videos?") },
            text = { Text("This will delete all imported local MP4 files stored inside Outerstellar.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        clearVideoCache(context)
                        cacheSizeText = calculateCacheSize(context)
                        showClearCacheDialog = false
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearCacheDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

// Helper utility functions for cache management
fun calculateCacheSize(context: Context): String {
    val videoDir = File(context.filesDir, "union_videos")
    if (!videoDir.exists()) return "0 KB"
    val sizeBytes = videoDir.listFiles()?.sumOf { it.length() } ?: 0L
    val sizeMb = sizeBytes / (1024.0 * 1024.0)
    return if (sizeMb < 1) "${sizeBytes / 1024} KB" else String.format("%.2f MB", sizeMb)
}

fun clearVideoCache(context: Context) {
    val videoDir = File(context.filesDir, "union_videos")
    if (videoDir.exists()) {
        videoDir.listFiles()?.forEach { it.delete() }
    }
}