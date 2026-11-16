package com.urchi.jarvis

import java.util.Locale
import kotlin.math.abs
import kotlin.math.pow

/** Offline deterministic arithmetic and linear-equation solver. Unsupported syntax returns null. */
object MathSolver {
    private data class Affine(val a: Double, val b: Double) {
        operator fun plus(o: Affine) = Affine(a + o.a, b + o.b)
        operator fun minus(o: Affine) = Affine(a - o.a, b - o.b)
        operator fun times(o: Affine): Affine? =
            if (abs(a) < EPS) Affine(b * o.a, b * o.b)
            else if (abs(o.a) < EPS) Affine(a * o.b, b * o.b) else null
        operator fun div(o: Affine): Affine {
            if (abs(o.a) > EPS) throw IllegalArgumentException("деление на выражение с переменной не поддерживается")
            if (abs(o.b) < EPS) throw ArithmeticException("деление на ноль")
            return Affine(a / o.b, b / o.b)
        }
    }

    private class Parser(source: String) {
        private val tokens = Regex("""(?:\d+(?:[.,]\d+)?|[a-zA-Zа-яА-Я]+|[()+*/^\-])""")
            .findAll(source.lowercase(Locale.ROOT).replace('×','*').replace('÷','/'))
            .map { it.value }.toList()
        private var pos = 0
        fun parse(): Affine {
            val v = expression()
            if (pos != tokens.size) throw IllegalArgumentException("лишний фрагмент")
            return v
        }
        private fun expression(): Affine {
            var v = term()
            while (peek("+") || peek("-")) {
                val op = take()
                val r = term()
                v = if (op == "+") v + r else v - r
            }
            return v
        }
        private fun term(): Affine {
            var v = power()
            while (peek("*") || peek("/") || peek("x") || peek("(")) {
                val explicit = peek("*") || peek("/")
                val op = if (explicit) take() else "*"
                val r = power()
                v = if (op == "*") v.times(r) ?: throw IllegalArgumentException("нелинейное умножение")
                    else v.div(r)
            }
            return v
        }
        private fun power(): Affine {
            var v = unary()
            if (peek("^")) {
                take()
                val e = unary()
                if (abs(e.a) > EPS || abs(e.b - e.b.toInt()) > EPS || e.b !in 0.0..8.0 || abs(v.a) > EPS)
                    throw IllegalArgumentException("степень не поддерживается")
                v = Affine(0.0, v.b.pow(e.b.toInt()))
            }
            return v
        }
        private fun unary(): Affine {
            if (peek("+")) { take(); return unary() }
            if (peek("-")) { take(); val v = unary(); return Affine(-v.a, -v.b) }
            if (peek("(")) {
                take()
                val v = expression()
                if (!peek(")")) throw IllegalArgumentException("нет закрывающей скобки")
                take()
                return v
            }
            val t = take()
            if (t == "x") return Affine(1.0, 0.0)
            return Affine(0.0, t.replace(',', '.').toDoubleOrNull()
                ?: throw IllegalArgumentException("неизвестное обозначение"))
        }
        private fun peek(s: String) = tokens.getOrNull(pos) == s
        private fun take(): String = tokens.getOrNull(pos++) ?: throw IllegalArgumentException("неполное выражение")
    }

    fun solve(raw: String): String? {
        val q = raw.lowercase(Locale.ROOT).replace('ё','е')
            .replace(Regex("^(реши|решить|посчитай|вычисли|найди)\\s+(задачу|уравнение|пример)?\\s*"), "")
            .replace("сколько будет", "").replace("посчитай", "").replace("вычисли", "")
            .replace("реши", "").replace("равно", "=")
            .replace("умножить на", "*").replace("разделить на", "/")
            .replace("плюс", "+").replace("минус", "-").replace("умножить", "*")
            .replace("делить на", "/").replace("икс", "x").replace("х", "x").trim()
        if (q.isBlank() || (!q.any { it.isDigit() } && !q.contains('x'))) return null
        val percentMatch = Regex("""(\d+(?:[.,]\d+)?)\s*(?:%|процент(?:а|ов)?)\s*(?:от|of)\s*(\d+(?:[.,]\d+)?)""")
            .find(q)
        if (percentMatch != null) {
            val percent = percentMatch.groupValues[1].replace(',', '.').toDoubleOrNull() ?: return null
            val base = percentMatch.groupValues[2].replace(',', '.').toDoubleOrNull() ?: return null
            return "Ответ: ${fmt(percent * base / 100.0)}."
        }
        if (!q.any { it in "+-*/^=()" } && !q.contains('x')) return null
        return try {
            if (q.count { it == '=' } > 1) return null
            if ('=' in q) {
                val sides = q.split('=', limit = 2)
                val l = Parser(sides[0]).parse()
                val r = Parser(sides[1]).parse()
                val coefficient = l.a - r.a
                val constant = r.b - l.b
                if (abs(coefficient) < EPS) {
                    if (abs(constant) < EPS) "Равенство верно при любом x."
                    else "Решений нет: переменная сократилась, но равенство неверно."
                } else {
                    val x = constant / coefficient
                    "Решение уравнения:\n1. Переносим слагаемые с x влево, числа вправо.\n" +
                        "2. Получаем ${fmt(coefficient)}x = ${fmt(constant)}.\n" +
                        "3. Делим обе части на ${fmt(coefficient)}.\nОтвет: x = ${fmt(x)}."
                }
            } else {
                val v = Parser(q).parse()
                if (abs(v.a) > EPS) null else "Результат: ${fmt(v.b)}."
            }
        } catch (_: ArithmeticException) { "Нельзя делить на ноль." }
        catch (_: Exception) { null }
    }

    private fun fmt(v: Double): String {
        val s = if (abs(v - v.toLong()) < EPS) v.toLong().toString()
            else String.format(Locale.ROOT, "%.6f", v).trimEnd('0').trimEnd('.')
        return s.replace('.', ',')
    }
    private const val EPS = 1e-9
}
