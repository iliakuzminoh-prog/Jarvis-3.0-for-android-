package com.urchi.jarvis

import java.util.Locale
import kotlin.math.abs

/**
 * Расширяемое локальное мозговое ядро JARVIS.
 * Это не LLM: здесь проверенные факты, словарь и небольшой решатель задач.
 */
object BrainKnowledge {
    private data class Fact(val keys: List<String>, val answer: String)

    private val facts = listOf(
        Fact(listOf("главный закон физики", "самый главный закон физики"),
            "Одного единственного «главного закона физики» нет. Если имеют в виду основной закон динамики Ньютона, то это второй закон: F = m·a — результирующая сила равна произведению массы на ускорение."),
        Fact(listOf("второй закон ньютона", "закон ньютона", "f=ma", "f = ma"),
            "Второй закон Ньютона: F = m·a. Ускорение тела прямо связано с результирующей силой и обратно связано с массой."),
        Fact(listOf("первый закон ньютона", "закон инерции"),
            "Первый закон Ньютона: если результирующая внешних сил равна нулю, тело сохраняет покой или движется равномерно и прямолинейно."),
        Fact(listOf("третий закон ньютона"),
            "Третий закон Ньютона: тела при взаимодействии действуют друг на друга силами, равными по модулю и противоположными по направлению."),
        Fact(listOf("закон ома", "ом"),
            "Закон Ома для участка цепи: I = U/R, где I — сила тока, U — напряжение, R — сопротивление."),
        Fact(listOf("закон архимеда"),
            "Закон Архимеда: на погружённое в жидкость или газ тело действует выталкивающая сила, равная весу вытесненной среды."),
        Fact(listOf("закон сохранения энергии", "сохранение энергии"),
            "Закон сохранения энергии: энергия не возникает из ничего и не исчезает, а переходит из одной формы в другую; в замкнутой системе её полная величина сохраняется."),
        Fact(listOf("скорость", "формула скорости"),
            "Средняя скорость: v = s/t, то есть пройденный путь делится на время."),
        Fact(listOf("плотность", "формула плотности"),
            "Плотность: ρ = m/V, то есть масса вещества делится на его объём."),
        Fact(listOf("мощность", "формула мощности"),
            "Мощность — скорость выполнения работы или передачи энергии: P = A/t."),
        Fact(listOf("вода в таблице менделеева", "как называется вода в таблице менделеева", "название воды в таблице менделеева"),
            "Вода не является химическим элементом и отдельной клеткой в таблице Менделеева не записана. Вода — химическое соединение H₂O: два атома водорода и один атом кислорода."),
        Fact(listOf("из чего состоит вода", "формула воды", "химическая формула воды"),
            "Химическая формула воды — H₂O: два атома водорода и один атом кислорода."),
        Fact(listOf("таблица менделеева", "периодическая таблица"),
            "Периодическая таблица содержит 118 официально признанных химических элементов. Они расположены по возрастанию атомного номера."),
        Fact(listOf("что такое атом"),
            "Атом состоит из ядра и электронной оболочки. В ядре находятся протоны и нейтроны, вокруг ядра — электроны."),
        Fact(listOf("что такое молекула"),
            "Молекула — электрически нейтральная частица, состоящая из связанных атомов и сохраняющая основные химические свойства вещества."),
        Fact(listOf("что такое электричество"),
            "Электрический ток — упорядоченное движение заряженных частиц. В металлах ток создаётся главным образом движением электронов."),
        Fact(listOf("что такое гравитация", "сила тяжести"),
            "Гравитация — взаимодействие тел, имеющих массу. Вблизи поверхности Земли F ≈ m·9,81 м/с²."),
        Fact(listOf("что такое фотосинтез"),
            "Фотосинтез — процесс, при котором растения, водоросли и некоторые микроорганизмы используют световую энергию для образования органических веществ из углекислого газа и воды; обычно выделяется кислород."),
        Fact(listOf("что такое днк"),
            "ДНК хранит наследственную информацию. Основные основания ДНК: аденин, тимин, гуанин и цитозин."),
        Fact(listOf("что такое клетка"),
            "Клетка — структурно-функциональная единица живого организма."),
        Fact(listOf("что такое алгоритм"),
            "Алгоритм — конечная последовательность однозначных действий для решения задачи или получения результата."),
        Fact(listOf("что такое интернет"),
            "Интернет — глобальная система взаимосвязанных компьютерных сетей, обменивающихся данными по согласованным протоколам."),
        Fact(listOf("мама пушкина", "мать пушкина", "как звали маму пушкина", "имя матери пушкина", "мать александра пушкина"),
            "Мать Александра Сергеевича Пушкина — Надежда Осиповна Пушкина (урождённая Ганнибал, 1775–1836), дочь Осипа Абрамовича Ганнибала."),
        Fact(listOf("отец пушкина", "папа пушкина", "как звали отца пушкина"),
            "Отец Александра Пушкина — Сергей Львович Пушкин (1770–1848), представитель старинного дворянского рода."),
        Fact(listOf("где родился пушкин", "место рождения пушкина"),
            "Александр Сергеевич Пушкин родился 6 июня 1799 года в Москве."),
        Fact(listOf("когда родился пушкин", "дата рождения пушкина"),
            "Александр Сергеевич Пушкин родился 6 июня 1799 года по новому стилю."),
        Fact(listOf("когда умер пушкин", "дата смерти пушкина"),
            "Александр Сергеевич Пушкин умер 10 февраля 1837 года после ранения на дуэли."),
        Fact(listOf("кто такой пушкин", "кто такой александр пушкин"),
            "Александр Сергеевич Пушкин — русский поэт, прозаик и драматург, одна из центральных фигур русской литературы XIX века."),
        Fact(listOf("что такое подлежащее"),
            "Подлежащее — главный член предложения, который называет предмет речи и обычно отвечает на вопросы «кто?» или «что?»."),
        Fact(listOf("что такое сказуемое"),
            "Сказуемое — главный член предложения, который сообщает о действии, состоянии или признаке подлежащего."),
        Fact(listOf("что такое дополнение"),
            "Дополнение — второстепенный член предложения, обозначающий предмет и отвечающий на вопросы косвенных падежей."),
        Fact(listOf("что такое определение"),
            "Определение — второстепенный член предложения, обозначающий признак предмета и отвечающий на вопросы «какой?», «чей?» и подобные."),
        Fact(listOf("что такое обстоятельство"),
            "Обстоятельство — второстепенный член предложения, обозначающий условия действия: место, время, причину, цель, способ действия и другие обстоятельства."),
        Fact(listOf("деепричастный оборот"),
            "Деепричастный оборот обычно обособляется запятыми и обозначает добавочное действие того же лица или предмета, что и основное действие."),
        Fact(listOf("причастный оборот"),
            "Причастный оборот состоит из причастия с зависимыми словами. Его обособление зависит прежде всего от положения относительно определяемого слова и контекста."),
        Fact(listOf("однородные определения"),
            "Однородные определения характеризуют один и тот же предмет непосредственно и обычно перечисляются с интонацией перечисления; постановка запятых зависит от смысловых отношений."),
        Fact(listOf("сложносочиненное предложение"),
            "Сложносочинённое предложение состоит из равноправных частей, связанных сочинительными союзами; между его частями часто ставится запятая."),
        Fact(listOf("сложноподчиненное предложение"),
            "Сложноподчинённое предложение состоит из главной и одной или нескольких придаточных частей, связанных подчинительными союзами или союзными словами."),
        Fact(listOf("обращение"),
            "Обращение называет того, к кому обращаются, и не является членом предложения; оно обычно обособляется знаками препинания."),
        Fact(listOf("английский язык времена"),
            "В английском выбор времени зависит от значения и контекста: Present Simple, Present Continuous, Past Simple, Present Perfect и другие формы описывают разные отношения между действием и временем."),
        Fact(listOf("what is a noun"),
            "A noun is a word that names a person, place, thing, idea, or other entity."),
        Fact(listOf("what is a verb"),
            "A verb expresses an action, event, process, or state."),
        Fact(listOf("what is an adjective"),
            "An adjective describes or modifies a noun or pronoun."),
        Fact(listOf("what is an adverb"),
            "An adverb commonly modifies a verb, adjective, another adverb, or an entire clause."),
        Fact(listOf("past tense english"),
            "English past forms describe events or states located before a reference time. The exact tense depends on whether the action is completed, ongoing, or related to another past event."),
        Fact(listOf("present perfect english"),
            "Present Perfect uses have/has plus a past participle and commonly connects a past event with the present."),
        Fact(listOf("difference between say and tell"),
            "Say focuses on the words or statement; tell normally takes a person as an object, as in tell someone something."),
        Fact(listOf("difference between do and make"),
            "Do is commonly used for activities, tasks and work; make is commonly used for creating, producing or causing something. The exact choice is lexical and depends on the expression."),
        Fact(listOf("what is photosynthesis"),
            "Photosynthesis is a process in which plants, algae and some microorganisms use light energy to produce organic compounds from carbon dioxide and water; oxygen is commonly released."),
        Fact(listOf("what is gravity"),
            "Gravity is the interaction associated with mass. Near Earth's surface, gravitational acceleration is approximately 9.81 m/s²."),
        Fact(listOf("what is dna"),
            "DNA is the molecule that stores hereditary information in living organisms and many viruses. Its four standard bases are adenine, thymine, guanine and cytosine."),
        Fact(listOf("what is an algorithm"),
            "An algorithm is a finite, well-defined sequence of steps for solving a problem or producing a result.")
    )

    fun answer(raw: String): String? {
        val q = normalize(raw)

        // Базовая беседа должна работать без Google и без сети.
        conversationAnswer(q)?.let { return it }
        facts.firstOrNull { fact -> fact.keys.any { q == normalize(it) || q.contains(normalize(it)) } }?.let { return it.answer }
        solveAppleProblem(q)?.let { return it }
        solveSimpleWordProblem(q)?.let { return it }
        if (q.matches(Regex(".*сколько.*яблок.*"))) {
            return "Сформулируй задачу полностью, например: «Было 5 яблок, съели 2. Сколько осталось?»"
        }
        return null
    }

    private fun conversationAnswer(q: String): String? {
        if (q.isBlank()) return "Я слушаю."
        if (matches(q, "привет", "здравствуй", "здарова", "добрый день", "доброе утро", "добрый вечер", "hello", "hi", "hey"))
            return if (looksEnglish(q)) "Hello! I'm JARVIS. How can I help?" else "Привет! Я JARVIS. Чем могу помочь?"
        if (matches(q, "как дела", "как ты", "how are you", "how's it going", "how are things"))
            return if (looksEnglish(q)) "I'm working normally and ready to help." else "Работаю нормально и готов помочь."
        if (matches(q, "спасибо", "благодарю", "thanks", "thank you"))
            return if (looksEnglish(q)) "You're welcome." else "Пожалуйста."
        if (matches(q, "кто ты", "что ты такое", "who are you", "what are you"))
            return if (looksEnglish(q)) "I'm JARVIS, your local assistant." else "Я JARVIS, твой локальный помощник."
        if (matches(q, "кто твой создатель", "кто тебя создал", "who created you", "who is your creator", "who made you"))
            return if (looksEnglish(q))
                "I was created for Ilya Kuzmin's JARVIS project. ChatGPT is not my creator; it is a separate AI service."
            else
                "Меня создали для проекта JARVIS Ильи Кузьмина. ChatGPT не является моим создателем — это отдельный ИИ-сервис."
        if (matches(q, "что ты умеешь", "what can you do", "what are your abilities"))
            return if (looksEnglish(q))
                "I can talk with you, answer my built-in knowledge, solve simple problems, control supported apps, and use my built-in browser when needed."
            else
                "Я могу разговаривать с тобой, отвечать по встроенным знаниям, решать простые задачи, управлять поддерживаемыми приложениями и при необходимости использовать встроенный браузер."
        if (matches(q, "до свидания", "пока", "bye", "goodbye"))
            return if (looksEnglish(q)) "Goodbye!" else "Пока!"
        return null
    }

    private fun matches(q: String, vararg phrases: String): Boolean =
        phrases.any { p -> q == normalize(p) || q.contains(normalize(p)) }

    private fun looksEnglish(q: String): Boolean =
        Regex("\b(the|you|your|how|what|who|are|is|can|hello|hi|thanks|thank|created|creator|made)\b").containsMatchIn(q)

    private fun solveAppleProblem(q: String): String? {
        if (!q.contains("яблок")) return null
        val nums = Regex("(?<![a-zа-я])\\d+(?:[.,]\\d+)?").findAll(q)
            .mapNotNull { it.value.replace(',', '.').toDoubleOrNull() }.toList()
        if (nums.isEmpty()) return null
        val plus = listOf("добавили", "принесли", "купили", "дали", "подарили", "прибавили").any { q.contains(it) }
        val minus = listOf("съели", "забрали", "отдали", "потеряли", "использовали").any { q.contains(it) }
        if (nums.size >= 2 && plus && !minus) return "Ответ: ${fmt(nums[0] + nums[1])} яблок."
        if (nums.size >= 2 && minus) return "Ответ: ${fmt(nums[0] - nums[1])} яблок."
        if (nums.size == 1 && q.contains("5 яблок")) return "Ответ: 5 яблок."
        return null
    }

    private fun solveSimpleWordProblem(q: String): String? {
        val nums = Regex("(?<![a-zа-я])\\d+(?:[.,]\\d+)?").findAll(q)
            .mapNotNull { it.value.replace(',', '.').toDoubleOrNull() }.toList()
        if (nums.size < 2) return null
        val add = listOf("всего", "вместе", "добавили", "принесли", "стало", "прибавили").any { q.contains(it) }
        val sub = listOf("осталось", "съели", "забрали", "отдали", "убрали", "потратили").any { q.contains(it) }
        if (add && !sub) return "Решение: ${fmt(nums[0])} + ${fmt(nums[1])} = ${fmt(nums[0] + nums[1])}."
        if (sub) return "Решение: ${fmt(nums[0])} − ${fmt(nums[1])} = ${fmt(nums[0] - nums[1])}."
        return null
    }

    private fun normalize(s: String): String =
        s.lowercase(Locale.ROOT).replace('ё', 'е').replace('²', '2').replace(Regex("\\s+"), " ").trim()

    private fun fmt(v: Double): String =
        if (abs(v - v.toLong()) < 1e-10) v.toLong().toString()
        else String.format(Locale.US, "%.4f", v).trimEnd('0').trimEnd('.')
}
