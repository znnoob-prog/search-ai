package com.example.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.SearchDatabase
import com.example.data.local.SearchQueryEntity
import com.example.data.model.AiOverviewData
import com.example.data.model.SearchFilter
import com.example.data.model.SearchResultItem
import com.example.data.repository.CloudSearchHistoryItem
import com.example.data.repository.FirestoreHistoryRepository
import com.example.data.repository.LocalHistoryRepository
import com.example.data.repository.RoomSearchRepository
import com.example.data.repository.SearchAiRepository
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private const val TAG = "SearchViewModel"

sealed interface AiOverviewUiState {
    data object Idle : AiOverviewUiState
    data object Loading : AiOverviewUiState
    data class Success(val data: AiOverviewData) : AiOverviewUiState
    data class Error(val message: String) : AiOverviewUiState
}

class SearchViewModel(application: Application) : AndroidViewModel(application) {

    private val searchAiRepository = SearchAiRepository()
    private val localHistoryRepository = LocalHistoryRepository(application.applicationContext)
    
    // 1. Local Room Database Repository
    private val roomDb = SearchDatabase.getDatabase(application.applicationContext)
    private val roomSearchRepository = RoomSearchRepository(roomDb.searchQueryDao())

    // 2. Cloud Firestore History Repository
    private val firestoreHistoryRepository = FirestoreHistoryRepository(application.applicationContext)

    // Room reactive Flow of search queries
    val roomRecentQueries: StateFlow<List<SearchQueryEntity>> = roomSearchRepository.recentQueries
        .catch { e ->
            Log.e(TAG, "Room recent queries flow error", e)
            emit(emptyList())
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Cloud search history Flow (observed when user is authenticated)
    val cloudSearchHistory: StateFlow<List<CloudSearchHistoryItem>> = firestoreHistoryRepository.observeCloudHistory()
        .catch { e ->
            Log.e(TAG, "Cloud history flow error", e)
            emit(emptyList())
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _submittedQuery = MutableStateFlow("")
    val submittedQuery: StateFlow<String> = _submittedQuery.asStateFlow()

    private val _activeFilter = MutableStateFlow(SearchFilter.ALL)
    val activeFilter: StateFlow<SearchFilter> = _activeFilter.asStateFlow()

    private val _isDeepThinking = MutableStateFlow(false)
    val isDeepThinking: StateFlow<Boolean> = _isDeepThinking.asStateFlow()

    private val _aiOverviewState = MutableStateFlow<AiOverviewUiState>(AiOverviewUiState.Idle)
    val aiOverviewState: StateFlow<AiOverviewUiState> = _aiOverviewState.asStateFlow()

    private val _searchResults = MutableStateFlow<List<SearchResultItem>>(emptyList())
    val searchResults: StateFlow<List<SearchResultItem>> = _searchResults.asStateFlow()

    private val _bookmarks = MutableStateFlow<List<SearchResultItem>>(emptyList())
    val bookmarks: StateFlow<List<SearchResultItem>> = _bookmarks.asStateFlow()

    private val _relatedQuestions = MutableStateFlow<List<String>>(emptyList())
    val relatedQuestions: StateFlow<List<String>> = _relatedQuestions.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    init {
        loadBookmarks()
    }

    fun loadBookmarks() {
        _bookmarks.value = localHistoryRepository.getBookmarks()
    }

    fun onQueryChange(newQuery: String) {
        _query.value = newQuery
    }

    fun setFilter(filter: SearchFilter) {
        _activeFilter.value = filter
        if (filter == SearchFilter.DEEP_THINK) {
            _isDeepThinking.value = true
            if (_submittedQuery.value.isNotBlank()) {
                performSearch(_submittedQuery.value, forceDeepThinking = true)
            }
        } else if (_isDeepThinking.value && filter != SearchFilter.DEEP_THINK) {
            _isDeepThinking.value = false
        }
    }

    fun toggleDeepThinking() {
        val next = !_isDeepThinking.value
        _isDeepThinking.value = next
        if (next) {
            _activeFilter.value = SearchFilter.DEEP_THINK
        } else if (_activeFilter.value == SearchFilter.DEEP_THINK) {
            _activeFilter.value = SearchFilter.ALL
        }
        if (_submittedQuery.value.isNotBlank()) {
            performSearch(_submittedQuery.value, forceDeepThinking = next)
        }
    }

    fun performSearch(searchQuery: String, forceDeepThinking: Boolean? = null) {
        val q = searchQuery.trim()
        if (q.isBlank()) return

        _query.value = q
        _submittedQuery.value = q
        _isSearching.value = true

        val deepThink = forceDeepThinking ?: _isDeepThinking.value

        // 1. Store in Local Room Database
        viewModelScope.launch {
            try {
                roomSearchRepository.saveQuery(q, deepThink)
            } catch (e: Exception) {
                Log.e(TAG, "Error saving to Room database", e)
            }
        }

        // 2. Store in Cloud Firestore if user is authenticated
        viewModelScope.launch {
            try {
                if (Firebase.auth.currentUser != null) {
                    val model = if (deepThink) SearchAiRepository.MODEL_CARBON else SearchAiRepository.MODEL_HYDROGEN
                    firestoreHistoryRepository.saveQueryToCloud(q, deepThink, model)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error syncing to cloud Firestore", e)
            }
        }

        // 3. Perform AI grounded overview and build search results
        viewModelScope.launch {
            _aiOverviewState.value = AiOverviewUiState.Loading

            try {
                val overview = searchAiRepository.getSearchOverview(q, isDeepThinking = deepThink)
                _aiOverviewState.value = AiOverviewUiState.Success(overview)
                _searchResults.value = buildResultsFromOverview(q, overview)

                if (overview.relatedQueries.isNotEmpty()) {
                    _relatedQuestions.value = overview.relatedQueries
                } else {
                    _relatedQuestions.value = searchAiRepository.getRelatedQuestions(q)
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Search overview timed out", e)
                _aiOverviewState.value = AiOverviewUiState.Error(
                    "Overview took longer than 60s. Tap retry to run search again."
                )
                _searchResults.value = generateFallbackResults(q)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Search overview error", e)
                _aiOverviewState.value = AiOverviewUiState.Error(
                    e.localizedMessage ?: "Failed to generate AI overview."
                )
                _searchResults.value = generateFallbackResults(q)
            } finally {
                _isSearching.value = false
            }
        }
    }

    fun performVisualSearch(bitmap: Bitmap) {
        _isSearching.value = true
        _submittedQuery.value = "Visual Search (Google Lens)"
        _query.value = "Visual Search"

        viewModelScope.launch {
            _aiOverviewState.value = AiOverviewUiState.Loading
            try {
                val overview = searchAiRepository.searchWithImage(bitmap)
                _aiOverviewState.value = AiOverviewUiState.Success(overview)
                _searchResults.value = buildResultsFromOverview("Visual Search", overview)
                _relatedQuestions.value = overview.relatedQueries

                roomSearchRepository.saveQuery("Visual Search", false)
            } catch (e: Exception) {
                Log.e(TAG, "Visual search error", e)
                _aiOverviewState.value = AiOverviewUiState.Error(
                    e.localizedMessage ?: "Failed to complete visual search."
                )
            } finally {
                _isSearching.value = false
            }
        }
    }

    fun toggleBookmark(item: SearchResultItem) {
        val isNowBookmarked = localHistoryRepository.toggleBookmark(item)
        _searchResults.value = _searchResults.value.map {
            if (it.url == item.url || it.id == item.id) it.copy(isBookmarked = isNowBookmarked) else it
        }
        loadBookmarks()
    }

    fun removeRoomQuery(q: String) {
        viewModelScope.launch {
            try {
                roomSearchRepository.deleteQuery(q)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to remove Room query", e)
            }
        }
    }

    fun clearAllRoomHistory() {
        viewModelScope.launch {
            try {
                roomSearchRepository.clearAll()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to clear Room queries", e)
            }
        }
    }

    fun deleteCloudHistoryItem(id: String) {
        viewModelScope.launch {
            try {
                firestoreHistoryRepository.deleteCloudQuery(id)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to delete cloud item", e)
            }
        }
    }

    fun clearAllCloudHistory() {
        viewModelScope.launch {
            try {
                firestoreHistoryRepository.clearAllCloudHistory()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to clear cloud history", e)
            }
        }
    }

    fun resetSearch() {
        _query.value = ""
        _submittedQuery.value = ""
        _aiOverviewState.value = AiOverviewUiState.Idle
        _searchResults.value = emptyList()
        _isSearching.value = false
    }

    private fun buildResultsFromOverview(query: String, overview: AiOverviewData): List<SearchResultItem> {
        val list = mutableListOf<SearchResultItem>()

        overview.groundedSources.forEachIndexed { index, source ->
            list.add(
                SearchResultItem(
                    id = "grounded_$index",
                    title = source.title,
                    snippet = "Verified source on ${source.domain} covering ${overview.query}. Detailed coverage, references, and related analysis.",
                    url = source.uri,
                    displayDomain = source.domain,
                    date = "Recent",
                    category = if (source.domain.contains("news") || source.domain.contains("times") || source.domain.contains("post")) "News" else "Web",
                    isBookmarked = localHistoryRepository.isBookmarked(source.uri)
                )
            )
        }

        if (list.size < 4) {
            val domains = listOf(
                Pair("wikipedia.org", "https://en.wikipedia.org/wiki/${query.replace(" ", "_")}"),
                Pair("nature.com", "https://www.nature.com/search?q=${query.replace(" ", "+")}"),
                Pair("technologyreview.com", "https://www.technologyreview.com/search?q=${query.replace(" ", "+")}"),
                Pair("theverge.com", "https://www.theverge.com/search?q=${query.replace(" ", "+")}")
            )
            domains.forEachIndexed { idx, (domain, url) ->
                if (list.none { it.displayDomain == domain }) {
                    list.add(
                        SearchResultItem(
                            id = "supplemental_$idx",
                            title = "$query - In-depth overview & analysis on $domain",
                            snippet = "Explore comprehensive reports, historical background, benchmarks, and community discussions regarding $query.",
                            url = url,
                            displayDomain = domain,
                            date = "2026",
                            category = "Web",
                            isBookmarked = localHistoryRepository.isBookmarked(url)
                        )
                    )
                }
            }
        }

        return list
    }

    private fun generateFallbackResults(query: String): List<SearchResultItem> {
        return listOf(
            SearchResultItem(
                id = "fb_1",
                title = "$query - Official Information & Guide",
                snippet = "Comprehensive overview, latest documentation, and reliable reference information about $query.",
                url = "https://www.google.com/search?q=${query.replace(" ", "+")}",
                displayDomain = "google.com",
                date = "Recent",
                category = "Web",
                isBookmarked = false
            ),
            SearchResultItem(
                id = "fb_2",
                title = "Understanding $query in Detail",
                snippet = "Key fundamentals, architecture, comparisons, and expert explanations for $query.",
                url = "https://en.wikipedia.org/wiki/${query.replace(" ", "_")}",
                displayDomain = "wikipedia.org",
                date = "2026",
                category = "Web",
                isBookmarked = false
            )
        )
    }
}
