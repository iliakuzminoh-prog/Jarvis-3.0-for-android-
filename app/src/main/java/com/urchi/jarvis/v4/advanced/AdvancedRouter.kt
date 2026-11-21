package com.urchi.jarvis.v4.advanced

import java.util.Locale

class AdvancedRouter {
    fun classify(raw: String): TaskIntent {
        val text = raw.trim()
        val n = text.lowercase(Locale.getDefault()).replace('ё','е').replace(Regex("\\s+")," ")
        if (n.isBlank()) return TaskIntent(TaskKind.UNKNOWN,text)
        fun intent(k: TaskKind, arg: String = "", c: Float = .9f, external: Boolean = false) =
            TaskIntent(k,text,arg,c,external)

        fun afterPrefix(prefix: String): String =
            n.removePrefix(prefix).trim()

        when {
            n.startsWith("запомни ") || n.startsWith("remember ") ->
                return intent(TaskKind.MEMORY,afterPrefix(if (n.startsWith("запомни ")) "запомни " else "remember "),.99f)
            n.startsWith("забудь ") || n.startsWith("forget ") ->
                return intent(TaskKind.MEMORY,afterPrefix(if (n.startsWith("забудь ")) "забудь " else "forget "),.99f)
            n.startsWith("поищи ") || n.startsWith("найди в интернете ") ||
                n.startsWith("загугли ") || n.startsWith("search web ") -> {
                val query = when {
                    n.startsWith("найди в интернете ") -> afterPrefix("найди в интернете ")
                    n.startsWith("search web ") -> afterPrefix("search web ")
                    n.startsWith("загугли ") -> afterPrefix("загугли ")
                    else -> afterPrefix("поищи ")
                }
                return intent(TaskKind.WEB,query,.98f,true)
            }
            n == "открой google" || n == "открой гугл" ->
                return intent(TaskKind.WEB,"google",.99f,true)
            n.startsWith("открой ") || n.startsWith("запусти ") ->
                return intent(TaskKind.APP,afterPrefix(if (n.startsWith("открой ")) "открой " else "запусти "),.92f)
            n.contains("сгенерируй видео") || n.contains("создай видео") -> {
                val prefix = if (n.contains("сгенерируй видео")) "сгенерируй видео" else "создай видео"
                return intent(TaskKind.VIDEO,n.substringAfter(prefix).trim(),.98f)
            }
            n.contains("сгенерируй аудио") || n.contains("создай аудио") -> {
                val prefix = if (n.contains("сгенерируй аудио")) "сгенерируй аудио" else "создай аудио"
                return intent(TaskKind.AUDIO,n.substringAfter(prefix).trim(),.98f)
            }
            n.contains("сгенерируй картинку") || n.contains("создай изображение") -> {
                val prompt = when {
                    n.contains("сгенерируй картинку") -> n.substringAfter("сгенерируй картинку").trim()
                    else -> n.substringAfter("создай изображение").trim()
                }
                return intent(TaskKind.IMAGE,prompt,.96f)
            }
            n.startsWith("проанализируй файл ") ->
                return intent(TaskKind.FILE,afterPrefix("проанализируй файл "),.95f)
            n.startsWith("проанализируй видео ") ->
                return intent(TaskKind.FILE,afterPrefix("проанализируй видео "),.95f)
            n.startsWith("проанализируй фото ") ->
                return intent(TaskKind.FILE,afterPrefix("проанализируй фото "),.95f)
            n.startsWith("собери apk ") || n.startsWith("собери aab ") ||
                n.startsWith("собери exe ") || n.startsWith("собери проект ") -> {
                val prefix = when {
                    n.startsWith("собери apk ") -> "собери apk "
                    n.startsWith("собери aab ") -> "собери aab "
                    n.startsWith("собери exe ") -> "собери exe "
                    else -> "собери проект "
                }
                return intent(TaskKind.BUILD,n,.96f)
            }
            n.contains("tiktok") || n.contains("тикток") -> return intent(TaskKind.TIKTOK,n,.9f,true)
            n.startsWith("код ") || n.startsWith("напиши код") || n.startsWith("исправь код") ->
                return intent(TaskKind.CODE,n,.95f)
            else -> return intent(TaskKind.CHAT,n,.55f)
        }
    }
}
