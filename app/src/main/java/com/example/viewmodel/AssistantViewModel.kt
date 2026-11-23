package com.example.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.ChatMessage
import com.example.data.repository.SearchAiRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private const val TAG = "AssistantViewModel"

class AssistantViewModel(application: Application) : AndroidViewModel(application) {

    private val searchAiRepository = SearchAiRepository()

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val _selectedModel = MutableStateFlow(SearchAiRepository.MODEL_HYDROGEN)
    val selectedModel: StateFlow<String> = _selectedModel.asStateFlow()

    private val _isThinkingEnabled = MutableStateFlow(false)
    val isThinkingEnabled: StateFlow<Boolean> = _isThinkingEnabled.asStateFlow()

    private val _currentContext = MutableStateFlow<String?>(null)
    val currentContext: StateFlow<String?> = _currentContext.asStateFlow()

    init {
        initSession()
    }

    private fun initSession() {
        searchAiRepository.startAssistantChat(
            modelName = _selectedModel.value,
            enableThinking = _isThinkingEnabled.value
        )
        if (_messages.value.isEmpty()) {
            _messages.value = listOf(
                ChatMessage(
                    isUser = false,
                    text = "Hi! I'm your Google Search AI Assistant. Ask me to clarify search results, dive deeper into any topic, draft an email, or organize reminders based on your findings.",
                    modelUsed = _selectedModel.value
                )
            )
        }
    }

    fun setContext(searchQuery: String, overviewSnippet: String) {
        _currentContext.value = "Search Query: \"$searchQuery\"\nAI Overview Summary: $overviewSnippet"
    }

    fun switchModel(modelName: String) {
        if (_selectedModel.value == modelName) return
        _selectedModel.value = modelName
        _isThinkingEnabled.value = (modelName == SearchAiRepository.MODEL_CARBON)
        searchAiRepository.startAssistantChat(modelName, enableThinking = _isThinkingEnabled.value)
    }

    fun toggleThinking() {
        val next = !_isThinkingEnabled.value
        _isThinkingEnabled.value = next
        if (next) {
            _selectedModel.value = SearchAiRepository.MODEL_CARBON
        }
        searchAiRepository.startAssistantChat(_selectedModel.value, enableThinking = next)
    }

    fun sendMessage(userText: String) {
        val prompt = userText.trim()
        if (prompt.isBlank() || _isGenerating.value) return

        val userMessage = ChatMessage(
            isUser = true,
            text = prompt
        )

        val assistantMessageId = java.util.UUID.randomUUID().toString()
        val initialAssistantMessage = ChatMessage(
            id = assistantMessageId,
            isUser = false,
            text = "",
            isStreaming = true,
            modelUsed = _selectedModel.value
        )

        _messages.value = _messages.value + userMessage + initialAssistantMessage
        _isGenerating.value = true

        viewModelScope.launch {
            val fullPrompt = if (_currentContext.value != null && _messages.value.size <= 3) {
                "${_currentContext.value}\n\nUser Question: $prompt"
            } else {
                prompt
            }

            val accumulatedText = StringBuilder()
            try {
                searchAiRepository.sendChatMessageStream(fullPrompt).collect { chunk ->
                    accumulatedText.append(chunk)
                    _messages.value = _messages.value.map { msg ->
                        if (msg.id == assistantMessageId) {
                            msg.copy(text = accumulatedText.toString(), isStreaming = true)
                        } else msg
                    }
                }

                _messages.value = _messages.value.map { msg ->
                    if (msg.id == assistantMessageId) {
                        msg.copy(
                            text = if (accumulatedText.isBlank()) "No response received." else accumulatedText.toString(),
                            isStreaming = false
                        )
                    } else msg
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Chat generation error", e)
                _messages.value = _messages.value.map { msg ->
                    if (msg.id == assistantMessageId) {
                        msg.copy(
                            text = "Error: ${e.localizedMessage ?: "Failed to generate response."}",
                            isStreaming = false
                        )
                    } else msg
                }
            } finally {
                _isGenerating.value = false
            }
        }
    }

    fun askAboutSearch() {
        val ctx = _currentContext.value
        if (ctx != null) {
            sendMessage("Can you break down the key takeaways and explain this simply?")
        } else {
            sendMessage("What are the most important things I should know about my recent search?")
        }
    }

    fun draftEmail() {
        sendMessage("Draft a professional email summarizing the key findings and recommendations from these search results.")
    }

    fun setReminderTask() {
        sendMessage("Extract actionable follow-up tasks and a reminder summary from these search results.")
    }

    fun clearChat() {
        _messages.value = emptyList()
        initSession()
    }
}
