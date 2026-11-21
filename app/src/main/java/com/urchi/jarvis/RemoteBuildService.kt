package com.urchi.jarvis

import android.content.Context
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

/**
 * Remote build bridge. Android does not execute received Windows binaries.
 * The remote endpoint must provide its own sandbox/container and compiler allow-list.
 */
object RemoteBuildService {
    private val executor = Executors.newCachedThreadPool()
    private val allowedOutputs = setOf("apk", "aab", "jar", "zip", "exe", "msi", "deb", "rpm", "appimage", "wasm")

    data class Request(
        val projectType: String,
        val language: String,
        val outputType: String,
        val source: String,
        val target: String = "default"
    )

    fun configure(context: Context, baseUrl: String, path: String) {
        context.getSharedPreferences("jarvis_build", Context.MODE_PRIVATE).edit()
            .putString("base", baseUrl.trim().trimEnd('/'))
            .putString("path", path.trim().let { if (it.startsWith("/")) it else "/$it" })
            .apply()
    }

    fun isConfigured(context: Context): Boolean {
        val p = context.getSharedPreferences("jarvis_build", Context.MODE_PRIVATE)
        return !p.getString("base", "").isNullOrBlank() &&
            !p.getString("path", "").isNullOrBlank() &&
            !JarvisSecretStore().get(context, "build_api_key").isNullOrBlank()
    }

    fun build(context: Context, request: Request, callback: (Result<File>) -> Unit) {
        val output = request.outputType.trim().lowercase()
        if (request.source.isBlank()) {
            callback(Result.failure(IllegalArgumentException("BUILD_SOURCE_EMPTY"))); return
        }
        if (output !in allowedOutputs) {
            callback(Result.failure(IllegalArgumentException("BUILD_OUTPUT_UNSUPPORTED"))); return
        }
        val prefs = context.getSharedPreferences("jarvis_build", Context.MODE_PRIVATE)
        val base = prefs.getString("base", "").orEmpty()
        val path = prefs.getString("path", "").orEmpty()
        val key = JarvisSecretStore().get(context, "build_api_key")
        if (base.isBlank() || path.isBlank() || key.isNullOrBlank()) {
            callback(Result.failure(IllegalStateException("BUILD_GATEWAY_NOT_CONFIGURED"))); return
        }

        executor.execute {
            try {
                val c = (URL(base + path).openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    connectTimeout = 10000
                    readTimeout = 180000
                    doOutput = true
                    setRequestProperty("Authorization", "Bearer " + key.trim())
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("Accept", "application/json")
                }
                try {
                    val body = JSONObject()
                        .put("project_type", request.projectType.take(80))
                        .put("language", request.language.take(80))
                        .put("output_type", output)
                        .put("target", request.target.take(120))
                        .put("source", request.source.take(2_000_000))
                    c.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
                    val status = c.responseCode
                    val response = (if (status in 200..299) c.inputStream else c.errorStream)
                        ?.bufferedReader()?.use { it.readText() }.orEmpty()
                    if (status !in 200..299) error("BUILD_GATEWAY_HTTP_$status:" + response.take(1500))
                    val json = JSONObject(response)
                    val artifactUrl = json.optString("artifact_url")
                        .ifBlank { json.optJSONObject("artifact")?.optString("url").orEmpty() }
                    if (artifactUrl.isBlank()) {
                        val jobId = json.optString("job_id").ifBlank { json.optString("id") }
                        if (jobId.isNotBlank()) error("BUILD_JOB_CREATED:" + jobId)
                        error("BUILD_GATEWAY_NO_ARTIFACT")
                    }
                    callback(Result.success(downloadArtifact(context, artifactUrl, output)))
                } finally {
                    c.disconnect()
                }
            } catch (e: Exception) {
                callback(Result.failure(e))
            }
        }
    }

    private fun downloadArtifact(context: Context, remoteUrl: String, outputType: String): File {
        val c = URL(remoteUrl).openConnection() as HttpURLConnection
        try {
            c.requestMethod = "GET"
            c.connectTimeout = 10000
            c.readTimeout = 180000
            c.connect()
            if (c.responseCode !in 200..299) error("BUILD_ARTIFACT_HTTP_" + c.responseCode)
            val file = File(context.cacheDir, "jarvis_build_" + System.currentTimeMillis() + "." + outputType)
            c.inputStream.use { input -> FileOutputStream(file).use { output -> input.copyTo(output) } }
            if (file.length() == 0L) {
                file.delete()
                error("BUILD_ARTIFACT_EMPTY")
            }
            return file
        } finally {
            c.disconnect()
        }
    }
}
