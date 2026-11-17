package com.urchi.jarvis.v5

import java.util.Locale

object RussianMorphology {
    private val endings = listOf("иями","ями","ами","ого","ему","ому","ыми","ими","ее","ие","ые","ое","ей","ий","ый","ой","ем","им","ым","ом","ах","ях","ам","ям","ов","ев","ы","и","а","я","у","ю","е","о")
    fun stem(word: String): String {
        val w = word.lowercase(Locale.ROOT).replace('ё','е')
        if (w.length <= 4) return w
        for (ending in endings) if (w.endsWith(ending) && w.length - ending.length >= 3) return w.dropLast(ending.length)
        return w
    }
    fun stemSet(text: String): Set<String> = V5TextPipeline.tokens(text).map { stem(it.normalized) }.filter { it.length >= 2 }.toSet()
    fun hasStem(text: String, vararg candidates: String): Boolean {
        val stems = stemSet(text)
        return candidates.any { c -> stem(c) in stems || stems.any { s -> s.startsWith(stem(c)) } }
    }
}
