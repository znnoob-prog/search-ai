package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.CalendarContract
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ChatMessage
import com.example.data.repository.SearchAiRepository
import com.example.ui.theme.GeminiAuroraEnd
import com.example.ui.theme.GeminiAuroraMiddle
import com.example.ui.theme.GeminiAuroraStart
import com.example.ui.theme.GeminiPurple
import com.example.ui.theme.GoogleBlue
import com.example.ui.theme.GoogleGreen
import com.example.ui.theme.GoogleRed
import com.example.ui.theme.GoogleYellow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiAssistantSheet(
    isOpen: Boolean,
    onDismiss: () -> Unit,
    messages: List<ChatMessage>,
    isGenerating: Boolean,
    selectedModel: String,
    onSwitchModel: (String) -> Unit,
    isThinkingEnabled: Boolean,
    onToggleThinking: () -> Unit,
    onSendMessage: (String) -> Unit,
    onClearChat: () -> Unit,
    currentContext: String?,
    modifier: Modifier = Modifier
) {
    if (!isOpen) return

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val listState = rememberLazyListState()
    val context = LocalContext.current
    var inputQuery by remember { mutableStateOf("") }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = null,
        modifier = modifier
            .fillMaxHeight(0.92f)
            .testTag("ai_assistant_modal_sheet"),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .imePadding()
        ) {
            // Sheet Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(GeminiAuroraStart, GeminiAuroraMiddle, GeminiAuroraEnd)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = "Google Search Assistant",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (isThinkingEnabled) "Powered by Gemini 4 Carbon (High Thinking)" else "Powered by Gemini 4",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isThinkingEnabled) GeminiPurple else GoogleBlue,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onClearChat,
                        modifier = Modifier.size(36.dp).testTag("clear_chat_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear Chat",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(36.dp).testTag("close_assistant_sheet")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            // Model Switcher Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedModel == SearchAiRepository.MODEL_HYDROGEN && !isThinkingEnabled,
                    onClick = { onSwitchModel(SearchAiRepository.MODEL_HYDROGEN) },
                    label = { Text("Hydrogen (Search)") },
                    leadingIcon = {
                        Icon(Icons.Default.AutoAwesome, null, modifier = Modifier.size(14.dp))
                    },
                    modifier = Modifier.testTag("model_chip_hydrogen")
                )

                FilterChip(
                    selected = selectedModel == SearchAiRepository.MODEL_CARBON || isThinkingEnabled,
                    onClick = { onToggleThinking() },
                    label = { Text("Carbon (Deep Thinking)") },
                    leadingIcon = {
                        Icon(Icons.Default.Psychology, null, modifier = Modifier.size(14.dp))
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = GeminiPurple.copy(alpha = 0.2f),
                        selectedLabelColor = GeminiPurple
                    ),
                    modifier = Modifier.testTag("model_chip_carbon")
                )

                FilterChip(
                    selected = selectedModel == SearchAiRepository.MODEL_FLASH_LITE,
                    onClick = { onSwitchModel(SearchAiRepository.MODEL_FLASH_LITE) },
                    label = { Text("Flash-Lite (Fast)") },
                    leadingIcon = {
                        Icon(Icons.Default.Speed, null, modifier = Modifier.size(14.dp))
                    },
                    modifier = Modifier.testTag("model_chip_lite")
                )

                FilterChip(
                    selected = selectedModel == SearchAiRepository.MODEL_NEON,
                    onClick = { onSwitchModel(SearchAiRepository.MODEL_NEON) },
                    label = { Text("Neon (Interactive)") },
                    modifier = Modifier.testTag("model_chip_neon")
                )
            }

            // Quick task shortcuts row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onSendMessage("Can you clarify the key points and answer common questions about this?") }
                        .testTag("action_clarify")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.HelpOutline, null, tint = GoogleBlue, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Clarify findings", style = MaterialTheme.typography.labelSmall)
                    }
                }

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onSendMessage("Draft a concise professional email summarizing these search results and next steps.") }
                        .testTag("action_draft_email")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Email, null, tint = GoogleRed, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Draft Email", style = MaterialTheme.typography.labelSmall)
                    }
                }

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onSendMessage("Extract actionable reminders and schedule recommendations from these results.") }
                        .testTag("action_set_reminder")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Alarm, null, tint = GoogleGreen, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Set Reminder", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            // Chat messages thread
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                items(messages, key = { it.id }) { message ->
                    ChatMessageBubble(
                        message = message,
                        onCopy = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("Assistant Response", message.text))
                            Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                        },
                        onShare = {
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, message.text)
                            }
                            context.startActivity(Intent.createChooser(intent, "Share response"))
                        },
                        onCreateCalendarReminder = {
                            try {
                                val calIntent = Intent(Intent.ACTION_INSERT).apply {
                                    data = CalendarContract.Events.CONTENT_URI
                                    putExtra(CalendarContract.Events.TITLE, "Google Search Task Reminder")
                                    putExtra(CalendarContract.Events.DESCRIPTION, message.text.take(200))
                                }
                                context.startActivity(calIntent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "Cannot open calendar", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }

            // Bottom Input Row
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = inputQuery,
                        onValueChange = { inputQuery = it },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("assistant_input_field"),
                        placeholder = {
                            Text(
                                "Ask a clarifying question or task...",
                                fontSize = 14.sp
                            )
                        },
                        shape = RoundedCornerShape(24.dp),
                        singleLine = false,
                        maxLines = 4,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                        ),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(
                            onSend = {
                                if (inputQuery.isNotBlank() && !isGenerating) {
                                    onSendMessage(inputQuery)
                                    inputQuery = ""
                                }
                            }
                        )
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Surface(
                        shape = CircleShape,
                        color = if (inputQuery.isNotBlank() && !isGenerating) GoogleBlue else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .clickable(enabled = inputQuery.isNotBlank() && !isGenerating) {
                                onSendMessage(inputQuery)
                                inputQuery = ""
                            }
                            .testTag("send_assistant_message_button"),
                        tonalElevation = 2.dp
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (isGenerating) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                    contentDescription = "Send",
                                    tint = if (inputQuery.isNotBlank()) Color.White else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChatMessageBubble(
    message: ChatMessage,
    onCopy: () -> Unit,
    onShare: () -> Unit,
    onCreateCalendarReminder: () -> Unit,
    modifier: Modifier = Modifier
) {
    var thoughtsExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag(if (message.isUser) "user_message_bubble" else "assistant_message_bubble"),
        horizontalAlignment = if (message.isUser) Alignment.End else Alignment.Start
    ) {
        if (message.isUser) {
            Surface(
                shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 4.dp),
                color = GoogleBlue,
                modifier = Modifier.fillMaxWidth(0.85f)
            ) {
                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                )
            }
        } else {
            Surface(
                shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 4.dp, bottomEnd = 18.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                modifier = Modifier.fillMaxWidth(0.92f)
            ) {
                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                    // Header with avatar & model badge
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = GeminiPurple,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Assistant",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        if (!message.modelUsed.isNullOrBlank()) {
                            Text(
                                text = message.modelUsed,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                fontSize = 10.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    if (!message.thoughts.isNullOrBlank()) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { thoughtsExpanded = !thoughtsExpanded }
                                .padding(vertical = 4.dp)
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "Deep Reasoning Thought Process",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = GeminiPurple
                                    )
                                    Icon(
                                        imageVector = if (thoughtsExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                AnimatedVisibility(visible = thoughtsExpanded) {
                                    Text(
                                        text = message.thoughts,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                    }

                    if (message.isStreaming && message.text.isBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                strokeWidth = 1.5.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Generating answer...",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        Text(
                            text = message.text,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            lineHeight = 21.sp
                        )
                    }

                    // Bottom actions on assistant message: copy, share, calendar reminder
                    if (message.text.isNotBlank() && !message.isStreaming) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = onCopy, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Default.ContentCopy, "Copy", modifier = Modifier.size(15.dp))
                            }
                            IconButton(onClick = onShare, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Default.Share, "Share", modifier = Modifier.size(15.dp))
                            }
                            // If looks like an action/reminder/task:
                            if (message.text.contains("reminder", ignoreCase = true) || message.text.contains("task", ignoreCase = true) || message.text.contains("schedule", ignoreCase = true)) {
                                OutlinedButton(
                                    onClick = onCreateCalendarReminder,
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Icon(Icons.Default.Alarm, null, modifier = Modifier.size(13.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Add to Calendar", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
