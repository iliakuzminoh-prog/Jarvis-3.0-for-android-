package com.urchi.jarvis

import android.content.Context
import java.util.concurrent.Executors

/**
 * Multi-pass coding pipeline.
 *
 * Pass 1: architecture + implementation.
 * Pass 2: adversarial review of the generated implementation.
 * Pass 3: repair using the review.
 *
 * It never claims that compilation succeeded: compilation is a separate CI/device
 * concern and must be verified by the build system.
 */
object CodeEngineeringEngine {
    data class Request(
        val task: String,
        val language: String = "Kotlin",
        val framework: String = "",
        val target: String = "",
        val existingCode: String = "",
        val constraints: List<String> = emptyList(),
        val requireTests: Boolean = true,
        val auditAndRepair: Boolean = true
    )

    data class Result(
        val implementation: String,
        val review: String = "",
        val repairedImplementation: String = ""
    )

    private val executor = Executors.newSingleThreadExecutor()

    fun generate(context: Context, request: Request, callback: (kotlin.Result<Result>) -> Unit) {
        if (request.task.trim().isEmpty()) {
            callback(kotlin.Result.failure(IllegalArgumentException("CODE_TASK_EMPTY")))
            return
        }
        executor.execute {
            AiOrchestrator.answer(
                context, buildPrompt(request), emptyList(), CODE_SYSTEM_PROMPT
            ) { first ->
                first.onFailure { callback(kotlin.Result.failure(it)); return@answer }
                val implementation = first.getOrThrow()
                if (!request.auditAndRepair) {
                    callback(kotlin.Result.success(Result(implementation = implementation)))
                    return@answer
                }
                reviewGenerated(context, request, implementation) { reviewed ->
                    reviewed.onFailure { callback(kotlin.Result.failure(it)); return@reviewGenerated }
                    val review = reviewed.getOrThrow()
                    repair(context, request, implementation, review) { repaired ->
                        repaired.onFailure { callback(kotlin.Result.failure(it)); return@repair }
                        callback(kotlin.Result.success(Result(
                            implementation = implementation,
                            review = review,
                            repairedImplementation = repaired.getOrThrow()
                        )))
                    }
                }
            }
        }
    }

    fun review(context: Context, language: String, code: String, callback: (kotlin.Result<String>) -> Unit) {
        if (code.trim().isEmpty()) {
            callback(kotlin.Result.failure(IllegalArgumentException("CODE_EMPTY")))
            return
        }
        val prompt = "Проведи adversarial code review.\n" +
            "Язык: " + language.ifBlank { "не указан" } +
            "\nКод:\n" + code.take(60000) +
            "\nНайди компиляционные, логические, concurrency, resource, security и API-дефекты. " +
            "Не хвали код ради вежливости. Верни критические ошибки, точные исправления и тесты."
        AiOrchestrator.answer(context, prompt, emptyList(), CODE_REVIEW_SYSTEM_PROMPT, callback)
    }

    private fun reviewGenerated(context: Context, r: Request, implementation: String, callback: (kotlin.Result<String>) -> Unit) {
        review(context, r.language, implementation, callback)
    }

    private fun repair(
        context: Context,
        r: Request,
        implementation: String,
        review: String,
        callback: (kotlin.Result<String>) -> Unit
    ) {
        val prompt = "Исправь реализацию после adversarial review.\n" +
            "Задача: " + r.task.trim() +
            "\nЯзык: " + r.language +
            "\nПервоначальная реализация:\n" + implementation.take(60000) +
            "\nReview:\n" + review.take(30000) +
            "\nВерни ПОЛНУЮ исправленную реализацию всех изменённых файлов. " +
            "Не выдумывай зависимости или API. Не утверждай, что код скомпилирован."
        AiOrchestrator.answer(context, prompt, emptyList(), CODE_REPAIR_SYSTEM_PROMPT, callback)
    }

    private fun buildPrompt(r: Request): String {
        val constraints = r.constraints.joinToString("\n") { "- " + it.trim() }
        val existing = if (r.existingCode.isBlank()) "Нет существующего кода." else r.existingCode.take(60000)
        return "Задача: " + r.task.trim() +
            "\nЯзык: " + r.language +
            "\nFramework/SDK: " + r.framework.ifBlank { "не указан" } +
            "\nЦель/платформа: " + r.target.ifBlank { "не указана" } +
            "\nОграничения:\n" + constraints.ifBlank { "- Не выдумывать API, зависимости и результаты тестов." } +
            "\nСуществующий код:\n" + existing +
            "\nТребования:\n" +
            "- Сначала продумай архитектуру и зависимости.\n" +
            "- Дай полный код всех изменяемых файлов.\n" +
            "- Не используй несуществующие классы, методы или библиотеки.\n" +
            "- Обработай ошибки, таймауты, отмену, ресурсы и секреты.\n" +
            "- " + if (r.requireTests) "Добавь тесты критической логики.\n" else "Укажи ручные проверки.\n" +
            "- Проверь edge cases, безопасность и производительность до выдачи ответа.\n" +
            "- Если данных недостаточно, явно укажи допущение."
    }

    private const val CODE_SYSTEM_PROMPT =
        "Ты senior software engineer. Пиши production-grade код. " +
        "Сначала моделируй зависимости и ограничения, затем реализацию. " +
        "Не выдумывай библиотеки, API, версии и результаты тестов. " +
        "Считай компиляцию и тесты неверными, пока они реально не проверены."

    private const val CODE_REVIEW_SYSTEM_PROMPT =
        "Ты adversarial reviewer production-кода. " +
        "Ищи реальные дефекты: компиляция, логика, security, concurrency, ресурсы, API и производительность. " +
        "Не соглашайся с автором автоматически."

    private const val CODE_REPAIR_SYSTEM_PROMPT =
        "Ты senior engineer на этапе исправления после строгого code review. " +
        "Исправляй только реальные проблемы, сохраняя совместимость. " +
        "Не выдумывай API и не заявляй о непроверенной компиляции."
}
