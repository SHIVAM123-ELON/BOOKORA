package com.example.presentation.components.ai

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Style
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.model.StudyDeck
import com.example.domain.model.StudyQuestion
import com.example.domain.model.StudyQuestionType
import com.example.domain.model.UiState
import com.example.presentation.viewmodel.StudyModeViewModel
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
fun AiStudyModeBottomSheet(
    userId: String,
    bookId: String,
    bookTitle: String,
    chapterTitle: String,
    chapterContent: String,
    viewModel: StudyModeViewModel,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val deckState by viewModel.deckState.collectAsStateWithLifecycle()
    val activeTab by viewModel.activeTab.collectAsStateWithLifecycle()

    val currentQIndex by viewModel.currentQuestionIndex.collectAsStateWithLifecycle()
    val userAnswers by viewModel.userAnswers.collectAsStateWithLifecycle()
    val isSubmitted by viewModel.isAnswerSubmitted.collectAsStateWithLifecycle()
    val score by viewModel.score.collectAsStateWithLifecycle()

    val currentFlashcardIndex by viewModel.currentFlashcardIndex.collectAsStateWithLifecycle()
    val isFlashcardFlipped by viewModel.isFlashcardFlipped.collectAsStateWithLifecycle()

    LaunchedEffect(bookId, chapterTitle) {
        viewModel.loadDeck(userId, bookId, chapterTitle, chapterContent)
    }

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
                .padding(bottom = 16.dp)
        ) {
            // Top App Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp),
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
                                imageVector = Icons.Default.School,
                                contentDescription = null,
                                tint = PolishPrimaryIndigo,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Column {
                        Text(
                            text = "AI Study Mode",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = PolishSlate900
                        )
                        Text(
                            text = chapterTitle,
                            style = MaterialTheme.typography.labelSmall,
                            color = PolishSlate500,
                            maxLines = 1
                        )
                    }
                }

                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = PolishSlate500)
                }
            }

            // Study Tabs
            PrimaryTabRow(
                selectedTabIndex = activeTab,
                containerColor = Color.White,
                contentColor = PolishPrimaryIndigo,
                divider = { Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(PolishSlate100)) }
            ) {
                Tab(
                    selected = activeTab == 0,
                    onClick = { viewModel.setActiveTab(0) },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(imageVector = Icons.Default.Quiz, contentDescription = null, modifier = Modifier.size(16.dp))
                            Text("Quiz", fontWeight = if (activeTab == 0) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                )
                Tab(
                    selected = activeTab == 1,
                    onClick = { viewModel.setActiveTab(1) },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(imageVector = Icons.Default.Style, contentDescription = null, modifier = Modifier.size(16.dp))
                            Text("Flashcards", fontWeight = if (activeTab == 1) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                )
                Tab(
                    selected = activeTab == 2,
                    onClick = { viewModel.setActiveTab(2) },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(imageVector = Icons.Default.Lightbulb, contentDescription = null, modifier = Modifier.size(16.dp))
                            Text("Revision Notes", fontWeight = if (activeTab == 2) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            when (val state = deckState) {
                is UiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = PolishPrimaryIndigo)
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "Generating Interactive Study Deck...",
                                style = MaterialTheme.typography.bodySmall,
                                color = PolishSlate500
                            )
                        }
                    }
                }
                is UiState.Error -> {
                    Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "Failed to create study deck", style = MaterialTheme.typography.titleMedium, color = PolishSlate900)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = state.message, style = MaterialTheme.typography.bodySmall, color = PolishSlate500)
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(onClick = { viewModel.loadDeck(userId, bookId, chapterTitle, chapterContent) }) {
                                Text("Retry")
                            }
                        }
                    }
                }
                is UiState.Empty -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(text = "No questions generated.", color = PolishSlate500)
                    }
                }
                is UiState.Success -> {
                    val deck = state.data

                    when (activeTab) {
                        0 -> StudyQuizTab(
                            deck = deck,
                            currentIndex = currentQIndex,
                            userAnswers = userAnswers,
                            isSubmitted = isSubmitted,
                            score = score,
                            onSelectOption = { qIdx, optIdx -> viewModel.selectOption(qIdx, optIdx) },
                            onSubmit = { viewModel.submitAnswer(deck) },
                            onNext = { viewModel.nextQuestion(deck) },
                            onReset = { viewModel.resetQuiz() }
                        )
                        1 -> StudyFlashcardsTab(
                            deck = deck,
                            currentIndex = currentFlashcardIndex,
                            isFlipped = isFlashcardFlipped,
                            onFlip = { viewModel.flipCard() },
                            onNext = { viewModel.nextFlashcard(deck) },
                            onPrev = { viewModel.prevFlashcard() }
                        )
                        2 -> StudyRevisionNotesTab(notes = deck.revisionNotes)
                    }
                }
            }
        }
    }
}

@Composable
fun StudyQuizTab(
    deck: StudyDeck,
    currentIndex: Int,
    userAnswers: Map<Int, Int>,
    isSubmitted: Boolean,
    score: Int,
    onSelectOption: (Int, Int) -> Unit,
    onSubmit: () -> Unit,
    onNext: () -> Unit,
    onReset: () -> Unit
) {
    val totalQuestions = deck.questions.size
    val isFinished = currentIndex >= totalQuestions - 1 && isSubmitted

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Progress & Score bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = PolishPrimaryLight,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "Question ${currentIndex + 1} of $totalQuestions",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = PolishPrimaryIndigo),
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }

            Surface(
                color = Color(0xFFFEF3C7),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "Score: $score / $totalQuestions",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = PolishAccentOrange),
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        val question = deck.questions.getOrNull(currentIndex)
        if (question != null) {
            Text(
                text = question.question,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, lineHeight = 24.sp),
                color = PolishSlate900
            )

            Spacer(modifier = Modifier.height(16.dp))

            val selectedOption = userAnswers[currentIndex]

            if (question.type == StudyQuestionType.SHORT_ANSWER) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = PolishBackground),
                    border = BorderStroke(1.dp, PolishSlate200)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Model Explanation / Answer:",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = PolishPrimaryIndigo
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = question.explanation,
                            style = MaterialTheme.typography.bodyMedium.copy(color = PolishSlate900, lineHeight = 20.sp)
                        )
                        if (question.memoryTip.isNotBlank()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFFEF3C7)
                            ) {
                                Text(
                                    text = "💡 Tip: ${question.memoryTip}",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold, color = PolishAccentOrange),
                                    modifier = Modifier.padding(8.dp)
                                )
                            }
                        }
                    }
                }
            } else {
                question.options.forEachIndexed { optIndex, optionText ->
                    val isOptionSelected = selectedOption == optIndex
                    val isCorrect = optIndex == question.correctAnswerIndex

                    val (containerColor, borderColor, textColor) = when {
                        isSubmitted && isCorrect -> Triple(Color(0xFFDCFCE7), Color(0xFF10B981), Color(0xFF166534))
                        isSubmitted && isOptionSelected && !isCorrect -> Triple(Color(0xFFFEE2E2), Color(0xFFEF4444), Color(0xFF991B1B))
                        isOptionSelected -> Triple(PolishPrimaryLight, PolishPrimaryIndigo, PolishPrimaryIndigo)
                        else -> Triple(PolishBackground, PolishSlate200, PolishSlate900)
                    }

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = containerColor,
                        border = BorderStroke(1.dp, borderColor),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .clickable(enabled = !isSubmitted) {
                                onSelectOption(currentIndex, optIndex)
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = if (isOptionSelected) PolishPrimaryIndigo else PolishSlate100,
                                modifier = Modifier.size(24.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = ('A'.code + optIndex).toChar().toString(),
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = if (isOptionSelected) Color.White else PolishSlate500
                                        )
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Text(
                                text = optionText,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = if (isOptionSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = textColor
                                ),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Explanation card if submitted
                if (isSubmitted) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = PolishBackground),
                        border = BorderStroke(1.dp, PolishSlate200)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "Explanation",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = PolishPrimaryIndigo)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = question.explanation,
                                style = MaterialTheme.typography.bodySmall.copy(color = PolishSlate700, lineHeight = 18.sp)
                            )
                            if (question.memoryTip.isNotBlank()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "💡 Tip: ${question.memoryTip}",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold, color = PolishAccentOrange)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Action Buttons
            if (!isSubmitted && question.type != StudyQuestionType.SHORT_ANSWER) {
                Button(
                    onClick = onSubmit,
                    enabled = selectedOption != null,
                    colors = ButtonDefaults.buttonColors(containerColor = PolishPrimaryIndigo),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("submit_quiz_answer_btn")
                ) {
                    Text("Check Answer", fontWeight = FontWeight.Bold)
                }
            } else if (currentIndex < totalQuestions - 1) {
                Button(
                    onClick = onNext,
                    colors = ButtonDefaults.buttonColors(containerColor = PolishPrimaryIndigo),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("next_quiz_question_btn")
                ) {
                    Text("Next Question", fontWeight = FontWeight.Bold)
                }
            } else {
                Button(
                    onClick = onReset,
                    colors = ButtonDefaults.buttonColors(containerColor = PolishPrimaryIndigo),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Retake Quiz", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun StudyFlashcardsTab(
    deck: StudyDeck,
    currentIndex: Int,
    isFlipped: Boolean,
    onFlip: () -> Unit,
    onNext: () -> Unit,
    onPrev: () -> Unit
) {
    val totalCards = deck.flashcards.size
    val currentCard = deck.flashcards.getOrNull(currentIndex)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Flashcard ${currentIndex + 1} of $totalCards",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = PolishSlate500
            )

            Text(
                text = "Tap card to flip",
                style = MaterialTheme.typography.labelSmall,
                color = PolishPrimaryIndigo
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (currentCard != null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .clickable { onFlip() }
                    .testTag("flashcard_card"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = if (isFlipped) PolishPrimaryLight else PolishBackground),
                border = BorderStroke(1.5.dp, if (isFlipped) PolishPrimaryIndigo else PolishSlate200),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isFlipped) PolishPrimaryIndigo else PolishSlate200
                        ) {
                            Text(
                                text = if (isFlipped) "DEFINITION & EXPLANATION" else "KEY CONCEPT",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (isFlipped) Color.White else PolishSlate700
                                ),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = if (isFlipped) currentCard.back else currentCard.front,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                lineHeight = 24.sp
                            ),
                            color = PolishSlate900
                        )

                        if (!isFlipped && currentCard.keyConcept.isNotBlank()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Topic: ${currentCard.keyConcept}",
                                style = MaterialTheme.typography.labelSmall,
                                color = PolishSlate500
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Navigation controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedButton(
                onClick = onPrev,
                enabled = currentIndex > 0,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous")
                Spacer(modifier = Modifier.width(6.dp))
                Text("Previous")
            }

            Button(
                onClick = onNext,
                enabled = currentIndex < totalCards - 1,
                colors = ButtonDefaults.buttonColors(containerColor = PolishPrimaryIndigo),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Next Card")
                Spacer(modifier = Modifier.width(6.dp))
                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next")
            }
        }
    }
}

@Composable
fun StudyRevisionNotesTab(notes: List<String>) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "High-Yield Summary Notes",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = PolishSlate900
        )
        Spacer(modifier = Modifier.height(12.dp))

        notes.forEachIndexed { index, note ->
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = PolishBackground,
                border = BorderStroke(1.dp, PolishSlate200),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 5.dp)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Surface(
                        shape = CircleShape,
                        color = PolishPrimaryLight,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "${index + 1}",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = PolishPrimaryIndigo
                                )
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = note,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = PolishSlate900,
                            lineHeight = 22.sp
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
