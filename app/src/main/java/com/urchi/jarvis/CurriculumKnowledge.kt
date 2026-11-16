package com.urchi.jarvis

import java.util.Locale

/**
 * Local curriculum reference layer. Entries are original short explanations,
 * not copied textbook chapters.
 */
object CurriculumKnowledge {
    private val facts = linkedMapOf(
        "русский орфография" to "Орфография изучает нормативное написание слов. Для проверки учитываются правило, морфема, часть речи, форма слова и словарная фиксация.",
        "безударная гласная корень" to "Проверяемую безударную гласную в корне обычно проверяют однокоренным словом, где она находится под ударением. Непроверяемые слова проверяют по словарю.",
        "приставки пре при" to "ПРЕ- может выражать высокую степень признака или значение, близкое к ПЕРЕ-. ПРИ- часто обозначает приближение, присоединение, неполноту действия или близость.",
        "жи ши" to "После Ж и Ш в школьной норме пишется И: жизнь, машина.",
        "ча ща" to "После Ч и Щ в школьной норме пишется А: чаща, часы.",
        "чу щу" to "После Ч и Щ в школьной норме пишется У: чудо, щука.",
        "не с прилагательными" to "Слитное или раздельное написание НЕ с прилагательными зависит от значения, противопоставления, зависимых слов и других условий.",
        "однородные члены" to "Однородные члены относятся к одному слову, отвечают на один вопрос и выполняют одинаковую синтаксическую функцию.",
        "прямая речь" to "Оформление прямой речи зависит от положения слов автора и реплики; используются кавычки или тире согласно типу конструкции.",
        "english alphabet" to "The English alphabet has 26 letters. Vowels are usually a, e, i, o, u; y can function as a vowel in some words.",
        "present simple" to "Present Simple is used for habits, routines, facts and stable situations. With he, she, it the main verb usually takes -s or -es.",
        "present continuous" to "Present Continuous is formed with am/is/are plus verb-ing and commonly describes an action happening around the present time.",
        "past simple" to "Past Simple describes completed past actions. Regular verbs commonly use -ed; irregular verbs have their own forms.",
        "future english" to "English future meaning can be expressed with will, be going to, Present Continuous and other constructions depending on intention and context.",
        "present perfect" to "Present Perfect is formed with have/has plus a past participle and links a past event to the present.",
        "past perfect" to "Past Perfect is formed with had plus a past participle and marks an action before another past reference point.",
        "modal verbs" to "Common English modal verbs include can, could, may, might, must, should and would. They express ability, possibility, obligation, advice and related meanings.",
        "articles a an the" to "A/an generally introduces a singular countable noun as non-specific; the normally points to something identifiable or specific.",
        "english plural" to "Many English plurals use -s or -es, while common irregular plurals include children, people, men, women and mice.",
        "english question" to "Questions often use an auxiliary such as do, does or did, or an appropriate form of be or a modal before the subject.",
        "english negation" to "Negation commonly uses not with an auxiliary or modal. With Present Simple and Past Simple, do, does or did often carries the negation.",
        "comparatives english" to "Short adjectives commonly form comparatives with -er and superlatives with -est; longer adjectives commonly use more and most.",
        "conditionals zero" to "Zero Conditional describes general truths: if or when plus Present Simple, Present Simple.",
        "conditionals first" to "First Conditional commonly describes a real or possible future condition: if plus Present Simple, will plus base verb.",
        "conditionals second" to "Second Conditional commonly describes hypothetical or unlikely situations: if plus Past Simple, would plus base verb.",
        "passive voice english" to "The passive voice focuses on the receiver of an action. A common form is be plus past participle.",
        "reported speech english" to "Reported speech conveys what someone said without directly quoting the original words; tense and pronouns can change with context.",
        "математика дроби" to "При сложении и вычитании дробей с разными знаменателями сначала находят общий знаменатель. При умножении дробей перемножают числители и знаменатели.",
        "математика проценты" to "p процентов от x равны x·p/100. Если известны часть и процент, целое находят как часть/(p/100).",
        "линейное уравнение" to "Для ax+b=0 при a не равно нулю решение x=-b/a.",
        "квадратное уравнение" to "Для ax²+bx+c=0 дискриминант D=b²-4ac. При D≥0 корни находят по формуле x=(-b±√D)/(2a).",
        "теорема пифагора" to "В прямоугольном треугольнике c²=a²+b², где c — гипотенуза.",
        "площадь прямоугольника" to "Площадь прямоугольника S=ab.",
        "площадь треугольника" to "Площадь треугольника S=ah/2.",
        "длина окружности" to "Длина окружности C=2πr.",
        "площадь круга" to "Площадь круга S=πr².",
        "среднее арифметическое" to "Среднее арифметическое равно сумме чисел, делённой на их количество.",
        "скорость путь время" to "Для равномерного движения v=s/t, s=vt, t=s/v.",
        "плотность" to "Плотность вещества ρ=m/V.",
        "ньютон второй закон" to "Равнодействующая сила связана с массой и ускорением: F=ma.",
        "закон ома" to "Для участка цепи закон Ома: U=IR.",
        "мощность электрическая" to "Электрическая мощность P=UI; также P=I²R и P=U²/R при соответствующих условиях.",
        "фотосинтез" to "Фотосинтез — синтез органических веществ из углекислого газа и воды с использованием энергии света; у растений обычно выделяется кислород.",
        "митоз" to "Митоз — деление клетки, при котором образуются две дочерние клетки с тем же набором хромосом.",
        "мейоз" to "Мейоз включает два последовательных деления и приводит к образованию клеток с уменьшенным вдвое набором хромосом.",
        "клетка" to "Клетка — структурно-функциональная единица живых организмов.",
        "днк" to "ДНК хранит наследственную информацию. Основные основания: аденин, тимин, гуанин и цитозин.",
        "экосистема" to "Экосистема включает живые организмы и неживую среду, связанные потоками энергии и круговоротом веществ.",
        "атом" to "Атом состоит из ядра с протонами и нейтронами и электронной оболочки.",
        "молекула" to "Молекула — электрически нейтральная частица из двух или более атомов, соединённых химическими связями.",
        "кислота химия" to "Кислоты — вещества, проявляющие кислотные свойства и участвующие в кислотно-основных реакциях.",
        "основание химия" to "Основания — вещества, проявляющие основные свойства и участвующие в кислотно-основных реакциях.",
        "периодический закон" to "Свойства химических элементов закономерно связаны с зарядом атомных ядер; таблица отражает периодичность этих свойств.",
        "французская революция" to "Французская революция началась в 1789 году и привела к глубоким политическим и социальным изменениям.",
        "промышленная революция" to "Промышленная революция связана с переходом к машинному производству и фабричной системе, начавшимся в Великобритании в XVIII веке.",
        "вторая мировая война" to "Вторая мировая война продолжалась с 1939 по 1945 год и стала крупнейшим вооружённым конфликтом XX века.",
        "ренессанс" to "Ренессанс — культурное движение Европы XIV–XVI веков, связанное с гуманизмом, искусством и интересом к античному наследию.",
        "алгоритм" to "Алгоритм — конечная последовательность однозначно заданных действий для решения задачи.",
        "информатика двоичная система" to "В двоичной системе используются цифры 0 и 1, а разряды соответствуют степеням основания 2.",
        "климат" to "Климат — многолетний режим погоды и статистических характеристик атмосферы в определённом регионе.",
        "атмосфера" to "Атмосфера — газовая оболочка Земли; основная масса сухого воздуха состоит из азота и кислорода.",
        "круговорот воды" to "Круговорот воды включает испарение, перенос пара, конденсацию, осадки, сток и другие процессы обмена водой.",
        "экономика инфляция" to "Инфляция — устойчивый рост общего уровня цен, при котором снижается покупательная способность денег.",
        "спрос экономика" to "Спрос — количество товара или услуги, которое покупатели готовы и способны приобрести при определённых условиях.",
        "государство" to "Государство — политическая организация общества с публичной властью, территорией, населением и системой институтов.",
        "право" to "Право — система общеобязательных норм, регулирующих общественные отношения и обеспечиваемых государственными механизмами.",
        "логика высказывания" to "Высказывание — предложение, которому можно приписать истинностное значение. Логические связки позволяют строить составные высказывания."
    )

    fun answer(query: String): String? {
        val q = normalize(query)
        if (q.isBlank()) return null
        val best = facts.entries.filter { q.contains(it.key) || it.key.contains(q) }.maxByOrNull { it.key.length }
        if (best != null) return best.value
        val prefixes = listOf("объясни ", "что такое ", "что значит ", "what is ", "define ", "explain ")
        val topic = prefixes.firstNotNullOfOrNull { p -> if (q.startsWith(p)) q.removePrefix(p).trim() else null }
        return topic?.let { t -> facts.entries.firstOrNull { t.contains(it.key) || it.key.contains(t) }?.value }
    }

    fun topicCount(): Int = facts.size

    private fun normalize(s: String) = s.lowercase(Locale.getDefault())
        .replace('ё', 'е').replace(Regex("[^\\p{L}\\p{N} ]"), " ")
        .replace(Regex("\\s+"), " ").trim()
}
