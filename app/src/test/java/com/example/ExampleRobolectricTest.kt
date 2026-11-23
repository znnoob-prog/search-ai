package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.SearchDatabase
import com.example.data.local.SearchQueryEntity
import com.example.data.model.SearchResultItem
import com.example.data.repository.LocalHistoryRepository
import com.example.data.repository.RoomSearchRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    private lateinit var db: SearchDatabase

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, SearchDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Search AI", appName)
    }

    @Test
    fun `test Room database insert and query`() = runBlocking {
        val dao = db.searchQueryDao()
        val repo = RoomSearchRepository(dao)

        repo.saveQuery("James Webb space discoveries", isDeepThinking = false)
        repo.saveQuery("Quantum computing algorithms", isDeepThinking = true)

        val queries = repo.recentQueries.first()
        assertEquals(2, queries.size)
        assertEquals("Quantum computing algorithms", queries[0].query)
        assertTrue(queries[0].isDeepThinking)

        repo.deleteQuery("James Webb space discoveries")
        val remaining = repo.recentQueries.first()
        assertEquals(1, remaining.size)
        assertEquals("Quantum computing algorithms", remaining[0].query)

        repo.clearAll()
        val emptyList = repo.recentQueries.first()
        assertTrue(emptyList.isEmpty())
    }

    @Test
    fun `test search history persistence and bookmark toggle`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = LocalHistoryRepository(context)

        repo.clearHistory()
        repo.addQuery("Quantum computing")
        val history = repo.getRecentQueries()
        assertTrue(history.contains("Quantum computing"))

        val testItem = SearchResultItem(
            id = "test_1",
            title = "Test Item",
            snippet = "Snippet",
            url = "https://example.com/test",
            displayDomain = "example.com"
        )

        val isBookmarked = repo.toggleBookmark(testItem)
        assertTrue(isBookmarked)
        assertTrue(repo.isBookmarked("https://example.com/test"))
    }
}
