package com.example.data.repository

import com.example.data.local.SearchQueryDao
import com.example.data.local.SearchQueryEntity
import kotlinx.coroutines.flow.Flow

class RoomSearchRepository(private val dao: SearchQueryDao) {

    val recentQueries: Flow<List<SearchQueryEntity>> = dao.getRecentQueries()

    suspend fun saveQuery(query: String, isDeepThinking: Boolean = false) {
        val trimmed = query.trim()
        if (trimmed.isNotBlank()) {
            dao.insertQuery(
                SearchQueryEntity(
                    query = trimmed,
                    timestamp = System.currentTimeMillis(),
                    isDeepThinking = isDeepThinking
                )
            )
        }
    }

    suspend fun deleteQuery(query: String) {
        dao.deleteQuery(query)
    }

    suspend fun clearAll() {
        dao.clearAllQueries()
    }
}
