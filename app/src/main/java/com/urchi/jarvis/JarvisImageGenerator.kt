package com.urchi.jarvis

import android.content.Context
import android.net.Uri
import java.net.HttpURLConnection
import java.net.URL

object JarvisImageGenerator {
    const val BASE_URL = "https://gen.pollinations.ai"
    const val DEFAULT_MODEL = "black-forest-labs/flux.1-schnell"

    fun generate(context: Context, prompt: String, apiKey: String?, model: String = DEFAULT_MODEL,
                 callback: (Result<java.io.File>) -> Unit) {
        Thread {
            try {
                val clean = prompt.trim()
                require(clean.isNotEmpty()) { "Пустой запрос" }
                require(clean.length <= 1000) { "Запрос слишком длинный" }
                val url = BASE_URL + "/image/" + Uri.encode(clean) +
                    "?model=" + Uri.encode(model)
                val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 15000
                    readTimeout = 90000
                    requestMethod = "GET"
                    setRequestProperty("Accept", "image/*")
                    if (!apiKey.isNullOrBlank()) {
                        setRequestProperty("Authorization", "Bearer " + apiKey.trim())
                    }
                    instanceFollowRedirects = true
                }
                conn.connect()
                if (conn.responseCode !in 200..299) error("Сервер генерации вернул HTTP " + conn.responseCode)
                val file = java.io.File.createTempFile("jarvis_image_", ".img", context.cacheDir)
                conn.inputStream.use { input -> file.outputStream().use { input.copyTo(it) } }
                conn.disconnect()
                if (file.length() <= 0L) error("Получено пустое изображение")
                callback(Result.success(file))
            } catch (e: Exception) { callback(Result.failure(e)) }
        }.start()
    }
}