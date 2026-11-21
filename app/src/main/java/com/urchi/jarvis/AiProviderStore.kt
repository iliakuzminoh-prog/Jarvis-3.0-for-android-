package com.urchi.jarvis

import android.content.Context

data class AiProviderConfig(
    val name: String,
    val baseUrl: String,
    val chatPath: String,
    val model: String,
    val systemPrompt: String,
    val fastModel: String = model
)

object AiProviderStore {
    private const val PREFS = "jarvis_ai"

    fun load(context: Context): AiProviderConfig {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val model = p.getString("model", "gpt-4.1-mini") ?: "gpt-4.1-mini"
        return AiProviderConfig(
            p.getString("name", "OpenAI-compatible") ?: "OpenAI-compatible",
            (p.getString("base", "https://api.openai.com/v1") ?: "").trim().trimEnd('/'),
            (p.getString("chat", "/chat/completions") ?: "/chat/completions").let { if (it.startsWith("/")) it else "/$it" },
            model,
            p.getString("system", DEFAULT_SYSTEM) ?: DEFAULT_SYSTEM,
            p.getString("fast_model", model)?.trim().orEmpty().ifBlank { model }
        )
    }

    fun save(context: Context, c: AiProviderConfig) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString("name", c.name.trim().ifBlank { "OpenAI-compatible" })
            .putString("base", c.baseUrl.trim().trimEnd('/'))
            .putString("chat", c.chatPath.trim().let { if (it.startsWith("/")) it else "/$it" })
            .putString("model", c.model.trim())
            .putString("fast_model", c.fastModel.trim().ifBlank { c.model.trim() })
            .putString("system", c.systemPrompt.trim())
            .apply()
    }

    fun setFastModel(context: Context, model: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString("fast_model", model.trim())
            .apply()
    }

    private const val DEFAULT_SYSTEM =
        "Ты JARVIS, мобильный AI-помощник. Отвечай точно и по делу. " +
        "Если данных недостаточно, прямо скажи об этом. Не утверждай, что действие устройства выполнено, " +
        "если Android его не подтвердил. Учитывай предыдущий контекст разговора."
}
