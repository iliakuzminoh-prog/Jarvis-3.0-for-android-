package com.urchi.jarvis.v5

import com.urchi.jarvis.v4.core.CommandKind
import com.urchi.jarvis.v4.core.ParsedCommand

class V5IntentRecognizer {
    fun recognize(raw:String):ParsedCommand {
        val original=raw.trim()
        val n=V5TextPipeline.normalize(original)
        if(n.isBlank()) return ParsedCommand(CommandKind.UNKNOWN,original,n)
        parseMemory(n,original)?.let{return it}
        parseDevice(n,original)?.let{return it}
        parseMedia(n,original)?.let{return it}
        parseApps(n,original)?.let{return it}
        parseWeb(n,original)?.let{return it}
        parseSchool(n,original)?.let{return it}
        return ParsedCommand(CommandKind.CONVERSATION,original,n,confidence=.35f)
    }
    private fun parseMemory(n:String,o:String):ParsedCommand? {
        V5TextPipeline.extractAfter(n,listOf("запомни","запиши в память","remember"))?.let{return ParsedCommand(CommandKind.MEMORY_REMEMBER,o,n,it,.98f)}
        V5TextPipeline.extractAfter(n,listOf("забудь","удали из памяти","forget"))?.let{return ParsedCommand(CommandKind.MEMORY_FORGET,o,n,it,.98f)}
        if(n in setOf("что ты помнишь","покажи память","покажи что ты помнишь","what do you remember")) return ParsedCommand(CommandKind.MEMORY_SEARCH,o,n,confidence=.99f)
        return null
    }
    private fun parseDevice(n:String,o:String):ParsedCommand? {
        if(V5TextPipeline.containsAny(n,setOf("уведомления","уведомление","шторка","notifications"))) return ParsedCommand(CommandKind.NOTIFICATION,o,n,confidence=.94f)
        if(n=="wifi"||n.contains("wifi")||RussianMorphology.hasStem(n,"вайфай","wifi","вайфая","вайфаю")) return ParsedCommand(CommandKind.WIFI_SETTINGS,o,n,confidence=.96f)
        if(V5TextPipeline.containsAny(n,setOf("блютуз","bluetooth"))) return ParsedCommand(CommandKind.BLUETOOTH_SETTINGS,o,n,confidence=.96f)
        if(V5TextPipeline.containsAny(n,setOf("назад","вернись","back")) && n.split(' ').size<=4) return ParsedCommand(CommandKind.SETTINGS,o,n,"back",.96f)
        if(n in setOf("домой","на главный экран","открой главный экран","перейди на главный экран","home","открой домашний экран"))
            return ParsedCommand(CommandKind.SETTINGS,o,n,"home",.98f)
        if(V5TextPipeline.containsAny(n,setOf("быстрые","быструю","панель","quick")) && V5TextPipeline.containsAny(n,setOf("настройки","настрой","settings")))
            return ParsedCommand(CommandKind.SETTINGS,o,n,"quick_settings",.92f)
        if(n in setOf("настройки","открой настройки","settings")) return ParsedCommand(CommandKind.SETTINGS,o,n,"settings",.98f)
        return null
    }
    private fun parseMedia(n:String,o:String):ParsedCommand? {
        if(V5TextPipeline.containsAny(n,setOf("пауза","останови","останов","pause")) && V5TextPipeline.containsAny(n,setOf("музыку","музыка","песня","трек","звук","media")))
            return ParsedCommand(CommandKind.MEDIA_PAUSE,o,n,confidence=.96f)
        if(V5TextPipeline.containsAny(n,setOf("продолжи","возобнови","play","resume")) && V5TextPipeline.containsAny(n,setOf("музыку","музыка","песня","трек","звук","media")))
            return ParsedCommand(CommandKind.MEDIA_PLAY,o,n,confidence=.95f)
        if(V5TextPipeline.containsAny(n,setOf("следующий","следующая","next")) && (n.contains("трек")||n.contains("песн")||n=="следующий"||n=="следующая"))
            return ParsedCommand(CommandKind.MEDIA_NEXT,o,n,confidence=.93f)
        if(V5TextPipeline.containsAny(n,setOf("предыдущий","предыдущая","previous")) && (n.contains("трек")||n.contains("песн")||n=="предыдущий"||n=="предыдущая"))
            return ParsedCommand(CommandKind.MEDIA_PREVIOUS,o,n,confidence=.93f)
        V5TextPipeline.extractAfter(n,listOf("включи на ютубе","найди на ютубе","включи видео","открой видео","play youtube"))?.let{return ParsedCommand(CommandKind.YOUTUBE,o,n,it,.96f)}
        return null
    }
    private fun parseApps(n:String,o:String):ParsedCommand? {
        val arg=V5TextPipeline.extractAfter(n,listOf("открой приложение","запусти приложение","открой","запусти","open app","launch")) ?: return null
        if(arg in setOf("настройки","браузер","сайт","страницу","settings","browser")) return null
        return ParsedCommand(CommandKind.OPEN_APP,o,n,arg,.90f)
    }
    private fun parseWeb(n:String,o:String):ParsedCommand? {
        V5TextPipeline.extractAfter(n,listOf("найди в интернете","поищи в интернете","найди информацию о","поищи","найди","search for","search"))?.let{return ParsedCommand(CommandKind.SEARCH_WEB,o,n,it,.89f)}
        return null
    }
    private fun parseSchool(n:String,o:String):ParsedCommand? {
        if(n.contains("таблиц")&&n.contains("мендел")) return ParsedCommand(CommandKind.SCHOOL,o,n,n,.98f)
        if(V5TextPipeline.containsAny(n,setOf("реши","решить","посчитай","вычисли","сколько")) && (n.any{it.isDigit()}||n.contains("площадь")||n.contains("процент")||n.contains("формул")))
            return ParsedCommand(CommandKind.CALCULATE,o,n,n,.86f)
        return null
    }
}