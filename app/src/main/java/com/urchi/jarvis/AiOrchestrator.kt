package com.urchi.jarvis

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

object AiOrchestrator {
    private val executor = Executors.newCachedThreadPool()
    fun answer(context: Context, userText: String, history: List<Pair<String, String>>, callback: (Result<String>) -> Unit) {
        val key = JarvisSecretStore().get(context, "ai_api_key")
        val c = AiProviderStore.load(context)
        if (key.isNullOrBlank() || c.baseUrl.isBlank() || c.model.isBlank()) {
            callback(Result.failure(IllegalStateException("AI provider is not configured"))); return
        }
        executor.execute {
            try {
                val connection = (URL(c.baseUrl + c.chatPath).openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"; connectTimeout = 15000; readTimeout = 60000; doOutput = true
                    setRequestProperty("Authorization", "Bearer $key")
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("Accept", "application/json")
                }
                val messages = JSONArray().put(JSONObject().put("role", "system").put("content", c.systemPrompt))
                history.takeLast(12).forEach { (role, content) ->
                    if (content.isNotBlank()) messages.put(JSONObject().put("role", role).put("content", content.take(6000)))
                }
                messages.put(JSONObject().put("role", "user").put("content", userText.take(12000)))
                val body = JSONObject().put("model", c.model).put("messages", messages).put("temperature", 0.3).put("stream", false)
                connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
                val status = connection.responseCode
                val text = (if (status in 200..299) connection.inputStream else connection.errorStream)?.bufferedReader()?.use { it.readText() }.orEmpty()
                connection.disconnect()
                if (status !in 200..299) throw IllegalStateException("HTTP $status: $text")
                val answer = JSONObject(text).optJSONArray("choices")?.optJSONObject(0)?.optJSONObject("message")?.optString("content").orEmpty().trim()
                if (answer.isBlank()) throw IllegalStateException("Провайдер не вернул текстовый ответ")
                callback(Result.success(answer))
            } catch (e: Exception) { callback(Result.failure(e)) }
        }
    }
}
