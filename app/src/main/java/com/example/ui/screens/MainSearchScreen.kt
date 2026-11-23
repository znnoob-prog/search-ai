package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FabPosition
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.SearchFilter
import com.example.ui.components.AccountDialog
import com.example.ui.components.AiAssistantSheet
import com.example.ui.components.AiOverviewCard
import com.example.ui.components.BookmarksDialog
import com.example.ui.components.FilterChipsRow
import com.example.ui.components.GoogleHeader
import com.example.ui.components.SearchBarSection
import com.example.ui.components.SearchResultCard
import com.example.ui.components.TrendingDiscoverFeed
import com.example.ui.theme.GeminiPurple
import com.example.ui.theme.ThemeMode
import com.example.viewmodel.AiOverviewUiState
import com.example.viewmodel.AssistantViewModel
import com.example.viewmodel.SearchViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth

@Composable
fun MainSearchScreen(
    searchViewModel: SearchViewModel,
    assistantViewModel: AssistantViewModel,
    currentThemeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    modifier: Modifier = Modifier
) {
    val query by searchViewModel.query.collectAsState()
    val submittedQuery by searchViewModel.submittedQuery.collectAsState()
    val activeFilter by searchViewModel.activeFilter.collectAsState()
    val isDeepThinking by searchViewModel.isDeepThinking.collectAsState()
    val aiOverviewState by searchViewModel.aiOverviewState.collectAsState()
    val searchResults by searchViewModel.searchResults.collectAsState()
    val bookmarks by searchViewModel.bookmarks.collectAsState()

    // 1. Local Room Database queries (reactive StateFlow)
    val roomRecentQueries by searchViewModel.roomRecentQueries.collectAsState()

    // 2. Cloud Firestore search history (reactive StateFlow)
    val cloudSearchHistory by searchViewModel.cloudSearchHistory.collectAsState()

    val assistantMessages by assistantViewModel.messages.collectAsState()
    val isAssistantGenerating by assistantViewModel.isGenerating.collectAsState()
    val selectedAssistantModel by assistantViewModel.selectedModel.collectAsState()
    val isAssistantThinking by assistantViewModel.isThinkingEnabled.collectAsState()
    val currentAssistantContext by assistantViewModel.currentContext.collectAsState()

    // Authentication state
    var currentUser by remember { mutableStateOf<FirebaseUser?>(Firebase.auth.currentUser) }
    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose { Firebase.auth.removeAuthStateListener(listener) }
    }

    var isAssistantOpen by remember { mutableStateOf(false) }
    var isBookmarksOpen by remember { mutableStateOf(false) }
    var isAccountOpen by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize().testTag("main_search_screen"),
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { isAssistantOpen = true },
                icon = {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "AI Assistant",
                        tint = Color.White
                    )
                },
                text = {
                    Text(
                        text = "AI Assistant",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                },
                containerColor = GeminiPurple,
                modifier = Modifier.testTag("open_assistant_fab")
            )
        },
        floatingActionButtonPosition = FabPosition.End
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Top Bar: Google Logo + Theme Toggle + Saved Items + Google Account Profile
            GoogleHeader(
                currentThemeMode = currentThemeMode,
                onThemeModeChange = onThemeModeChange,
                bookmarkCount = bookmarks.size,
                onOpenBookmarks = { isBookmarksOpen = true },
                onOpenAssistant = { isAssistantOpen = true },
                currentUser = currentUser,
                onOpenAccount = { isAccountOpen = true }
            )

            // Search Bar Section with Local Room Quick Access & Dropdown Suggestions
            SearchBarSection(
                query = query,
                onQueryChange = { searchViewModel.onQueryChange(it) },
                onSearch = { searchViewModel.performSearch(it) },
                roomRecentQueries = roomRecentQueries,
                onRemoveRoomQuery = { searchViewModel.removeRoomQuery(it) },
                onClearAllRoomQueries = { searchViewModel.clearAllRoomHistory() },
                isDeepThinking = isDeepThinking,
                onToggleDeepThinking = { searchViewModel.toggleDeepThinking() },
                onVisualSearch = { searchViewModel.performVisualSearch(it) }
            )

            // Filter Chips Row (All, AI Overview, News, Images, Web, Deep Think)
            FilterChipsRow(
                selectedFilter = activeFilter,
                onFilterSelected = { searchViewModel.setFilter(it) },
                isDeepThinking = isDeepThinking
            )

            // Main Content Area: Discover feed when idle vs. Results when submitted
            if (submittedQuery.isBlank()) {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    item {
                        TrendingDiscoverFeed(
                            roomRecentQueries = roomRecentQueries,
                            onQueryClick = { searchViewModel.performSearch(it) },
                            onRemoveRecentQuery = { searchViewModel.removeRoomQuery(it) },
                            onClearAllHistory = { searchViewModel.clearAllRoomHistory() },
                            onOpenAssistant = { isAssistantOpen = true }
                        )
                    }
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    // 1. AI Overview Section (at top of search results page)
                    if (activeFilter == SearchFilter.ALL || activeFilter == SearchFilter.AI_OVERVIEW || activeFilter == SearchFilter.DEEP_THINK) {
                        item {
                            AiOverviewCard(
                                uiState = aiOverviewState,
                                isDeepThinking = isDeepThinking,
                                onRetry = { searchViewModel.performSearch(submittedQuery) },
                                onAskAssistant = { q, overview ->
                                    assistantViewModel.setContext(q, overview)
                                    isAssistantOpen = true
                                },
                                onFollowUpQueryClick = { followUp ->
                                    searchViewModel.performSearch(followUp)
                                }
                            )
                        }
                    }

                    // 2. Regular Search Results section
                    if (searchResults.isNotEmpty() && activeFilter != SearchFilter.AI_OVERVIEW) {
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = when (activeFilter) {
                                        SearchFilter.NEWS -> "Top News Results"
                                        SearchFilter.IMAGES -> "Image & Visual Results"
                                        SearchFilter.WEB -> "Web Results"
                                        else -> "Web Results & Sources"
                                    },
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        val filteredResults = when (activeFilter) {
                            SearchFilter.NEWS -> searchResults.filter { it.category == "News" || it.displayDomain.contains("news") || it.displayDomain.contains("times") }
                            else -> searchResults
                        }.ifEmpty { searchResults }

                        items(filteredResults, key = { it.id }) { item ->
                            SearchResultCard(
                                item = item,
                                onToggleBookmark = { searchViewModel.toggleBookmark(it) }
                            )
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(72.dp))
                    }
                }
            }
        }

        // Dedicated AI Assistant Modal Sheet
        AiAssistantSheet(
            isOpen = isAssistantOpen,
            onDismiss = { isAssistantOpen = false },
            messages = assistantMessages,
            isGenerating = isAssistantGenerating,
            selectedModel = selectedAssistantModel,
            onSwitchModel = { assistantViewModel.switchModel(it) },
            isThinkingEnabled = isAssistantThinking,
            onToggleThinking = { assistantViewModel.toggleThinking() },
            onSendMessage = { assistantViewModel.sendMessage(it) },
            onClearChat = { assistantViewModel.clearChat() },
            currentContext = currentAssistantContext
        )

        // Bookmarks Modal Sheet
        BookmarksDialog(
            isOpen = isBookmarksOpen,
            onDismiss = { isBookmarksOpen = false },
            bookmarks = bookmarks,
            onToggleBookmark = { searchViewModel.toggleBookmark(it) }
        )

        // Google Account & Cloud History Modal Sheet
        AccountDialog(
            isOpen = isAccountOpen,
            onDismiss = { isAccountOpen = false },
            currentUser = currentUser,
            cloudHistory = cloudSearchHistory,
            onDeleteCloudItem = { searchViewModel.deleteCloudHistoryItem(it) },
            onClearAllCloudHistory = { searchViewModel.clearAllCloudHistory() },
            onQueryClick = { searchViewModel.performSearch(it) }
        )
    }
}
