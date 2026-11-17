package com.urchi.jarvis.v4.core

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

class MemoryStore(context: Context) {
    data class Memory(
        val id: Long,
        val text: String,
        val category: String,
        val createdAt: Long,
        val pinned: Boolean
    )

    private val prefs = context.getSharedPreferences("jarvis_memory_v4", Context.MODE_PRIVATE)
    private val lock = Any()

    fun remember(text: String, category: String = "general", pinned: Boolean = false): Memory {
        val clean = text.trim()
        require(clean.isNotEmpty()) { "Memory text must not be empty" }
        require(clean.length <= MAX_TEXT_LENGTH) { "Memory text is too long" }
        val safeCategory = category.trim().take(MAX_CATEGORY_LENGTH).ifEmpty { "general" }
        val now = System.currentTimeMillis()
        val item = Memory(now, clean, safeCategory, now, pinned)
        synchronized(lock) {
            val all = readAll().toMutableList()
            all.removeAll { it.text.equals(clean, ignoreCase = true) }
            all.add(item)
            writeAll(all.takeLast(MAX_MEMORIES))
        }
        return item
    }

    fun forget(text: String): Int {
        val query = text.trim().take(MAX_QUERY_LENGTH)
        if (query.isEmpty()) return 0
        synchronized(lock) {
            val all = readAll()
            val kept = all.filterNot {
                it.text.equals(query, true) || it.text.contains(query, true)
            }
            writeAll(kept)
            return all.size - kept.size
        }
    }

    fun search(query: String, limit: Int = 8): List<Memory> {
        val q = query.trim().take(MAX_QUERY_LENGTH).lowercase(Locale.getDefault())
        if (q.isEmpty()) return emptyList()
        val safeLimit = limit.coerceIn(1, MAX_SEARCH_RESULTS)
        return synchronized(lock) {
            readAll().sortedWith(
                compareByDescending<Memory> { if (it.pinned) 1 else 0 }
                    .thenByDescending { score(it.text, q) }
                    .thenByDescending { it.createdAt }
            ).filter { score(it.text, q) > 0 }.take(safeLimit)
        }
    }

    fun recent(limit: Int = 12): List<Memory> =
        synchronized(lock) {
            readAll().sortedByDescending { it.createdAt }
                .take(limit.coerceIn(1, MAX_SEARCH_RESULTS))
        }

    fun clearUnpinned() {
        synchronized(lock) { writeAll(readAll().filter { it.pinned }) }
    }

    private fun score(text: String, query: String): Int {
        val normalized = text.lowercase(Locale.getDefault())
        if (normalized == query) return 100
        if (normalized.contains(query)) return 70
        return query.split(Regex("\\s+"))
            .filter { it.length > 2 }
            .count { normalized.contains(it) } * 10
    }

    private fun readAll(): List<Memory> {
        val raw = prefs.getString(KEY, "[]") ?: "[]"
        val array = try { JSONArray(raw) } catch (_: Exception) { JSONArray() }
        return buildList {
            for (i in 0 until array.length()) {
                val o = array.optJSONObject(i) ?: continue
                val text = o.optString("text").trim()
                if (text.isEmpty()) continue
                add(Memory(
                    o.optLong("id", i.toLong()),
                    text,
                    o.optString("category", "general"),
                    o.optLong("createdAt", 0L),
                    o.optBoolean("pinned", false)
                ))
            }
        }
    }

    private fun writeAll(items: List<Memory>) {
        val array = JSONArray()
        items.take(MAX_MEMORIES).forEach {
            array.put(JSONObject()
                .put("id", it.id)
                .put("text", it.text)
                .put("category", it.category)
                .put("createdAt", it.createdAt)
                .put("pinned", it.pinned))
        }
        prefs.edit().putString(KEY, array.toString()).apply()
    }

    companion object {
        private const val KEY = "memories"
        private const val MAX_MEMORIES = 200_000
        private const val MAX_TEXT_LENGTH = 1000
        private const val MAX_CATEGORY_LENGTH = 48
        private const val MAX_QUERY_LENGTH = 200
        private const val MAX_SEARCH_RESULTS = 50
    }
}
