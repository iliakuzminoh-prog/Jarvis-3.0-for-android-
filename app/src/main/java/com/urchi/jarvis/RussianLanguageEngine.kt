package com.urchi.jarvis

import java.util.Locale
import kotlin.math.abs

/** Локальный языковой слой: разговорные формулировки, RU/EN и простая арифметика. */
object RussianLanguageEngine {
    private data class Intent(val patterns: List<String>, val ru: String, val en: String)

    private val intents = listOf(
        Intent(listOf("привет","здравствуй","здравствуйте","здорово","здарова","добрый день","доброе утро","добрый вечер","hello","hi","hey"),"Привет. Я слушаю.","Hello. I'm listening."),
        Intent(listOf("как дела","как ты","как поживаешь","как жизнь","что нового","how are you","how are things","how is it going"),"Работаю нормально. Готов помочь.","I'm working normally. I'm ready to help."),
        Intent(listOf("спасибо","спс","благодарю","thank you","thanks","thx"),"Пожалуйста.","You're welcome."),
        Intent(listOf("пожалуйста","не за что","не стоит","you are welcome","you're welcome"),"Обращайся.","Anytime."),
        Intent(listOf("кто ты","ты кто","что ты такое","расскажи о себе","who are you","what are you"),"Я JARVIS — локальный помощник этого проекта.","I'm JARVIS, the local assistant in this project."),
        Intent(listOf("кто твой создатель","кто тебя создал","кто тебя сделал","кто автор","who created you","who is your creator","who made you"),"Меня создали для проекта JARVIS Ильи Кузьмина. ChatGPT не является моим создателем — это отдельный ИИ-сервис.","I was created for Ilya Kuzmin's JARVIS project. ChatGPT is not my creator; it is a separate AI service."),
        Intent(listOf("что ты умеешь","что ты можешь","твои возможности","какие у тебя функции","what can you do","what are your abilities"),"Я умею работать с командами устройства, встроенными знаниями, простыми задачами, поиском и поддерживаемыми приложениями.","I can work with device commands, built-in knowledge, simple problems, search, and supported apps."),
        Intent(listOf("ты меня слышишь","ты слышишь меня","слышишь меня","can you hear me","do you hear me"),"Да, я получил твою команду.","Yes, I received your command."),
        Intent(listOf("ты здесь","ты тут","are you there","are you here"),"Да, я здесь.","Yes, I'm here."),
        Intent(listOf("молодец","круто","класс","отлично","хорошая работа","good job","great","nice"),"Принято.","Noted."),
        Intent(listOf("пока","до свидания","увидимся","bye","goodbye","see you"),"Пока.","Goodbye."),
        Intent(listOf("доброй ночи","спокойной ночи","good night"),"Спокойной ночи.","Good night."),
        Intent(listOf("сколько тебе лет","твой возраст","how old are you"),"У меня нет человеческого возраста. Я программный помощник JARVIS.","I don't have a human age. I'm the JARVIS software assistant."),
        Intent(listOf("где ты","ты где","where are you"),"Я работаю внутри приложения JARVIS.","I run inside the JARVIS app."),
        Intent(listOf("ты настоящий","ты живой","are you real","are you alive"),"Я программа, а не живой человек.","I'm software, not a living person.")
    )

    private val ruNumbers = mapOf(
        "ноль" to 0L,"один" to 1L,"два" to 2L,"три" to 3L,"четыре" to 4L,"пять" to 5L,
        "шесть" to 6L,"семь" to 7L,"восемь" to 8L,"девять" to 9L,"десять" to 10L,
        "одиннадцать" to 11L,"двенадцать" to 12L,"тринадцать" to 13L,"четырнадцать" to 14L,
        "пятнадцать" to 15L,"шестнадцать" to 16L,"семнадцать" to 17L,"восемнадцать" to 18L,
        "девятнадцать" to 19L,"двадцать" to 20L,"тридцать" to 30L,"сорок" to 40L,
        "пятьдесят" to 50L,"сто" to 100L,"тысяча" to 1000L,"тысячи" to 1000L,"тысяч" to 1000L
    )
    private val enNumbers = mapOf(
        "zero" to 0L,"one" to 1L,"two" to 2L,"three" to 3L,"four" to 4L,"five" to 5L,
        "six" to 6L,"seven" to 7L,"eight" to 8L,"nine" to 9L,"ten" to 10L,"eleven" to 11L,
        "twelve" to 12L,"thirteen" to 13L,"fourteen" to 14L,"fifteen" to 15L,"sixteen" to 16L,
        "seventeen" to 17L,"eighteen" to 18L,"nineteen" to 19L,"twenty" to 20L,
        "thirty" to 30L,"forty" to 40L,"fifty" to 50L,"hundred" to 100L,"thousand" to 1000L
    )

    fun answer(raw: String): String? {
        val q = normalize(raw)
        if (q.isBlank()) return null
        arithmetic(q)?.let { return it }
        val english = looksEnglish(q)
        val hit = intents.map { it to score(q, it.patterns) }
            .filter { it.second > 0 }.maxByOrNull { it.second }
            ?.takeIf { it.second >= 2 || q.length < 45 }
        return hit?.first?.let { if (english) it.en else it.ru }
    }

    private fun score(q: String, patterns: List<String>): Int {
        var best = 0
        for (p in patterns) {
            val n = normalize(p)
            val s = when {
                q == n -> 100
                q.startsWith(n + " ") || q.endsWith(" " + n) -> 60
                q.contains(" $n ") -> 50
                q.contains(n) -> 35
                else -> tokenOverlap(q, n)
            }
            if (s > best) best = s
        }
        return best
    }

    private fun tokenOverlap(a: String, b: String): Int {
        val aa = a.split(" ").filter { it.length >= 3 }.toSet()
        val bb = b.split(" ").filter { it.length >= 3 }.toSet()
        if (aa.isEmpty() || bb.isEmpty()) return 0
        return aa.intersect(bb).size * 3
    }

    private fun arithmetic(q: String): String? {
        val normalized = replaceNumberWords(q)
        val match = Regex("(?<!\\d)(\\d+(?:[.,]\\d+)?)[ ]*([+\\-×*÷/])[ ]*(\\d+(?:[.,]\\d+)?)(?!\\d)").find(normalized) ?: return null
        val a = match.groupValues[1].replace(',', '.').toDoubleOrNull() ?: return null
        val b = match.groupValues[3].replace(',', '.').toDoubleOrNull() ?: return null
        val result = when (match.groupValues[2]) {
            "+" -> a + b
            "-" -> a - b
            "×", "*" -> a * b
            "÷", "/" -> if (b == 0.0) return "На ноль делить нельзя." else a / b
            else -> return null
        }
        return (if (looksEnglish(q)) "Answer: " else "Ответ: ") + format(result)
    }

    private fun replaceNumberWords(q: String): String {
        var result = q
        val all = ruNumbers + enNumbers
        all.entries.sortedByDescending { it.key.length }.forEach { (word, value) ->
            result = result.replace(Regex("\\b" + Regex.escape(word) + "\\b"), value.toString())
        }
        return result
    }

    private fun looksEnglish(q: String): Boolean =
        Regex("\\b(the|you|your|how|what|who|are|is|can|hello|hi|thanks|thank|created|creator|made|answer|please)\\b")
            .containsMatchIn(q) || q.count { it in 'a'..'z' } > q.count { it in 'а'..'я' } * 2

    private fun normalize(value: String): String =
        value.lowercase(Locale.ROOT).replace('ё','е').replace('—','-').replace(Regex("\\s+")," ").trim()

    private fun format(v: Double): String =
        if (abs(v - v.toLong()) < 1e-10) v.toLong().toString()
        else String.format(Locale.US,"%.6f",v).trimEnd('0').trimEnd('.')
}
