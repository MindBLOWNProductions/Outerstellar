package com.mindblownproductions.union

import android.content.Context
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory

data class Article(
    val title: String,
    val link: String,
    val source: String,
    val pubDate: String
)

@Composable
fun NewsScreen() {
    val categories = listOf("World", "Tech", "Sports", "Entertainment")
    var selectedCategoryIndex by remember { mutableStateOf(0) }

    // Updated RSS feed URLs for robust parsing
    val rssUrls = listOf(
        "https://feeds.bbci.co.uk/news/world/rss.xml",                 // World
        "https://rss.nytimes.com/services/xml/rss/nyt/Technology.xml", // Tech
        "https://rss.nytimes.com/services/xml/rss/nyt/Sports.xml",     // Sports
        "https://feeds.bbci.co.uk/news/entertainment_and_arts/rss.xml" // Entertainment
    )

    var articles by remember { mutableStateOf<List<Article>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current

    // Fetch news articles when category changes
    LaunchedEffect(selectedCategoryIndex) {
        isLoading = true
        errorMessage = null
        try {
            articles = fetchRssFeed(rssUrls[selectedCategoryIndex], categories[selectedCategoryIndex])
            // Trigger a notification for the top breaking headline when loaded
            if (articles.isNotEmpty()) {
                onNewHeadlineReceived(context, articles.first().title)
            }
        } catch (e: Exception) {
            errorMessage = "Could not load news. Check your internet connection."
        }
        isLoading = false
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Top Category Bar
        ScrollableTabRow(
            selectedTabIndex = selectedCategoryIndex,
            edgePadding = 16.dp
        ) {
            categories.forEachIndexed { index, category ->
                Tab(
                    selected = selectedCategoryIndex == index,
                    onClick = { selectedCategoryIndex = index },
                    text = { Text(category) }
                )
            }
        }

        // News Content Area
        Box(modifier = Modifier.fillMaxSize()) {
            when {
                isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                errorMessage != null -> {
                    Text(
                        text = errorMessage ?: "Unknown error",
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp),
                        color = MaterialTheme.colorScheme.error
                    )
                }
                articles.isEmpty() -> {
                    Text(
                        text = "No articles found.",
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(articles) { article ->
                            NewsArticleCard(article = article) {
                                openArticleUrl(context, article.link)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NewsArticleCard(article: Article, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Source Tag
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = MaterialTheme.shapes.small
            ) {
                Text(
                    text = article.source,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Headline
            Text(
                text = article.title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium
            )

            if (article.pubDate.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = article.pubDate,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

// Function to safely open original news links in Custom Tabs
fun openArticleUrl(context: Context, url: String) {
    try {
        val builder = CustomTabsIntent.Builder()
        val customTabsIntent = builder.build()
        customTabsIntent.launchUrl(context, Uri.parse(url))
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

// Notification trigger helper function placed at the root file level
fun onNewHeadlineReceived(context: Context, newHeadline: String) {
    NotificationHelper.sendNewsNotification(
        context = context,
        title = "Breaking News",
        message = newHeadline
    )
}

// Robust RSS Parser executed off the main UI thread
suspend fun fetchRssFeed(feedUrl: String, sourceName: String): List<Article> = withContext(Dispatchers.IO) {
    val articlesList = mutableListOf<Article>()
    try {
        val url = URL(feedUrl)
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Android; Mobile)")
        connection.connectTimeout = 8000
        connection.readTimeout = 8000
        connection.instanceFollowRedirects = true

        val inputStream = connection.inputStream
        val factory = XmlPullParserFactory.newInstance()
        val parser = factory.newPullParser()
        parser.setInput(inputStream, "UTF-8")

        var eventType = parser.eventType
        var currentTitle = ""
        var currentLink = ""
        var currentPubDate = ""
        var insideItem = false

        while (eventType != XmlPullParser.END_DOCUMENT) {
            val tagName = parser.name
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    if (tagName.equals("item", ignoreCase = true)) {
                        insideItem = true
                    } else if (insideItem) {
                        when (tagName.lowercase()) {
                            "title" -> currentTitle = parser.nextText()
                            "link" -> currentLink = parser.nextText()
                            "pubdate" -> currentPubDate = parser.nextText()
                        }
                    }
                }
                XmlPullParser.END_TAG -> {
                    if (tagName.equals("item", ignoreCase = true)) {
                        if (currentTitle.isNotEmpty()) {
                            articlesList.add(
                                Article(
                                    title = currentTitle.trim(),
                                    link = currentLink.trim(),
                                    source = sourceName,
                                    pubDate = currentPubDate.trim()
                                )
                            )
                        }
                        insideItem = false
                        currentTitle = ""
                        currentLink = ""
                        currentPubDate = ""
                    }
                }
            }
            eventType = parser.next()
        }
        connection.disconnect()
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return@withContext articlesList
}