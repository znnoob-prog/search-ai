package com.example.data.repository

import android.graphics.Bitmap
import android.util.Log
import com.example.data.model.AiOverviewData
import com.example.data.model.GroundedSource
import com.example.data.model.SearchResultItem
import com.google.firebase.Firebase
import com.google.firebase.ai.Chat
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.ThinkingLevel
import com.google.firebase.ai.type.Tool
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import com.google.firebase.ai.type.thinkingConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

private const val TAG = "SearchAiRepository"

class SearchAiRepository {

    // Model names as specified in guidelines & requirements
    companion object {
        const val MODEL_HYDROGEN = "gemini-4-hydrogen"       // General search & grounding
        const val MODEL_CARBON = "gemini-4-carbon"           // Deep reasoning & high thinking
        const val MODEL_FLASH_LITE = "gemini-3.1-flash-lite" // Ultra-fast queries
        const val MODEL_NEON = "gemini-4-neon"               // Complex interactive tasks
    }

    // 1. Grounded Search AI Model (gemini-4-hydrogen with Google Search Tool)
    private val groundedSearchModel by lazy {
        Firebase.ai.generativeModel(
            modelName = MODEL_HYDROGEN,
            tools = listOf(Tool.googleSearch()),
            systemInstruction = content {
                text(
                    "You are Google Search AI Overview. Provide a clear, comprehensive, and up-to-date AI synthesis " +
                    "for the user's search query. Highlight direct answers, key takeaways, and essential context with " +
                    "clean markdown (bullet points, bold text). Maintain an objective, accurate, and authoritative tone."
                )
            }
        )
    }

    // 2. Deep Reasoning Model (gemini-4-carbon with ThinkingLevel.HIGH, no maxOutputTokens)
    private val deepThinkingModel by lazy {
        val config = generationConfig {
            thinkingConfig = thinkingConfig {
                thinkingLevel = ThinkingLevel.HIGH
                includeThoughts = true
            }
        }
        Firebase.ai.generativeModel(
            modelName = MODEL_CARBON,
            generationConfig = config,
            tools = listOf(Tool.googleSearch()),
            systemInstruction = content {
                text(
                    "You are Google Search Deep Think Engine. You specialize in deep reasoning, multi-faceted analysis, " +
                    "and solving complex queries. Provide rigorous, structured answers with exhaustive rationale, " +
                    "evidence from search grounding, and logical clarity."
                )
            }
        )
    }

    // 3. Fast Suggestion Model (gemini-3.1-flash-lite)
    private val fastModel by lazy {
        Firebase.ai.generativeModel(
            modelName = MODEL_FLASH_LITE,
            systemInstruction = content {
                text("You provide ultra-fast, concise answers and related query suggestions for search.")
            }
        )
    }

    // 4. Lens / Visual Search Model
    private val visualSearchModel by lazy {
        Firebase.ai.generativeModel(
            modelName = MODEL_HYDROGEN,
            tools = listOf(Tool.googleSearch()),
            systemInstruction = content {
                text(
                    "You are Google Lens AI. Analyze the image provided, identify what it is, and provide key facts, " +
                    "context, and helpful search insights about the subject in the photo."
                )
            }
        )
    }

    // Chat sessions for the Assistant
    private var activeChatSession: Chat? = null
    private var activeChatModelName: String = MODEL_HYDROGEN

    /**
     * Executes grounded Google Search with Gemini AI Overview
     */
    suspend fun getSearchOverview(query: String, isDeepThinking: Boolean = false): AiOverviewData = withContext(Dispatchers.IO) {
        val targetModel = if (isDeepThinking) deepThinkingModel else groundedSearchModel
        val modelUsed = if (isDeepThinking) MODEL_CARBON else MODEL_HYDROGEN

        try {
            val response = withTimeout(60_000L) {
                targetModel.generateContent(query)
            }

            val text = response.text.orEmpty().ifBlank {
                "No overview available for this search."
            }

            val thoughtSummary = response.thoughtSummary

            // Extract Grounding metadata
            val sources = mutableListOf<GroundedSource>()
            val relatedQueries = mutableListOf<String>()

            val candidate = response.candidates.firstOrNull()
            candidate?.groundingMetadata?.let { gm ->
                gm.groundingChunks.forEach { chunk ->
                    chunk.web?.let { web ->
                        val uri = web.uri.orEmpty()
                        val rawDomain = web.domain.orEmpty()
                        val domain = rawDomain.ifBlank { extractDomain(uri) }
                        val rawTitle = web.title.orEmpty()
                        val title = rawTitle.ifBlank { domain }
                        if (uri.isNotBlank() && sources.none { it.uri == uri }) {
                            sources.add(
                                GroundedSource(
                                    title = title,
                                    uri = uri,
                                    domain = domain
                                )
                            )
                        }
                    }
                }
                relatedQueries.addAll(gm.webSearchQueries)
            }

            AiOverviewData(
                query = query,
                overviewMarkdown = text,
                thoughtSummary = thoughtSummary,
                groundedSources = sources,
                relatedQueries = relatedQueries,
                isDeepThinking = isDeepThinking,
                modelUsed = modelUsed
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Search overview generation failed for query: $query", e)
            throw e
        }
    }

    /**
     * Analyze image with Visual Search (Google Lens)
     */
    suspend fun searchWithImage(bitmap: Bitmap, prompt: String = "What is this and what are key facts about it?"): AiOverviewData = withContext(Dispatchers.IO) {
        try {
            val response = withTimeout(60_000L) {
                visualSearchModel.generateContent(
                    content {
                        image(bitmap)
                        text(prompt)
                    }
                )
            }

            val text = response.text.orEmpty().ifBlank {
                "Visual search completed with no details."
            }

            val sources = mutableListOf<GroundedSource>()
            response.candidates.firstOrNull()?.groundingMetadata?.groundingChunks?.forEach { chunk ->
                chunk.web?.let { web ->
                    val uri = web.uri.orEmpty()
                    val rawDomain = web.domain.orEmpty()
                    val domain = rawDomain.ifBlank { extractDomain(uri) }
                    val rawTitle = web.title.orEmpty()
                    val title = rawTitle.ifBlank { domain }
                    if (uri.isNotBlank() && sources.none { it.uri == uri }) {
                        sources.add(
                            GroundedSource(
                                title = title,
                                uri = uri,
                                domain = domain
                            )
                        )
                    }
                }
            }

            AiOverviewData(
                query = "Visual Search: Image Analysis",
                overviewMarkdown = text,
                thoughtSummary = response.thoughtSummary,
                groundedSources = sources,
                relatedQueries = listOf("Similar images", "Learn more about this", "Where to buy"),
                isDeepThinking = false,
                modelUsed = MODEL_HYDROGEN
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Visual search failed", e)
            throw e
        }
    }

    /**
     * Starts or resets the Assistant Chat session with specified model
     */
    fun startAssistantChat(modelName: String = MODEL_HYDROGEN, enableThinking: Boolean = false): Chat {
        val model = when {
            enableThinking || modelName == MODEL_CARBON -> {
                val config = generationConfig {
                    thinkingConfig = thinkingConfig {
                        thinkingLevel = ThinkingLevel.HIGH
                        includeThoughts = true
                    }
                }
                Firebase.ai.generativeModel(
                    modelName = MODEL_CARBON,
                    generationConfig = config,
                    systemInstruction = content {
                        text(
                            "You are the Google Search AI Assistant with Deep Reasoning enabled. " +
                            "Think step-by-step and deliver rigorous, helpful, and highly insightful responses."
                        )
                    }
                )
            }
            modelName == MODEL_FLASH_LITE -> Firebase.ai.generativeModel(
                modelName = MODEL_FLASH_LITE,
                systemInstruction = content {
                    text("You are the Google Search Assistant, providing fast and concise answers.")
                }
            )
            modelName == MODEL_NEON -> Firebase.ai.generativeModel(
                modelName = MODEL_NEON,
                systemInstruction = content {
                    text("You are the Google Search Assistant, specializing in interactive, creative, and dynamic problem solving.")
                }
            )
            else -> Firebase.ai.generativeModel(
                modelName = MODEL_HYDROGEN,
                tools = listOf(Tool.googleSearch()),
                systemInstruction = content {
                    text(
                        "You are the Google Search AI Assistant. You help users explore topics, find accurate information, " +
                        "answer follow-up queries, compare items, and solve everyday problems using Google Search knowledge."
                    )
                }
            )
        }

        activeChatModelName = modelName
        activeChatSession = model.startChat()
        return activeChatSession!!
    }

    /**
     * Sends message to Assistant Chat and streams response chunks
     */
    fun sendChatMessageStream(userPrompt: String): Flow<String> = flow {
        if (activeChatSession == null) {
            startAssistantChat(MODEL_HYDROGEN)
        }
        val chat = activeChatSession!!

        withTimeout(60_000L) {
            chat.sendMessageStream(userPrompt).collect { chunk ->
                chunk.text?.let { emit(it) }
            }
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Generates fast related search questions
     */
    suspend fun getRelatedQuestions(query: String): List<String> = withContext(Dispatchers.IO) {
        try {
            val prompt = "Based on the search query \"$query\", list 4 popular follow-up search questions users ask. Return one question per line, no numbering, no preamble."
            val response = withTimeout(15_000L) {
                fastModel.generateContent(prompt)
            }
            response.text?.lines()
                ?.map { it.trim().removePrefix("-").removePrefix("•").removePrefix("1.").removePrefix("2.").removePrefix("3.").removePrefix("4.").trim() }
                ?.filter { it.isNotBlank() }
                ?.take(4) ?: emptyList()
        } catch (e: Exception) {
            listOf(
                "What are the benefits of $query?",
                "How does $query compare to alternatives?",
                "Latest updates on $query",
                "History and future of $query"
            )
        }
    }

    private fun extractDomain(uri: String): String {
        return try {
            val clean = uri.removePrefix("https://").removePrefix("http://")
            clean.substringBefore("/").removePrefix("www.")
        } catch (e: Exception) {
            "web"
        }
    }
}
