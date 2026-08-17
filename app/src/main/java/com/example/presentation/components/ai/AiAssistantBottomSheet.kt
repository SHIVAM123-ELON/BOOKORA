package com.example.presentation.components.ai

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.model.AiChatMessage
import com.example.domain.model.ExplanationMode
import com.example.domain.model.MessageRole
import com.example.presentation.viewmodel.AiReadingAssistantViewModel
import com.example.ui.theme.PolishAccentOrange
import com.example.ui.theme.PolishBackground
import com.example.ui.theme.PolishPrimaryIndigo
import com.example.ui.theme.PolishPrimaryLight
import com.example.ui.theme.PolishSlate100
import com.example.ui.theme.PolishSlate200
import com.example.ui.theme.PolishSlate400
import com.example.ui.theme.PolishSlate500
import com.example.ui.theme.PolishSlate700
import com.example.ui.theme.PolishSlate900

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiAssistantBottomSheet(
    userId: String,
    bookId: String,
    bookTitle: String,
    chapterTitle: String?,
    viewModel: AiReadingAssistantViewModel,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val history by viewModel.getConversationHistory(userId, bookId).collectAsStateWithLifecycle()
    val explanationMode by viewModel.explanationMode.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    val contextSnippet by viewModel.contextSnippet.collectAsStateWithLifecycle()

    var inputText by remember { mutableStateOf("") }
    var showModeSelector by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()

    LaunchedEffect(history.size) {
        if (history.isNotEmpty()) {
            listState.animateScrollToItem(history.size - 1)
        }
    }

    val suggestedPrompts = listOf(
        "Explain this",
        "Summarize this section",
        "Give me key points",
        "Create questions",
        "Explain simply",
        "What should I remember?"
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.88f)
                .padding(bottom = 16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = PolishPrimaryLight,
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = PolishPrimaryIndigo,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Column {
                        Text(
                            text = "BOOKORA AI Assistant",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = PolishSlate900
                        )
                        Text(
                            text = chapterTitle ?: bookTitle,
                            style = MaterialTheme.typography.labelSmall,
                            color = PolishSlate500,
                            maxLines = 1
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { showModeSelector = !showModeSelector },
                        modifier = Modifier.testTag("ai_mode_selector_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Select Mode",
                            tint = PolishPrimaryIndigo
                        )
                    }

                    IconButton(
                        onClick = { viewModel.clearChat(userId, bookId) },
                        modifier = Modifier.testTag("clear_chat_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Clear History",
                            tint = PolishSlate400
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = PolishSlate500
                        )
                    }
                }
            }

            // Explanation Mode Selector Bar
            AnimatedVisibility(visible = showModeSelector) {
                Surface(
                    color = PolishBackground,
                    border = BorderStroke(1.dp, PolishSlate100),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)) {
                        Text(
                            text = "Explanation Mode",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = PolishSlate700
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ExplanationMode.values().forEach { mode ->
                                FilterChip(
                                    selected = explanationMode == mode,
                                    onClick = {
                                        viewModel.setExplanationMode(mode)
                                        showModeSelector = false
                                    },
                                    label = {
                                        Text(
                                            text = mode.label,
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = PolishPrimaryIndigo,
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // Active Mode Chip Tag
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = PolishPrimaryLight
                ) {
                    Text(
                        text = "Mode: ${explanationMode.label}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = PolishPrimaryIndigo
                        ),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                if (contextSnippet != null) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFFEF3C7)
                    ) {
                        Text(
                            text = "Context Selected",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = PolishAccentOrange
                            ),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Chat Messages List
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            ) {
                if (history.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "How can I help you with this book?",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = PolishSlate900
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Ask questions, explore concepts, or get instant summaries.",
                            style = MaterialTheme.typography.bodySmall,
                            color = PolishSlate500
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        Text(
                            text = "Suggested Questions",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = PolishSlate400
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        suggestedPrompts.forEach { prompt ->
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = PolishBackground,
                                border = BorderStroke(1.dp, PolishSlate200),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable {
                                        viewModel.sendMessage(userId, bookId, prompt)
                                    }
                            ) {
                                Text(
                                    text = prompt,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                    color = PolishSlate700,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                                )
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(history) { message ->
                            ChatMessageBubble(
                                message = message,
                                onFollowUpClick = { followUp ->
                                    viewModel.sendMessage(userId, bookId, followUp)
                                }
                            )
                        }

                        if (isLoading) {
                            item {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        color = PolishPrimaryIndigo,
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "BOOKORA AI is thinking...",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = PolishSlate500
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Error notice
            if (errorMessage != null) {
                Surface(
                    color = Color(0xFFFEE2E2),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = errorMessage ?: "",
                        style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFDC2626)),
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }

            // Bottom Prompt Input Bar
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                shape = RoundedCornerShape(20.dp),
                color = PolishBackground,
                border = BorderStroke(1.dp, PolishSlate200)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        placeholder = { Text("Ask anything about this book...", color = PolishSlate400, fontSize = 14.sp) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("ai_assistant_input"),
                        singleLine = false,
                        maxLines = 3,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent
                        )
                    )

                    IconButton(
                        onClick = {
                            if (inputText.isNotBlank()) {
                                val text = inputText
                                inputText = ""
                                viewModel.sendMessage(userId, bookId, text)
                            }
                        },
                        enabled = inputText.isNotBlank() && !isLoading,
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(if (inputText.isNotBlank()) PolishPrimaryIndigo else PolishSlate200)
                            .testTag("ai_assistant_send_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "Send",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ChatMessageBubble(
    message: AiChatMessage,
    onFollowUpClick: (String) -> Unit
) {
    val isUser = message.role == MessageRole.USER

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 18.dp,
                topEnd = 18.dp,
                bottomStart = if (isUser) 18.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 18.dp
            ),
            color = if (isUser) PolishPrimaryIndigo else PolishBackground,
            border = if (isUser) null else BorderStroke(1.dp, PolishSlate200),
            modifier = Modifier.fillMaxWidth(0.92f)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                if (!isUser) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = PolishPrimaryIndigo,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "BOOKORA AI",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = PolishPrimaryIndigo
                            )
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                }

                Text(
                    text = message.content,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = if (isUser) Color.White else PolishSlate900,
                        lineHeight = 22.sp
                    )
                )
            }
        }

        // Suggested Follow-up chips
        if (!isUser && message.suggestedFollowUps.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Column(
                modifier = Modifier.fillMaxWidth(0.92f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                message.suggestedFollowUps.forEach { followUp ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = PolishPrimaryLight.copy(alpha = 0.6f),
                        border = BorderStroke(1.dp, PolishPrimaryIndigo.copy(alpha = 0.2f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onFollowUpClick(followUp) }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "💬 $followUp",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = PolishPrimaryIndigo
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}
