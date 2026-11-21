package com.urchi.jarvis

import java.util.Locale

/**
 * Cheap local classifier. It runs before any network request and deliberately
 * avoids pretending that "simple" means "easy": long, multi-part or coding/media
 * requests stay on the full reasoning path.
 */
object FastPathClassifier {
    enum class Level { FAST, NORMAL, DEEP }

    fun classify(text: String): Level {
        val q = text.trim().lowercase(Locale.ROOT).replace('ё', 'е')
        if (q.isBlank()) return Level.FAST
        if (q.length > 420) return Level.DEEP
        if (listOf(
                "напиши код", "создай программу", "исправь код", "write code",
                "generate code", "create a program", "сгенерируй видео",
                "создай видео", "сгенерируй картинку", "создай изображение",
                "проанализируй", "собери apk", "собери exe", "собери проект",
                "план", "архитектур", "сравни", "докажи", "почему", "пошагово",
                "разбери", "исследуй", "review", "refactor", "debug"
            ).any { q.contains(it) }) return Level.DEEP
        val words = q.split(Regex("\\s+")).filter { it.isNotBlank() }
        if (words.size <= 14 && q.length <= 180) return Level.FAST
        return Level.NORMAL
    }

    fun isFast(text: String) = classify(text) == Level.FAST
}
