package com.urchi.jarvis

import android.content.Context
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONObject

object GifGenerationService {
    fun generate(context: Context, prompt: String, baseUrl: String, path: String, model: String, callback: (Result<File>) -> Unit) {
        val key = JarvisSecretStore().get(context, "gif_api_key")
        if (key.isNullOrBlank()) { callback(Result.failure(IllegalStateException("GIF_API_KEY_MISSING"))); return }
        Thread {
            try {
                require(prompt.trim().isNotEmpty()) { "GIF_PROMPT_EMPTY" }
                require(baseUrl.isNotBlank() && path.isNotBlank()) { "GIF_PROVIDER_NOT_CONFIGURED" }
                val connection = (URL(baseUrl.trimEnd('/') + "/" + path.trimStart('/')).openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"; connectTimeout = 20000; readTimeout = 120000; doOutput = true
                    setRequestProperty("Authorization", "Bearer " + key.trim())
                    setRequestProperty("Content-Type", "application/json"); setRequestProperty("Accept", "application/json")
                }
                try {
                    val body = JSONObject().put("model", model).put("prompt", prompt.trim().take(8000))
                    connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
                    val status = connection.responseCode
                    val text = (if (status in 200..299) connection.inputStream else connection.errorStream)?.bufferedReader()?.use { it.readText() }.orEmpty()
                    if (status !in 200..299) error("GIF provider HTTP " + status + ": " + text.take(4000))
                    val json = JSONObject(text)
                    val directUrl = json.optString("gif_url").ifBlank { json.optString("url") }.ifBlank { json.optJSONObject("data")?.optString("url").orEmpty() }
                    if (directUrl.isBlank()) {
                        val id = json.optString("id")
                        if (id.isBlank()) error("GIF response contains neither gif_url/url/id")
                        error("GIF_JOB_CREATED:" + id)
                    }
                    callback(Result.success(download(context, directUrl)))
                } finally { connection.disconnect() }
            } catch (e: Exception) { callback(Result.failure(e)) }
        }.start()
    }

    private fun download(context: Context, remoteUrl: String): File {
        val connection = URL(remoteUrl).openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = 20000; connection.readTimeout = 120000; connection.requestMethod = "GET"; connection.connect()
            if (connection.responseCode !in 200..299) error("GIF download HTTP " + connection.responseCode)
            val file = File(context.cacheDir, "jarvis_gif_" + System.currentTimeMillis() + ".gif")
            connection.inputStream.use { input -> FileOutputStream(file).use { output -> input.copyTo(output) } }
            if (file.length() <= 0L) { file.delete(); error("GIF download returned an empty file") }
            return file
        } finally { connection.disconnect() }
    }
}
