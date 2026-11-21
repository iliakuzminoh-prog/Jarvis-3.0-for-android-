package com.urchi.jarvis

object CodeEngineeringEngine {
    data class Request(
        val task: String, val language: String = "Kotlin", val framework: String = "",
        val target: String = "", val existingCode: String = "",
        val constraints: List<String> = emptyList(), val requireTests: Boolean = true
    )

    fun generate(context: android.content.Context, request: Request, callback: (Result<String>) -> Unit) {
        if (request.task.trim().isEmpty()) { callback(Result.failure(IllegalArgumentException("CODE_TASK_EMPTY"))); return }
        AiOrchestrator.answer(context, buildPrompt(request), emptyList(), CODE_SYSTEM_PROMPT, callback)
    }

    fun review(context: android.content.Context, language: String, code: String, callback: (Result<String>) -> Unit) {
        if (code.trim().isEmpty()) { callback(Result.failure(IllegalArgumentException("CODE_EMPTY"))); return }
        val prompt = "Проведи adversarial code review. Язык: " + language +
            "\nКод:\n" + code.take(50000) +
            "\nНайди компиляционные, логические, concurrency, resource, security и API-дефекты. " +
            "Верни критические ошибки, точные исправления и тесты для каждой критической ошибки."
        AiOrchestrator.answer(context, prompt, emptyList(), CODE_REVIEW_SYSTEM_PROMPT, callback)
    }

    private fun buildPrompt(r: Request): String {
        val constraints = r.constraints.joinToString("\n") { "- " + it.trim() }
        val existing = if (r.existingCode.isBlank()) "Нет существующего кода." else r.existingCode.take(50000)
        return "Задача: " + r.task.trim() +
            "\nЯзык: " + r.language +
            "\nFramework/SDK: " + r.framework.ifBlank { "не указан" } +
            "\nЦель/платформа: " + r.target.ifBlank { "не указана" } +
            "\nОграничения:\n" + constraints.ifBlank { "- Не выдумывать API, зависимости и результаты тестов." } +
            "\nСуществующий код:\n" + existing +
            "\nТребования:\n- Зафиксируй архитектуру и зависимости." +
            "\n- Дай полный код всех изменяемых файлов." +
            "\n- Не используй несуществующие классы, методы или библиотеки." +
            "\n- Обработай ошибки, таймауты, отмену, ресурсы и секреты." +
            "\n- " + if (r.requireTests) "Добавь тесты критической логики." else "Укажи ручные проверки." +
            "\n- Проведи самопроверку на компиляцию, edge cases, безопасность и производительность." +
            "\n- Если чего-то не знаешь, явно обозначь допущение."
    }

    private const val CODE_SYSTEM_PROMPT = "Ты senior software engineer и adversarial code reviewer. " +
        "Цель — корректность и проверяемость, а не объём. Не выдумывай библиотеки, API, версии и результаты тестов. " +
        "Ищи собственные ошибки до выдачи результата."

    private const val CODE_REVIEW_SYSTEM_PROMPT = "Ты adversarial reviewer production-кода. " +
        "Не соглашайся автоматически. Ищи компиляционные, логические, security, concurrency, resource и compatibility-дефекты."
}
