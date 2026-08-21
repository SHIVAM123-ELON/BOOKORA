package com.example.presentation.screens.voice

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.model.voice.*
import com.example.presentation.components.voice.VoiceOrbVisualizer
import com.example.presentation.components.voice.VoicePersonaSelectorSheet
import com.example.presentation.viewmodel.voice.VoiceConversationViewModel
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceConversationScreen(
    viewModel: VoiceConversationViewModel,
    bookId: String? = null,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showPersonaSheet by remember { mutableStateOf(false) }
    var showTextInputDialog by remember { mutableStateOf(false) }
    var textInputQuery by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    // Initialize session when screen loads
    LaunchedEffect(bookId) {
        viewModel.startSession(bookId)
    }

    // Auto-scroll transcript when new messages arrive
    LaunchedEffect(uiState.messages.size, uiState.currentStreamingText) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            Surface(
                color = Color.White,
                shadowElevation = 2.dp
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                viewModel.endSession()
                                onNavigateBack()
                            },
                            modifier = Modifier.testTag("voice_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Back",
                                tint = PolishSlate900
                            )
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Bookora Voice Companion",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    ),
                                    color = PolishSlate900
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    color = Color(0xFFE0E7FF),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "LIVE API",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = PolishPrimaryIndigo
                                        ),
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = "Model: gemini-3.1-flash-live-preview • ${formatDuration(uiState.sessionDurationSeconds)}",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = PolishSlate500
                            )
                        }

                        // Persona selection button
                        FilledTonalButton(
                            onClick = { showPersonaSheet = true },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = PolishPrimaryLight,
                                contentColor = PolishPrimaryIndigo
                            ),
                            modifier = Modifier.testTag("voice_select_persona_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.RecordVoiceOver,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = uiState.activePersona.name,
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }

                    HorizontalDivider(color = PolishSlate100)
                }
            }
        },
        containerColor = PolishBackground
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Book Context Banner (if available)
            uiState.bookContext?.let { book ->
                Surface(
                    color = Color.White,
                    modifier = Modifier.fillMaxWidth(),
                    border = BorderStroke(1.dp, PolishSlate100)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(6.dp)),
                            color = PolishPrimaryLight
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.AutoStories,
                                    contentDescription = null,
                                    tint = PolishPrimaryIndigo,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Discussing: ${book.title}",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                ),
                                color = PolishSlate900,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "by ${book.author} • Progress: ${book.readingProgressPercent ?: 0}%",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = PolishSlate500
                            )
                        }
                    }
                }
            }

            // Mode Selector Row
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(VoiceMode.values()) { mode ->
                    val isSelected = mode == uiState.activeMode
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.selectMode(mode) },
                        label = {
                            Text(
                                text = mode.title,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = when (mode) {
                                    VoiceMode.BOOK_DISCUSS -> Icons.Default.Forum
                                    VoiceMode.READING_COACH -> Icons.Default.School
                                    VoiceMode.STORYTELLER -> Icons.Default.TheaterComedy
                                    VoiceMode.DISCOVERY -> Icons.Default.Explore
                                },
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PolishPrimaryIndigo,
                            selectedLabelColor = Color.White,
                            selectedLeadingIconColor = Color.White,
                            containerColor = Color.White,
                            labelColor = PolishSlate700
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = if (isSelected) PolishPrimaryIndigo else PolishSlate200
                        )
                    )
                }
            }

            // Orb Visualizer & Status Area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.42f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    VoiceOrbVisualizer(
                        state = uiState.connectionState,
                        audioLevel = uiState.audioLevel,
                        personaName = uiState.activePersona.name
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Status Pill
                    Surface(
                        color = when (uiState.connectionState) {
                            LiveConnectionState.LISTENING -> Color(0xFFD1FAE5)
                            LiveConnectionState.SPEAKING -> Color(0xFFE0E7FF)
                            LiveConnectionState.THINKING -> Color(0xFFFEF3C7)
                            LiveConnectionState.ERROR -> Color(0xFFFEE2E2)
                            else -> Color(0xFFF1F5F9)
                        },
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(
                            1.dp,
                            when (uiState.connectionState) {
                                LiveConnectionState.LISTENING -> Color(0xFF10B981)
                                LiveConnectionState.SPEAKING -> PolishPrimaryIndigo
                                LiveConnectionState.THINKING -> Color(0xFFF59E0B)
                                LiveConnectionState.ERROR -> Color(0xFFEF4444)
                                else -> PolishSlate300
                            }
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when (uiState.connectionState) {
                                            LiveConnectionState.LISTENING -> Color(0xFF10B981)
                                            LiveConnectionState.SPEAKING -> PolishPrimaryIndigo
                                            LiveConnectionState.THINKING -> Color(0xFFF59E0B)
                                            LiveConnectionState.ERROR -> Color(0xFFEF4444)
                                            else -> PolishSlate400
                                        }
                                    )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = when (uiState.connectionState) {
                                    LiveConnectionState.LISTENING -> "Listening to your voice..."
                                    LiveConnectionState.SPEAKING -> "Gemini Live speaking..."
                                    LiveConnectionState.THINKING -> "Thinking..."
                                    LiveConnectionState.ERROR -> uiState.errorMessage ?: "Connection Error"
                                    LiveConnectionState.CONNECTING -> "Connecting to Live API..."
                                    else -> "Tap Mic to Talk"
                                },
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = when (uiState.connectionState) {
                                        LiveConnectionState.LISTENING -> Color(0xFF065F46)
                                        LiveConnectionState.SPEAKING -> PolishPrimaryIndigo
                                        LiveConnectionState.THINKING -> Color(0xFF92400E)
                                        LiveConnectionState.ERROR -> Color(0xFF991B1B)
                                        else -> PolishSlate700
                                    }
                                )
                            )
                        }
                    }
                }
            }

            // Transcript Scroll Area
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.38f)
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(20.dp),
                color = Color.White,
                border = BorderStroke(1.dp, PolishSlate100)
            ) {
                if (uiState.messages.isEmpty() && uiState.currentStreamingText.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.GraphicEq,
                                contentDescription = null,
                                tint = PolishSlate300,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Live Voice Transcript",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = PolishSlate700
                            )
                            Text(
                                text = "Spoken conversation with Gemini 3.1 Flash Live appears here in real-time.",
                                style = MaterialTheme.typography.bodySmall,
                                color = PolishSlate400,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(uiState.messages) { message ->
                            VoiceMessageBubble(message = message, personaName = uiState.activePersona.name)
                        }

                        // Current live streaming bubble
                        if (uiState.currentStreamingText.isNotEmpty()) {
                            item {
                                VoiceMessageBubble(
                                    message = VoiceChatMessage(
                                        id = "streaming",
                                        sender = VoiceSender.GEMINI_LIVE,
                                        text = uiState.currentStreamingText,
                                        isStreaming = true
                                    ),
                                    personaName = uiState.activePersona.name
                                )
                            }
                        }
                    }
                }
            }

            // Quick Topic Suggestion Prompts
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val suggestions = when (uiState.activeMode) {
                    VoiceMode.BOOK_DISCUSS -> listOf(
                        "Summarize main theme",
                        "Analyze character motivations",
                        "Explain key plot twists",
                        "What is the deeper symbolism?"
                    )
                    VoiceMode.READING_COACH -> listOf(
                        "Quiz me on chapter 1",
                        "Explain difficult vocabulary",
                        "Summarize key takeaways",
                        "How can I apply these lessons?"
                    )
                    VoiceMode.STORYTELLER -> listOf(
                        "Read opening in dramatic voice",
                        "Roleplay the protagonist",
                        "Describe scene atmosphere",
                        "What happens in next climax?"
                    )
                    VoiceMode.DISCOVERY -> listOf(
                        "Recommend books like this",
                        "Suggest top sci-fi masterworks",
                        "Find fast-paced thrillers",
                        "What's trending this week?"
                    )
                }

                items(suggestions) { prompt ->
                    SuggestionChip(
                        onClick = { viewModel.sendTextMessage(prompt) },
                        label = {
                            Text(
                                text = prompt,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp)
                            )
                        },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = Color.White,
                            labelColor = PolishSlate800
                        ),
                        border = SuggestionChipDefaults.suggestionChipBorder(
                            enabled = true,
                            borderColor = PolishSlate200
                        )
                    )
                }
            }

            // Bottom Floating Controls Dock
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(24.dp),
                color = Color.White,
                shadowElevation = 6.dp,
                border = BorderStroke(1.dp, PolishSlate100)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Keyboard Text Query Button
                    IconButton(
                        onClick = { showTextInputDialog = true },
                        modifier = Modifier.testTag("voice_type_text_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Keyboard,
                            contentDescription = "Type text query",
                            tint = PolishSlate700
                        )
                    }

                    // Speaker Mute/Unmute Toggle
                    IconButton(
                        onClick = { viewModel.toggleSpeakerMute() },
                        modifier = Modifier.testTag("voice_speaker_mute_button")
                    ) {
                        Icon(
                            imageVector = if (uiState.isSpeakerMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                            contentDescription = "Speaker audio",
                            tint = if (uiState.isSpeakerMuted) Color(0xFFEF4444) else PolishSlate700
                        )
                    }

                    // Large Center Microphone FAB
                    Surface(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .clickable { viewModel.toggleListening() }
                            .testTag("voice_mic_toggle_button"),
                        color = if (uiState.connectionState == LiveConnectionState.LISTENING) {
                            Color(0xFF10B981)
                        } else {
                            PolishPrimaryIndigo
                        },
                        shadowElevation = 8.dp
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (uiState.connectionState == LiveConnectionState.LISTENING) {
                                    Icons.Default.Mic
                                } else {
                                    Icons.Default.MicNone
                                },
                                contentDescription = "Toggle Microphone",
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }

                    // Interrupt / Stop Speaking Button
                    IconButton(
                        onClick = { viewModel.interruptSpeech() },
                        enabled = uiState.connectionState == LiveConnectionState.SPEAKING,
                        modifier = Modifier.testTag("voice_interrupt_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.StopCircle,
                            contentDescription = "Interrupt speech",
                            tint = if (uiState.connectionState == LiveConnectionState.SPEAKING) PolishPrimaryIndigo else PolishSlate300
                        )
                    }

                    // Clear Transcript / Reset Button
                    IconButton(
                        onClick = { viewModel.clearConversation() },
                        modifier = Modifier.testTag("voice_clear_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Clear transcript",
                            tint = PolishSlate700
                        )
                    }
                }
            }
        }
    }

    // Persona Selector Sheet
    if (showPersonaSheet) {
        VoicePersonaSelectorSheet(
            selectedPersona = uiState.activePersona,
            onSelectPersona = { newPersona ->
                viewModel.selectPersona(newPersona)
            },
            onDismiss = { showPersonaSheet = false }
        )
    }

    // Text Input Fallback Dialog
    if (showTextInputDialog) {
        AlertDialog(
            onDismissRequest = { showTextInputDialog = false },
            title = {
                Text(
                    text = "Type Message to Gemini Live",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                OutlinedTextField(
                    value = textInputQuery,
                    onValueChange = { textInputQuery = it },
                    placeholder = { Text("Ask anything about the book...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("voice_text_input_field"),
                    shape = RoundedCornerShape(12.dp),
                    maxLines = 3
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (textInputQuery.isNotBlank()) {
                            viewModel.sendTextMessage(textInputQuery)
                            textInputQuery = ""
                            showTextInputDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PolishPrimaryIndigo),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("voice_send_text_button")
                ) {
                    Text("Send")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTextInputDialog = false }) {
                    Text("Cancel")
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(20.dp)
        )
    }
}

@Composable
fun VoiceMessageBubble(
    message: VoiceChatMessage,
    personaName: String
) {
    val isUser = message.sender == VoiceSender.USER

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isUser) {
            Surface(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape),
                color = PolishPrimaryLight
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.AutoStories,
                        contentDescription = null,
                        tint = PolishPrimaryIndigo,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        Surface(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isUser) 16.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 16.dp
            ),
            color = if (isUser) PolishPrimaryIndigo else Color(0xFFF1F5F9),
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                if (!isUser) {
                    Text(
                        text = "$personaName (Live)",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = PolishPrimaryIndigo,
                            fontSize = 10.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                }
                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                    color = if (isUser) Color.White else PolishSlate800
                )
            }
        }

        if (isUser) {
            Spacer(modifier = Modifier.width(8.dp))
            Surface(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape),
                color = PolishSlate200
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = PolishSlate700,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

fun formatDuration(seconds: Long): String {
    val m = seconds / 60
    val s = seconds % 60
    return String.format("%02d:%02d", m, s)
}
