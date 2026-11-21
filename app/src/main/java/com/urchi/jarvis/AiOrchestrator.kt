package com.urchi.jarvis

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

object AiOrchestrator {
    private const val MAX_ATTEMPTS = 2
    private val executor = Executors.newCachedThreadPool()

    fun answer(context: Context, userText: String, history: List<Pair<String, String>>, callback: (Result<String>) -> Unit) {
        val key = JarvisSecretStore().get(context, "ai_api_key")
        val c = AiProviderStore.load(context)
        if (key.isNullOrBlank() || c.baseUrl.isBlank() || c.model.isBlank()) {
            callback(Result.failure(IllegalStateException("AI provider is not configured"))); return
        }
        executor.execute {
            var lastError: Throwable? = null
            for (attempt in 1..MAX_ATTEMPTS) {
                try {
                    callback(Result.success(requestOnce(key, c, userText, history)))
                    return@execute
                } catch (e: RetryableAiException) {
                    lastError = e
                    if (!e.retryable || attempt == MAX_ATTEMPTS) break
                    Thread.sleep(900L)
                } catch (e: Exception) {
                    callback(Result.failure(e)); return@execute
                }
            }
            callback(Result.failure(lastError ?: IllegalStateException("AI request failed")))
        }
    }

    private fun requestOnce(key: String, c: AiProviderConfig, userText: String, history: List<Pair<String, String>>): String {
        val connection = (URL(c.baseUrl + c.chatPath).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"; connectTimeout = 15000; readTimeout = 60000; doOutput = true
            setRequestProperty("Authorization", "Bearer $key")
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Accept", "application/json")
        }
        try {
            val messages = JSONArray().put(JSONObject().put("role", "system").put("content", c.systemPrompt))
            history.takeLast(12).forEach { (role, content) ->
                if (content.isNotBlank()) messages.put(JSONObject().put("role", role).put("content", content.take(6000)))
            }
            messages.put(JSONObject().put("role", "user").put("content", userText.take(12000)))
            val body = JSONObject().put("model", c.model).put("messages", messages).put("temperature", 0.3).put("stream", false)
            connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            val status = connection.responseCode
            val text = (if (status in 200..299) connection.inputStream else connection.errorStream)?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (status !in 200..299) {
                val retryable = status == 408 || status == 409 || status == 425 || status == 429 || status in 500..599
                throw RetryableAiException(status, text, retryable)
            }
            val answer = extractAnswer(JSONObject(text))
            if (answer.isBlank()) throw IllegalStateException("Provider returned no text response")
            return answer
        } finally { connection.disconnect() }
    }

    private class RetryableAiException(val status: Int, message: String, val retryable: Boolean) :
        IllegalStateException("HTTP $status: $message")

    private fun extractAnswer(json: JSONObject): String {
        val chatContent = json.optJSONArray("choices")?.optJSONObject(0)?.optJSONObject("message")?.optString("content").orEmpty()
        if (chatContent.isNotBlank()) return chatContent.trim()
        val outputText = json.optString("output_text").trim()
        if (outputText.isNotBlank()) return outputText
        val output = json.optJSONArray("output")
        if (output != null) for (i in 0 until output.length()) {
            val content = output.optJSONObject(i)?.optJSONArray("content") ?: continue
            for (j in 0 until content.length()) {
                val value = content.optJSONObject(j)?.optString("text").orEmpty().trim()
                if (value.isNotBlank()) return value
            }
        }
        return ""
    }
}
