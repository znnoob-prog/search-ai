package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.data.model.SearchResultItem
import org.json.JSONArray
import org.json.JSONObject

class LocalHistoryRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("search_history_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_HISTORY = "recent_queries"
        private const val KEY_BOOKMARKS = "bookmarked_items"
        private const val KEY_THEME = "saved_theme_mode"
        private const val MAX_HISTORY_SIZE = 25
    }

    fun getRecentQueries(): List<String> {
        val raw = prefs.getString(KEY_HISTORY, null) ?: return defaultSuggestions()
        return try {
            val array = JSONArray(raw)
            (0 until array.length()).map { array.getString(it) }
        } catch (e: Exception) {
            defaultSuggestions()
        }
    }

    fun addQuery(query: String) {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return
        val current = getRecentQueries().toMutableList()
        current.remove(trimmed)
        current.add(0, trimmed)
        if (current.size > MAX_HISTORY_SIZE) {
            current.removeAt(current.size - 1)
        }
        val array = JSONArray(current)
        prefs.edit().putString(KEY_HISTORY, array.toString()).apply()
    }

    fun removeQuery(query: String) {
        val current = getRecentQueries().toMutableList()
        current.remove(query)
        val array = JSONArray(current)
        prefs.edit().putString(KEY_HISTORY, array.toString()).apply()
    }

    fun clearHistory() {
        prefs.edit().remove(KEY_HISTORY).apply()
    }

    fun getBookmarks(): List<SearchResultItem> {
        val raw = prefs.getString(KEY_BOOKMARKS, null) ?: return emptyList()
        return try {
            val array = JSONArray(raw)
            val list = mutableListOf<SearchResultItem>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    SearchResultItem(
                        id = obj.optString("id", java.util.UUID.randomUUID().toString()),
                        title = obj.getString("title"),
                        snippet = obj.optString("snippet", ""),
                        url = obj.getString("url"),
                        displayDomain = obj.optString("displayDomain", ""),
                        date = obj.optString("date", ""),
                        category = obj.optString("category", "Web"),
                        imageUrl = if (obj.has("imageUrl") && !obj.isNull("imageUrl")) obj.getString("imageUrl") else null,
                        isBookmarked = true
                    )
                )
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun toggleBookmark(item: SearchResultItem): Boolean {
        val current = getBookmarks().toMutableList()
        val index = current.indexOfFirst { it.url == item.url || it.id == item.id }
        val isNowBookmarked: Boolean
        if (index >= 0) {
            current.removeAt(index)
            isNowBookmarked = false
        } else {
            current.add(0, item.copy(isBookmarked = true))
            isNowBookmarked = true
        }

        val array = JSONArray()
        for (b in current) {
            val obj = JSONObject().apply {
                put("id", b.id)
                put("title", b.title)
                put("snippet", b.snippet)
                put("url", b.url)
                put("displayDomain", b.displayDomain)
                put("date", b.date)
                put("category", b.category)
                put("imageUrl", b.imageUrl)
            }
            array.put(obj)
        }
        prefs.edit().putString(KEY_BOOKMARKS, array.toString()).apply()
        return isNowBookmarked
    }

    fun isBookmarked(url: String): Boolean {
        return getBookmarks().any { it.url == url }
    }

    fun getSavedThemeMode(): String {
        return prefs.getString(KEY_THEME, "SYSTEM") ?: "SYSTEM"
    }

    fun saveThemeMode(mode: String) {
        prefs.edit().putString(KEY_THEME, mode).apply()
    }

    private fun defaultSuggestions(): List<String> {
        return listOf(
            "James Webb Space Telescope discoveries",
            "Next-gen battery technology breakthroughs",
            "Best national parks in autumn",
            "How does quantum computing work",
            "Electric vehicle charging standards comparison"
        )
    }
}
