package com.urchi.jarvis.v4.advanced

import java.util.Locale

class AdvancedRouter {
    fun classify(raw: String): TaskIntent {
        val text = raw.trim()
        val n = text.lowercase(Locale.getDefault()).replace('ё','е').replace(Regex("\\s+")," ")
        if (n.isBlank()) return TaskIntent(TaskKind.UNKNOWN,text)
        fun intent(k: TaskKind, arg: String = "", c: Float = .9f, external: Boolean = false) =
            TaskIntent(k,text,arg,c,external)

        when {
            n.startsWith("запомни ") || n.startsWith("remember ") -> return intent(TaskKind.MEMORY,n.substringAfter(' '),.99f)
            n.startsWith("забудь ") || n.startsWith("forget ") -> return intent(TaskKind.MEMORY,n.substringAfter(' '),.99f)
            n.startsWith("поищи ") || n.startsWith("найди в интернете ") ||
                n.startsWith("загугли ") || n.startsWith("search web ") ->
                return intent(TaskKind.WEB,n.substringAfter(' '),.98f,true)
            n == "открой google" || n == "открой гугл" ->
                return intent(TaskKind.WEB,"google",.99f,true)
            n.startsWith("открой ") || n.startsWith("запусти ") ->
                return intent(TaskKind.APP,n.substringAfter(' '),.92f)
            n.contains("сгенерируй видео") || n.contains("создай видео") ->
                return intent(TaskKind.VIDEO,n.substringAfter("видео").trim(),.98f)
            n.contains("сгенерируй аудио") || n.contains("создай аудио") ->
                return intent(TaskKind.AUDIO,n.substringAfter("аудио").trim(),.98f)
            n.contains("сгенерируй картинку") || n.contains("создай изображение") ->
                return intent(TaskKind.IMAGE,n.substringAfterLast(' ').trim(),.96f)
            n.startsWith("проанализируй файл ") || n.startsWith("проанализируй видео ") ||
                n.startsWith("проанализируй фото ") -> return intent(TaskKind.FILE,n.substringAfter(' '),.95f)
            n.startsWith("собери apk") || n.startsWith("собери aab") ||
                n.startsWith("собери exe") || n.startsWith("собери проект") ->
                return intent(TaskKind.BUILD,n,.96f)
            n.contains("tiktok") || n.contains("тикток") -> return intent(TaskKind.TIKTOK,n,.9f,true)
            n.startsWith("код ") || n.startsWith("напиши код") || n.startsWith("исправь код") ->
                return intent(TaskKind.CODE,n,.95f)
            else -> return intent(TaskKind.CHAT,n,.55f)
        }
    }
}
