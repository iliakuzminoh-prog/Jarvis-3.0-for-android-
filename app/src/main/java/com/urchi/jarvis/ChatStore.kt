package com.urchi.jarvis

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

class ChatStore(context: Context) {
    data class Message(val role: String, val text: String, val time: Long)
    data class Chat(val id: Long, var title: String, var updatedAt: Long, val messages: MutableList<Message>)

    private val prefs = context.getSharedPreferences("jarvis_chats_v1", Context.MODE_PRIVATE)
    private val lock = Any()

    fun list(): List<Chat> = synchronized(lock) { read().sortedByDescending { it.updatedAt } }

    fun getOrCreate(): Chat = synchronized(lock) {
        val all = read().toMutableList()
        all.maxByOrNull { it.updatedAt }?.let { return it }
        val now = System.currentTimeMillis()
        val chat = Chat(now, "Новый чат", now, mutableListOf())
        all.add(chat); write(all); chat
    }

    fun create(title: String = "Новый чат"): Chat = synchronized(lock) {
        val now = System.currentTimeMillis()
        val chat = Chat(now, title.trim().take(80).ifBlank { "Новый чат" }, now, mutableListOf())
        val all = read().toMutableList(); all.add(chat); write(all); chat
    }

    fun rename(id: Long, title: String) = synchronized(lock) {
        val all = read().toMutableList()
        all.firstOrNull { it.id == id }?.apply {
            this.title = title.trim().take(80).ifBlank { "Без названия" }
            updatedAt = System.currentTimeMillis()
        }
        write(all)
    }

    fun delete(id: Long) = synchronized(lock) { write(read().filterNot { it.id == id }) }

    fun add(id: Long, role: String, text: String) = synchronized(lock) {
        val clean = text.trim().take(6000)
        if (clean.isBlank()) return
        val all = read().toMutableList()
        val chat = all.firstOrNull { it.id == id } ?: return
        chat.messages.add(Message(if (role == "user") "user" else "assistant", clean, System.currentTimeMillis()))
        if (chat.title == "Новый чат" && role == "user")
            chat.title = clean.replace(Regex("\\s+"), " ").take(48)
        if (chat.messages.size > MAX_MESSAGES)
            chat.messages.subList(0, chat.messages.size - MAX_MESSAGES).clear()
        chat.updatedAt = System.currentTimeMillis()
        write(all)
    }

    fun search(query: String): List<Chat> {
        val q = query.trim().lowercase(Locale.getDefault())
        if (q.isBlank()) return list()
        return list().filter { c ->
            c.title.lowercase(Locale.getDefault()).contains(q) ||
                c.messages.any { it.text.lowercase(Locale.getDefault()).contains(q) }
        }
    }

    fun messageCount(): Int = list().sumOf { it.messages.size }

    private fun read(): List<Chat> {
        val raw = prefs.getString(KEY, "[]") ?: "[]"
        val arr = try { JSONArray(raw) } catch (_: Exception) { JSONArray() }
        return buildList {
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                val ma = o.optJSONArray("messages") ?: JSONArray()
                val messages = mutableListOf<Message>()
                for (j in 0 until ma.length()) {
                    val m = ma.optJSONObject(j) ?: continue
                    val text = m.optString("text").trim()
                    if (text.isNotBlank()) messages.add(Message(
                        if (m.optString("role") == "user") "user" else "assistant",
                        text, m.optLong("time", 0L)
                    ))
                }
                add(Chat(o.optLong("id", i.toLong()), o.optString("title", "Новый чат"),
                    o.optLong("updatedAt", 0L), messages))
            }
        }
    }

    private fun write(chats: List<Chat>) {
        val arr = JSONArray()
        chats.sortedByDescending { it.updatedAt }.take(MAX_CHATS).forEach { c ->
            val ma = JSONArray()
            c.messages.takeLast(MAX_MESSAGES).forEach { m ->
                ma.put(JSONObject().put("role", m.role).put("text", m.text).put("time", m.time))
            }
            arr.put(JSONObject().put("id", c.id).put("title", c.title)
                .put("updatedAt", c.updatedAt).put("messages", ma))
        }
        prefs.edit().putString(KEY, arr.toString()).apply()
    }

    companion object {
        private const val KEY = "chats"
        private const val MAX_CHATS = 10_000
        private const val MAX_MESSAGES = 200
    }
}
