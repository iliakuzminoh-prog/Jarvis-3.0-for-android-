package com.urchi.jarvis

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URLEncoder
import java.net.URL
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * Background web retrieval without opening a browser window.
 * It deliberately uses public machine-readable endpoints instead of scraping
 * the user's visible browser session.
 */
class WebInfoService : AutoCloseable {
    data class Result(val title: String, val source: String, val text: String, val url: String)

    private val executor = Executors.newCachedThreadPool()

    fun lookup(query: String, callback: (kotlin.Result<List<Result>>) -> Unit) {
        val q = query.trim().take(300)
        if (q.isBlank()) {
            callback(kotlin.Result.failure(IllegalArgumentException("Пустой запрос")))
            return
        }
        executor.execute {
            val result = runCatching {
                val list = mutableListOf<Result>()
                wikipedia(q)?.let { list += it }
                list += googleNews(q).take(6)
                if (list.isEmpty()) error("Веб-источники не вернули результатов")
                list.distinctBy { it.url }.take(8)
            }
            callback(result)
        }
    }

    private fun wikipedia(query: String): Result? {
        val lang = if (query.any { it in 'а'..'я' || it in 'А'..'Я' }) "ru" else "en"
        val directTitle = URLEncoder.encode(query, "UTF-8")
        val direct = pageSummary(lang, directTitle)
        if (direct != null) return direct

        // Search first, then fetch the best matching encyclopedia page. This is
        // substantially more reliable for natural questions such as "кем была
        // мама Пушкина и как её звали?", where the whole sentence is not a page title.
        val searchUrl = URL(
            "https://$lang.wikipedia.org/w/api.php?action=query&list=search&srsearch=" +
                URLEncoder.encode(query, "UTF-8") +
                "&srlimit=3&format=json&utf8=1"
        )
        val searchJson = get(searchUrl) ?: return null
        val results = JSONObject(searchJson).optJSONObject("query")?.optJSONArray("search")
            ?: return null
        for (i in 0 until results.length()) {
            val title = results.optJSONObject(i)?.optString("title").orEmpty()
            if (title.isBlank()) continue
            pageSummary(lang, URLEncoder.encode(title, "UTF-8"))?.let { return it }
        }
        return null
    }

    private fun pageSummary(lang: String, encodedTitle: String): Result? {
        val json = get(URL("https://$lang.wikipedia.org/api/rest_v1/page/summary/$encodedTitle"))
            ?: return null
        val o = JSONObject(json)
        val extract = o.optString("extract").trim()
        if (extract.isBlank()) return null
        return Result(
            o.optString("title", encodedTitle),
            "Wikipedia ($lang)",
            extract.take(3500),
            o.optJSONObject("content_urls")?.optJSONObject("desktop")?.optString("page")
                .orEmpty()
        )
    }

    private fun googleNews(query: String): List<Result> {
        val encoded = URLEncoder.encode(query, "UTF-8")
        val xml = get(URL("https://news.google.com/rss/search?q=$encoded&hl=ru&gl=RU&ceid=RU:ru"))
            ?: return emptyList()
        val regex = Regex(
            "<item>(.*?)</item>",
            setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE)
        )
        return regex.findAll(xml).mapNotNull { m ->
            val block = m.groupValues[1]
            fun tag(name: String): String {
                val v = Regex("<$name[^>]*>(.*?)</$name>", RegexOption.DOT_MATCHES_ALL)
                    .find(block)?.groupValues?.getOrNull(1).orEmpty()
                return v.replace("<![CDATA[", "").replace("]]>", "")
                    .replace(Regex("<[^>]+>"), "").trim()
            }
            val title = tag("title")
            val link = tag("link")
            if (title.isBlank() || link.isBlank()) null
            else Result(title, tag("source").ifBlank { "Новости" }, tag("description").ifBlank { title }.take(1200), link)
        }.toList()
    }

    private fun get(url: URL): String? {
        val c = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 7000
            readTimeout = 9000
            setRequestProperty("User-Agent", "JARVIS/4 Android assistant")
            setRequestProperty("Accept", "application/json, application/rss+xml, application/xml, text/xml, text/plain")
        }
        return try {
            if (c.responseCode !in 200..299) return null
            BufferedReader(InputStreamReader(c.inputStream, Charsets.UTF_8)).use { it.readText() }
        } finally { c.disconnect() }
    }

    override fun close() {
        executor.shutdownNow()
        executor.awaitTermination(1, TimeUnit.SECONDS)
    }
}
