package com.urchi.jarvis.v5

import java.util.Locale

data class Token(val original:String,val normalized:String,val index:Int)

object V5TextPipeline {
    private val punctuation=Regex("""[^\\p{L}\\p{N}\\s]""")
    private val spaces=Regex("""\\s+""")
    private val typoRules=mapOf(
        "ютубе" to "ютуб","ютуб" to "ютуб","телеграмм" to "телеграм",
        "вайфай" to "wifi","вайфая" to "wifi","вайфаю" to "wifi",
        "блютуз" to "bluetooth","громчее" to "громче"
    )
    fun normalize(input:String):String {
        var n=input.lowercase(Locale.ROOT).replace('ё','е').replace(punctuation," ").replace(spaces," ").trim()
        typoRules.forEach { (a,b)-> n=Regex("""\\b${a}\\b""").replace(n,b) }
        return n
    }
    fun tokens(input:String):List<Token> =
        input.trim().split(Regex("""\\s+""")).filter(String::isNotBlank).mapIndexed { i,s->Token(s,normalize(s),i) }
    fun containsAny(text:String,words:Set<String>):Boolean=tokens(text).any{it.normalized in words}
    fun extractAfter(text:String,prefixes:List<String>):String? {
        val n=normalize(text)
        val p=prefixes.map(::normalize).firstOrNull{n.startsWith(it+" ")||n==it}?:return null
        return n.removePrefix(p).trim().ifBlank{null}
    }
}
