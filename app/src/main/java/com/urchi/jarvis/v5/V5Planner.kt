package com.urchi.jarvis.v5

/**
 * Converts a natural multi-action request into ordered segments.
 * This is deliberately deterministic: the action executor can verify every
 * step instead of pretending that one successful launch means the whole plan succeeded.
 */
class V5Planner {
    fun split(raw: String): List<String> {
        val n = V5TextPipeline.normalize(raw)
        if (n.isBlank()) return emptyList()
        val protected = n
            .replace(Regex("""\bа потом\b"""), "§")
            .replace(Regex("""\bзатем\b"""), "§")
            .replace(Regex("""\bпосле этого\b"""), "§")
            .replace(Regex("""\bи потом\b"""), "§")
        return protected.split('§', ';')
            .flatMap { part ->
                val p = part.trim()
                if (p.contains(" и ") && p.length > 90) p.split(Regex("""\s+и\s+(?=(открой|запусти|включи|выключи|найди|поставь|перейди|отправь|сделай))"""))
                else listOf(p)
            }
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
    }
}
