package com.urchi.jarvis

import android.os.Handler
import android.os.Looper
import android.util.Xml
import java.io.BufferedInputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

/**
 * Получает свежие заголовки из публичных RSS/Atom-лент.
 * Новости не вшиваются в APK и не изменяют модель: JARVIS получает их при запросе.
 */
class NewsFeedStore {
    enum class Region(val title: String) { RUSSIA("Россия"), WORLD("Мир") }

    data class Article(
        val title: String,
        val source: String,
        val url: String,
        val published: String
    )

    private data class Feed(val source: String, val url: String, val region: Region)

    private val feeds = listOf(
        Feed("РИА Новости", "https://ria.ru/export/rss2/index.xml", Region.RUSSIA),
        Feed("ТАСС", "https://tass.ru/rss/v2.xml", Region.RUSSIA),
        Feed("Deutsche Welle", "https://rss.dw.com/rdf/rss-en-all", Region.WORLD),
        Feed("Al Jazeera", "https://www.aljazeera.com/xml/rss/all.xml", Region.WORLD)
    )

    private val executor = Executors.newFixedThreadPool(4)
    private val main = Handler(Looper.getMainLooper())

    fun load(region: Region, limit: Int = 12, callback: (Result<List<Article>>) -> Unit) {
        val safeLimit = limit.coerceIn(1, 30)
        executor.execute {
            val articles = mutableListOf<Article>()
            var lastError: Throwable? = null

            for (feed in feeds.filter { it.region == region }) {
                try {
                    articles += fetchFeed(feed)
                    if (articles.size >= safeLimit) break
                } catch (t: Throwable) {
                    lastError = t
                }
            }

            val result = if (articles.isNotEmpty()) {
                Result.success(articles.distinctBy { it.url.ifBlank { it.title } }.take(safeLimit))
            } else {
                Result.failure(lastError ?: IllegalStateException("Новостные ленты недоступны"))
            }
            main.post { callback(result) }
        }
    }

    private fun fetchFeed(feed: Feed): List<Article> {
        val connection = (URL(feed.url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 8000
            readTimeout = 10000
            requestMethod = "GET"
            setRequestProperty("User-Agent", "JARVIS-Android/1.1")
            setRequestProperty("Accept", "application/rss+xml, application/atom+xml, application/xml, text/xml")
            instanceFollowRedirects = true
        }
        return try {
            if (connection.responseCode !in 200..299) {
                throw IllegalStateException("HTTP ${connection.responseCode}")
            }
            BufferedInputStream(connection.inputStream).use { parseXml(it, feed.source) }
        } finally {
            connection.disconnect()
        }
    }

    private fun parseXml(stream: BufferedInputStream, source: String): List<Article> {
        val parser = Xml.newPullParser().apply { setInput(stream, "UTF-8") }
        val result = mutableListOf<Article>()
        var event = parser.eventType
        var inItem = false
        var title = ""
        var link = ""
        var published = ""
        var currentTag = ""

        while (event != org.xmlpull.v1.XmlPullParser.END_DOCUMENT && result.size < 20) {
            when (event) {
                org.xmlpull.v1.XmlPullParser.START_TAG -> {
                    currentTag = parser.name.lowercase()
                    if (currentTag == "item" || currentTag == "entry") {
                        inItem = true
                        title = ""
                        link = ""
                        published = ""
                    } else if (inItem && currentTag == "link") {
                        parser.getAttributeValue(null, "href")?.trim()?.let { if (it.isNotBlank()) link = it }
                    }
                }
                org.xmlpull.v1.XmlPullParser.TEXT -> if (inItem) {
                    val text = parser.text?.trim().orEmpty()
                    when (currentTag) {
                        "title" -> if (text.isNotBlank()) title = text
                        "link" -> if (text.isNotBlank() && link.isBlank()) link = text
                        "pubdate", "published", "updated", "dc:date" ->
                            if (text.isNotBlank()) published = text
                    }
                }
                org.xmlpull.v1.XmlPullParser.END_TAG -> {
                    val name = parser.name.lowercase()
                    if (inItem && (name == "item" || name == "entry")) {
                        if (title.isNotBlank()) {
                            result += Article(
                                title.replace(Regex("\\s+"), " ").trim(),
                                source,
                                link.trim(),
                                published.trim()
                            )
                        }
                        inItem = false
                    }
                    currentTag = ""
                }
            }
            event = parser.next()
        }
        return result
    }

    fun close() { executor.shutdownNow() }
}
