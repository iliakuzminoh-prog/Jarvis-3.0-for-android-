package com.urchi.jarvis

import java.util.Locale

/**
 * Explicit Russian orthography helper.
 * It only activates for spelling/orthography questions; ordinary conversation
 * is never rewritten as a spelling task.
 */
object SpellCheckEngine {
    data class Correction(val input: String, val correct: String, val explanation: String)

    private val corrections = mapOf(
        "женидьба" to Correction("женидьба", "женитьба",
            "Пишется «женитьба», через «т» и с мягким знаком перед «б». Проверочное слово — «женить»: буква «т» сохраняется."),
        "жыраф" to Correction("жыраф", "жираф",
            "«Жираф» пишется с буквой «и»: сочетания «жи–ши» пишутся с «и»."),
        "жыли" to Correction("жыли", "жили",
            "«Жили» пишется с «и»: после «ж» в сочетании «жи–ши» пишется «и»."),
        "шыпы" to Correction("шыпы", "шипы",
            "«Шипы» пишется с «и»: после «ш» в сочетании «жи–ши» пишется «и»."),
        "карова" to Correction("карова", "корова",
            "«Корова» пишется через безударную «о» в первом слоге. Проверочное слово — «кóровы» (форма множественного числа сохраняет корень; для проверки безударной гласной лучше подобрать однокоренное слово с ударением)."),
        "малако" to Correction("малако", "молоко",
            "«Молоко» пишется с двумя буквами «о» в безударных слогах; проверочное слово — «мóлок»."),
        "щяс" to Correction("щяс", "сейчас",
            "Нормативное написание — «сейчас». Это словарное слово, написание нужно запомнить."),
        "пажалуста" to Correction("пажалуста", "пожалуйста",
            "Правильно: «пожалуйста». Это словарное слово; в нём пишется «-уйста»."),
        "извените" to Correction("извените", "извините",
            "Правильно: «извините», через «и» после «в»."),
        "превет" to Correction("превет", "привет",
            "Правильно: «привет», через «и»."),
        "вообщем" to Correction("вообщем", "в общем",
            "В значении «в целом» правильно писать раздельно: «в общем». Слово «вообще» пишется слитно, но имеет другое значение.")
    )

    fun answer(raw: String): String? {
        val q = normalize(raw)
        if (!isSpellingQuestion(q)) return null
        val token = q.split(' ').lastOrNull()?.trim() ?: return null
        val correction = corrections[token]
        if (correction != null) {
            return "Правильно: «${correction.correct}».\n\n${correction.explanation}\n\nПример: «${example(correction.correct)}»."
        }
        val candidates = corrections.values.distinct()
            .map { it to distance(token, it.input) }
            .filter { it.second <= 2 }
            .sortedBy { it.second }
        val best = candidates.firstOrNull()
        return if (best != null) {
            "Возможно, ты имел в виду «${best.first.correct}». Но без контекста я не буду выдавать догадку за точную проверку. Пришли всё предложение — проверю написание в контексте."
        } else {
            "Я распознал вопрос о правописании слова «$token», но в моём локальном орфографическом словаре пока нет надёжного совпадения. Не буду выдумывать правило. Пришли предложение целиком или разреши проверить слово по словарному источнику."
        }
    }

    private fun isSpellingQuestion(q: String): Boolean =
        listOf("как правильно написать", "как пишется", "как правильно пишется",
            "как правильно по русски", "как написать по русски", "проверь правописание",
            "проверь орфографию", "правописание слова", "орфография слова")
            .any { q.contains(it) }

    private fun example(word: String): String = when (word) {
        "женитьба" -> "Их женитьба состоялась летом."
        "жираф" -> "Жираф живёт в Африке."
        "жили" -> "Раньше они жили в деревне."
        "шипы" -> "У розы острые шипы."
        "корова" -> "Корова пасётся на лугу."
        "молоко" -> "Я налил молоко в стакан."
        "сейчас" -> "Я сейчас закончу работу."
        "пожалуйста" -> "Передай книгу, пожалуйста."
        "извините" -> "Извините за опоздание."
        "привет" -> "Привет, как дела?"
        "в общем" -> "В общем, задача решена."
        else -> word
    }

    private fun normalize(s: String): String = s.lowercase(Locale.ROOT)
        .replace('ё', 'е').replace(Regex("[^\\p{L}\\p{N} ]"), " ")
        .replace(Regex("\\s+"), " ").trim()

    private fun distance(a: String, b: String): Int {
        if (a == b) return 0
        if (a.isEmpty()) return b.length
        if (b.isEmpty()) return a.length
        var previous = IntArray(b.length + 1) { it }
        for (i in a.indices) {
            val current = IntArray(b.length + 1)
            current[0] = i + 1
            for (j in b.indices) {
                val cost = if (a[i] == b[j]) 0 else 1
                current[j + 1] = minOf(current[j] + 1, previous[j + 1] + 1, previous[j] + cost)
                if (i > 0 && j > 0 && a[i] == b[j - 1] && a[i - 1] == b[j]) {
                    current[j + 1] = minOf(current[j + 1], (if (j > 1) previous[j - 1] else 0) + 1)
                }
            }
            previous = current
        }
        return previous[b.length]
    }
}
