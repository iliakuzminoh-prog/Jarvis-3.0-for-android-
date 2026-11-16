package com.urchi.jarvis

import java.util.Locale

/**
 * Explicit classifier for media requests. Must run before knowledge/conversation
 * handlers so words such as "video" cannot be answered as unrelated school facts.
 */
object MediaIntentRouter {
    enum class Kind {
        YOUTUBE_RECOMMENDATIONS,
        VK_RECOMMENDATIONS,
        YOUTUBE_SEARCH,
        VK_VIDEO_SEARCH,
        VK_MUSIC_SEARCH,
        MUSIC_NAME_REQUIRED
    }

    data class Request(val kind: Kind, val query: String = "")

    fun resolve(raw: String): Request? {
        val q = normalize(raw)
        if (q.isBlank()) return null

        val asksVideo = listOf("видео", "ролик", "видеоролик").any { q.contains(it) }
        val asksRecommendation = listOf("рекомендац", "любое видео", "любой ролик", "что-нибудь посмотреть",
            "что нибудь посмотреть", "любое из").any { q.contains(it) }
        val vk = q.contains("вк") || q.contains("вконтакте") || q.contains("vk")
        val youtube = q.contains("ютуб") || q.contains("youtube")

        if (asksVideo && asksRecommendation && vk) return Request(Kind.VK_RECOMMENDATIONS)
        if (asksVideo && asksRecommendation && youtube) return Request(Kind.YOUTUBE_RECOMMENDATIONS)

        if ((q.contains("видео вк") || q.contains("вк видео") || q.contains("видео в вк")) && !asksRecommendation) {
            val query = extract(q, listOf("видео вк", "вк видео", "видео в вк", "вконтакте видео"))
            return Request(Kind.VK_VIDEO_SEARCH, query)
        }
        if (youtube && asksVideo) {
            val query = extract(q, listOf("ютуб", "youtube", "видео", "ролик", "включи", "найди", "поищи", "запусти"))
            return if (asksRecommendation && query.isBlank()) Request(Kind.YOUTUBE_RECOMMENDATIONS)
                else Request(Kind.YOUTUBE_SEARCH, query)
        }

        val songVerb = listOf("включи песню", "включи трек", "найди песню", "найди трек", "поставь песню")
            .firstOrNull { q.startsWith(it) }
        if (songVerb != null) {
            val title = q.removePrefix(songVerb).trim()
            return if (title.isBlank()) Request(Kind.MUSIC_NAME_REQUIRED)
                else Request(Kind.VK_MUSIC_SEARCH, title)
        }
        return null
    }

    private fun extract(q: String, markers: List<String>): String {
        var result = q
        markers.forEach { result = result.replace(it, " ") }
        return result.replace(Regex("\\b(из|в|на|мне|пожалуйста|любое|любой)\\b"), " ")
            .replace(Regex("\\s+"), " ").trim()
    }

    private fun normalize(value: String) = value.lowercase(Locale.ROOT).replace('ё', 'е')
        .replace(Regex("[^\\p{L}\\p{N} ]"), " ").replace(Regex("\\s+"), " ").trim()
}
