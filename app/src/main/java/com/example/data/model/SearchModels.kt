package com.example.data.model

data class SearchResultItem(
    val id: String,
    val title: String,
    val snippet: String,
    val url: String,
    val displayDomain: String,
    val date: String = "",
    val category: String = "Web",
    val imageUrl: String? = null,
    val isBookmarked: Boolean = false
)

data class GroundedSource(
    val title: String,
    val uri: String,
    val domain: String
)

data class AiOverviewData(
    val query: String,
    val overviewMarkdown: String,
    val thoughtSummary: String? = null,
    val groundedSources: List<GroundedSource> = emptyList(),
    val relatedQueries: List<String> = emptyList(),
    val isDeepThinking: Boolean = false,
    val modelUsed: String = "gemini-4-hydrogen"
)

enum class SearchFilter(val label: String) {
    ALL("All"),
    AI_OVERVIEW("AI Overview"),
    NEWS("News"),
    IMAGES("Images"),
    WEB("Web"),
    DEEP_THINK("Deep Think")
}

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val isUser: Boolean,
    val text: String,
    val thoughts: String? = null,
    val sources: List<GroundedSource> = emptyList(),
    val timestamp: Long = System.currentTimeMillis(),
    val modelUsed: String? = null,
    val isStreaming: Boolean = false
)
