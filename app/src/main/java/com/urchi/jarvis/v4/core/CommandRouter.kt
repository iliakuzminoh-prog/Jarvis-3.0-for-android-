package com.urchi.jarvis.v4.core

import java.util.Locale

class CommandRouter {
    fun parse(input: String): ParsedCommand {
        val original = input.trim()
        val n = normalize(original)
        if (n.isEmpty()) return ParsedCommand(CommandKind.UNKNOWN, original, n)
        routeMemory(n, original)?.let { return it }
        routeMedia(n, original)?.let { return it }
        routeYouTube(n, original)?.let { return it }
        routeApps(n, original)?.let { return it }
        routeSettings(n, original)?.let { return it }
        routeDevice(n, original)?.let { return it }
        routeSchool(n, original)?.let { return it }
        routeMath(n, original)?.let { return it }
        routeWeb(n, original)?.let { return it }
        return ParsedCommand(CommandKind.CONVERSATION, original, n, confidence = .55f)
    }

    private fun routeMemory(n: String, o: String): ParsedCommand? {
        val remember = listOf("запомни ", "запиши в память ", "remember ")
            .firstOrNull { n.startsWith(it) }
        if (remember != null)
            return ParsedCommand(CommandKind.MEMORY_REMEMBER, o, n,
                n.removePrefix(remember).trim(), .98f)
        val forget = listOf("забудь ", "удали из памяти ", "forget ")
            .firstOrNull { n.startsWith(it) }
        if (forget != null)
            return ParsedCommand(CommandKind.MEMORY_FORGET, o, n,
                n.removePrefix(forget).trim(), .98f)
        if (n == "что ты помнишь" || n == "покажи память" ||
            n == "what do you remember")
            return ParsedCommand(CommandKind.MEMORY_SEARCH, o, n, confidence = .99f)
        return null
    }

    private fun routeMedia(n: String, o: String): ParsedCommand? = when {
        n in setOf("пауза", "поставь на паузу", "останови музыку",
            "пауза музыка", "pause", "pause music") ->
            ParsedCommand(CommandKind.MEDIA_PAUSE, o, n, confidence = .99f)
        n in setOf("продолжи", "продолжи музыку", "play", "resume", "resume music") ->
            ParsedCommand(CommandKind.MEDIA_PLAY, o, n, confidence = .99f)
        n in setOf("следующий трек", "следующая песня", "next", "next song") ->
            ParsedCommand(CommandKind.MEDIA_NEXT, o, n, confidence = .99f)
        n in setOf("предыдущий трек", "предыдущая песня", "previous", "previous song") ->
            ParsedCommand(CommandKind.MEDIA_PREVIOUS, o, n, confidence = .99f)
        else -> null
    }

    private fun routeYouTube(n: String, o: String): ParsedCommand? {
        val prefixes = listOf(
            "включи видео ", "включи на ютубе ", "найди на ютубе ",
            "открой видео ", "play youtube ", "open youtube "
        )
        val p = prefixes.firstOrNull { n.startsWith(it) } ?: return null
        val q = n.removePrefix(p).trim()
        if (q.isEmpty()) return null
        return ParsedCommand(CommandKind.YOUTUBE, o, n, q, .96f)
    }

    private fun routeApps(n: String, o: String): ParsedCommand? {
        val prefixes = listOf("открой приложение ", "запусти приложение ",
            "открой ", "запусти ", "open app ", "launch ")
        val p = prefixes.firstOrNull { n.startsWith(it) } ?: return null
        val arg = n.removePrefix(p).trim()
        if (arg.isEmpty()) return null
        if (arg in setOf("сайт", "страницу", "браузер", "website", "browser"))
            return null
        return ParsedCommand(CommandKind.OPEN_APP, o, n, arg, .88f)
    }

    private fun routeSettings(n: String, o: String): ParsedCommand? = when {
        n.contains("настройки wi-fi") || n.contains("настройки wifi") ->
            ParsedCommand(CommandKind.WIFI_SETTINGS, o, n, confidence = .98f)
        n.contains("bluetooth") || n.contains("блютуз") ->
            ParsedCommand(CommandKind.BLUETOOTH_SETTINGS, o, n, confidence = .98f)
        n == "настройки" || n == "открой настройки" || n == "settings" ->
            ParsedCommand(CommandKind.SETTINGS, o, n, confidence = .98f)
        n == "включи vpn" || n == "открой vpn" || n == "vpn" ->
            ParsedCommand(CommandKind.VPN, o, n, confidence = .95f)
        else -> null
    }

    private fun routeDevice(n: String, o: String): ParsedCommand? = when {
        n.contains("заряд батареи") || n.contains("процентов батареи") ||
            n == "battery" -> ParsedCommand(CommandKind.DEVICE_INFO, o, n, "battery", .95f)
        n.contains("свободной памяти") || n.contains("сколько памяти") ||
            n == "storage" -> ParsedCommand(CommandKind.DEVICE_INFO, o, n, "storage", .95f)
        n == "помощь" || n == "help" || n == "что ты умеешь" ->
            ParsedCommand(CommandKind.HELP, o, n, confidence = .99f)
        else -> null
    }

    private fun routeSchool(n: String, o: String): ParsedCommand? {
        val markers = listOf(
            "реши задачу", "решить задачу", "таблица менделеева",
            "формула", "пример по математике", "домашнее задание",
            "solve", "equation", "periodic table"
        )
        return if (markers.any { n.contains(it) })
            ParsedCommand(CommandKind.SCHOOL, o, n, n, .90f) else null
    }

    private fun routeMath(n: String, o: String): ParsedCommand? {
        if (!n.any { it.isDigit() }) return null
        return if (n.any { it in "+-*/=()" })
            ParsedCommand(CommandKind.CALCULATE, o, n, n, .82f) else null
    }

    private fun routeWeb(n: String, o: String): ParsedCommand? {
        val prefixes = listOf("поищи в интернете ", "найди в интернете ",
            "загугли ", "search web ", "search ")
        val p = prefixes.firstOrNull { n.startsWith(it) } ?: return null
        return ParsedCommand(CommandKind.SEARCH_WEB, o, n,
            n.removePrefix(p).trim(), .96f)
    }

    private fun normalize(s: String): String =
        s.lowercase(Locale.getDefault())
            .replace('ё', 'е')
            .replace(Regex("\\s+"), " ")
            .trim()
}
