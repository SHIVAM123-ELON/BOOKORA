package com.example.presentation.components.ai

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.model.AuthorAiPromptType
import com.example.domain.model.UiState
import com.example.presentation.viewmodel.AuthorAiAssistantViewModel
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
fun AuthorAiAssistantDialog(
    userId: String,
    initialTitle: String,
    viewModel: AuthorAiAssistantViewModel,
    onApplySuggestion: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val selectedType by viewModel.selectedType.collectAsStateWithLifecycle()
    val aiResultState by viewModel.aiResultState.collectAsStateWithLifecycle()
    val clipboardManager = LocalClipboardManager.current

    var bookTitle by remember { mutableStateOf(initialTitle) }
    var contextNotes by remember { mutableStateOf("") }
    var targetAudience by remember { mutableStateOf("") }
    var copiedIndex by remember { mutableStateOf<Int?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f)
                .padding(horizontal = 20.dp, vertical = 6.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
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
                            text = "Author AI Studio Assistant",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = PolishSlate900
                        )
                        Text(
                            text = "Generate descriptions, subtitles, tags & promotional copy",
                            style = MaterialTheme.typography.labelSmall,
                            color = PolishSlate500
                        )
                    }
                }

                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = PolishSlate500)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Prompt Type Chips
            Text(
                text = "Select Task",
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
                AuthorAiPromptType.values().forEach { type ->
                    FilterChip(
                        selected = selectedType == type,
                        onClick = { viewModel.selectType(type) },
                        label = { Text(type.label, style = MaterialTheme.typography.labelSmall) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PolishPrimaryIndigo,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Inputs
            OutlinedTextField(
                value = bookTitle,
                onValueChange = { bookTitle = it },
                label = { Text("Book Title / Topic") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("author_ai_title_input"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PolishPrimaryIndigo,
                    unfocusedBorderColor = PolishSlate200
                ),
                shape = RoundedCornerShape(14.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = contextNotes,
                onValueChange = { contextNotes = it },
                label = { Text("Key Themes / Manuscript Outline") },
                placeholder = { Text("e.g. Clean architecture, unidirectional data flow, real production examples...") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PolishPrimaryIndigo,
                    unfocusedBorderColor = PolishSlate200
                ),
                shape = RoundedCornerShape(14.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = targetAudience,
                onValueChange = { targetAudience = it },
                label = { Text("Target Audience (Optional)") },
                placeholder = { Text("e.g. Senior software engineers, students, beginners...") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PolishPrimaryIndigo,
                    unfocusedBorderColor = PolishSlate200
                ),
                shape = RoundedCornerShape(14.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    viewModel.generate(
                        userId = userId,
                        title = bookTitle.ifBlank { "Untitled Manuscript" },
                        context = contextNotes,
                        audience = targetAudience.ifBlank { null }
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("generate_author_ai_btn"),
                colors = ButtonDefaults.buttonColors(containerColor = PolishPrimaryIndigo),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Generate Suggestions", fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Results Section
            when (val state = aiResultState) {
                is UiState.Loading -> {
                    Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = PolishPrimaryIndigo)
                    }
                }
                is UiState.Error -> {
                    Surface(
                        color = Color(0xFFFEE2E2),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = state.message,
                            style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFDC2626)),
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
                is UiState.Success -> {
                    val result = state.data
                    Text(
                        text = "Generated Suggestions (${result.suggestions.size})",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = PolishSlate900
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    result.suggestions.forEachIndexed { index, suggestion ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 5.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = PolishBackground),
                            border = BorderStroke(1.dp, PolishSlate200)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = suggestion,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = PolishSlate900,
                                        lineHeight = 22.sp
                                    )
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(
                                        onClick = {
                                            clipboardManager.setText(AnnotatedString(suggestion))
                                            copiedIndex = index
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ContentCopy,
                                            contentDescription = "Copy",
                                            tint = if (copiedIndex == index) PolishAccentOrange else PolishSlate500,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(6.dp))

                                    Button(
                                        onClick = {
                                            onApplySuggestion(suggestion)
                                            onDismiss()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = PolishPrimaryIndigo),
                                        shape = RoundedCornerShape(10.dp),
                                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Text("Apply to Book", fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }
                null -> {}
                is UiState.Empty -> {}
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
