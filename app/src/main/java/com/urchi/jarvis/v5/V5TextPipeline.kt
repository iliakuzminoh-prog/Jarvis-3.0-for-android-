package com.urchi.jarvis.v5

import java.util.Locale

data class Token(val original: String, val normalized: String, val index: Int)

object V5TextPipeline {
    private val punctuation = Regex("""[^\p{L}\p{N}\s]""")
    private val spaces = Regex("""\s+""")

    fun normalize(input: String): String = input.lowercase(Locale.ROOT).replace('ё', 'ё'.lowercase()[0]).replace(punctuation, " ").replace(spaces, " ").trim()
    fun tokens(input: String): List<Token> = input.trim().split(Regex("""\s+""")).filter { it.isNotBlank() }.mapIndexed { i, s -> Token(s, normalize(s), i) }
    fun containsAny(text: String, words: Set<String>): Boolean = tokens(text).any { it.normalized in words }
    fun extractAfter(text: String, prefixes: List<String>): String? {
        val n = normalize(text)
        val p = prefixes.map(::normalize).firstOrNull { n.startsWith(it + " ") || n == it } ?: return null
        return n.removePrefix(p).trim().ifBlank { null }
    }
}
