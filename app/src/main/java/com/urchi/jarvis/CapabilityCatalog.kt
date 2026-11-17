package com.urchi.jarvis

import java.util.Locale

/**
 * Scalable phrase grammar rather than a fake list of ten thousand unrelated
 * functions. It produces thousands of valid Russian/English command variants
 * from real device actions, so new wording does not require another giant
 * when-block in MainActivity.
 */
object CapabilityCatalog {
    private val verbs = listOf(
        "открой", "запусти", "включи", "покажи", "перейди в", "зайди в",
        "открой мне", "запусти мне", "включить", "открыть", "launch", "open", "start"
    )

    private val targets = mapOf(
        "youtube" to listOf("ютуб", "youtube", "ютюб", "ютуп", "ютубе"),
        "telegram" to listOf("телеграм", "телега", "telegram", "телеграмм"),
        "vk" to listOf("вк", "вконтакте", "vk"),
        "browser" to listOf("браузер", "интернет", "хром", "chrome", "яндекс"),
        "settings" to listOf("настройки", "параметры", "системные настройки"),
        "files" to listOf("файлы", "файловый менеджер", "проводник", "загрузки"),
        "gallery" to listOf("галерея", "фото", "фотографии"),
        "camera" to listOf("камеру", "камера", "фотоаппарат"),
        "calculator" to listOf("калькулятор", "калькуляцию"),
        "maps" to listOf("карты", "карты гугл", "навигатор"),
        "music" to listOf("музыку", "музыка", "плеер", "музыкальный плеер"),
        "chatgpt" to listOf("чатгпт", "chatgpt", "чат джипити"),
        "accessibility" to listOf("спецвозможности", "специальные возможности"),
        "wifi" to listOf("вайфай", "wi fi", "wifi"),
        "bluetooth" to listOf("блютуз", "bluetooth")
    )

    private val modifiers = listOf(
        "", " пожалуйста", " сейчас", " на телефоне", " на устройстве",
        " мне", " быстро", " прямо сейчас", " приложение", " программу",
        " без браузера", " с телефона", " на android", " в приложении"
    )

    private val generated: Map<String, String> by lazy {
        val out = LinkedHashMap<String, String>()
        targets.forEach { (canonical, names) ->
            verbs.forEach { verb ->
                names.forEach { name ->
                    modifiers.forEach { modifier ->
                        val phrase = normalize("$verb $name$modifier")
                        if (phrase.isNotBlank()) out[phrase] = canonical
                    }
                }
            }
        }
        out
    }

    /** Number of concrete phrase variants generated from the grammar. */
    val phraseCount: Int get() = generated.size

    fun resolve(raw: String): String? {
        val q = normalize(raw)
        generated[q]?.let { return canonical(it) }

        // Fuzzy match only when the command is clearly an app/device action.
        if (q.length < 4) return null
        val best = generated.entries
            .asSequence()
            .map { it to editDistance(q, it.key) }
            .filter { it.second <= maxOf(2, q.length / 7) }
            .minByOrNull { it.second }
        return best?.first?.value?.let(::canonical)
    }

    private fun canonical(target: String): String = when (target) {
        "youtube" -> "открой ютуб"
        "telegram" -> "открой телеграм"
        "vk" -> "открой вк"
        "browser" -> "открой браузер"
        "settings" -> "настройки"
        "files" -> "открой файлы"
        "gallery" -> "открой галерею"
        "camera" -> "открой камеру"
        "calculator" -> "открой калькулятор"
        "maps" -> "открой карты"
        "music" -> "открой музыку"
        "chatgpt" -> "открой chatgpt"
        "accessibility" -> "открой специальные возможности"
        "wifi" -> "открой настройки wi-fi"
        "bluetooth" -> "открой bluetooth"
        else -> target
    }

    private fun normalize(value: String): String =
        value.lowercase(Locale.ROOT).replace('ё', 'е')
            .replace(Regex("""[^\p{L}\p{N} ]"""), " ")
            .replace(Regex("""\s+"""), " ").trim()

    private fun editDistance(a: String, b: String): Int {
        if (a == b) return 0
        if (a.isEmpty()) return b.length
        if (b.isEmpty()) return a.length
        var p = IntArray(b.length + 1) { it }
        for (i in a.indices) {
            val n = IntArray(b.length + 1)
            n[0] = i + 1
            for (j in b.indices) {
                n[j + 1] = minOf(n[j] + 1, p[j + 1] + 1, p[j] + if (a[i] == b[j]) 0 else 1)
            }
            p = n
        }
        return p[b.length]
    }
}
