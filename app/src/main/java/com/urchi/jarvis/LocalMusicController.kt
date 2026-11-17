package com.urchi.jarvis

import android.content.Context
import android.net.Uri
import android.media.MediaPlayer

/**
 * Plays user-selected local audio through a persisted SAF URI.
 * Android keeps storage access scoped to the document the user selected.
 */
class LocalMusicController(private val context: Context) {
    private val prefs = context.getSharedPreferences("jarvis_local_music", Context.MODE_PRIVATE)
    private var player: MediaPlayer? = null
    var currentName: String = prefs.getString("name", "") ?: ""
        private set

    fun remember(uri: Uri, name: String) {
        val flags = android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
        try { context.contentResolver.takePersistableUriPermission(uri, flags) } catch (_: Exception) { }
        prefs.edit().putString("uri", uri.toString()).putString("name", name).apply()
        currentName = name
    }

    fun playSaved(): Result<String> = runCatching {
        val raw = prefs.getString("uri", null) ?: error("Нет выбранного аудиофайла.")
        val uri = Uri.parse(raw)
        player?.release()
        player = MediaPlayer().apply {
            setDataSource(context, uri)
            setOnCompletionListener { release(); player = null }
            prepare()
            start()
        }
        currentName.ifBlank { "локальный файл" }
    }

    fun play(uri: Uri, name: String): Result<String> {
        remember(uri, name)
        return playSaved()
    }

    fun pause(): Boolean = player?.let { if (it.isPlaying) { it.pause(); true } else false } ?: false
    fun resume(): Boolean = player?.let { if (!it.isPlaying) { it.start(); true } else false } ?: false
    fun stop() { player?.stop(); player?.release(); player = null }

    fun hasSaved(): Boolean = !prefs.getString("uri", null).isNullOrBlank()

    fun release() {
        player?.release()
        player = null
    }
}
