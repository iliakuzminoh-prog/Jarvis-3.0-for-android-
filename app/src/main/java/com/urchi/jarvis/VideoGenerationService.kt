package com.urchi.jarvis

import android.content.Context
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors
import org.json.JSONObject

object VideoGenerationService {
    private val executor = Executors.newCachedThreadPool()
    fun generate(context: Context, prompt: String, baseUrl: String, path: String, model: String, callback: (Result<File>) -> Unit) {
        val key = JarvisSecretStore().get(context, "video_api_key")
        if (key.isNullOrBlank()) { callback(Result.failure(IllegalStateException("VIDEO_API_KEY_MISSING"))); return }
        executor.execute {
            try {
                val connection = (URL(baseUrl.trimEnd('/') + "/" + path.trimStart('/')).openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"; connectTimeout = 20000; readTimeout = 120000; doOutput = true
                    setRequestProperty("Authorization", "Bearer $key")
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("Accept", "application/json")
                }
                val body = JSONObject().put("model", model).put("prompt", prompt.take(8000))
                connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
                val status = connection.responseCode
                val text = (if (status in 200..299) connection.inputStream else connection.errorStream)?.bufferedReader()?.use { it.readText() }.orEmpty()
                connection.disconnect()
                if (status !in 200..299) throw IllegalStateException("HTTP $status: $text")
                val json = JSONObject(text)
                val directUrl = json.optString("video_url").ifBlank { json.optString("url") }.ifBlank { json.optJSONObject("data")?.optString("url").orEmpty() }
                if (directUrl.isBlank()) {
                    val id = json.optString("id")
                    if (id.isBlank()) throw IllegalStateException("Ответ не содержит video_url/url/id")
                    throw IllegalStateException("VIDEO_JOB_CREATED:$id")
                }
                callback(Result.success(downloadToCache(context, directUrl)))
            } catch (e: Exception) { callback(Result.failure(e)) }
        }
    }
    private fun downloadToCache(context: Context, remoteUrl: String): File {
        val connection = URL(remoteUrl).openConnection() as HttpURLConnection
        connection.connectTimeout = 20000; connection.readTimeout = 120000; connection.connect()
        if (connection.responseCode !in 200..299) throw IllegalStateException("Видео не удалось скачать: HTTP " + connection.responseCode)
        val file = File(context.cacheDir, "jarvis_video_" + System.currentTimeMillis() + ".mp4")
        connection.inputStream.use { input -> FileOutputStream(file).use { output -> input.copyTo(output) } }
        connection.disconnect()
        return file
    }
}
