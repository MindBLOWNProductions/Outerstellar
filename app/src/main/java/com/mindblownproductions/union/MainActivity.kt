package com.mindblownproductions.union

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize Notification Channel for news alerts
        NotificationHelper.createNotificationChannel(this)

        setContent {
            // Manage theme state at the root level so Settings can update it
            var currentTheme by remember { mutableStateOf(ThemeSetting.AUTO) }
            val darkTheme = when (currentTheme) {
                ThemeSetting.LIGHT -> false
                ThemeSetting.DARK -> true
                ThemeSetting.AUTO -> isSystemInDarkTheme()
            }

            // Wrap in MaterialTheme using your dynamic theme state
            MaterialTheme(
                colorScheme = if (darkTheme) darkColorScheme() else lightColorScheme()
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen(
                        currentTheme = currentTheme,
                        onThemeChanged = { currentTheme = it }
                    )
                }
            }
        }
    }
}

@Composable
fun MainScreen(
    currentTheme: ThemeSetting,
    onThemeChanged: (ThemeSetting) -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    var isFullscreen by remember { mutableStateOf(false) }
    val items = listOf("Home", "TV", "Games", "News", "Music", "Settings")

    Scaffold(
        bottomBar = {
            // Hidden while a video is fullscreen
            if (!isFullscreen) {
                NavigationBar {
                    items.forEachIndexed { index, item ->
                        NavigationBarItem(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            label = { Text(item) },
                            icon = { /* Add icons later */ }
                        )
                    }
                }
            }
        }
    ) { innerPadding: PaddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(if (isFullscreen) PaddingValues(0.dp) else innerPadding)
        ) {
            when (selectedTab) {
                0 -> HomeScreen(onNavigateToTab = { targetTab -> selectedTab = targetTab })
                1 -> TvHubScreen(
                    isFullscreen = isFullscreen,
                    onFullscreenChange = { isFullscreen = it }
                )
                2 -> GamesScreen()
                3 -> NewsScreen()
                4 -> MusicScreen()
                5 -> SettingsScreen(
                    currentTheme = currentTheme,
                    onThemeChanged = onThemeChanged
                )
            }
        }
    }
}